package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.ChatRepository
import com.example.data.firebase.SocialRepository
import com.example.data.model.AppNotification
import com.example.data.model.BlockedUser
import com.example.data.model.CallSession
import com.example.data.model.Channel
import com.example.data.model.ChannelPost
import com.example.data.model.Community
import com.example.data.model.Contact
import com.example.data.model.Conversation
import com.example.data.model.FriendRequest
import com.example.data.model.MediaFile
import com.example.data.model.Message
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.util.MediaUtils
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val chatRepo: ChatRepository,
    private val socialRepo: SocialRepository,
    val currentUserId: String
) : ViewModel() {

    private companion object {
        const val TAG = "MainVM"
        const val STOP_TIMEOUT_MS = 5000L
    }

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val myProfileState: StateFlow<UiState<UserProfile?>> =
        chatRepo.observeMyProfile(currentUserId).toUiState("profile")

    val privateInfoState: StateFlow<UiState<UserPrivateInfo?>> =
        chatRepo.observePrivateInfo(currentUserId).toUiState("private info")

    val discoverableUsersState: StateFlow<UiState<List<UserProfile>>> =
        chatRepo.observeDiscoverableUsers().toUiState("users")

    val contactsState: StateFlow<UiState<List<Contact>>> =
        chatRepo.observeContacts(currentUserId).toUiState("contacts")

    val blockedUsersState: StateFlow<UiState<List<BlockedUser>>> =
        chatRepo.observeBlockedUsers(currentUserId).toUiState("blocked users")

    val friendRequestsState: StateFlow<UiState<List<FriendRequest>>> =
        chatRepo.observeFriendRequests(currentUserId).toUiState("friend requests")

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        chatRepo.observeConversations(currentUserId).toUiState("conversations")

    val storiesState: StateFlow<UiState<List<Story>>> =
        socialRepo.observeActiveStories(currentUserId).toUiState("stories")

    val channelsState: StateFlow<UiState<List<Channel>>> =
        socialRepo.observeChannels(currentUserId).toUiState("channels")

    val communitiesState: StateFlow<UiState<List<Community>>> =
        socialRepo.observeCommunities(currentUserId).toUiState("communities")

    val callsState: StateFlow<UiState<List<CallSession>>> =
        socialRepo.observeCalls(currentUserId).toUiState("calls")

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        socialRepo.observeNotifications(currentUserId).toUiState("notifications")

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeMessagesState: StateFlow<UiState<List<Message>>> =
        _activeConversationId.flatMapLatest { convId ->
            if (convId.isNullOrBlank()) flowOf(emptyList())
            else chatRepo.observeMessages(convId, currentUserId)
        }.toUiState("messages")

    private val _activeChannel = MutableStateFlow<Channel?>(null)
    val activeChannelPostsState: StateFlow<UiState<List<ChannelPost>>> =
        _activeChannel.flatMapLatest { ch ->
            if (ch == null) flowOf(emptyList())
            else socialRepo.observeChannelPosts(ch, currentUserId)
        }.toUiState("channel posts")

    private val _mediaCache = MutableStateFlow<Map<String, MediaFile>>(emptyMap())
    val mediaCache: StateFlow<Map<String, MediaFile>> = _mediaCache.asStateFlow()
    private val inflightMediaIds = mutableSetOf<String>()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepo.updatePresence(online = true)
        }
    }

    fun selectConversation(conversationId: String?) {
        _activeConversationId.value = conversationId
    }

    fun selectChannel(channel: Channel?) {
        _activeChannel.value = channel
    }

    fun ensureMediaLoaded(mediaId: String?) {
        if (mediaId.isNullOrBlank()) return
        if (_mediaCache.value.containsKey(mediaId) || !inflightMediaIds.add(mediaId)) return
        viewModelScope.launch {
            val result = chatRepo.getMediaFile(mediaId)
            result.getOrNull()?.let { media ->
                _mediaCache.value = _mediaCache.value + (mediaId to media)
            }
            inflightMediaIds.remove(mediaId)
        }
    }

    fun emitFeedback(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    // --- PROFILE & SETTINGS ---

    fun completeProfileSetup(
        context: Context,
        displayName: String,
        username: String,
        bio: String,
        statusText: String,
        phoneNumber: String,
        phoneVerified: Boolean,
        avatarUri: Uri?,
        onDone: () -> Unit
    ) {
        val firebaseUser = Firebase.auth.currentUser
        val email = firebaseUser?.email.orEmpty()
        val googlePhoto = firebaseUser?.photoUrl?.toString().orEmpty()

        viewModelScope.launch {
            _isBusy.value = true
            var avatarMediaId = ""
            if (avatarUri != null) {
                val payload = MediaUtils.compressImageUriToPayload(context, avatarUri)
                if (payload != null) {
                    chatRepo.uploadMediaPayload(payload, emptyList(), publicReadable = true)
                        .onSuccess { avatarMediaId = it }
                }
            }
            val result = chatRepo.saveUserProfileSetup(
                displayName = displayName,
                username = username,
                bio = bio,
                statusText = statusText,
                photoUrl = googlePhoto,
                avatarMediaId = avatarMediaId,
                phoneNumber = phoneNumber,
                phoneVerified = phoneVerified,
                email = email
            )
            _isBusy.value = false
            result.onSuccess {
                onDone()
            }.onFailure { err ->
                emitFeedback(err.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun updateProfilePhoto(context: Context, uri: Uri, currentProfile: UserProfile, privateInfo: UserPrivateInfo?) {
        viewModelScope.launch {
            _isBusy.value = true
            val payload = MediaUtils.compressImageUriToPayload(context, uri)
            if (payload == null) {
                _isBusy.value = false
                emitFeedback("Could not process selected photo.")
                return@launch
            }
            val uploadRes = chatRepo.uploadMediaPayload(payload, emptyList(), publicReadable = true)
            uploadRes.onSuccess { mediaId ->
                chatRepo.saveUserProfileSetup(
                    displayName = currentProfile.displayName,
                    username = currentProfile.username,
                    bio = currentProfile.bio,
                    statusText = currentProfile.statusText,
                    photoUrl = currentProfile.photoUrl,
                    avatarMediaId = mediaId,
                    phoneNumber = privateInfo?.phoneNumber.orEmpty(),
                    phoneVerified = privateInfo?.phoneVerified ?: false,
                    email = privateInfo?.email ?: Firebase.auth.currentUser?.email.orEmpty(),
                    discoverable = currentProfile.discoverable
                ).onSuccess {
                    emitFeedback("Profile photo updated")
                }.onFailure {
                    emitFeedback(it.localizedMessage ?: "Failed to save photo")
                }
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Photo upload failed")
            }
            _isBusy.value = false
        }
    }

    fun updateProfileDetails(
        currentProfile: UserProfile,
        privateInfo: UserPrivateInfo?,
        displayName: String,
        bio: String,
        statusText: String,
        phoneNumber: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            chatRepo.saveUserProfileSetup(
                displayName = displayName,
                username = currentProfile.username,
                bio = bio,
                statusText = statusText,
                photoUrl = currentProfile.photoUrl,
                avatarMediaId = currentProfile.avatarMediaId,
                phoneNumber = phoneNumber,
                phoneVerified = privateInfo?.phoneVerified ?: false,
                email = privateInfo?.email ?: Firebase.auth.currentUser?.email.orEmpty(),
                discoverable = currentProfile.discoverable
            ).onSuccess {
                emitFeedback("Profile updated")
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to update profile")
            }
            _isBusy.value = false
        }
    }

    fun updatePrivacySettings(
        discoverable: Boolean,
        privacyLastSeen: String,
        privacyProfilePhoto: String,
        readReceiptsEnabled: Boolean,
        notificationsEnabled: Boolean,
        biometricEnabled: Boolean
    ) {
        viewModelScope.launch {
            chatRepo.updatePrivacySettings(
                discoverable = discoverable,
                privacyLastSeen = privacyLastSeen,
                privacyProfilePhoto = privacyProfilePhoto,
                readReceiptsEnabled = readReceiptsEnabled,
                notificationsEnabled = notificationsEnabled,
                biometricEnabled = biometricEnabled
            ).onSuccess {
                emitFeedback("Settings saved")
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to update settings")
            }
        }
    }

    fun setOfflineBeforeSignOut(onReady: () -> Unit) {
        viewModelScope.launch {
            chatRepo.updatePresence(online = false)
            onReady()
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            chatRepo.deleteAccountData().onSuccess {
                _isBusy.value = false
                onDeleted()
            }.onFailure {
                _isBusy.value = false
                emitFeedback(it.localizedMessage ?: "Could not delete account data")
            }
        }
    }

    // --- QR, FRIENDS & CONTACTS ---

    suspend fun fetchUserProfileForQr(uid: String): UserProfile? {
        return chatRepo.getUserProfile(uid).getOrNull()
    }

    fun sendFriendRequest(myProfile: UserProfile, target: UserProfile) {
        viewModelScope.launch {
            chatRepo.sendFriendRequest(myProfile, target).onSuccess {
                socialRepo.sendNotification(
                    recipientId = target.userId,
                    senderName = myProfile.displayName,
                    type = "friend_request",
                    title = "New Friend Request",
                    body = "${myProfile.displayName} (@${myProfile.username}) wants to connect!",
                    targetId = myProfile.userId
                )
                emitFeedback("Friend request sent to ${target.displayName}")
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to send request")
            }
        }
    }

    fun respondToFriendRequest(request: FriendRequest, accept: Boolean) {
        viewModelScope.launch {
            chatRepo.respondToFriendRequest(request, accept).onSuccess {
                emitFeedback(if (accept) "Added ${request.fromName} to contacts!" else "Request declined")
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Could not respond to request")
            }
        }
    }

    fun addContactAndStartChat(
        myProfile: UserProfile,
        partner: UserProfile,
        onOpenChat: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            chatRepo.addContactDirectly(partner)
            val convRes = chatRepo.getOrCreateDirectConversation(myProfile, partner)
            _isBusy.value = false
            convRes.onSuccess { convId ->
                onOpenChat(convId)
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to start conversation")
            }
        }
    }

    fun blockUser(targetUid: String, displayName: String) {
        viewModelScope.launch {
            chatRepo.blockUser(targetUid, displayName).onSuccess {
                emitFeedback("Blocked $displayName")
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to block user")
            }
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            chatRepo.unblockUser(targetUid).onSuccess {
                emitFeedback("User unblocked")
            }
        }
    }

    fun reportUser(reportedUserId: String, reason: String, details: String) {
        viewModelScope.launch {
            socialRepo.submitUserReport(reportedUserId, reason, details).onSuccess {
                emitFeedback("Report submitted. Thank you for keeping Chatting App safe.")
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to submit report")
            }
        }
    }

    // --- CONVERSATIONS & MESSAGING ---

    fun createGroupChat(
        title: String,
        description: String,
        memberUids: List<String>,
        communityId: String = "",
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            chatRepo.createGroupConversation(title, description, memberUids, communityId)
                .onSuccess { convId ->
                    _isBusy.value = false
                    onCreated(convId)
                }.onFailure {
                    _isBusy.value = false
                    emitFeedback(it.localizedMessage ?: "Failed to create group")
                }
        }
    }

    fun sendTextMessage(
        context: Context,
        conversation: Conversation,
        sender: UserProfile,
        text: String,
        type: String = "text",
        replyTo: Message? = null
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(
                conversation = conversation,
                senderProfile = sender,
                type = type,
                text = text,
                replyTo = replyTo
            ).onSuccess {
                notifyOtherMembers(context, conversation, sender, text.take(100))
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to send message")
            }
        }
    }

    fun sendMediaUriMessage(
        context: Context,
        conversation: Conversation,
        sender: UserProfile,
        uri: Uri,
        isImage: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val payload = if (isImage) {
                MediaUtils.compressImageUriToPayload(context, uri)
            } else {
                MediaUtils.encodeSmallFileUriToPayload(context, uri)
            }
            if (payload == null) {
                _isBusy.value = false
                emitFeedback("Could not read or compress selected media")
                return@launch
            }
            val uploadRes = chatRepo.uploadMediaPayload(payload, conversation.memberUids)
            uploadRes.onSuccess { mediaId ->
                val msgType = when {
                    isImage -> "image"
                    payload.mimeType.startsWith("video/") -> "video"
                    payload.mimeType.startsWith("audio/") -> "audio"
                    else -> "file"
                }
                chatRepo.sendMessage(
                    conversation = conversation,
                    senderProfile = sender,
                    type = msgType,
                    text = payload.fileName,
                    mediaId = mediaId,
                    mediaName = payload.fileName,
                    mediaMimeType = payload.mimeType,
                    mediaSizeBytes = payload.sizeBytes
                ).onSuccess {
                    notifyOtherMembers(context, conversation, sender, "Sent ${payload.fileName}")
                }
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Upload failed")
            }
            _isBusy.value = false
        }
    }

    fun sendVoiceMessage(
        context: Context,
        conversation: Conversation,
        sender: UserProfile,
        file: java.io.File,
        durationSec: Int
    ) {
        viewModelScope.launch {
            val payload = MediaUtils.encodeAudioFileToPayload(file)
            if (payload == null) {
                emitFeedback("Voice note could not be encoded")
                return@launch
            }
            chatRepo.uploadMediaPayload(payload, conversation.memberUids)
                .onSuccess { mediaId ->
                    chatRepo.sendMessage(
                        conversation = conversation,
                        senderProfile = sender,
                        type = "audio",
                        text = "Voice message",
                        mediaId = mediaId,
                        mediaName = payload.fileName,
                        mediaMimeType = payload.mimeType,
                        mediaSizeBytes = payload.sizeBytes,
                        audioDurationSec = durationSec.toLong()
                    )
                    notifyOtherMembers(context, conversation, sender, "🎤 Voice message (${durationSec}s)")
                }.onFailure {
                    emitFeedback(it.localizedMessage ?: "Failed to send voice note")
                }
        }
    }

    fun sendLocationMessage(
        conversation: Conversation,
        sender: UserProfile,
        lat: Double,
        lng: Double,
        label: String
    ) {
        viewModelScope.launch {
            chatRepo.sendMessage(
                conversation = conversation,
                senderProfile = sender,
                type = "location",
                text = label,
                latitude = lat,
                longitude = lng,
                locationLabel = label
            ).onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to share location")
            }
        }
    }

    fun sendContactMessage(
        conversation: Conversation,
        sender: UserProfile,
        contactName: String,
        contactPhoneOrHandle: String
    ) {
        viewModelScope.launch {
            chatRepo.sendMessage(
                conversation = conversation,
                senderProfile = sender,
                type = "contact",
                text = contactName,
                sharedContactName = contactName,
                sharedContactPhone = contactPhoneOrHandle
            ).onFailure {
                emitFeedback(it.localizedMessage ?: "Failed to share contact")
            }
        }
    }

    private suspend fun notifyOtherMembers(
        context: Context,
        conversation: Conversation,
        sender: UserProfile,
        preview: String
    ) {
        NotificationHelper.ensureChannels(context)
        conversation.memberUids.filter { it != sender.userId }.take(5).forEach { target ->
            if (target !in conversation.mutedByUids) {
                socialRepo.sendNotification(
                    recipientId = target,
                    senderName = sender.displayName,
                    type = "message",
                    title = if (conversation.type == "group") "${sender.displayName} in ${conversation.title}" else sender.displayName,
                    body = preview,
                    targetId = conversation.id
                )
            }
        }
    }

    fun markConversationRead(conversation: Conversation, messages: List<Message>) {
        viewModelScope.launch {
            chatRepo.markConversationRead(conversation)
            chatRepo.markMessagesAsRead(conversation.id, messages)
        }
    }

    fun setTyping(conversation: Conversation, typing: Boolean) {
        viewModelScope.launch { chatRepo.setTypingStatus(conversation, typing) }
    }

    fun toggleConversationPin(conversation: Conversation) {
        val enabled = currentUserId !in conversation.pinnedByUids
        viewModelScope.launch { chatRepo.toggleConversationFlag(conversation, "pinnedByUids", enabled) }
    }

    fun toggleConversationArchive(conversation: Conversation) {
        val enabled = currentUserId !in conversation.archivedByUids
        viewModelScope.launch { chatRepo.toggleConversationFlag(conversation, "archivedByUids", enabled) }
    }

    fun toggleConversationMute(conversation: Conversation) {
        val enabled = currentUserId !in conversation.mutedByUids
        viewModelScope.launch { chatRepo.toggleConversationFlag(conversation, "mutedByUids", enabled) }
    }

    fun editMessage(conversationId: String, messageId: String, newText: String) {
        viewModelScope.launch {
            chatRepo.editMessage(conversationId, messageId, newText).onFailure {
                emitFeedback("Failed to edit message")
            }
        }
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            chatRepo.deleteMessage(conversationId, messageId)
        }
    }

    fun reactToMessage(conversationId: String, message: Message, emoji: String) {
        viewModelScope.launch { chatRepo.reactToMessage(conversationId, message, emoji) }
    }

    fun toggleStarMessage(conversationId: String, message: Message) {
        viewModelScope.launch {
            chatRepo.toggleStarMessage(conversationId, message).onSuccess {
                emitFeedback(if (currentUserId in message.starredByUids) "Removed from starred" else "Message starred")
            }
        }
    }

    fun pinMessage(conversationId: String, message: Message) {
        viewModelScope.launch { chatRepo.pinMessageInConversation(conversationId, message) }
    }

    fun forwardMessage(targetConv: Conversation, sender: UserProfile, original: Message) {
        viewModelScope.launch {
            chatRepo.sendMessage(
                conversation = targetConv,
                senderProfile = sender,
                type = original.type,
                text = original.text,
                mediaId = original.mediaId.ifBlank { null },
                mediaName = original.mediaName.ifBlank { null },
                mediaMimeType = original.mediaMimeType.ifBlank { null },
                mediaSizeBytes = original.mediaSizeBytes.takeIf { it > 0 },
                forwardedFrom = original.senderName
            ).onSuccess {
                emitFeedback("Message forwarded")
            }
        }
    }

    fun leaveGroup(conversation: Conversation, onLeft: () -> Unit) {
        viewModelScope.launch {
            chatRepo.leaveGroup(conversation).onSuccess {
                emitFeedback("Left group")
                onLeft()
            }
        }
    }

    fun updateGroupInfo(conversationId: String, title: String, description: String) {
        viewModelScope.launch {
            chatRepo.updateGroupInfo(conversationId, title, description).onSuccess {
                emitFeedback("Group updated")
            }
        }
    }

    // --- STORIES, CHANNELS, COMMUNITIES, CALLS ---

    fun publishStory(
        context: Context,
        author: UserProfile,
        caption: String,
        bgHex: String,
        imageUri: Uri?,
        visibility: String,
        contactUids: List<String>
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            var mediaId: String? = null
            var type = "text"
            if (imageUri != null) {
                val payload = MediaUtils.compressImageUriToPayload(context, imageUri)
                if (payload != null) {
                    chatRepo.uploadMediaPayload(payload, contactUids, publicReadable = visibility == "everyone")
                        .onSuccess {
                            mediaId = it
                            type = "image"
                        }
                }
            }
            socialRepo.publishStory(author, type, caption, bgHex, mediaId, visibility, contactUids)
                .onSuccess { emitFeedback("Story posted for 24 hours!") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Failed to post story") }
            _isBusy.value = false
        }
    }

    fun markStoryViewed(story: Story) {
        viewModelScope.launch { socialRepo.markStoryViewed(story) }
    }

    fun reactToStory(story: Story, emoji: String) {
        viewModelScope.launch {
            socialRepo.reactToStory(story, emoji).onSuccess {
                emitFeedback("Reacted $emoji")
            }
        }
    }

    fun createChannel(name: String, description: String, visibility: String, commentsEnabled: Boolean) {
        viewModelScope.launch {
            socialRepo.createChannel(name, description, visibility, commentsEnabled)
                .onSuccess { emitFeedback("Channel created!") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Failed to create channel") }
        }
    }

    fun toggleChannelSubscription(channel: Channel) {
        viewModelScope.launch { socialRepo.toggleChannelSubscription(channel) }
    }

    fun publishChannelPost(channel: Channel, authorName: String, text: String) {
        viewModelScope.launch {
            socialRepo.publishChannelPost(channel, authorName, text)
                .onFailure { emitFeedback(it.localizedMessage ?: "Failed to publish post") }
        }
    }

    fun reactToChannelPost(channelId: String, post: ChannelPost, emoji: String) {
        viewModelScope.launch { socialRepo.reactToChannelPost(channelId, post, emoji) }
    }

    fun createCommunity(name: String, description: String, announcement: String, memberUids: List<String>, groupIds: List<String>) {
        viewModelScope.launch {
            socialRepo.createCommunity(name, description, announcement, memberUids, groupIds)
                .onSuccess { emitFeedback("Community created!") }
                .onFailure { emitFeedback(it.localizedMessage ?: "Failed to create community") }
        }
    }

    fun linkGroupToCommunity(community: Community, groupId: String) {
        viewModelScope.launch {
            socialRepo.linkGroupToCommunity(community, groupId)
                .onSuccess { emitFeedback("Group linked to ${community.name}") }
        }
    }

    fun startCall(caller: UserProfile, callee: UserProfile, callType: String, onCallCreated: (String) -> Unit) {
        viewModelScope.launch {
            socialRepo.startCall(caller, callee, callType).onSuccess { callId ->
                socialRepo.sendNotification(
                    recipientId = callee.userId,
                    senderName = caller.displayName,
                    type = "call",
                    title = "Incoming ${if (callType == "video") "Video" else "Voice"} Call",
                    body = "${caller.displayName} is calling you",
                    targetId = callId
                )
                onCallCreated(callId)
            }.onFailure {
                emitFeedback(it.localizedMessage ?: "Unable to start call")
            }
        }
    }

    fun updateCallStatus(callId: String, status: String, durationSeconds: Long = 0L) {
        viewModelScope.launch { socialRepo.updateCallStatus(callId, status, durationSeconds) }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch { socialRepo.markNotificationRead(currentUserId, notificationId) }
    }
}
