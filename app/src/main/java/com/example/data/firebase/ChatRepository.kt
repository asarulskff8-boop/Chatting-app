package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.BlockedUser
import com.example.data.model.Contact
import com.example.data.model.Conversation
import com.example.data.model.FriendRequest
import com.example.data.model.MediaFile
import com.example.data.model.Message
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.util.EncodedMediaPayload
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(
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

    // --- USER PROFILE & PRIVATE INFO ---

    fun observeMyProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user profile stream." }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId).snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .withFirestoreLogging(uid, OperationType.GET, path)
        )
    }

    fun observePrivateInfo(userId: String): Flow<UserPrivateInfo?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/private/info"
        emitAll(
            db.collection("users").document(userId).collection("private").document("info").snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserPrivateInfo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .withFirestoreLogging(uid, OperationType.GET, path)
        )
    }

    fun observeDiscoverableUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .whereEqualTo("discoverable", true)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.profileCompleted }
                        .sortedBy { it.displayName.lowercase() }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    suspend fun getUserProfile(targetUid: String): Result<UserProfile?> {
        val path = "users/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank())
            val snap = db.collection("users").document(targetUid).get().await()
            val profile = if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfileSetup(
        displayName: String,
        username: String,
        bio: String,
        statusText: String,
        photoUrl: String,
        avatarMediaId: String,
        phoneNumber: String,
        phoneVerified: Boolean,
        email: String,
        discoverable: Boolean = true
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val publicPath = "users/$uid"
        val privatePath = "users/$uid/private/info"
        return try {
            requireUserId()
            val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "").take(30)
            require(cleanUsername.length >= 3) { "Username must be at least 3 characters." }
            val cleanName = displayName.trim().take(80)
            require(cleanName.isNotEmpty()) { "Full name is required." }

            val publicDoc = db.collection("users").document(uid)
            val existingPublic = publicDoc.get().await()
            val publicCode = existingPublic.getString("publicCode")
                ?: ("CA-" + uid.take(6).uppercase())

            if (!existingPublic.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "username" to cleanUsername,
                    "displayName" to cleanName,
                    "bio" to bio.trim().take(240),
                    "statusText" to statusText.trim().ifBlank { "Connect Beyond Messages." }.take(60),
                    "photoUrl" to photoUrl.take(500),
                    "avatarMediaId" to avatarMediaId.take(128),
                    "publicCode" to publicCode,
                    "online" to true,
                    "profileCompleted" to true,
                    "discoverable" to discoverable,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                publicDoc.set(payload).await()
            } else {
                publicDoc.update(
                    mapOf(
                        "username" to cleanUsername,
                        "displayName" to cleanName,
                        "bio" to bio.trim().take(240),
                        "statusText" to statusText.trim().ifBlank { "Connect Beyond Messages." }.take(60),
                        "photoUrl" to photoUrl.take(500),
                        "avatarMediaId" to avatarMediaId.take(128),
                        "publicCode" to publicCode,
                        "online" to true,
                        "profileCompleted" to true,
                        "discoverable" to discoverable,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val privateDoc = db.collection("users").document(uid).collection("private").document("info")
            val existingPrivate = privateDoc.get().await()
            if (!existingPrivate.exists()) {
                privateDoc.set(
                    mapOf(
                        "userId" to uid,
                        "email" to email.trim().take(120),
                        "phoneNumber" to phoneNumber.trim().take(30),
                        "phoneVerified" to phoneVerified,
                        "biometricEnabled" to false,
                        "privacyLastSeen" to "everyone",
                        "privacyProfilePhoto" to "everyone",
                        "readReceiptsEnabled" to true,
                        "notificationsEnabled" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                privateDoc.update(
                    mapOf(
                        "email" to email.trim().take(120),
                        "phoneNumber" to phoneNumber.trim().take(30),
                        "phoneVerified" to phoneVerified,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$publicPath | $privatePath")
            Result.failure(e)
        }
    }

    suspend fun updatePresence(online: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            val doc = db.collection("users").document(uid)
            val snap = doc.get().await()
            if (snap.exists()) {
                doc.update(
                    mapOf(
                        "online" to online,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(
        discoverable: Boolean,
        privacyLastSeen: String,
        privacyProfilePhoto: String,
        readReceiptsEnabled: Boolean,
        notificationsEnabled: Boolean,
        biometricEnabled: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            requireUserId()
            db.collection("users").document(uid).update(
                mapOf(
                    "discoverable" to discoverable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).collection("private").document("info").update(
                mapOf(
                    "privacyLastSeen" to privacyLastSeen,
                    "privacyProfilePhoto" to privacyProfilePhoto,
                    "readReceiptsEnabled" to readReceiptsEnabled,
                    "notificationsEnabled" to notificationsEnabled,
                    "biometricEnabled" to biometricEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/private/info")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        return try {
            requireUserId()
            db.collection("users").document(uid).collection("private").document("info").delete().await()
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    // --- CONTACTS, BLOCKED & FRIEND REQUESTS ---

    fun observeContacts(userId: String): Flow<List<Contact>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/contacts"
        emitAll(
            db.collection("users").document(userId).collection("contacts").snapshots()
                .map { snap ->
                    snap.toObjects(Contact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/blocked"
        emitAll(
            db.collection("users").document(userId).collection("blocked").snapshots()
                .map { snap ->
                    snap.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName.lowercase() }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeFriendRequests(userId: String): Flow<List<FriendRequest>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "friend_requests"
        emitAll(
            db.collection("friend_requests")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    suspend fun sendFriendRequest(myProfile: UserProfile, target: UserProfile): Result<Unit> {
        val reqId = "${myProfile.userId}_${target.userId}"
        val path = "friend_requests/$reqId"
        return try {
            val uid = requireUserId()
            require(uid == myProfile.userId && target.userId != uid)
            val docRef = db.collection("friend_requests").document(reqId)
            val existing = docRef.get().await()
            if (existing.exists() && existing.getString("status") == "pending") {
                return Result.success(Unit)
            }
            docRef.set(
                mapOf(
                    "fromUid" to uid,
                    "toUid" to target.userId,
                    "fromName" to myProfile.displayName.take(80),
                    "fromUsername" to myProfile.username.take(30),
                    "fromPhotoUrl" to myProfile.photoUrl.take(500),
                    "toName" to target.displayName.take(80),
                    "toUsername" to target.username.take(30),
                    "status" to "pending",
                    "participantUids" to listOf(uid, target.userId),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
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

    suspend fun respondToFriendRequest(request: FriendRequest, accept: Boolean): Result<Unit> {
        val path = "friend_requests/${request.id}"
        return try {
            val uid = requireUserId()
            require(request.toUid == uid)
            val status = if (accept) "accepted" else "rejected"
            db.collection("friend_requests").document(request.id).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (accept) {
                addContactForUser(
                    ownerUid = uid,
                    contactUid = request.fromUid,
                    displayName = request.fromName,
                    username = request.fromUsername,
                    photoUrl = request.fromPhotoUrl
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addContactDirectly(target: UserProfile): Result<Unit> {
        return try {
            val uid = requireUserId()
            addContactForUser(
                ownerUid = uid,
                contactUid = target.userId,
                displayName = target.displayName,
                username = target.username,
                photoUrl = target.photoUrl,
                avatarMediaId = target.avatarMediaId,
                publicCode = target.publicCode
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/contacts")
            Result.failure(e)
        }
    }

    private suspend fun addContactForUser(
        ownerUid: String,
        contactUid: String,
        displayName: String,
        username: String,
        photoUrl: String = "",
        avatarMediaId: String = "",
        publicCode: String = ""
    ) {
        val docRef = db.collection("users").document(ownerUid).collection("contacts").document(contactUid)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            docRef.set(
                mapOf(
                    "contactUid" to contactUid,
                    "displayName" to displayName.take(80),
                    "username" to username.take(30),
                    "photoUrl" to photoUrl.take(500),
                    "avatarMediaId" to avatarMediaId.take(128),
                    "publicCode" to publicCode.take(24),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun blockUser(targetUid: String, displayName: String, reason: String = "Blocked by user"): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/blocked/$targetUid"
        return try {
            requireUserId()
            db.collection("users").document(uid).collection("blocked").document(targetUid).set(
                mapOf(
                    "blockedUid" to targetUid,
                    "displayName" to displayName.take(80),
                    "reason" to reason.take(200),
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

    suspend fun unblockUser(targetUid: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid/blocked/$targetUid"
        return try {
            requireUserId()
            db.collection("users").document(uid).collection("blocked").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- MEDIA UPLOAD & DOWNLOAD ---

    suspend fun uploadMediaPayload(
        payload: EncodedMediaPayload,
        memberUids: List<String>,
        publicReadable: Boolean = false
    ): Result<String> {
        val mediaId = "media_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "media_files/$mediaId"
        return try {
            val uid = requireUserId()
            require(payload.base64Data.length <= 450_000) { "Media exceeds 450KB limit for direct sync." }
            val safeMembers = (memberUids + uid).distinct().take(100)
            db.collection("media_files").document(mediaId).set(
                mapOf(
                    "ownerId" to uid,
                    "memberUids" to safeMembers,
                    "publicReadable" to publicReadable,
                    "mimeType" to payload.mimeType.take(100),
                    "fileName" to payload.fileName.take(200),
                    "sizeBytes" to payload.sizeBytes.coerceIn(0L, 450_000L),
                    "base64Data" to payload.base64Data,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(mediaId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun getMediaFile(mediaId: String): Result<MediaFile?> {
        if (mediaId.isBlank()) return Result.success(null)
        val path = "media_files/$mediaId"
        return try {
            requireUserId()
            val snap = db.collection("media_files").document(mediaId).get().await()
            val media = if (snap.exists()) {
                snap.toObject(MediaFile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(media)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    // --- CONVERSATIONS & MESSAGES ---

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "conversations"
        emitAll(
            db.collection("conversations")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastMessageAt ?: it.updatedAt ?: Timestamp(0, 0) }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    fun observeMessages(conversationId: String, userId: String): Flow<List<Message>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Message::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .withFirestoreLogging(uid, OperationType.LIST, path)
        )
    }

    suspend fun getOrCreateDirectConversation(myProfile: UserProfile, partner: UserProfile): Result<String> {
        val pair = listOf(myProfile.userId, partner.userId).sorted()
        val convId = "dm_${pair[0]}_${pair[1]}"
        val path = "conversations/$convId"
        return try {
            val uid = requireUserId()
            require(uid == myProfile.userId)
            val docRef = db.collection("conversations").document(convId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "type" to "direct",
                        "title" to "",
                        "description" to "",
                        "photoUrl" to "",
                        "createdBy" to uid,
                        "memberUids" to pair,
                        "adminUids" to pair,
                        "pinnedByUids" to emptyList<String>(),
                        "archivedByUids" to emptyList<String>(),
                        "mutedByUids" to emptyList<String>(),
                        "typingUids" to emptyList<String>(),
                        "unreadCounts" to mapOf(pair[0] to 0L, pair[1] to 0L),
                        "lastMessageText" to "Say hello!",
                        "lastMessageType" to "none",
                        "lastMessageSenderId" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupConversation(
        title: String,
        description: String,
        memberUids: List<String>,
        communityId: String = ""
    ): Result<String> {
        val convId = "grp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "conversations/$convId"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Group name is required." }
            val allMembers = (memberUids + uid).distinct().take(50)
            val inviteCode = "GRP-" + convId.takeLast(6).uppercase()
            val unreadMap = allMembers.associateWith { 0L }

            val payload = mutableMapOf<String, Any>(
                "type" to "group",
                "title" to cleanTitle,
                "description" to description.trim().take(300),
                "inviteCode" to inviteCode,
                "createdBy" to uid,
                "memberUids" to allMembers,
                "adminUids" to listOf(uid),
                "pinnedByUids" to emptyList<String>(),
                "archivedByUids" to emptyList<String>(),
                "mutedByUids" to emptyList<String>(),
                "typingUids" to emptyList<String>(),
                "unreadCounts" to unreadMap,
                "lastMessageText" to "Group created",
                "lastMessageType" to "system",
                "lastMessageSenderId" to uid,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (communityId.isNotBlank()) {
                payload["communityId"] = communityId
            }
            db.collection("conversations").document(convId).set(payload).await()
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderProfile: UserProfile,
        type: String,
        text: String,
        mediaId: String? = null,
        mediaName: String? = null,
        mediaMimeType: String? = null,
        mediaSizeBytes: Long? = null,
        audioDurationSec: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationLabel: String? = null,
        sharedContactName: String? = null,
        sharedContactPhone: String? = null,
        replyTo: Message? = null,
        forwardedFrom: String? = null
    ): Result<Unit> {
        val msgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "conversations/${conversation.id}/messages/$msgId"
        return try {
            val uid = requireUserId()
            require(uid == senderProfile.userId)
            val payload = mapOf(
                "conversationId" to conversation.id,
                "senderId" to uid,
                "senderName" to senderProfile.displayName.take(80),
                "senderPhotoUrl" to senderProfile.photoUrl.take(500).ifBlank { null },
                "memberUids" to conversation.memberUids.take(50),
                "type" to type,
                "text" to text.trim().take(4000),
                "mediaId" to mediaId?.takeIf { it.isNotBlank() },
                "mediaName" to mediaName?.takeIf { it.isNotBlank() }?.take(200),
                "mediaMimeType" to mediaMimeType?.takeIf { it.isNotBlank() }?.take(100),
                "mediaSizeBytes" to mediaSizeBytes,
                "audioDurationSec" to audioDurationSec,
                "latitude" to latitude,
                "longitude" to longitude,
                "locationLabel" to locationLabel?.takeIf { it.isNotBlank() }?.take(200),
                "sharedContactName" to sharedContactName?.takeIf { it.isNotBlank() }?.take(100),
                "sharedContactPhone" to sharedContactPhone?.takeIf { it.isNotBlank() }?.take(40),
                "replyToMessageId" to replyTo?.id?.takeIf { it.isNotBlank() },
                "replyToSenderName" to replyTo?.senderName?.takeIf { it.isNotBlank() }?.take(80),
                "replyToText" to replyTo?.text?.takeIf { it.isNotBlank() }?.take(300),
                "forwardedFrom" to forwardedFrom?.takeIf { it.isNotBlank() }?.take(100),
                "reactions" to emptyMap<String, String>(),
                "readByUids" to listOf(uid),
                "starredByUids" to emptyList<String>(),
                "edited" to false,
                "deleted" to false,
                "pinned" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("conversations").document(conversation.id)
                .collection("messages").document(msgId)
                .set(payload)
                .await()

            val preview = when (type) {
                "image" -> "📷 Photo"
                "video" -> "🎥 Video"
                "audio" -> "🎤 Voice message (${audioDurationSec ?: 0}s)"
                "file" -> "📎 ${mediaName ?: "Document"}"
                "contact" -> "👤 Contact: ${sharedContactName ?: "Shared"}"
                "location" -> "📍 Live location"
                "sticker" -> text.ifBlank { "✨ Sticker" }
                else -> text.trim().take(120)
            }
            val updatedUnread = conversation.unreadCounts.toMutableMap()
            conversation.memberUids.forEach { member ->
                if (member == uid) {
                    updatedUnread[member] = 0L
                } else {
                    updatedUnread[member] = (updatedUnread[member] ?: 0L) + 1L
                }
            }
            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    "lastMessageText" to preview.take(500),
                    "lastMessageType" to type,
                    "lastMessageSenderId" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "unreadCounts" to updatedUnread,
                    "typingUids" to (conversation.typingUids - uid),
                    "updatedAt" to FieldValue.serverTimestamp()
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

    suspend fun markConversationRead(conversation: Conversation): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val currentUnread = conversation.unreadCounts[uid] ?: 0L
        if (currentUnread == 0L) return Result.success(Unit)
        val path = "conversations/${conversation.id}"
        return try {
            val updatedUnread = conversation.unreadCounts.toMutableMap().apply { put(uid, 0L) }
            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    "unreadCounts" to updatedUnread,
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

    suspend fun setTypingStatus(conversation: Conversation, isTyping: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val currentlyTyping = uid in conversation.typingUids
        if (currentlyTyping == isTyping) return Result.success(Unit)
        val updatedTyping = if (isTyping) (conversation.typingUids + uid).distinct() else (conversation.typingUids - uid)
        return try {
            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    "typingUids" to updatedTyping,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "conversations/${conversation.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleConversationFlag(
        conversation: Conversation,
        field: String,
        enable: Boolean
    ): Result<Unit> {
        val path = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            val list = when (field) {
                "pinnedByUids" -> conversation.pinnedByUids
                "archivedByUids" -> conversation.archivedByUids
                "mutedByUids" -> conversation.mutedByUids
                else -> return Result.failure(IllegalArgumentException("Unsupported flag $field"))
            }
            val updated = if (enable) (list + uid).distinct() else (list - uid)
            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    field to updated,
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

    suspend fun editMessage(conversationId: String, messageId: String, newText: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            requireUserId()
            val clean = newText.trim().take(4000)
            require(clean.isNotEmpty())
            db.document(path).update(
                mapOf(
                    "text" to clean,
                    "edited" to true,
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

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/$messageId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "text" to "This message was deleted",
                    "deleted" to true,
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

    suspend fun reactToMessage(conversationId: String, message: Message, emoji: String): Result<Unit> {
        val path = "conversations/$conversationId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val updated = message.reactions.toMutableMap()
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

    suspend fun toggleStarMessage(conversationId: String, message: Message): Result<Unit> {
        val path = "conversations/$conversationId/messages/${message.id}"
        return try {
            val uid = requireUserId()
            val updated = if (uid in message.starredByUids) {
                message.starredByUids - uid
            } else {
                (message.starredByUids + uid).distinct()
            }
            db.document(path).update(
                mapOf(
                    "starredByUids" to updated,
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

    suspend fun pinMessageInConversation(conversationId: String, message: Message): Result<Unit> {
        val msgPath = "conversations/$conversationId/messages/${message.id}"
        return try {
            requireUserId()
            val newPinned = !message.pinned
            db.document(msgPath).update(
                mapOf(
                    "pinned" to newPinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "pinnedMessageId" to if (newPinned) message.id else "",
                    "pinnedMessageText" to if (newPinned) message.text.take(300) else "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, msgPath)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(conversationId: String, messages: List<Message>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val unreadFromOthers = messages.filter { it.senderId != uid && uid !in it.readByUids }.takeLast(15)
            unreadFromOthers.forEach { msg ->
                db.collection("conversations").document(conversationId)
                    .collection("messages").document(msg.id)
                    .update(
                        mapOf(
                            "readByUids" to (msg.readByUids + uid).distinct(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages")
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(conversation: Conversation): Result<Unit> {
        val path = "conversations/${conversation.id}"
        return try {
            val uid = requireUserId()
            val newMembers = conversation.memberUids - uid
            val newAdmins = (conversation.adminUids - uid).ifEmpty { newMembers.take(1) }
            if (newMembers.isEmpty()) {
                db.document(path).delete().await()
            } else {
                db.document(path).update(
                    mapOf(
                        "memberUids" to newMembers,
                        "adminUids" to newAdmins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateGroupInfo(conversationId: String, title: String, description: String): Result<Unit> {
        val path = "conversations/$conversationId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf(
                    "title" to title.trim().take(100),
                    "description" to description.trim().take(300),
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
}
