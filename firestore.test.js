const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const CHARLIE_UID = "charlie_uid_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const pastDate = (offsetMs = 2000) => new Date(Date.now() - offsetMs);

test("Unauthenticated users are denied across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("conversations").get());
  await assertFails(unauth.collection("stories").get());
  await assertFails(unauth.collection("channels").get());
  await assertFails(unauth.collection("calls").get());
  await assertFails(
    unauth.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice",
      displayName: "Alice",
      online: true,
      profileCompleted: true,
      discoverable: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("UserProfile and PII PrivateInfo split isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(5000);

  // Create public profile (Kotlin-aligned payload, no `id` field)
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_chat",
      displayName: "Alice Rivera",
      bio: "Hello!",
      statusText: "Connect Beyond Messages.",
      photoUrl: "",
      avatarMediaId: "",
      publicCode: "CA-ALICE1",
      online: true,
      profileCompleted: true,
      discoverable: true,
      lastSeenAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Create private PII doc
  await assertSucceeds(
    alice
      .collection("users")
      .doc(ALICE_UID)
      .collection("private")
      .doc("info")
      .set({
        userId: ALICE_UID,
        email: "alice@example.com",
        phoneNumber: "+15550101",
        phoneVerified: true,
        biometricEnabled: false,
        privacyLastSeen: "everyone",
        privacyProfilePhoto: "everyone",
        readReceiptsEnabled: true,
        notificationsEnabled: true,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Bob can get Alice's public profile and query discoverable users
  await assertSucceeds(bob.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    bob.collection("users").where("discoverable", "==", true).limit(50).get()
  );

  // Bob CANNOT read or write Alice's private PII doc
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("private").doc("info").get()
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).update({ displayName: "Hacked" })
  );

  // Reject ghost field & @DocumentId `id` injection
  await assertFails(
    bob.collection("users").doc(BOB_UID).set({
      id: BOB_UID,
      userId: BOB_UID,
      username: "bob_chat",
      displayName: "Bob",
      online: true,
      profileCompleted: true,
      discoverable: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject future timestamp tampering
  await assertFails(
    bob.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_chat",
      displayName: "Bob",
      online: true,
      profileCompleted: true,
      discoverable: true,
      createdAt: new Date(Date.now() + 120000),
      updatedAt: now,
    })
  );
});

test("Friend requests: create, respond, terminal lock, and cross-user isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(5000);
  const reqId = `${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    alice.collection("friend_requests").doc(reqId).set({
      fromUid: ALICE_UID,
      toUid: BOB_UID,
      fromName: "Alice",
      fromUsername: "alice_chat",
      fromPhotoUrl: "",
      toName: "Bob",
      toUsername: "bob_chat",
      status: "pending",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Queries aligned to member participation succeed; unfiltered fail
  await assertSucceeds(
    bob
      .collection("friend_requests")
      .where("participantUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(charlie.collection("friend_requests").get());
  await assertFails(charlie.collection("friend_requests").doc(reqId).get());

  // Bob accepts request
  await assertSucceeds(
    bob.collection("friend_requests").doc(reqId).update({
      status: "accepted",
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state locking: cannot modify after accepted
  await assertFails(
    bob.collection("friend_requests").doc(reqId).update({
      status: "rejected",
      updatedAt: pastDate(500),
    })
  );
});

test("Conversations, Messages, and MediaFiles: member isolation, reactions, edit & delete", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(10000);
  const convId = `dm_${ALICE_UID}_${BOB_UID}`;

  await assertSucceeds(
    alice.collection("conversations").doc(convId).set({
      type: "direct",
      title: "",
      description: "",
      photoUrl: "",
      createdBy: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID, BOB_UID],
      pinnedByUids: [],
      archivedByUids: [],
      mutedByUids: [],
      typingUids: [],
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      lastMessageText: "Say hello!",
      lastMessageType: "none",
      lastMessageSenderId: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Member query succeeds; non-member fails
  await assertSucceeds(
    bob
      .collection("conversations")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(charlie.collection("conversations").doc(convId).get());

  // Upload media payload
  const mediaId = "media_abc123";
  await assertSucceeds(
    alice.collection("media_files").doc(mediaId).set({
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID, BOB_UID],
      publicReadable: false,
      mimeType: "image/jpeg",
      fileName: "photo.jpg",
      sizeBytes: 1024,
      base64Data: "AQIDBA==",
      createdAt: now,
    })
  );
  await assertSucceeds(bob.collection("media_files").doc(mediaId).get());
  await assertFails(charlie.collection("media_files").doc(mediaId).get());

  // Send message
  const msgRef = alice
    .collection("conversations")
    .doc(convId)
    .collection("messages")
    .doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      conversationId: convId,
      senderId: ALICE_UID,
      senderName: "Alice",
      memberUids: [ALICE_UID, BOB_UID],
      type: "text",
      text: "Hey Bob!",
      reactions: {},
      readByUids: [ALICE_UID],
      starredByUids: [],
      edited: false,
      deleted: false,
      pinned: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can list messages and add reaction, but CANNOT edit Alice's text
  await assertSucceeds(
    bob
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertSucceeds(
    bob
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc("msg_1")
      .update({
        reactions: { [BOB_UID]: "❤️" },
        readByUids: [ALICE_UID, BOB_UID],
        updatedAt: pastDate(1000),
      })
  );
  await assertFails(
    bob
      .collection("conversations")
      .doc(convId)
      .collection("messages")
      .doc("msg_1")
      .update({
        text: "Spoofed!",
        edited: true,
        updatedAt: pastDate(1000),
      })
  );

  // Alice can edit her own message
  await assertSucceeds(
    msgRef.update({
      text: "Hey Bob! Updated.",
      edited: true,
      updatedAt: pastDate(500),
    })
  );
});

test("Stories, Channels, Communities, Calls, Notifications, and Reports", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(5000);

  // Story
  await assertSucceeds(
    alice.collection("stories").doc("story_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice_chat",
      type: "text",
      caption: "Building Chatting App!",
      backgroundColorHex: "#4C1D95",
      visibility: "everyone",
      visibleToUids: [ALICE_UID, BOB_UID],
      viewerUids: [ALICE_UID],
      reactions: {},
      expiresAtMs: Date.now() + 86400000,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bob.collection("stories").where("visibility", "==", "everyone").get()
  );
  await assertSucceeds(
    bob.collection("stories").doc("story_1").update({
      viewerUids: [ALICE_UID, BOB_UID],
      reactions: { [BOB_UID]: "🔥" },
      updatedAt: pastDate(1000),
    })
  );

  // Channel & Post
  await assertSucceeds(
    alice.collection("channels").doc("ch_1").set({
      name: "Product Updates",
      description: "Official news",
      ownerId: ALICE_UID,
      adminUids: [ALICE_UID],
      subscriberUids: [ALICE_UID],
      subscriberCount: 1,
      visibility: "public",
      inviteCode: "CH-123456",
      commentsEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bob.collection("channels").where("visibility", "==", "public").get()
  );
  await assertSucceeds(
    bob.collection("channels").doc("ch_1").update({
      subscriberUids: [ALICE_UID, BOB_UID],
      subscriberCount: 2,
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    alice.collection("channels").doc("ch_1").collection("posts").doc("post_1").set({
      channelId: "ch_1",
      channelOwnerId: ALICE_UID,
      channelVisibility: "public",
      subscriberUids: [ALICE_UID, BOB_UID],
      authorId: ALICE_UID,
      authorName: "Alice",
      text: "Welcome to our channel!",
      reactions: {},
      commentCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Community
  await assertSucceeds(
    alice.collection("communities").doc("comm_1").set({
      name: "Design Guild",
      description: "All design groups",
      ownerId: ALICE_UID,
      adminUids: [ALICE_UID],
      memberUids: [ALICE_UID, BOB_UID],
      groupIds: [],
      announcementText: "Welcome!",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bob.collection("communities").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(charlie.collection("communities").doc("comm_1").get());

  // Call session + terminal state lock
  await assertSucceeds(
    alice.collection("calls").doc("call_1").set({
      callerId: ALICE_UID,
      callerName: "Alice",
      calleeId: BOB_UID,
      calleeName: "Bob",
      participantUids: [ALICE_UID, BOB_UID],
      callType: "video",
      status: "ringing",
      sdpOffer: "v=0",
      durationSeconds: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(charlie.collection("calls").doc("call_1").get());
  await assertSucceeds(
    bob.collection("calls").doc("call_1").update({
      status: "connected",
      sdpAnswer: "v=0",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    alice.collection("calls").doc("call_1").update({
      status: "ended",
      durationSeconds: 42,
      updatedAt: pastDate(500),
    })
  );
  // Terminal call cannot be reopened
  await assertFails(
    bob.collection("calls").doc("call_1").update({
      status: "connected",
      updatedAt: pastDate(100),
    })
  );

  // Notifications & Reports
  await assertSucceeds(
    alice.collection("users").doc(BOB_UID).collection("notifications").doc("n_1").set({
      recipientId: BOB_UID,
      senderId: ALICE_UID,
      senderName: "Alice",
      type: "message",
      title: "New Message",
      body: "Hey Bob!",
      read: false,
      createdAt: now,
    })
  );
  await assertSucceeds(
    bob.collection("users").doc(BOB_UID).collection("notifications").doc("n_1").update({
      read: true,
    })
  );
  await assertFails(
    charlie.collection("users").doc(BOB_UID).collection("notifications").get()
  );
  await assertSucceeds(
    alice.collection("reports").doc("rep_1").set({
      reporterId: ALICE_UID,
      reportedUserId: CHARLIE_UID,
      reason: "Spam",
      details: "Unsolicited messages",
      createdAt: now,
    })
  );
  await assertFails(bob.collection("reports").doc("rep_1").get());
});
