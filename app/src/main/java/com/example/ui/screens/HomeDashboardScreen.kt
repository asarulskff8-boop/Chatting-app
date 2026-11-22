package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppNotification
import com.example.data.model.CallSession
import com.example.data.model.Conversation
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.prefs.UserPreferences
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.LightOnSurfaceVariant
import com.example.ui.theme.LightOutlineVariant
import com.example.ui.theme.LightSurface
import com.example.ui.theme.SkyDeep
import com.example.ui.theme.SkyMist
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.SkySoft
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.MainViewModel
import com.example.util.NotificationHelper
import kotlinx.coroutines.launch

private enum class HomeTab(val label: String, val icon: ImageVector, val tag: String) {
    CHATS("Chats", Icons.Rounded.ChatBubble, "nav_chats"),
    UPDATES("Updates", Icons.Rounded.AutoAwesome, "nav_updates"),
    COMMUNITIES("Communities", Icons.Rounded.Groups, "nav_communities"),
    CALLS("Calls", Icons.Rounded.Call, "nav_calls"),
    PROFILE("Profile", Icons.Rounded.Person, "nav_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    viewModel: MainViewModel,
    myProfile: UserProfile,
    userPrefs: UserPreferences,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetWallpaperIndex: (Int) -> Unit,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val privateInfoState by viewModel.privateInfoState.collectAsStateWithLifecycle()
    val discoverableUsersState by viewModel.discoverableUsersState.collectAsStateWithLifecycle()
    val blockedUsersState by viewModel.blockedUsersState.collectAsStateWithLifecycle()
    val friendRequestsState by viewModel.friendRequestsState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val channelsState by viewModel.channelsState.collectAsStateWithLifecycle()
    val communitiesState by viewModel.communitiesState.collectAsStateWithLifecycle()
    val callsState by viewModel.callsState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val activeChannelPostsState by viewModel.activeChannelPostsState.collectAsStateWithLifecycle()
    val mediaCache by viewModel.mediaCache.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var openedConversationId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeCallId by rememberSaveable { mutableStateOf<String?>(null) }
    var showQrScanner by rememberSaveable { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var scannedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var notifiedIds by remember { mutableStateOf(setOf<String>()) }

    var hasNotifPerm by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifPerm = granted
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(openedConversationId) {
        viewModel.selectConversation(openedConversationId)
    }

    val privateInfo = (privateInfoState as? UiState.Success)?.data
    val discoverableUsers = (discoverableUsersState as? UiState.Success)?.data ?: emptyList()
    val blockedUsers = (blockedUsersState as? UiState.Success)?.data ?: emptyList()
    val friendRequests = (friendRequestsState as? UiState.Success)?.data ?: emptyList()
    val conversations = (conversationsState as? UiState.Success)?.data ?: emptyList()
    val calls = (callsState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()

    val usersById = remember(discoverableUsers, myProfile) {
        buildMap {
            discoverableUsers.forEach { put(it.userId, it) }
            put(myProfile.userId, myProfile)
        }
    }

    val incomingRingingCall: CallSession? = remember(calls, myProfile.userId) {
        calls.firstOrNull { it.status == "ringing" && it.calleeId == myProfile.userId }
    }

    val unreadNotifications = remember(notifications) { notifications.count { !it.read } }
    val totalUnreadChats = remember(conversations, myProfile.userId) {
        conversations.filter { myProfile.userId !in it.archivedByUids }
            .sumOf { (it.unreadCounts[myProfile.userId] ?: 0L).toInt() }
    }
    val pendingFriendRequests = remember(friendRequests, myProfile.userId) {
        friendRequests.count { it.toUid == myProfile.userId && it.status == "pending" }
    }

    // Trigger local notifications when new notifications/calls arrive
    LaunchedEffect(notifications, incomingRingingCall, privateInfo?.notificationsEnabled) {
        val enabled = privateInfo?.notificationsEnabled ?: true
        if (!enabled) return@LaunchedEffect

        if (incomingRingingCall != null && incomingRingingCall.id !in notifiedIds) {
            notifiedIds = notifiedIds + incomingRingingCall.id
            NotificationHelper.showNotification(
                context = context,
                id = incomingRingingCall.id.hashCode(),
                channelId = NotificationHelper.CHANNEL_CALLS,
                title = "Incoming ${if (incomingRingingCall.callType == "video") "Video" else "Voice"} Call",
                body = "${incomingRingingCall.callerName} is calling you"
            )
        }
        notifications.filter { !it.read && it.id !in notifiedIds }.take(3).forEach { notif ->
            notifiedIds = notifiedIds + notif.id
            NotificationHelper.showNotification(
                context = context,
                id = notif.id.hashCode(),
                channelId = NotificationHelper.CHANNEL_MESSAGES,
                title = notif.title,
                body = notif.body
            )
        }
    }

    // Full-screen routes layered cleanly above tabs
    val currentCallSession = calls.firstOrNull { it.id == activeCallId }
    if (activeCallId != null && currentCallSession != null) {
        ActiveCallScreen(
            call = currentCallSession,
            myUserId = myProfile.userId,
            onAcceptCall = {
                viewModel.updateCallStatus(currentCallSession.id, "connected", 0L)
            },
            onDeclineOrEndCall = { durationSec ->
                val targetStatus = if (
                    currentCallSession.status == "ringing" &&
                    currentCallSession.calleeId == myProfile.userId
                ) "declined" else "ended"
                viewModel.updateCallStatus(currentCallSession.id, targetStatus, durationSec)
                activeCallId = null
            }
        )
        return
    }

    if (showQrScanner) {
        QrScannerScreen(
            onScannedUid = { scannedUid ->
                showQrScanner = false
                if (scannedUid == myProfile.userId) {
                    viewModel.emitFeedback("That is your own QR code!")
                } else {
                    scope.launch {
                        val profile = viewModel.fetchUserProfileForQr(scannedUid)
                        if (profile != null) {
                            scannedUserProfile = profile
                        } else {
                            viewModel.emitFeedback("No user found for that QR code.")
                        }
                    }
                }
            },
            onClose = { showQrScanner = false }
        )
        return
    }

    val openedConversation: Conversation? = conversations.firstOrNull { it.id == openedConversationId }
    if (openedConversationId != null && openedConversation != null) {
        val partnerUid = openedConversation.memberUids.firstOrNull { it != myProfile.userId }
        val partner = partnerUid?.let { usersById[it] }
        ChatDetailScreen(
            conversation = openedConversation,
            myProfile = myProfile,
            partner = partner,
            messagesState = activeMessagesState,
            allConversations = conversations,
            usersById = usersById,
            mediaCache = mediaCache,
            isBusy = isBusy,
            chatStyleIndex = userPrefs.chatWallpaperIndex,
            onRequestMedia = viewModel::ensureMediaLoaded,
            onBack = { openedConversationId = null },
            onSendText = { text, type, replyTo ->
                viewModel.sendTextMessage(context, openedConversation, myProfile, text, type, replyTo)
            },
            onSendMediaUri = { uri, isImage ->
                viewModel.sendMediaUriMessage(context, openedConversation, myProfile, uri, isImage)
            },
            onSendVoiceFile = { file, durationSec ->
                viewModel.sendVoiceMessage(context, openedConversation, myProfile, file, durationSec)
            },
            onSendLocation = { lat, lng, label ->
                viewModel.sendLocationMessage(openedConversation, myProfile, lat, lng, label)
            },
            onSendContact = { name, handle ->
                viewModel.sendContactMessage(openedConversation, myProfile, name, handle)
            },
            onMarkRead = { msgs -> viewModel.markConversationRead(openedConversation, msgs) },
            onTypingChange = { typing -> viewModel.setTyping(openedConversation, typing) },
            onEditMessage = { msgId, newText -> viewModel.editMessage(openedConversation.id, msgId, newText) },
            onDeleteMessage = { msgId -> viewModel.deleteMessage(openedConversation.id, msgId) },
            onReactMessage = { msg, emoji -> viewModel.reactToMessage(openedConversation.id, msg, emoji) },
            onStarMessage = { msg -> viewModel.toggleStarMessage(openedConversation.id, msg) },
            onPinMessage = { msg -> viewModel.pinMessage(openedConversation.id, msg) },
            onForwardMessage = { targetConv, msg -> viewModel.forwardMessage(targetConv, myProfile, msg) },
            onStartVoiceCall = {
                if (partner != null) {
                    viewModel.startCall(myProfile, partner, "audio") { callId ->
                        activeCallId = callId
                    }
                } else {
                    viewModel.emitFeedback("Voice calls are available in direct chats.")
                }
            },
            onStartVideoCall = {
                if (partner != null) {
                    viewModel.startCall(myProfile, partner, "video") { callId ->
                        activeCallId = callId
                    }
                } else {
                    viewModel.emitFeedback("Video calls are available in direct chats.")
                }
            },
            onLeaveGroup = {
                viewModel.leaveGroup(openedConversation) {
                    openedConversationId = null
                }
            },
            onUpdateGroup = { title, desc ->
                viewModel.updateGroupInfo(openedConversation.id, title, desc)
            },
            onBlockPartner = { target ->
                viewModel.blockUser(target.userId, target.displayName)
                openedConversationId = null
            },
            onReportPartner = { target, reason, details ->
                viewModel.reportUser(target.userId, reason, details)
            },
            onShowMessage = viewModel::emitFeedback
        )
        return
    }

    val currentTab = HomeTab.entries.getOrElse(selectedTabIndex) { HomeTab.CHATS }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (!useRail) {
                    Column {
                        HorizontalDivider(color = LightOutlineVariant)
                        NavigationBar(
                            containerColor = LightSurface,
                            tonalElevation = 0.dp
                        ) {
                            HomeTab.entries.forEachIndexed { idx, tab ->
                                val badgeCount = when (tab) {
                                    HomeTab.CHATS -> totalUnreadChats
                                    HomeTab.PROFILE -> pendingFriendRequests
                                    HomeTab.CALLS -> if (incomingRingingCall != null) 1 else 0
                                    else -> 0
                                }
                                NavigationBarItem(
                                    selected = selectedTabIndex == idx,
                                    onClick = { selectedTabIndex = idx },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SkyDeep,
                                        selectedTextColor = SkyDeep,
                                        indicatorColor = SkySoft,
                                        unselectedIconColor = LightOnSurfaceVariant,
                                        unselectedTextColor = LightOnSurfaceVariant
                                    ),
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (badgeCount > 0) {
                                                    Badge(containerColor = SkyPrimary, contentColor = Color.White) {
                                                        Text(badgeCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    },
                                    label = { Text(tab.label, fontWeight = if (selectedTabIndex == idx) FontWeight.Bold else FontWeight.Medium, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = LightSurface,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HomeTab.entries.forEachIndexed { idx, tab ->
                            val badgeCount = when (tab) {
                                HomeTab.CHATS -> totalUnreadChats
                                HomeTab.PROFILE -> pendingFriendRequests
                                HomeTab.CALLS -> if (incomingRingingCall != null) 1 else 0
                                else -> 0
                            }
                            NavigationRailItem(
                                selected = selectedTabIndex == idx,
                                onClick = { selectedTabIndex = idx },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SkyDeep,
                                    selectedTextColor = SkyDeep,
                                    indicatorColor = SkySoft,
                                    unselectedIconColor = LightOnSurfaceVariant,
                                    unselectedTextColor = LightOnSurfaceVariant
                                ),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge(containerColor = SkyPrimary, contentColor = Color.White) {
                                                    Text(badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .statusBarsPadding()
                ) {
                    // Incoming ringing call banner
                    AnimatedVisibility(visible = incomingRingingCall != null) {
                        incomingRingingCall?.let { call ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SkyMist),
                                border = BorderStroke(1.5.dp, SkyPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(name = call.callerName, photoUrl = call.callerPhotoUrl, size = 44.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = call.callerName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Incoming ${if (call.callType == "video") "video" else "voice"} call...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SkyDeep
                                        )
                                    }
                                    Surface(
                                        onClick = { viewModel.updateCallStatus(call.id, "declined", 0L) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.CallEnd, contentDescription = "Decline Call", tint = Color.White)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        onClick = {
                                            viewModel.updateCallStatus(call.id, "connected", 0L)
                                            activeCallId = call.id
                                        },
                                        shape = CircleShape,
                                        color = EmeraldOnline,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.Call, contentDescription = "Accept Call", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentTab) {
                            HomeTab.CHATS -> ChatsTab(
                                myProfile = myProfile,
                                conversationsState = conversationsState,
                                usersById = usersById,
                                mediaCache = mediaCache,
                                unreadNotificationCount = unreadNotifications,
                                onRequestMedia = viewModel::ensureMediaLoaded,
                                onOpenConversation = { convId -> openedConversationId = convId },
                                onStartDirectChat = { partner ->
                                    viewModel.addContactAndStartChat(myProfile, partner) { convId ->
                                        openedConversationId = convId
                                    }
                                },
                                onCreateGroup = { title, desc, memberUids ->
                                    viewModel.createGroupChat(title, desc, memberUids) { convId ->
                                        openedConversationId = convId
                                    }
                                },
                                onTogglePin = viewModel::toggleConversationPin,
                                onToggleArchive = viewModel::toggleConversationArchive,
                                onToggleMute = viewModel::toggleConversationMute,
                                onOpenQrScanner = { showQrScanner = true },
                                onOpenNotifications = { showNotificationsSheet = true },
                                onOpenProfileTab = { selectedTabIndex = HomeTab.PROFILE.ordinal }
                            )

                            HomeTab.UPDATES -> UpdatesTab(
                                myProfile = myProfile,
                                storiesState = storiesState,
                                channelsState = channelsState,
                                activeChannelPostsState = activeChannelPostsState,
                                mediaCache = mediaCache,
                                onRequestMedia = viewModel::ensureMediaLoaded,
                                onPublishStory = { caption, bgHex, imageUri, visibility ->
                                    val contactUids = discoverableUsers.map { it.userId }.filter { it != myProfile.userId }
                                    viewModel.publishStory(context, myProfile, caption, bgHex, imageUri, visibility, contactUids)
                                },
                                onMarkStoryViewed = viewModel::markStoryViewed,
                                onReactToStory = viewModel::reactToStory,
                                onCreateChannel = viewModel::createChannel,
                                onToggleSubscribeChannel = viewModel::toggleChannelSubscription,
                                onSelectChannel = viewModel::selectChannel,
                                onPublishChannelPost = { ch, text -> viewModel.publishChannelPost(ch, myProfile.displayName, text) },
                                onReactChannelPost = viewModel::reactToChannelPost
                            )

                            HomeTab.COMMUNITIES -> CommunitiesTab(
                                myProfile = myProfile,
                                communitiesState = communitiesState,
                                conversations = conversations,
                                usersById = usersById,
                                onCreateCommunity = viewModel::createCommunity,
                                onCreateGroupInCommunity = { community, title, desc, memberUids ->
                                    viewModel.createGroupChat(title, desc, memberUids, community?.id.orEmpty()) { convId ->
                                        if (community != null) {
                                            viewModel.linkGroupToCommunity(community, convId)
                                        }
                                        openedConversationId = convId
                                    }
                                },
                                onOpenGroupChat = { convId -> openedConversationId = convId }
                            )

                            HomeTab.CALLS -> CallsTab(
                                myProfile = myProfile,
                                callsState = callsState,
                                usersById = usersById,
                                mediaCache = mediaCache,
                                onStartCall = { target, callType ->
                                    viewModel.startCall(myProfile, target, callType) { callId ->
                                        activeCallId = callId
                                    }
                                },
                                onOpenCallScreen = { call ->
                                    activeCallId = call.id
                                }
                            )

                            HomeTab.PROFILE -> ProfileTab(
                                myProfile = myProfile,
                                privateInfo = privateInfo,
                                friendRequests = friendRequests,
                                blockedUsers = blockedUsers,
                                userPrefs = userPrefs,
                                mediaCache = mediaCache,
                                onRequestMedia = viewModel::ensureMediaLoaded,
                                onUpdatePhoto = { uri ->
                                    viewModel.updateProfilePhoto(context, uri, myProfile, privateInfo)
                                },
                                onEditProfile = { name, bio, status, phone ->
                                    viewModel.updateProfileDetails(myProfile, privateInfo, name, bio, status, phone)
                                },
                                onUpdatePrivacy = viewModel::updatePrivacySettings,
                                onSetThemeMode = onSetThemeMode,
                                onSetLanguage = onSetLanguage,
                                onSetWallpaperIndex = onSetWallpaperIndex,
                                onRespondFriendRequest = viewModel::respondToFriendRequest,
                                onUnblockUser = viewModel::unblockUser,
                                onOpenQrScanner = { showQrScanner = true },
                                onLogout = {
                                    viewModel.setOfflineBeforeSignOut(onLogout)
                                },
                                onDeleteAccount = {
                                    viewModel.deleteAccount(onAccountDeleted)
                                },
                                onShowMessage = viewModel::emitFeedback
                            )
                        }
                    }
                }
            }
        }
    }

    // Notifications Center Bottom Sheet
    if (showNotificationsSheet) {
        ModalBottomSheet(onDismissRequest = { showNotificationsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications", style = MaterialTheme.typography.titleLarge)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                        TextButton(onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Enable Alerts")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (notifications.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = SkyPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You're all caught up!", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif: AppNotification ->
                            Card(
                                onClick = {
                                    viewModel.markNotificationRead(notif.id)
                                    showNotificationsSheet = false
                                    when (notif.type) {
                                        "message" -> if (notif.targetId.isNotBlank()) openedConversationId = notif.targetId
                                        "call" -> selectedTabIndex = HomeTab.CALLS.ordinal
                                        "friend_request" -> selectedTabIndex = HomeTab.PROFILE.ordinal
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.read) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            formatRelativeTime(notif.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(notif.body, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Scanned QR User Profile Preview Dialog
    scannedUserProfile?.let { target ->
        AlertDialog(
            onDismissRequest = { scannedUserProfile = null },
            title = { Text("Friend Found via QR") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    UserAvatar(
                        name = target.displayName,
                        photoUrl = target.photoUrl,
                        avatarMediaId = target.avatarMediaId,
                        mediaCache = mediaCache,
                        onRequestMedia = viewModel::ensureMediaLoaded,
                        size = 76.dp,
                        showOnline = true,
                        online = target.online
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(target.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("@${target.username} • ${target.publicCode}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (target.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(target.bio, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val user = target
                    scannedUserProfile = null
                    viewModel.addContactAndStartChat(myProfile, user) { convId ->
                        openedConversationId = convId
                    }
                }) {
                    Text("Message")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    val user = target
                    scannedUserProfile = null
                    viewModel.sendFriendRequest(myProfile, user)
                }) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Friend")
                }
            }
        )
    }
}
