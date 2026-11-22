package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.ChannelPost
import com.example.data.model.MediaFile
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkeletonPostList
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.ShimmerBase
import com.example.ui.theme.SkyMist
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkySoft
import com.example.util.MediaUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesTab(
    myProfile: UserProfile,
    storiesState: UiState<List<Story>>,
    channelsState: UiState<List<Channel>>,
    activeChannelPostsState: UiState<List<ChannelPost>>,
    mediaCache: Map<String, MediaFile>,
    onRequestMedia: (String) -> Unit,
    onPublishStory: (caption: String, bgHex: String, imageUri: Uri?, visibility: String) -> Unit,
    onMarkStoryViewed: (Story) -> Unit,
    onReactToStory: (Story, String) -> Unit,
    onCreateChannel: (name: String, description: String, visibility: String, commentsEnabled: Boolean) -> Unit,
    onToggleSubscribeChannel: (Channel) -> Unit,
    onSelectChannel: (Channel?) -> Unit,
    onPublishChannelPost: (Channel, String) -> Unit,
    onReactChannelPost: (String, ChannelPost, String) -> Unit
) {
    var showCreateStorySheet by remember { mutableStateOf(false) }
    var viewingStory by remember { mutableStateOf<Story?>(null) }
    var showCreateChannelSheet by remember { mutableStateOf(false) }
    var openedChannel by remember { mutableStateOf<Channel?>(null) }
    var channelSearch by remember { mutableStateOf("") }

    val stories = (storiesState as? UiState.Success)?.data ?: emptyList()
    val channels = (channelsState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Updates & Channels",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "24-hour stories & real-time broadcast channels",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stories Strip
        item {
            SectionHeader(
                title = "Stories",
                trailing = {
                    Text(
                        text = "+ Add Story",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showCreateStorySheet = true }
                            .padding(6.dp)
                            .testTag("add_story_button")
                    )
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable { showCreateStorySheet = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Post Story", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "My Story",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                    }
                }
                when (storiesState) {
                    is UiState.Loading -> {
                        items(4) {
                            val shimmer = rememberInfiniteTransition(label = "storyShimmer")
                            val pulseAlpha by shimmer.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 0.95f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(850),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "storyAlpha"
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(72.dp).alpha(pulseAlpha)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(ShimmerBase)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(ShimmerBase)
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = LightSurface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Unable to load stories",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp)
                                )
                            }
                        }
                    }
                    is UiState.Success -> {
                        items(stories, key = { it.id }) { story ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable {
                                viewingStory = story
                                onMarkStoryViewed(story)
                            }
                    ) {
                        UserAvatar(
                            name = story.authorName,
                            photoUrl = story.authorPhotoUrl,
                            mediaCache = mediaCache,
                            size = 64.dp,
                            hasStoryRing = true
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = story.authorName.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                        }
                    }
                }
            }
        }

        // Channels Header + Search
        item {
            SectionHeader(
                title = "Channels",
                trailing = {
                    Button(
                        onClick = { showCreateChannelSheet = true },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("create_channel_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create")
                    }
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = channelSearch,
                onValueChange = { channelSearch = it },
                placeholder = { Text("Discover channels...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = LightSurface,
                    focusedBorderColor = SkyPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        val filteredChannels = channels.filter {
            channelSearch.isBlank() || it.name.contains(channelSearch, ignoreCase = true) ||
                it.description.contains(channelSearch, ignoreCase = true)
        }

        when (channelsState) {
            is UiState.Loading -> {
                item {
                    SkeletonPostList(count = 3)
                }
            }
            is UiState.Error -> {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Campaign,
                        title = "Could not load channels",
                        subtitle = channelsState.message
                    )
                }
            }
            is UiState.Success -> {
                if (filteredChannels.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.Campaign,
                            title = "No channels yet",
                            subtitle = "Launch a public or private broadcast channel to share announcements, media, and updates.",
                            actionLabel = "Create Channel",
                            onAction = { showCreateChannelSheet = true }
                        )
                    }
                } else {
                    items(filteredChannels, key = { it.id }) { channel ->
                        val isSubscribed = myProfile.userId in channel.subscriberUids
                        Card(
                            onClick = {
                                openedChannel = channel
                                onSelectChannel(channel)
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = LightSurface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (channel.visibility == "private") Icons.Rounded.Lock else Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(channel.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(
                                text = "${channel.subscriberCount} subscribers • ${channel.description.ifBlank { channel.inviteCode }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isSubscribed) {
                            OutlinedButton(
                                onClick = { onToggleSubscribeChannel(channel) },
                                shape = CircleShape
                            ) { Text("Joined") }
                        } else {
                            Button(
                                onClick = { onToggleSubscribeChannel(channel) },
                                shape = CircleShape
                            ) { Text("Follow") }
                        }
                    }
                }
                    }
                }
            }
        }
    }

    // Create Story Sheet
    if (showCreateStorySheet) {
        var caption by remember { mutableStateOf("") }
        var visibility by remember { mutableStateOf("everyone") }
        var selectedBg by remember { mutableStateOf("#E0F2FE") }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) selectedUri = uri
        }
        ModalBottomSheet(onDismissRequest = { showCreateStorySheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Post 24h Story", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                if (selectedUri != null) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Story image",
                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Write a status or caption...") },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("#E0F2FE", "#DCFCE7", "#F0F9FF", "#FFE4E6").forEach { hex ->
                        Surface(
                            onClick = { selectedBg = hex },
                            shape = CircleShape,
                            color = Color(android.graphics.Color.parseColor(hex)),
                            modifier = Modifier.size(32.dp)
                        ) {}
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Photo")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = visibility == "everyone",
                        onClick = { visibility = "everyone" },
                        label = { Text("Everyone") }
                    )
                    FilterChip(
                        selected = visibility == "contacts",
                        onClick = { visibility = "contacts" },
                        label = { Text("Contacts Only") }
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    enabled = caption.isNotBlank() || selectedUri != null,
                    onClick = {
                        onPublishStory(caption, selectedBg, selectedUri, visibility)
                        showCreateStorySheet = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Share Story") }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Full-screen Story Viewer
    viewingStory?.let { story ->
        val media = mediaCache[story.mediaId]
        if (story.mediaId.isNotBlank() && media == null) onRequestMedia(story.mediaId)
        val bmp = remember(media?.base64Data) {
            media?.base64Data?.let { MediaUtils.decodeBase64ToBitmap(it) }
        }
        var progress by remember(story.id) { mutableStateOf(0f) }
        LaunchedEffect(story.id) {
            while (progress < 1f) {
                delay(50)
                progress += 0.01f
            }
            viewingStory = null
        }

        Dialog(
            onDismissRequest = { viewingStory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val pastelBg = remember(story.backgroundColorHex) {
                val parsed = try {
                    Color(android.graphics.Color.parseColor(story.backgroundColorHex))
                } catch (_: Exception) {
                    SkySoft
                }
                val luminance = 0.299f * parsed.red + 0.587f * parsed.green + 0.114f * parsed.blue
                if (luminance < 0.65f) SkySoft else parsed
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(LightBackground, pastelBg)))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = SkyPrimary,
                        trackColor = SkyPrimary.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        UserAvatar(name = story.authorName, photoUrl = story.authorPhotoUrl, size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                story.authorName,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                formatRelativeTime(story.createdAt),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(onClick = { viewingStory = null }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Story",
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (story.caption.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (bmp != null) Color.White.copy(alpha = 0.88f) else Color.Transparent,
                                modifier = Modifier
                                    .align(if (bmp != null) Alignment.BottomCenter else Alignment.Center)
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = story.caption,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.headlineMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${story.viewerUids.size} views",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("🔥", "❤️", "😂", "👏").forEach { emoji ->
                                Surface(
                                    onClick = { onReactToStory(story, emoji) },
                                    shape = CircleShape,
                                    color = LightSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    tonalElevation = 1.dp
                                ) {
                                    Text(emoji, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Channel Sheet
    if (showCreateChannelSheet) {
        var chName by remember { mutableStateOf("") }
        var chDesc by remember { mutableStateOf("") }
        var chVisibility by remember { mutableStateOf("public") }
        var commentsEnabled by remember { mutableStateOf(true) }
        ModalBottomSheet(onDismissRequest = { showCreateChannelSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Create Broadcast Channel", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = chName,
                    onValueChange = { chName = it },
                    label = { Text("Channel Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = chDesc,
                    onValueChange = { chDesc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = chVisibility == "public", onClick = { chVisibility = "public" }, label = { Text("Public") })
                    FilterChip(selected = chVisibility == "private", onClick = { chVisibility = "private" }, label = { Text("Private") })
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Allow reactions & comments")
                    Switch(checked = commentsEnabled, onCheckedChange = { commentsEnabled = it })
                }
                Button(
                    enabled = chName.isNotBlank(),
                    onClick = {
                        onCreateChannel(chName, chDesc, chVisibility, commentsEnabled)
                        showCreateChannelSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Launch Channel") }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Channel Feed Sheet
    openedChannel?.let { channel ->
        val posts = (activeChannelPostsState as? UiState.Success)?.data ?: emptyList()
        var postText by remember { mutableStateOf("") }
        val isAdmin = myProfile.userId in channel.adminUids
        ModalBottomSheet(onDismissRequest = {
            openedChannel = null
            onSelectChannel(null)
        }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(channel.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${channel.subscriberCount} subscribers • Invite: ${channel.inviteCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (activeChannelPostsState) {
                        is UiState.Loading -> {
                            item {
                                SkeletonPostList(count = 2)
                            }
                        }
                        is UiState.Error -> {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Rounded.Campaign,
                                    title = "Unable to load posts",
                                    subtitle = activeChannelPostsState.message
                                )
                            }
                        }
                        is UiState.Success -> {
                            if (posts.isEmpty()) {
                                item {
                                    EmptyStateCard(
                                        icon = Icons.Rounded.Campaign,
                                        title = "No posts yet",
                                        subtitle = "Broadcast posts in this channel will appear here."
                                    )
                                }
                            } else {
                                items(posts, key = { it.id }) { post ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = LightSurface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(post.authorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(post.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("👍", "🔥", "💜").forEach { em ->
                                                    val count = post.reactions.values.count { it == em }
                                                    Surface(
                                                        onClick = { onReactChannelPost(channel.id, post, em) },
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            if (count > 0) "$em $count" else em,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
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
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = { Text("Broadcast an update...") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            enabled = postText.isNotBlank(),
                            onClick = {
                                onPublishChannelPost(channel, postText)
                                postText = ""
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Publish")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}
