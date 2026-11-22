package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.firebase.AuthManager
import com.example.data.firebase.ChatRepository
import com.example.data.firebase.SocialRepository
import com.example.data.model.UiState
import com.example.data.prefs.AppPreferences
import com.example.data.prefs.UserPreferences
import com.example.ui.components.ChatSparkEmblem
import com.example.ui.components.OrbitalPortalBackground
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.ChattingAppTheme
import com.example.ui.theme.SkyPrimary
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BackgroundSyncWorker
import com.example.util.BiometricHelper
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannels(this)
        BackgroundSyncWorker.schedule(this)

        val appPrefs = AppPreferences(applicationContext)

        setContent {
            val userPrefs by appPrefs.preferencesFlow.collectAsStateWithLifecycle(initialValue = UserPreferences())
            ChattingAppTheme(themeMode = userPrefs.themeMode) {
                ChattingAppRoot(appPrefs = appPrefs, userPrefs = userPrefs)
            }
        }
    }
}

@Composable
private fun ChattingAppRoot(
    appPrefs: AppPreferences,
    userPrefs: UserPreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val authUser by AuthManager.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var autoSignInAttempted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(showSplash, userPrefs.keepMeSignedIn) {
        if (!showSplash && !autoSignInAttempted && Firebase.auth.currentUser == null && userPrefs.keepMeSignedIn) {
            autoSignInAttempted = true
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val currentUser = authUser
    if (currentUser == null) {
        AuthScreen(
            initialEmail = userPrefs.lastSignedInEmail,
            keepSignedIn = userPrefs.keepMeSignedIn,
            onKeepSignedInChange = { enabled ->
                scope.launch { appPrefs.setKeepMeSignedIn(enabled) }
            },
            onEmailRemembered = { email ->
                scope.launch { appPrefs.setLastSignedInEmail(email) }
            },
            onAuthSuccess = {
                Firebase.auth.currentUser?.email?.let { signedEmail ->
                    scope.launch { appPrefs.setLastSignedInEmail(signedEmail) }
                }
            }
        )
        return
    }

    // Re-scope repositories and ViewModel whenever the authenticated UID changes
    key(currentUser.uid) {
        val dbId = remember(context) { context.getString(R.string.firestore_database_id) }
        val viewModel: MainViewModel = viewModel(
            key = "main_vm_${currentUser.uid}",
            factory = remember(currentUser.uid, dbId) {
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(
                            chatRepo = ChatRepository(context),
                            socialRepo = SocialRepository(context),
                            currentUserId = currentUser.uid
                        ) as T
                    }
                }
            }
        )

        val profileState by viewModel.myProfileState.collectAsStateWithLifecycle()
        val privateInfoState by viewModel.privateInfoState.collectAsStateWithLifecycle()
        val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

        var setupCompleteGate by rememberSaveable(currentUser.uid) { mutableStateOf(false) }
        var unlockedThisSession by rememberSaveable(currentUser.uid) { mutableStateOf(false) }

        val activity = context as? FragmentActivity
        val privateInfo = (privateInfoState as? UiState.Success)?.data
        val requiresBiometric = (privateInfo?.biometricEnabled == true) &&
            activity != null &&
            BiometricHelper.isBiometricAvailable(activity) &&
            !unlockedThisSession

        when (val pState = profileState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    OrbitalPortalBackground(modifier = Modifier.fillMaxSize())
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ChatSparkEmblem(size = 84.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(color = SkyPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Syncing your encrypted workspace...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is UiState.Error -> {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Unable to load profile", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            pState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            AuthManager.signOut(credentialManager, scope)
                        }) {
                            Text("Sign Out & Retry")
                        }
                    }
                }
            }

            is UiState.Success -> {
                val profile = pState.data
                if (profile == null || !profile.profileCompleted || setupCompleteGate) {
                    ProfileSetupScreen(
                        isBusy = isBusy,
                        onSaveProfile = { displayName, username, bio, statusText, phone, verified, avatarUri, onDone ->
                            setupCompleteGate = true
                            viewModel.completeProfileSetup(
                                context = context,
                                displayName = displayName,
                                username = username,
                                bio = bio,
                                statusText = statusText,
                                phoneNumber = phone,
                                phoneVerified = verified,
                                avatarUri = avatarUri,
                                onDone = onDone
                            )
                        },
                        onReadyFinished = {
                            setupCompleteGate = false
                        }
                    )
                } else if (requiresBiometric) {
                    val activity = context as? FragmentActivity
                    LaunchedEffect(Unit) {
                        if (activity != null) {
                            BiometricHelper.authenticate(
                                activity = activity,
                                title = "Unlock Chatting App",
                                subtitle = "Verify your identity to access messages",
                                onSuccess = { unlockedThisSession = true },
                                onError = { viewModel.emitFeedback(it) }
                            )
                        }
                    }
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = "Locked",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Chatting App is Locked", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Use fingerprint or device biometrics to unlock your chats.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        BiometricHelper.authenticate(
                                            activity = activity,
                                            title = "Unlock Chatting App",
                                            subtitle = "Verify your identity to access messages",
                                            onSuccess = { unlockedThisSession = true },
                                            onError = { viewModel.emitFeedback(it) }
                                        )
                                    } else {
                                        unlockedThisSession = true
                                    }
                                },
                                modifier = Modifier.testTag("unlock_biometric_button")
                            ) {
                                Text("Unlock Now")
                            }
                        }
                    }
                } else {
                    HomeDashboardScreen(
                        viewModel = viewModel,
                        myProfile = profile,
                        userPrefs = userPrefs,
                        onSetThemeMode = { mode -> scope.launch { appPrefs.setThemeMode(mode) } },
                        onSetLanguage = { lang -> scope.launch { appPrefs.setLanguageCode(lang) } },
                        onSetWallpaperIndex = { idx -> scope.launch { appPrefs.setChatWallpaperIndex(idx) } },
                        onLogout = { AuthManager.signOut(credentialManager, scope) },
                        onAccountDeleted = { AuthManager.signOut(credentialManager, scope) }
                    )
                }
            }
        }
    }
}
