package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.CallSession
import com.example.data.model.Channel
import com.example.data.model.ChannelPost
import com.example.data.model.Community
import com.example.data.model.Story
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.withFirestoreLogging(uid: String, op: OperationType, path: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- STORIES (24H EXPIRATION) ---

    fun observeActiveStories(userId: String): Flow<List<Story>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val publicFlow = db.collection("stories")
            .whereEqualTo("visibility", "everyone")
            .snapshots()
            .map { it.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .withFirestoreLogging(uid, OperationType.LIST, "stories")

        val privateFlow = db.collection("stories")
            .whereArrayContains("visibleToUids", userId)
            .snapshots()
            .map { it.toObjects(Story::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .withFirestoreLogging(uid, OperationType.LIST, "stories")

        emitAll(
            combine(publicFlow, privateFlow) { pub, priv ->
                val now = System.currentTimeMillis()
                (pub + priv)
                    .distinctBy { it.id }
                    .filter { it.expiresAtMs > now }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
        )
    }

    suspend fun publishStory(
        author: UserProfile,
        type: String,
        caption: String,
        backgroundColorHex: String,
        mediaId: String?,
        visibility: String,
        contactUids: List<String>
    ): Result<Unit> {
        val storyId = "st_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "stories/$storyId"
        return try {
            val uid = requireUserId()
            require(uid == author.userId)
            val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
            val visibleTo = (contactUids + uid).distinct().take(100)
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to author.displayName.take(80),
                "authorUsername" to author.username.take(30),
                "authorPhotoUrl" to author.photoUrl.take(500).ifBlank { null },
                "type" to type,
                "caption" to caption.trim().take(500),
                "backgroundColorHex" to backgroundColorHex.take(20),
                "mediaId" to mediaId?.takeIf { it.isNotBlank() },
                "visibility" to visibility,
                "visibleToUids" to visibleTo,
                "viewerUids" to listOf(uid),
                "reactions" to emptyMap<String, String>(),
                "expiresAtMs" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markStoryViewed(story: Story): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (uid in story.viewerUids) return Result.success(Unit)
        val path = "stories/${story.id}"
        return try {
            db.document(path).update(
                mapOf(
                    "viewerUids" to (story.viewerUids + uid).distinct().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reactToStory(story: Story, emoji: String): Result<Unit> {
        val path = "stories/${story.id}"
        return try {
            val uid = requireUserId()
            val updated = story.reactions.toMutableMap().apply { put(uid, emoji) }
            db.document(path).update(
                mapOf(
                    "reactions" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CHANNELS & POSTS ---

    fun observeChannels(userId: String): Flow<List<Channel>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val publicFlow = db.collection("channels")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { it.toObjects(Channel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .withFirestoreLogging(uid, OperationType.LIST, "channels")

        val subscribedFlow = db.collection("channels")
            .whereArrayContains("subscriberUids", userId)
            .snapshots()
            .map { it.toObjects(Channel::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .withFirestoreLogging(uid, OperationType.LIST, "channels")

        emitAll(
            combine(publicFlow, subscribedFlow) { pub, sub ->
                (pub + sub).distinctBy { it.id }
                    .sortedByDescending { it.subscriberCount }
            }
        )
    }

    fun observeChannelPosts(channel: Channel, userId: String): Flow<List<ChannelPost>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "channels/${channel.id}/posts"
        val query = if (channel.visibility == "public") {
            db.collection("channels").document(channel.id).collection("posts")
                .whereEqualTo("channelVisibility", "public")
        } else {
            db.collection("channels").document(channel.id).collection("posts")
                .whereArrayContains("subscriberUids", userId)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(ChannelPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    suspend fun createChannel(
        name: String,
        description: String,
        visibility: String,
        commentsEnabled: Boolean
    ): Result<String> {
        val channelId = "ch_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "channels/$channelId"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty())
            val inviteCode = "CH-" + channelId.takeLast(6).uppercase()
            db.document(path).set(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(400),
                    "ownerId" to uid,
                    "adminUids" to listOf(uid),
                    "subscriberUids" to listOf(uid),
                    "subscriberCount" to 1L,
                    "visibility" to visibility,
                    "inviteCode" to inviteCode,
                    "commentsEnabled" to commentsEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(channelId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleChannelSubscription(channel: Channel): Result<Unit> {
        val path = "channels/${channel.id}"
        return try {
            val uid = requireUserId()
            val isSub = uid in channel.subscriberUids
            val updated = if (isSub) (channel.subscriberUids - uid) else (channel.subscriberUids + uid).distinct()
            db.document(path).update(
                mapOf(
                    "subscriberUids" to updated,
                    "subscriberCount" to updated.size.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishChannelPost(
        channel: Channel,
        authorName: String,
        text: String,
        mediaId: String? = null,
        mediaName: String? = null,
        mediaMimeType: String? = null
    ): Result<Unit> {
        val postId = "post_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "channels/${channel.id}/posts/$postId"
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "channelId" to channel.id,
                "channelOwnerId" to channel.ownerId,
                "channelVisibility" to channel.visibility,
                "subscriberUids" to channel.subscriberUids.take(200),
                "authorId" to uid,
                "authorName" to authorName.take(80),
                "text" to text.trim().take(4000),
                "mediaId" to mediaId?.takeIf { it.isNotBlank() },
                "mediaName" to mediaName?.takeIf { it.isNotBlank() },
                "mediaMimeType" to mediaMimeType?.takeIf { it.isNotBlank() },
                "reactions" to emptyMap<String, String>(),
                "commentCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun reactToChannelPost(channelId: String, post: ChannelPost, emoji: String): Result<Unit> {
        val path = "channels/$channelId/posts/${post.id}"
        return try {
            val uid = requireUserId()
            val updated = post.reactions.toMutableMap()
            if (updated[uid] == emoji) updated.remove(uid) else updated[uid] = emoji
            db.document(path).update(
                mapOf(
                    "reactions" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- COMMUNITIES ---

    fun observeCommunities(userId: String): Flow<List<Community>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "communities"
        emitAll(
            db.collection("communities")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Community::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    suspend fun createCommunity(
        name: String,
        description: String,
        announcementText: String,
        memberUids: List<String>,
        groupIds: List<String>
    ): Result<String> {
        val commId = "comm_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        val path = "communities/$commId"
        return try {
            val uid = requireUserId()
            val allMembers = (memberUids + uid).distinct().take(100)
            db.document(path).set(
                mapOf(
                    "name" to name.trim().take(80),
                    "description" to description.trim().take(400),
                    "ownerId" to uid,
                    "adminUids" to listOf(uid),
                    "memberUids" to allMembers,
                    "groupIds" to groupIds.distinct().take(30),
                    "announcementText" to announcementText.trim().ifBlank { "Welcome to ${name.trim()}" }.take(500),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(commId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun linkGroupToCommunity(community: Community, groupId: String): Result<Unit> {
        val path = "communities/${community.id}"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "groupIds" to (community.groupIds + groupId).distinct().take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- CALLS (VOICE & VIDEO SIGNALING + HISTORY) ---

    fun observeCalls(userId: String): Flow<List<CallSession>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "calls"
        emitAll(
            db.collection("calls")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    suspend fun startCall(
        caller: UserProfile,
        callee: UserProfile,
        callType: String
    ): Result<String> {
        val callId = "call_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "calls/$callId"
        return try {
            val uid = requireUserId()
            require(uid == caller.userId)
            val sdpOffer = "v=0\no=- ${System.currentTimeMillis()} 2 IN IP4 127.0.0.1\ns=ChattingApp-$callType"
            val payload = mapOf(
                "callerId" to uid,
                "callerName" to caller.displayName.take(80),
                "callerPhotoUrl" to caller.photoUrl.take(500).ifBlank { null },
                "calleeId" to callee.userId,
                "calleeName" to callee.displayName.take(80),
                "calleePhotoUrl" to callee.photoUrl.take(500).ifBlank { null },
                "participantUids" to listOf(uid, callee.userId),
                "callType" to callType,
                "status" to "ringing",
                "sdpOffer" to sdpOffer,
                "durationSeconds" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.document(path).set(payload).await()
            Result.success(callId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCallStatus(callId: String, status: String, durationSeconds: Long = 0L): Result<Unit> {
        val path = "calls/$callId"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (status == "connected") {
                updates["sdpAnswer"] = "v=0\no=- ${System.currentTimeMillis()} 2 IN IP4 127.0.0.1\ns=ChattingApp-Answer"
            }
            db.document(path).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- NOTIFICATIONS & REPORTS ---

    fun observeNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/notifications"
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    suspend fun sendNotification(
        recipientId: String,
        senderName: String,
        type: String,
        title: String,
        body: String,
        targetId: String = ""
    ): Result<Unit> {
        val notifId = "notif_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "users/$recipientId/notifications/$notifId"
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "recipientId" to recipientId,
                "senderId" to uid,
                "senderName" to senderName.take(80),
                "type" to type,
                "title" to title.take(120),
                "body" to body.take(300),
                "targetId" to targetId.take(128).ifBlank { null },
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            requireUserId()
            db.document(path).update("read", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitUserReport(reportedUserId: String, reason: String, details: String): Result<Unit> {
        val reportId = "rep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "reports/$reportId"
        return try {
            val uid = requireUserId()
            db.document(path).set(
                mapOf(
                    "reporterId" to uid,
                    "reportedUserId" to reportedUserId,
                    "reason" to reason.trim().take(100),
                    "details" to details.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
