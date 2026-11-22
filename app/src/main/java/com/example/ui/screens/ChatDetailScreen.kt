package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import com.example.data.model.Conversation
import com.example.data.model.MediaFile
import com.example.data.model.Message
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SkeletonMessageList
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatCallDuration
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.SkyDeep
import com.example.ui.theme.SkyMist
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkySoft
import com.example.util.MediaUtils
import com.example.util.VoiceAudioController
import com.google.android.gms.location.LocationServices
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation,
    myProfile: UserProfile,
    partner: UserProfile?,
    messagesState: UiState<List<Message>>,
    allConversations: List<Conversation>,
    usersById: Map<String, UserProfile>,
    mediaCache: Map<String, MediaFile>,
    isBusy: Boolean,
    chatStyleIndex: Int = 0,
    onRequestMedia: (String) -> Unit,
    onBack: () -> Unit,
    onSendText: (text: String, type: String, replyTo: Message?) -> Unit,
    onSendMediaUri: (uri: Uri, isImage: Boolean) -> Unit,
    onSendVoiceFile: (file: File, durationSec: Int) -> Unit,
    onSendLocation: (lat: Double, lng: Double, label: String) -> Unit,
    onSendContact: (name: String, handle: String) -> Unit,
    onMarkRead: (List<Message>) -> Unit,
    onTypingChange: (Boolean) -> Unit,
    onEditMessage: (messageId: String, newText: String) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onReactMessage: (Message, String) -> Unit,
    onStarMessage: (Message) -> Unit,
    onPinMessage: (Message) -> Unit,
    onForwardMessage: (targetConv: Conversation, msg: Message) -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onLeaveGroup: () -> Unit,
    onUpdateGroup: (title: String, desc: String) -> Unit,
    onBlockPartner: (UserProfile) -> Unit,
    onReportPartner: (UserProfile, String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val voiceController = remember(context) { VoiceAudioController(context) }

    var inputText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    var forwardTargetMsg by remember { mutableStateOf<Message?>(null) }
    var previewBitmapBase64 by remember { mutableStateOf<String?>(null) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showStickerTray by remember { mutableStateOf(false) }
    var showGroupInfoSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var playingAudioId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            onTypingChange(false)
            voiceController.stopPlayback()
            if (isRecordingAudio) voiceController.stopRecording()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            onSendMediaUri(uri, mime.startsWith("image/"))
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onSendMediaUri(uri, false)
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (voiceController.startRecording()) isRecordingAudio = true
            else onShowMessage("Unable to start microphone recorder.")
        } else {
            onShowMessage("Microphone permission is required for voice messages.")
        }
    }

    @SuppressLint("MissingPermission")
    fun shareCurrentLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                onSendLocation(loc.latitude, loc.longitude, "Live Location (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)})")
            } else {
                onShowMessage("Location unavailable right now. Ensure GPS is turned on.")
            }
        }.addOnFailureListener {
            onShowMessage("Unable to read location.")
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) shareCurrentLocation()
        else onShowMessage("Location permission denied.")
    }

    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            onMarkRead(messages)
        }
    }

    val headerTitle = if (conversation.type == "group") conversation.title else (partner?.displayName ?: "Chat")
    val otherTyping = conversation.typingUids.any { it != myProfile.userId }
    val subtitle = when {
        otherTyping -> "typing..."
        conversation.type == "group" -> "${conversation.memberUids.size} members"
        partner?.online == true -> "Online"
        partner?.lastSeenAt != null -> "Last seen ${formatRelativeTime(partner.lastSeenAt)}"
        else -> "Offline"
    }

    val chatCanvasColor = when (chatStyleIndex) {
        1 -> SkyMist
        2 -> Color(0xFFDCE9F7)
        else -> LightBackground
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = chatCanvasColor,
        topBar = {
            Surface(color = LightSurface, tonalElevation = 1.dp, shadowElevation = 2.dp) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        UserAvatar(
                            name = headerTitle,
                            photoUrl = partner?.photoUrl ?: conversation.photoUrl,
                            avatarMediaId = partner?.avatarMediaId.orEmpty(),
                            mediaCache = mediaCache,
                            onRequestMedia = onRequestMedia,
                            size = 42.dp,
                            showOnline = conversation.type == "direct",
                            online = partner?.online == true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showGroupInfoSheet = true }
                        ) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (otherTyping || partner?.online == true) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (partner != null) {
                            IconButton(onClick = onStartVoiceCall, modifier = Modifier.testTag("voice_call_button")) {
                                Icon(Icons.Rounded.Call, contentDescription = "Voice Call")
                            }
                            IconButton(onClick = onStartVideoCall, modifier = Modifier.testTag("video_call_button")) {
                                Icon(Icons.Rounded.Videocam, contentDescription = "Video Call")
                            }
                        }
                        IconButton(onClick = { showGroupInfoSheet = true }) {
                            Icon(Icons.Rounded.Info, contentDescription = "Chat Details")
                        }
                    }

                    if (conversation.pinnedMessageText.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.PushPin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = conversation.pinnedMessageText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (isBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = LightSurface, tonalElevation = 0.dp) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // Reply / Edit Banner
                    val activeReply = replyTarget
                    val activeEdit = editingMessage
                    if (activeReply != null || activeEdit != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (activeEdit != null) "Editing message" else "Replying to ${activeReply?.senderName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = activeEdit?.text ?: activeReply?.text.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = {
                                    replyTarget = null
                                    editingMessage = null
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                                }
                            }
                        }
                    }

                    if (showStickerTray) {
                        val stickers = listOf("💜", "🚀", "✨", "😂", "🔥", "🎉", "👏", "🫶")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            stickers.forEach { st ->
                                Surface(
                                    onClick = {
                                        onSendText(st, "sticker", replyTarget)
                                        replyTarget = null
                                        showStickerTray = false
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(st, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showStickerTray = !showStickerTray }) {
                            Icon(Icons.Rounded.EmojiEmotions, contentDescription = "Stickers")
                        }
                        IconButton(onClick = { showAttachSheet = true }, modifier = Modifier.testTag("attach_button")) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = "Attach Media or File")
                        }
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                onTypingChange(it.isNotBlank())
                            },
                            placeholder = { Text(if (isRecordingAudio) "Recording voice note..." else "Message...") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = LightSurface,
                                unfocusedContainerColor = Color(0xFFEDF2F7),
                                focusedBorderColor = SkyPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f).testTag("message_input")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (inputText.isNotBlank()) {
                            Surface(
                                onClick = {
                                    val txt = inputText
                                    inputText = ""
                                    onTypingChange(false)
                                    val edit = editingMessage
                                    if (edit != null) {
                                        onEditMessage(edit.id, txt)
                                        editingMessage = null
                                    } else {
                                        onSendText(txt, "text", replyTarget)
                                        replyTarget = null
                                    }
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp).testTag("send_message_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White)
                                }
                            }
                        } else {
                            Surface(
                                onClick = {
                                    if (isRecordingAudio) {
                                        val (file, dur) = voiceController.stopRecording()
                                        isRecordingAudio = false
                                        if (file != null) onSendVoiceFile(file, dur)
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            if (voiceController.startRecording()) isRecordingAudio = true
                                        } else {
                                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                shape = CircleShape,
                                color = if (isRecordingAudio) MaterialTheme.colorScheme.error else SkySoft,
                                modifier = Modifier.size(48.dp).testTag("voice_record_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isRecordingAudio) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                        contentDescription = if (isRecordingAudio) "Stop recording" else "Record voice note",
                                        tint = if (isRecordingAudio) Color.White else SkyDeep
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when (messagesState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    SkeletonMessageList(count = 5)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        icon = Icons.Rounded.ChatBubbleOutline,
                        title = "Unable to load messages",
                        subtitle = messagesState.message
                    )
                }
            }
            is UiState.Success -> {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            icon = Icons.Rounded.ChatBubbleOutline,
                            title = "Say hello!",
                            subtitle = "Send a message, photo, voice note, or sticker to start chatting."
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isMine = msg.senderId == myProfile.userId
                            val media = mediaCache[msg.mediaId]
                            if (msg.mediaId.isNotBlank() && media == null) {
                                onRequestMedia(msg.mediaId)
                            }

                            MessageBubble(
                                message = msg,
                                isMine = isMine,
                                showSenderName = conversation.type == "group" && !isMine,
                                media = media,
                                isPlayingAudio = playingAudioId == msg.id,
                                onLongPress = { selectedMessage = msg },
                                onImageTap = { b64 -> previewBitmapBase64 = b64 },
                                onToggleAudioPlay = { m ->
                                    if (playingAudioId == msg.id) {
                                        voiceController.stopPlayback()
                                        playingAudioId = null
                                    } else {
                                        playingAudioId = msg.id
                                        voiceController.playBase64Audio(m.fileName, m.base64Data) {
                                            playingAudioId = null
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Message Action Bottom Sheet
    selectedMessage?.let { msg ->
        ModalBottomSheet(onDismissRequest = { selectedMessage = null }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("❤️", "😂", "😮", "😢", "🙏", "👍").forEach { emoji ->
                        Surface(
                            onClick = {
                                onReactMessage(msg, emoji)
                                selectedMessage = null
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(emoji, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SheetActionRow(Icons.AutoMirrored.Rounded.Reply, "Reply") {
                    replyTarget = msg
                    selectedMessage = null
                }
                SheetActionRow(Icons.AutoMirrored.Rounded.Forward, "Forward") {
                    forwardTargetMsg = msg
                    selectedMessage = null
                }
                SheetActionRow(Icons.Rounded.ContentCopy, "Copy Text") {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Message", msg.text))
                    onShowMessage("Copied to clipboard")
                    selectedMessage = null
                }
                SheetActionRow(Icons.Rounded.Star, if (myProfile.userId in msg.starredByUids) "Unstar Message" else "Star Message") {
                    onStarMessage(msg)
                    selectedMessage = null
                }
                SheetActionRow(Icons.Rounded.PushPin, if (msg.pinned) "Unpin Message" else "Pin Message") {
                    onPinMessage(msg)
                    selectedMessage = null
                }
                if (msg.senderId == myProfile.userId && !msg.deleted) {
                    if (msg.type == "text") {
                        SheetActionRow(Icons.Rounded.Edit, "Edit Message") {
                            editingMessage = msg
                            inputText = msg.text
                            selectedMessage = null
                        }
                    }
                    SheetActionRow(Icons.Rounded.Delete, "Delete Message", tint = MaterialTheme.colorScheme.error) {
                        onDeleteMessage(msg.id)
                        selectedMessage = null
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Attachments Sheet
    if (showAttachSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Share in Chat", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(14.dp))
                SheetActionRow(Icons.Rounded.Image, "Photo or Video (Photo Picker)") {
                    showAttachSheet = false
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
                SheetActionRow(Icons.Rounded.Description, "Document (PDF, Word, Excel, ZIP)") {
                    showAttachSheet = false
                    docPicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/zip", "*/*"))
                }
                SheetActionRow(Icons.Rounded.LocationOn, "Share Current Location") {
                    showAttachSheet = false
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) shareCurrentLocation()
                    else locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                SheetActionRow(Icons.Rounded.Person, "Share My Contact Card") {
                    showAttachSheet = false
                    onSendContact(myProfile.displayName, "@${myProfile.username} • ${myProfile.publicCode}")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Forward Message Picker
    forwardTargetMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { forwardTargetMsg = null },
            title = { Text("Forward Message To") },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(allConversations, key = { it.id }) { targetConv ->
                        val label = if (targetConv.type == "group") targetConv.title
                        else {
                            val pId = targetConv.memberUids.firstOrNull { it != myProfile.userId }
                            usersById[pId]?.displayName ?: "Direct Chat"
                        }
                        Surface(
                            onClick = {
                                onForwardMessage(targetConv, msg)
                                forwardTargetMsg = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(label, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { forwardTargetMsg = null }) { Text("Cancel") }
            }
        )
    }

    // Full-screen Image Preview
    previewBitmapBase64?.let { b64 ->
        val bmp = remember(b64) { MediaUtils.decodeBase64ToBitmap(b64) }
        Dialog(onDismissRequest = { previewBitmapBase64 = null }) {
            Surface(shape = RoundedCornerShape(24.dp), color = LightSurface, tonalElevation = 6.dp) {
                Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Full image preview",
                            modifier = Modifier.fillMaxWidth().height(380.dp).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Surface(
                        onClick = { previewBitmapBase64 = null },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    // Chat / Group Info Sheet (Block / Report / Edit Group / Leave Group)
    if (showGroupInfoSheet) {
        var editGroupName by remember(conversation.title) { mutableStateOf(conversation.title) }
        var editGroupDesc by remember(conversation.description) { mutableStateOf(conversation.description) }
        ModalBottomSheet(onDismissRequest = { showGroupInfoSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(headerTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(6.dp))
                if (conversation.type == "group") {
                    Text(
                        "Invite Code: ${conversation.inviteCode.ifBlank { conversation.id.take(8) }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (myProfile.userId in conversation.adminUids) {
                        OutlinedTextField(
                            value = editGroupName,
                            onValueChange = { editGroupName = it },
                            label = { Text("Group Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editGroupDesc,
                            onValueChange = { editGroupDesc = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onUpdateGroup(editGroupName, editGroupDesc)
                                showGroupInfoSheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save Group Info") }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    SheetActionRow(Icons.Rounded.Delete, "Leave Group", tint = MaterialTheme.colorScheme.error) {
                        showGroupInfoSheet = false
                        onLeaveGroup()
                    }
                } else if (partner != null) {
                    Text("@${partner.username} • ${partner.bio.ifBlank { partner.statusText }}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    SheetActionRow(Icons.Rounded.Block, "Block ${partner.displayName}", tint = MaterialTheme.colorScheme.error) {
                        showGroupInfoSheet = false
                        onBlockPartner(partner)
                    }
                    SheetActionRow(Icons.Rounded.Report, "Report User", tint = MaterialTheme.colorScheme.error) {
                        showGroupInfoSheet = false
                        showReportDialog = true
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showReportDialog && partner != null) {
        var reason by remember { mutableStateOf("Spam or Harassment") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report ${partner.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") })
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    onReportPartner(partner, reason, details)
                    showReportDialog = false
                }) { Text("Submit Report") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    showSenderName: Boolean,
    media: MediaFile?,
    isPlayingAudio: Boolean,
    onLongPress: () -> Unit,
    onImageTap: (String) -> Unit,
    onToggleAudioPlay: (MediaFile) -> Unit
) {
    val alignment = if (isMine) Alignment.End else Alignment.Start
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (isMine) 20.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 20.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            shape = bubbleShape,
            color = if (isMine) SkyPrimary else LightSurface,
            border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = if (isMine) 1.dp else 0.5.dp,
            modifier = Modifier
                .widthIn(max = 310.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            val contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (showSenderName) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isMine) Color.White else SkyDeep
                    )
                }
                if (message.forwardedFrom.isNotBlank()) {
                    Text(
                        text = "Forwarded from ${message.forwardedFrom}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.78f),
                        fontStyle = FontStyle.Italic
                    )
                }
                if (message.replyToMessageId.isNotBlank()) {
                    Surface(
                        color = if (isMine) Color.White.copy(alpha = 0.22f) else SkySoft,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = message.replyToSenderName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMine) Color.White else SkyDeep
                            )
                            Text(
                                text = message.replyToText,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.88f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                when {
                    message.deleted -> {
                        Text(
                            text = "This message was deleted",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }
                    message.type == "sticker" -> {
                        Text(message.text, style = MaterialTheme.typography.displayMedium)
                    }
                    message.type == "image" -> {
                        val bmp = remember(media?.base64Data) {
                            media?.base64Data?.let { MediaUtils.decodeBase64ToBitmap(it) }
                        }
                        if (bmp != null && media != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = message.mediaName.ifBlank { "Image" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onImageTap(media.base64Data) },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isMine) Color.White.copy(alpha = 0.2f) else SkyMist),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Loading image...", color = contentColor, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    message.type == "audio" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = media != null) { media?.let(onToggleAudioPlay) }
                                .padding(vertical = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isMine) Color.White else SkyPrimary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlayingAudio) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play voice note",
                                        tint = if (isMine) SkyPrimary else Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Voice message", style = MaterialTheme.typography.labelLarge, color = contentColor)
                                Text(
                                    formatCallDuration(message.audioDurationSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    message.type == "file" || message.type == "video" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Description,
                                contentDescription = null,
                                tint = if (isMine) Color.White else SkyPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    message.mediaName.ifBlank { message.text },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${MediaUtils.formatFileSize(message.mediaSizeBytes)} • ${message.mediaMimeType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }
                    message.type == "location" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = if (isMine) Color.White else SkyPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(message.locationLabel.ifBlank { message.text }, style = MaterialTheme.typography.bodyMedium, color = contentColor)
                        }
                    }
                    message.type == "contact" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = if (isMine) Color.White else SkyPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(message.sharedContactName, style = MaterialTheme.typography.titleSmall, color = contentColor)
                                Text(message.sharedContactPhone, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
                            }
                        }
                    }
                    else -> {
                        Text(message.text, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (message.edited) {
                        Text("Edited", style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.75f))
                    }
                    if (message.starredByUids.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Starred",
                            tint = if (isMine) Color.White else SkyPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = formatRelativeTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.76f)
                    )
                    if (isMine) {
                        val readByOthers = message.readByUids.any { it != message.senderId }
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = if (readByOthers) "Read" else "Delivered",
                            tint = if (readByOthers) Color.White else Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 2.dp, start = 6.dp, end = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.values.groupingBy { it }.eachCount().forEach { (emoji, count) ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = if (count > 1) "$emoji $count" else emoji,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
