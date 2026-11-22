package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.ui.components.OrbitalPortalBackground
import com.example.ui.theme.InputFocusedSurface
import com.example.ui.theme.InputRestSurface
import com.example.ui.theme.LightOutline
import com.example.ui.theme.LightSurface
import com.example.ui.theme.SkyBright
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkySoft
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun ProfileSetupScreen(
    isBusy: Boolean,
    onSaveProfile: (
        displayName: String,
        username: String,
        bio: String,
        statusText: String,
        phoneNumber: String,
        phoneVerified: Boolean,
        avatarUri: Uri?,
        onDone: () -> Unit
    ) -> Unit,
    onReadyFinished: () -> Unit
) {
    val context = LocalContext.current
    val firebaseUser = Firebase.auth.currentUser
    val googleName = firebaseUser?.displayName.orEmpty()
    val googleEmail = firebaseUser?.email.orEmpty()
    val googlePhone = firebaseUser?.phoneNumber.orEmpty()
    val googlePhoto = firebaseUser?.photoUrl?.toString().orEmpty()
    val defaultUsername = remember(googleEmail, googleName) {
        val base = googleEmail.substringBefore("@").ifBlank { googleName }
            .lowercase().replace(Regex("[^a-z0-9_]"), "")
        if (base.length >= 3) base.take(20) else "user_${firebaseUser?.uid?.take(5).orEmpty().lowercase()}"
    }

    var step by remember { mutableIntStateOf(0) } // 0: Identity, 1: Contact & Bio, 2: Ready!
    var displayName by remember { mutableStateOf(googleName.ifBlank { "New Member" }) }
    var username by remember { mutableStateOf(defaultUsername) }
    var bio by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Connect Beyond Messages.") }
    var phoneNumber by remember { mutableStateOf(googlePhone) }
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) selectedAvatarUri = uri
    }

    val statusPresets = listOf(
        "Available",
        "Connect Beyond Messages.",
        "Focusing",
        "At Work",
        "Up for a call"
    )

    if (step == 2) {
        AccountReadyCelebrationScreen(onContinue = onReadyFinished)
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitalPortalBackground(modifier = Modifier.fillMaxSize(), centerYFraction = 0.16f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    IconButton(onClick = { step -= 1 }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text = "Complete Your Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Step ${step + 1} of 2 • Account Setup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (step + 1) / 2f },
                color = SkyPrimary,
                trackColor = SkySoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(6.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = LightSurface,
                border = BorderStroke(1.2.dp, LightOutline)
            ) {
                val setupFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputFocusedSurface,
                    unfocusedContainerColor = InputRestSurface,
                    focusedBorderColor = SkyPrimary,
                    unfocusedBorderColor = LightOutline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    AnimatedContent(targetState = step, label = "setupStep") { currentStep ->
                        Column(
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .padding(24.dp)
                        ) {
                            if (currentStep == 0) {
                                // Step 1: Avatar + Full Name + Username + Google Email
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .size(104.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(listOf(SkyPrimary, SkyBright))
                                        )
                                        .clickable {
                                            photoPicker.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                        .testTag("pick_avatar_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        selectedAvatarUri != null -> {
                                            AsyncImage(
                                                model = selectedAvatarUri,
                                                contentDescription = "Selected avatar",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        googlePhoto.isNotBlank() -> {
                                            AsyncImage(
                                                model = googlePhoto,
                                                contentDescription = "Google avatar",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                Icons.Rounded.CameraAlt,
                                                contentDescription = "Add profile photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tap to choose profile photo",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("Full Name") },
                                    leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = setupFieldColors,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_name_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it.lowercase().replace(Regex("[^a-z0-9_]"), "") },
                                    label = { Text("Unique Username") },
                                    prefix = { Text("@") },
                                    leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                                    supportingText = { Text("3-30 characters, letters, numbers & underscore") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = setupFieldColors,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_username_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = googleEmail,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Verified Google Email") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = setupFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (validationError != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = validationError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        val cleanUser = username.trim()
                                        if (displayName.trim().isEmpty()) {
                                            validationError = "Please enter your full name."
                                        } else if (cleanUser.length < 3) {
                                            validationError = "Username must be at least 3 characters."
                                        } else {
                                            validationError = null
                                            step = 1
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("setup_next_button")
                                ) {
                                    Text("Continue", style = MaterialTheme.typography.titleMedium)
                                }
                            } else {
                                // Step 2: Phone Number + Short Bio + Status
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("Phone Number (Optional)") },
                                    placeholder = { Text("+1 555 0199") },
                                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = setupFieldColors,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_phone_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { bio = it.take(240) },
                                    label = { Text("Short Bio") },
                                    placeholder = { Text("Tell friends a bit about yourself...") },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = setupFieldColors,
                                    modifier = Modifier.fillMaxWidth().testTag("setup_bio_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = statusText,
                                    onValueChange = { statusText = it.take(60) },
                                    label = { Text("Profile Status") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = setupFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    statusPresets.take(3).forEach { preset ->
                                        Surface(
                                            onClick = { statusText = preset },
                                            shape = CircleShape,
                                            color = if (statusText == preset) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = preset,
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Your unique User ID & personal QR code will be generated automatically.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    enabled = !isBusy,
                                    onClick = {
                                        onSaveProfile(
                                            displayName,
                                            username,
                                            bio,
                                            statusText,
                                            phoneNumber,
                                            googlePhone.isNotBlank() && phoneNumber.trim() == googlePhone,
                                            selectedAvatarUri
                                        ) {
                                            step = 2
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("setup_finish_button")
                                ) {
                                    if (isBusy) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Complete Account Setup", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountReadyCelebrationScreen(onContinue: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(2200)
        onContinue()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OrbitalPortalBackground(modifier = Modifier.fillMaxSize(), centerYFraction = 0.45f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = SkyPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(104.dp)
                    .scale(scale.value)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Account Ready",
                        tint = Color.White,
                        modifier = Modifier.size(58.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Your Account Is Ready!",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Welcome to Chatting App. Connect Beyond Messages.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onContinue,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Enter Dashboard", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    }
}
