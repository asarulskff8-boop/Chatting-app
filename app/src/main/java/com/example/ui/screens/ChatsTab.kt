package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Conversation
import com.example.data.model.MediaFile
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SkeletonConversationList
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.SkyMist
import com.example.ui.theme.SkyPrimary

private enum class ChatFilter(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    PINNED("Pinned"),
    ARCHIVED("Archived")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatsTab(
    myProfile: UserProfile,
    conversationsState: UiState<List<Conversation>>,
    usersById: Map<String, UserProfile>,
    mediaCache: Map<String, MediaFile>,
    unreadNotificationCount: Int,
    onRequestMedia: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onStartDirectChat: (UserProfile) -> Unit,
    onCreateGroup: (title: String, description: String, memberUids: List<String>) -> Unit,
    onTogglePin: (Conversation) -> Unit,
    onToggleArchive: (Conversation) -> Unit,
    onToggleMute: (Conversation) -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfileTab: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ChatFilter.ALL) }
    var showNewChatSheet by remember { mutableStateOf(false) }
    var showNewGroupSheet by remember { mutableStateOf(false) }
    var actionTargetConv by remember { mutableStateOf<Conversation?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = "New Chat")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header with avatar, title, QR scan & notifications
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = myProfile.displayName,
                    photoUrl = myProfile.photoUrl,
                    avatarMediaId = myProfile.avatarMediaId,
                    mediaCache = mediaCache,
                    onRequestMedia = onRequestMedia,
                    size = 44.dp,
                    showOnline = true,
                    online = true,
                    modifier = Modifier.combinedClickable(onClick = onOpenProfileTab)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = myProfile.statusText.ifBlank { "Connect Beyond Messages." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onOpenQrScanner, modifier = Modifier.testTag("open_qr_scanner_button")) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR Code")
                }
                IconButton(onClick = onOpenNotifications, modifier = Modifier.testTag("open_notifications_button")) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge { Text(unreadNotificationCount.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = "Notifications")
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search messages or people...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = SkyPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("chats_search_input")
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            when (conversationsState) {
                is UiState.Loading -> SkeletonConversationList()
                is UiState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        EmptyStateCard(
                            icon = Icons.Rounded.ChatBubbleOutline,
                            title = "Could not load conversations",
                            subtitle = conversationsState.message
                        )
                    }
                }
                is UiState.Success -> {
                    val uid = myProfile.userId
                    val filtered = conversationsState.data.filter { conv ->
                        val isArchived = uid in conv.archivedByUids
                        val isPinned = uid in conv.pinnedByUids
                        val unread = conv.unreadCounts[uid] ?: 0L
                        val partner = if (conv.type == "direct") {
                            val partnerId = conv.memberUids.firstOrNull { it != uid }
                            usersById[partnerId]
                        } else null
                        val displayTitle = if (conv.type == "group") conv.title else (partner?.displayName ?: "Direct Chat")

                        val matchesFilter = when (selectedFilter) {
                            ChatFilter.ALL -> !isArchived
                            ChatFilter.UNREAD -> !isArchived && unread > 0L
                            ChatFilter.PINNED -> !isArchived && isPinned
                            ChatFilter.ARCHIVED -> isArchived
                        }
                        val matchesQuery = searchQuery.isBlank() ||
                            displayTitle.contains(searchQuery, ignoreCase = true) ||
                            conv.lastMessageText.contains(searchQuery, ignoreCase = true)

                        matchesFilter && matchesQuery
                    }.sortedWith(
                        compareByDescending<Conversation> { uid in it.pinnedByUids }
                            .thenByDescending { it.lastMessageAt?.seconds ?: it.updatedAt?.seconds ?: 0L }
                    )

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            EmptyStateCard(
                                icon = Icons.Rounded.ChatBubbleOutline,
                                title = if (selectedFilter == ChatFilter.ARCHIVED) "No archived chats" else "Start your first conversation",
                                subtitle = "Tap New Chat or scan a friend's QR code to connect beyond messages.",
                                actionLabel = "New Chat",
                                onAction = { showNewChatSheet = true }
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filtered, key = { it.id }) { conv ->
                                val partner = if (conv.type == "direct") {
                                    val partnerId = conv.memberUids.firstOrNull { it != uid }
                                    usersById[partnerId]
                                } else null
                                val displayTitle = if (conv.type == "group") conv.title else (partner?.displayName ?: "Direct Chat")
                                val isPinned = uid in conv.pinnedByUids
                                val isMuted = uid in conv.mutedByUids
                                val unread = conv.unreadCounts[uid] ?: 0L
                                val otherTyping = conv.typingUids.any { it != uid }

                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPinned) SkyMist else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isPinned) SkyPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { onOpenConversation(conv.id) },
                                            onLongClick = { actionTargetConv = conv }
                                        )
                                        .testTag("conversation_card_${conv.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            name = displayTitle,
                                            photoUrl = partner?.photoUrl ?: conv.photoUrl,
                                            avatarMediaId = partner?.avatarMediaId.orEmpty(),
                                            mediaCache = mediaCache,
                                            onRequestMedia = onRequestMedia,
                                            size = 52.dp,
                                            showOnline = conv.type == "direct",
                                            online = partner?.online ?: false
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = displayTitle,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (isPinned) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            Icons.Rounded.PushPin,
                                                            contentDescription = "Pinned",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    if (isMuted) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            Icons.Rounded.NotificationsOff,
                                                            contentDescription = "Muted",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = formatRelativeTime(conv.lastMessageAt ?: conv.updatedAt),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (unread > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (otherTyping) "typing..." else conv.lastMessageText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (otherTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (unread > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = SkyPrimary
                                                    ) {
                                                        Text(
                                                            text = unread.coerceAtMost(99).toString(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Long-press Conversation Actions Sheet
    actionTargetConv?.let { conv ->
        val isPinned = myProfile.userId in conv.pinnedByUids
        val isArchived = myProfile.userId in conv.archivedByUids
        val isMuted = myProfile.userId in conv.mutedByUids
        ModalBottomSheet(onDismissRequest = { actionTargetConv = null }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Chat Actions", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                SheetActionRow(
                    icon = Icons.Rounded.PushPin,
                    label = if (isPinned) "Unpin Chat" else "Pin Chat",
                    onClick = {
                        onTogglePin(conv)
                        actionTargetConv = null
                    }
                )
                SheetActionRow(
                    icon = if (isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                    label = if (isArchived) "Unarchive Chat" else "Archive Chat",
                    onClick = {
                        onToggleArchive(conv)
                        actionTargetConv = null
                    }
                )
                SheetActionRow(
                    icon = Icons.Rounded.NotificationsOff,
                    label = if (isMuted) "Unmute Notifications" else "Mute Notifications",
                    onClick = {
                        onToggleMute(conv)
                        actionTargetConv = null
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // New Chat / Directory Sheet
    if (showNewChatSheet) {
        val otherUsers = usersById.values.filter { it.userId != myProfile.userId }
        ModalBottomSheet(onDismissRequest = { showNewChatSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New Conversation", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = {
                            showNewChatSheet = false
                            showNewGroupSheet = true
                        },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Group")
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (otherUsers.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Rounded.QrCodeScanner,
                        title = "No other users discovered yet",
                        subtitle = "Share or scan a friend's QR code to connect instantly!",
                        actionLabel = "Scan QR Code",
                        onAction = {
                            showNewChatSheet = false
                            onOpenQrScanner()
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(otherUsers, key = { it.userId }) { user ->
                            Card(
                                onClick = {
                                    showNewChatSheet = false
                                    onStartDirectChat(user)
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        name = user.displayName,
                                        photoUrl = user.photoUrl,
                                        avatarMediaId = user.avatarMediaId,
                                        mediaCache = mediaCache,
                                        onRequestMedia = onRequestMedia,
                                        size = 44.dp,
                                        showOnline = true,
                                        online = user.online
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "@${user.username} • ${user.statusText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Create New Group Sheet
    if (showNewGroupSheet) {
        var groupTitle by remember { mutableStateOf("") }
        var groupDesc by remember { mutableStateOf("") }
        val selectedMembers = remember { mutableStateListOf<String>() }
        val otherUsers = usersById.values.filter { it.userId != myProfile.userId }

        ModalBottomSheet(onDismissRequest = { showNewGroupSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Create Group Chat", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = groupTitle,
                    onValueChange = { groupTitle = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = groupDesc,
                    onValueChange = { groupDesc = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select Members", style = MaterialTheme.typography.labelLarge)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(otherUsers, key = { it.userId }) { user ->
                        val checked = user.userId in selectedMembers
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (it) selectedMembers.add(user.userId)
                                    else selectedMembers.remove(user.userId)
                                }
                            )
                            Text("${user.displayName} (@${user.username})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    enabled = groupTitle.isNotBlank(),
                    onClick = {
                        showNewGroupSheet = false
                        onCreateGroup(groupTitle, groupDesc, selectedMembers.toList())
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Create Group")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = tint)
        }
    }
}
