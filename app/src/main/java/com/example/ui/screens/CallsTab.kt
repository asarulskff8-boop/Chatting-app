package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.CallSession
import com.example.data.model.MediaFile
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OrbitalPortalBackground
import com.example.ui.components.SkeletonConversationList
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatCallDuration
import com.example.ui.components.formatRelativeTime
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.SkyDeep
import com.example.ui.theme.SkyMist
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkySoft
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsTab(
    myProfile: UserProfile,
    callsState: UiState<List<CallSession>>,
    usersById: Map<String, UserProfile>,
    mediaCache: Map<String, MediaFile>,
    onStartCall: (target: UserProfile, callType: String) -> Unit,
    onOpenCallScreen: (CallSession) -> Unit
) {
    var showNewCallSheet by remember { mutableStateOf(false) }
    val calls = (callsState as? UiState.Success)?.data ?: emptyList()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewCallSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("new_call_fab")
            ) {
                Icon(Icons.Rounded.Call, contentDescription = "Start Call")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Text(
                    text = "Voice & Video Calls",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-time audio & HD video calls with call history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when (callsState) {
                is UiState.Loading -> {
                    item {
                        SkeletonConversationList(count = 4)
                    }
                }
                is UiState.Error -> {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Call,
                            title = "Could not load call history",
                            subtitle = callsState.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (calls.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Rounded.Call,
                                title = "No call history yet",
                                subtitle = "Tap the Call button to start a voice or video call with any contact.",
                                actionLabel = "Make a Call",
                                onAction = { showNewCallSheet = true }
                            )
                        }
                    } else {
                        items(calls, key = { it.id }) { call ->
                            val isOutgoing = call.callerId == myProfile.userId
                            val partnerId = if (isOutgoing) call.calleeId else call.callerId
                            val partnerName = if (isOutgoing) call.calleeName else call.callerName
                            val partnerPhoto = if (isOutgoing) call.calleePhotoUrl else call.callerPhotoUrl
                            val partner = usersById[partnerId]

                            Card(
                                onClick = {
                                    if (call.status == "ringing" || call.status == "connected") {
                                        onOpenCallScreen(call)
                                    } else if (partner != null) {
                                        onStartCall(partner, call.callType)
                                    }
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = LightSurface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = partnerName,
                                photoUrl = partnerPhoto,
                                avatarMediaId = partner?.avatarMediaId.orEmpty(),
                                mediaCache = mediaCache,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(partnerName, style = MaterialTheme.typography.titleSmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val dirIcon = when {
                                        call.status == "missed" || call.status == "declined" -> Icons.AutoMirrored.Rounded.CallMissed
                                        isOutgoing -> Icons.AutoMirrored.Rounded.CallMade
                                        else -> Icons.AutoMirrored.Rounded.CallReceived
                                    }
                                    val tint = if (call.status == "missed" || call.status == "declined") {
                                        MaterialTheme.colorScheme.error
                                    } else EmeraldOnline
                                    Icon(dirIcon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${call.status.replaceFirstChar { it.uppercase() }} • ${formatCallDuration(call.durationSeconds)} • ${formatRelativeTime(call.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (partner != null) {
                                IconButton(onClick = { onStartCall(partner, "audio") }) {
                                    Icon(Icons.Rounded.Call, contentDescription = "Audio call")
                                }
                                IconButton(onClick = { onStartCall(partner, "video") }) {
                                    Icon(Icons.Rounded.Videocam, contentDescription = "Video call")
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

    if (showNewCallSheet) {
        val otherUsers = usersById.values.filter { it.userId != myProfile.userId }
        ModalBottomSheet(onDismissRequest = { showNewCallSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Start Call", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                if (otherUsers.isEmpty()) {
                    Text("Add friends via QR Code or directory first.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(otherUsers, key = { it.userId }) { user ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = LightBackground),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(name = user.displayName, photoUrl = user.photoUrl, size = 42.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(user.displayName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        showNewCallSheet = false
                                        onStartCall(user, "audio")
                                    }) { Icon(Icons.Rounded.Call, contentDescription = "Voice Call") }
                                    IconButton(onClick = {
                                        showNewCallSheet = false
                                        onStartCall(user, "video")
                                    }) { Icon(Icons.Rounded.Videocam, contentDescription = "Video Call") }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ActiveCallScreen(
    call: CallSession,
    myUserId: String,
    onAcceptCall: () -> Unit,
    onDeclineOrEndCall: (durationSec: Long) -> Unit
) {
    BackHandler { onDeclineOrEndCall(0L) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(call.callType == "video") }
    var useFrontCamera by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPerm = results[Manifest.permission.CAMERA] ?: hasCameraPerm
        hasMicPerm = results[Manifest.permission.RECORD_AUDIO] ?: hasMicPerm
    }

    LaunchedEffect(Unit) {
        val toRequest = buildList {
            if (!hasMicPerm) add(Manifest.permission.RECORD_AUDIO)
            if (call.callType == "video" && !hasCameraPerm) add(Manifest.permission.CAMERA)
        }
        if (toRequest.isNotEmpty()) permLauncher.launch(toRequest.toTypedArray())
    }

    DisposableEffect(Unit) {
        onDispose {
            audioManager.isMicrophoneMute = false
            audioManager.isSpeakerphoneOn = false
        }
    }

    LaunchedEffect(isMuted, isSpeakerOn) {
        audioManager.isMicrophoneMute = isMuted
        audioManager.isSpeakerphoneOn = isSpeakerOn
    }

    LaunchedEffect(call.status) {
        if (call.status == "connected") {
            while (true) {
                delay(1000)
                elapsedSeconds += 1
            }
        }
    }

    val isIncomingRinging = call.status == "ringing" && call.calleeId == myUserId
    val partnerName = if (call.callerId == myUserId) call.calleeName else call.callerName
    val partnerPhoto = if (call.callerId == myUserId) call.calleePhotoUrl else call.callerPhotoUrl

    val hasLiveVideoBackground = call.callType == "video" && hasCameraPerm
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasLiveVideoBackground) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val selector = if (useFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, selector, preview)
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.15f))
            )
        } else {
            OrbitalPortalBackground(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = if (hasLiveVideoBackground) Color.White.copy(alpha = 0.9f) else SkySoft
                ) {
                    Text(
                        text = if (call.callType == "video") "HD Video Call • Encrypted" else "Voice Call • Encrypted",
                        color = SkyDeep,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                UserAvatar(name = partnerName, photoUrl = partnerPhoto, size = 104.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (hasLiveVideoBackground) Color.White.copy(alpha = 0.85f) else Color.Transparent
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            partnerName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (call.status) {
                                "ringing" -> if (isIncomingRinging) "Incoming call..." else "Ringing..."
                                "connected" -> formatCallDuration(elapsedSeconds)
                                else -> call.status.replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = SkyDeep
                        )
                    }
                }
            }

            if (isIncomingRinging) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Surface(
                        onClick = { onDeclineOrEndCall(0L) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(72.dp).testTag("decline_call_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Surface(
                        onClick = onAcceptCall,
                        shape = CircleShape,
                        color = EmeraldOnline,
                        modifier = Modifier.size(72.dp).testTag("accept_call_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Call, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = LightSurface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallControlBtn(
                            icon = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            label = if (isMuted) "Unmute" else "Mute",
                            active = isMuted,
                            onClick = { isMuted = !isMuted }
                        )
                        CallControlBtn(
                            icon = if (isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            label = "Speaker",
                            active = isSpeakerOn,
                            onClick = { isSpeakerOn = !isSpeakerOn }
                        )
                        if (call.callType == "video") {
                            CallControlBtn(
                                icon = Icons.Rounded.Cameraswitch,
                                label = "Flip",
                                active = false,
                                onClick = { useFrontCamera = !useFrontCamera }
                            )
                        }
                        Surface(
                            onClick = { onDeclineOrEndCall(elapsedSeconds) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(60.dp).testTag("end_call_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CallEnd, contentDescription = "End Call", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (active) SkyPrimary else SkyMist,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = if (active) Color.White else SkyDeep)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
