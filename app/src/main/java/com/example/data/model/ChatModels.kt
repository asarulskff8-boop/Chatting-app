package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val statusText: String = "Connect Beyond Messages.",
    val photoUrl: String = "",
    val avatarMediaId: String = "",
    val publicCode: String = "",
    val online: Boolean = false,
    val profileCompleted: Boolean = false,
    val discoverable: Boolean = true,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPrivateInfo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val biometricEnabled: Boolean = false,
    val privacyLastSeen: String = "everyone",
    val privacyProfilePhoto: String = "everyone",
    val readReceiptsEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Contact(
    @DocumentId val id: String = "",
    val contactUid: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val avatarMediaId: String = "",
    val publicCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val blockedUid: String = "",
    val displayName: String = "",
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromName: String = "",
    val fromUsername: String = "",
    val fromPhotoUrl: String = "",
    val toName: String = "",
    val toUsername: String = "",
    val status: String = "pending",
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val type: String = "direct",
    val title: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val communityId: String = "",
    val inviteCode: String = "",
    val createdBy: String = "",
    val memberUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val pinnedByUids: List<String> = emptyList(),
    val archivedByUids: List<String> = emptyList(),
    val mutedByUids: List<String> = emptyList(),
    val typingUids: List<String> = emptyList(),
    val unreadCounts: Map<String, Long> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageType: String = "none",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val pinnedMessageId: String = "",
    val pinnedMessageText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Message(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val memberUids: List<String> = emptyList(),
    val type: String = "text",
    val text: String = "",
    val mediaId: String = "",
    val mediaName: String = "",
    val mediaMimeType: String = "",
    val mediaSizeBytes: Long = 0L,
    val audioDurationSec: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "",
    val sharedContactName: String = "",
    val sharedContactPhone: String = "",
    val replyToMessageId: String = "",
    val replyToSenderName: String = "",
    val replyToText: String = "",
    val forwardedFrom: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val readByUids: List<String> = emptyList(),
    val starredByUids: List<String> = emptyList(),
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MediaFile(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val publicReadable: Boolean = false,
    val mimeType: String = "",
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val base64Data: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val type: String = "text",
    val caption: String = "",
    val backgroundColorHex: String = "#4C1D95",
    val mediaId: String = "",
    val visibility: String = "everyone",
    val visibleToUids: List<String> = emptyList(),
    val viewerUids: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(),
    val expiresAtMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Channel(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val ownerId: String = "",
    val adminUids: List<String> = emptyList(),
    val subscriberUids: List<String> = emptyList(),
    val subscriberCount: Long = 0L,
    val visibility: String = "public",
    val inviteCode: String = "",
    val commentsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChannelPost(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val channelOwnerId: String = "",
    val channelVisibility: String = "public",
    val subscriberUids: List<String> = emptyList(),
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val mediaId: String = "",
    val mediaName: String = "",
    val mediaMimeType: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val commentCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Community(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val photoUrl: String = "",
    val ownerId: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val groupIds: List<String> = emptyList(),
    val announcementText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhotoUrl: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val calleePhotoUrl: String = "",
    val participantUids: List<String> = emptyList(),
    val callType: String = "audio",
    val status: String = "ringing",
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val durationSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "message",
    val title: String = "",
    val body: String = "",
    val targetId: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
