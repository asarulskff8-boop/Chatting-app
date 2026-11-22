package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import com.example.data.firebase.AuthManager
import com.example.ui.components.AppleBrandLogo
import com.example.ui.components.ChatSparkEmblem
import com.example.ui.components.GoogleBrandLogo
import com.example.ui.components.OrbitalPortalBackground
import com.example.ui.theme.InputFocusedSurface
import com.example.ui.theme.InputRestSurface
import com.example.ui.theme.LightOutline
import com.example.ui.theme.LightSurface
import com.example.ui.theme.SkyBright
import com.example.ui.theme.SkyDeep
import com.example.ui.theme.SkyMist
import com.example.ui.theme.SkyPrimary
import com.example.util.BiometricHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    initialEmail: String,
    keepSignedIn: Boolean,
    onKeepSignedInChange: (Boolean) -> Unit,
    onEmailRemembered: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showHelpSheet by remember { mutableStateOf(false) }
    var showForgotSheet by remember { mutableStateOf(false) }
    var showCreateAccountSheet by remember { mutableStateOf(false) }
    var showAppleInfoSheet by remember { mutableStateOf(false) }
    var revealHeading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        revealHeading = true
    }

    fun launchRealGoogleSignIn() {
        errorMessage = null
        isLoading = true
        if (email.isNotBlank()) onEmailRemembered(email.trim())
        AuthManager.onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
                errorMessage = "Google Sign-In was cancelled. Select an account to continue."
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Top orbital purple hero backdrop
        OrbitalPortalBackground(
            modifier = Modifier.fillMaxSize(),
            centerYFraction = 0.25f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Header Row: Brand logo + Need Help pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChatSparkEmblem(size = 34.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Chatting App",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Surface(
                    onClick = { showHelpSheet = true },
                    shape = CircleShape,
                    color = SkyMist,
                    border = BorderStroke(1.dp, SkyPrimary.copy(alpha = 0.28f)),
                    modifier = Modifier.testTag("need_help_button")
                ) {
                    Text(
                        text = "Need help?",
                        style = MaterialTheme.typography.labelLarge,
                        color = SkyDeep,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Clean, balanced hero logo + animated greeting (wrap-height prevents clipping)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ChatSparkEmblem(size = 76.dp)
                Spacer(modifier = Modifier.height(14.dp))
                AnimatedVisibility(
                    visible = revealHeading,
                    enter = fadeIn() + slideInVertically { it / 3 }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Hello again.",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pick up right where you left off.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Soft Cool Slate elevated sheet with crisp border
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = LightSurface,
                border = BorderStroke(1.2.dp, LightOutline),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 22.dp)
                    ) {
                        // Email Label + Input
                        Text(
                            text = "Email",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("hello@chattingapp.io") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyPrimary,
                                unfocusedBorderColor = LightOutline,
                                focusedContainerColor = InputFocusedSurface,
                                unfocusedContainerColor = InputRestSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Label + Input with Show/Hide toggle
                        Text(
                            text = "Password",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("••••••••••••") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { showPassword = !showPassword }) {
                                    Text(
                                        text = if (showPassword) "Hide" else "Show",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SkyPrimary,
                                unfocusedBorderColor = LightOutline,
                                focusedContainerColor = InputFocusedSurface,
                                unfocusedContainerColor = InputRestSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Keep me signed in + Forgot?
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable { onKeepSignedInChange(!keepSignedIn) }
                            ) {
                                Checkbox(
                                    checked = keepSignedIn,
                                    onCheckedChange = onKeepSignedInChange,
                                    colors = CheckboxDefaults.colors(checkedColor = SkyPrimary)
                                )
                                Text(
                                    text = "Keep me signed in",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(
                                onClick = { showForgotSheet = true },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = "Forgot?",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interactive Slide-to-Sign-In button
                        SlideToSignInButton(
                            isLoading = isLoading,
                            onCompleted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                launchRealGoogleSignIn()
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Google | Apple | Biometric quick auth buttons with authentic vector logos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickAuthPill(
                                label = "Google",
                                provider = QuickAuthProvider.GOOGLE,
                                onClick = { launchRealGoogleSignIn() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("google_sign_in_button")
                            )
                            QuickAuthPill(
                                label = "Apple",
                                provider = QuickAuthProvider.APPLE,
                                onClick = { showAppleInfoSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("apple_sign_in_button")
                            )
                            QuickAuthPill(
                                label = "Biometric",
                                provider = QuickAuthProvider.BIOMETRIC,
                                onClick = {
                                    val activity = context as? FragmentActivity
                                    if (activity == null) {
                                        errorMessage = "Biometric prompt unavailable in current host."
                                    } else {
                                        BiometricHelper.authenticate(
                                            activity = activity,
                                            title = "Sign in with Biometrics",
                                            subtitle = "Verify identity to continue with Google Account",
                                            onSuccess = { launchRealGoogleSignIn() },
                                            onError = { msg -> errorMessage = msg }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("biometric_sign_in_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom Create account link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "New to Chatting App? ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Create account",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showCreateAccountSheet = true }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("create_account_button")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelpSheet) {
        ModalBottomSheet(onDismissRequest = { showHelpSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sign-In Assistance", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Chatting App secures every account with verified Google Identity via Android Credential Manager. Drag the sky-blue slider or tap Google to sign in securely without sharing raw passwords.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        showHelpSheet = false
                        launchRealGoogleSignIn()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue with Google Sign-In")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showForgotSheet) {
        ModalBottomSheet(onDismissRequest = { showForgotSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Account Recovery", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Your Chatting App identity is linked to your Google account. You can sign in directly with Google Credential Manager or manage recovery via Google Account Recovery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        showForgotSheet = false
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://accounts.google.com/signin/recovery")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Google Account Recovery")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAppleInfoSheet) {
        ModalBottomSheet(onDismissRequest = { showAppleInfoSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Apple Sign-In on Android", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Apple ID federated OAuth requires Apple Developer Services configuration in Firebase Console. On Android, we recommend signing in immediately with your Google Account via Credential Manager.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        showAppleInfoSheet = false
                        launchRealGoogleSignIn()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in with Google")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCreateAccountSheet) {
        ModalBottomSheet(onDismissRequest = { showCreateAccountSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Create Your Chatting App Account", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Step 1: Verify your identity securely with Google.\nStep 2: Customize your @username, phone verification, avatar, and status.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        showCreateAccountSheet = false
                        launchRealGoogleSignIn()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Verify with Google & Set Up Profile")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SlideToSignInButton(
    isLoading: Boolean,
    onCompleted: () -> Unit
) {
    val thumbSize = 52.dp
    val trackHeight = 62.dp
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(SkyDeep, SkyPrimary, SkyBright)
                )
            )
            .testTag("slide_to_sign_in_track")
            .clickable(enabled = !isLoading) { onCompleted() },
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val maxOffsetPx = with(density) { (maxWidth - thumbSize - 10.dp).toPx().coerceAtLeast(1f) }
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }

        // Center prompt text
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Connecting...",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Text(
                    text = "Slide to sign in",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Right-side chevrons
        if (!isLoading) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp),
                horizontalArrangement = Arrangement.spacedBy((-8).dp)
            ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        // Draggable crisp white thumb with sky-blue arrow
        Box(
            modifier = Modifier
                .padding(start = 5.dp)
                .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color.White, SkyMist))
                )
                .pointerInput(isLoading, maxOffsetPx) {
                    if (isLoading) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetPx >= maxOffsetPx * 0.68f) {
                                dragOffsetPx = maxOffsetPx
                                onCompleted()
                            }
                            scope.launch { dragOffsetPx = 0f }
                        },
                        onDragCancel = { dragOffsetPx = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxOffsetPx)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Slide to sign in with Google",
                tint = SkyDeep,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private enum class QuickAuthProvider {
    GOOGLE,
    APPLE,
    BIOMETRIC
}

@Composable
private fun QuickAuthPill(
    label: String,
    provider: QuickAuthProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = InputRestSurface,
        border = BorderStroke(1.2.dp, LightOutline),
        shadowElevation = 1.dp,
        modifier = modifier.height(50.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (provider) {
                QuickAuthProvider.GOOGLE -> GoogleBrandLogo(modifier = Modifier.size(18.dp))
                QuickAuthProvider.APPLE -> AppleBrandLogo(
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                QuickAuthProvider.BIOMETRIC -> Icon(
                    imageVector = Icons.Rounded.Fingerprint,
                    contentDescription = null,
                    tint = SkyPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
