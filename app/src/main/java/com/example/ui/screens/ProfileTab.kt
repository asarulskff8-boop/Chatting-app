package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.BlockedUser
import com.example.data.model.FriendRequest
import com.example.data.model.MediaFile
import com.example.data.model.UserPrivateInfo
import com.example.data.model.UserProfile
import com.example.data.prefs.UserPreferences
import com.example.ui.components.SectionHeader
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightOutline
import com.example.ui.theme.LightSurface
import com.example.ui.theme.SkyPrimary
import com.example.ui.theme.ThemeMode
import com.example.util.MediaUtils
import com.example.util.QrCodeUtils
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(
    myProfile: UserProfile,
    privateInfo: UserPrivateInfo?,
    friendRequests: List<FriendRequest>,
    blockedUsers: List<BlockedUser>,
    userPrefs: UserPreferences,
    mediaCache: Map<String, MediaFile>,
    onRequestMedia: (String) -> Unit,
    onUpdatePhoto: (Uri) -> Unit,
    onEditProfile: (name: String, bio: String, status: String, phone: String) -> Unit,
    onUpdatePrivacy: (discoverable: Boolean, lastSeen: String, photo: String, readReceipts: Boolean, notifications: Boolean, biometric: Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetWallpaperIndex: (Int) -> Unit,
    onRespondFriendRequest: (FriendRequest, Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onOpenQrScanner: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val apkDownloadUrl = remember { context.getString(R.string.app_download_url) }
    val apkReleasesUrl = remember { context.getString(R.string.app_releases_url) }
    val qrLink = remember(myProfile.userId) { QrCodeUtils.buildUserProfileLink(myProfile.userId) }
    val qrBitmap = remember(qrLink) { QrCodeUtils.generateQrBitmap(qrLink, 420) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUpdatePhoto(uri)
    }

    var showEditSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showBlockedSheet by remember { mutableStateOf(false) }
    var showStorageSheet by remember { mutableStateOf(false) }
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val incomingPending = friendRequests.filter { it.toUid == myProfile.userId && it.status == "pending" }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Profile & QR Identity",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Profile Hero Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.2.dp, LightOutline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        UserAvatar(
                            name = myProfile.displayName,
                            photoUrl = myProfile.photoUrl,
                            avatarMediaId = myProfile.avatarMediaId,
                            mediaCache = mediaCache,
                            onRequestMedia = onRequestMedia,
                            size = 88.dp,
                            showOnline = true,
                            online = true
                        )
                        Surface(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(myProfile.displayName, style = MaterialTheme.typography.titleLarge)
                    Text("@${myProfile.username} • ${myProfile.publicCode}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (myProfile.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(myProfile.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${privateInfo?.email.orEmpty()} ${if (!privateInfo?.phoneNumber.isNullOrBlank()) "• ${privateInfo?.phoneNumber}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "ID: ${myProfile.userId.take(14)}... • Joined ${formatRelativeTime(myProfile.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showEditSheet = true }, shape = CircleShape) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Profile")
                        }
                        Button(onClick = onOpenQrScanner, shape = CircleShape, modifier = Modifier.testTag("profile_scan_qr_button")) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan QR")
                        }
                    }
                }
            }
        }

        // Personal QR Code Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.2.dp, LightOutline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Your Personal QR Code", style = MaterialTheme.typography.titleMedium)
                    Text("Friends can scan this to add you instantly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (qrBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            tonalElevation = 1.dp
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Personal QR Code",
                                modifier = Modifier.size(180.dp).padding(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Profile Link", qrLink))
                            onShowMessage("Profile link copied!")
                        }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Link")
                        }
                        Button(onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, "Connect with me on Chatting App: $qrLink")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Profile QR"))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }

        // App APK Download Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.2.dp, LightOutline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Download App APK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Install Chatting App on any Android device instantly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkDownloadUrl))
                                context.startActivity(intent)
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("download_apk_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download APK")
                        }
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("APK Link", apkDownloadUrl))
                                onShowMessage("Download link copied!")
                            },
                            shape = CircleShape
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Link")
                        }
                    }
                }
            }
        }

        // Incoming Friend Requests
        if (incomingPending.isNotEmpty()) {
            item { SectionHeader(title = "Friend Requests (${incomingPending.size})") }
            items(incomingPending, key = { it.id }) { req ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.fromName, style = MaterialTheme.typography.titleSmall)
                            Text("@${req.fromUsername}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onRespondFriendRequest(req, false) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { onRespondFriendRequest(req, true) }) {
                            Icon(Icons.Rounded.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Settings Section
        item { SectionHeader(title = "Account & App Settings") }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface),
                border = BorderStroke(1.2.dp, LightOutline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SheetActionRow(Icons.Rounded.Security, "Privacy, Security & Notifications") { showPrivacySheet = true }
                    SheetActionRow(Icons.Rounded.LightMode, "Appearance (Cool Slate Light)") {
                        onSetThemeMode(ThemeMode.LIGHT)
                        onShowMessage("Cool Slate Light & Royal Blue theme is active")
                    }
                    SheetActionRow(Icons.Rounded.Wallpaper, "Chat Style (Cool Slate)") { showWallpaperSheet = true }
                    SheetActionRow(Icons.Rounded.Language, "Language (${userPrefs.languageCode})") { showLanguageSheet = true }
                    SheetActionRow(Icons.Rounded.Block, "Blocked Contacts (${blockedUsers.size})") { showBlockedSheet = true }
                    SheetActionRow(Icons.Rounded.Storage, "Storage & Cache Management") { showStorageSheet = true }
                    SheetActionRow(Icons.Rounded.Download, "Download Latest APK") {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkDownloadUrl)))
                        }.onFailure {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkReleasesUrl)))
                            }.onFailure {
                                onShowMessage("Open Releases in browser: $apkReleasesUrl")
                            }
                        }
                    }
                    SheetActionRow(Icons.Rounded.Share, "Share App Download Link") {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Chatting App APK", apkDownloadUrl))
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, "Download Chatting App (Android APK): $apkDownloadUrl")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Chatting App APK"))
                    }
                    SheetActionRow(Icons.AutoMirrored.Rounded.Help, "Help & Support") { showHelpSheet = true }
                    SheetActionRow(Icons.AutoMirrored.Rounded.Logout, "Log Out", tint = MaterialTheme.colorScheme.primary) { onLogout() }
                    SheetActionRow(Icons.Rounded.DeleteForever, "Delete Account", tint = MaterialTheme.colorScheme.error) { showDeleteConfirm = true }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
    }

    if (showEditSheet) {
        var name by remember(myProfile.displayName) { mutableStateOf(myProfile.displayName) }
        var bio by remember(myProfile.bio) { mutableStateOf(myProfile.bio) }
        var status by remember(myProfile.statusText) { mutableStateOf(myProfile.statusText) }
        var phone by remember(privateInfo?.phoneNumber) { mutableStateOf(privateInfo?.phoneNumber.orEmpty()) }
        ModalBottomSheet(onDismissRequest = { showEditSheet = false }) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Edit Profile", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onEditProfile(name, bio, status, phone)
                        showEditSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Profile") }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showPrivacySheet) {
        var discoverable by remember(myProfile.discoverable) { mutableStateOf(myProfile.discoverable) }
        var readReceipts by remember(privateInfo?.readReceiptsEnabled) { mutableStateOf(privateInfo?.readReceiptsEnabled ?: true) }
        var notifications by remember(privateInfo?.notificationsEnabled) { mutableStateOf(privateInfo?.notificationsEnabled ?: true) }
        var biometric by remember(privateInfo?.biometricEnabled) { mutableStateOf(privateInfo?.biometricEnabled ?: false) }
        var lastSeenVis by remember(privateInfo?.privacyLastSeen) { mutableStateOf(privateInfo?.privacyLastSeen ?: "everyone") }
        ModalBottomSheet(onDismissRequest = { showPrivacySheet = false }) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Privacy, Security & Notifications", style = MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Discoverable in directory")
                    Switch(checked = discoverable, onCheckedChange = { discoverable = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Read Receipts")
                    Switch(checked = readReceipts, onCheckedChange = { readReceipts = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Push & Local Notifications")
                    Switch(checked = notifications, onCheckedChange = { notifications = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Biometric App Lock")
                    Switch(checked = biometric, onCheckedChange = { biometric = it })
                }
                Text("Last Seen Visibility", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("everyone", "contacts", "nobody").forEach { opt ->
                        FilterChip(selected = lastSeenVis == opt, onClick = { lastSeenVis = opt }, label = { Text(opt.replaceFirstChar { it.uppercase() }) })
                    }
                }
                Button(
                    onClick = {
                        onUpdatePrivacy(discoverable, lastSeenVis, lastSeenVis, readReceipts, notifications, biometric)
                        showPrivacySheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Save Security Settings") }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Select Language", style = MaterialTheme.typography.titleLarge)
                listOf("English", "Spanish", "French", "German", "Hindi", "Arabic", "Japanese").forEach { lang ->
                    SheetActionRow(Icons.Rounded.Language, lang) {
                        onSetLanguage(lang)
                        showLanguageSheet = false
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showWallpaperSheet) {
        ModalBottomSheet(onDismissRequest = { showWallpaperSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Cool Slate Chat Theme Style", style = MaterialTheme.typography.titleLarge)
                listOf("Cool Slate Minimal", "Royal Breeze Mist", "Soft Ice Slate").forEachIndexed { idx, label ->
                    SheetActionRow(Icons.Rounded.Wallpaper, label) {
                        onSetWallpaperIndex(idx)
                        showWallpaperSheet = false
                        onShowMessage("Chat style updated to $label")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showBlockedSheet) {
        ModalBottomSheet(onDismissRequest = { showBlockedSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Blocked Contacts", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                if (blockedUsers.isEmpty()) {
                    Text("You have no blocked contacts.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    blockedUsers.forEach { b ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(b.displayName, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onUnblockUser(b.blockedUid) }) { Text("Unblock") }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showStorageSheet) {
        val cacheBytes = remember(context.cacheDir) {
            context.cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        }
        AlertDialog(
            onDismissRequest = { showStorageSheet = false },
            title = { Text("Storage Management") },
            text = { Text("Cached voice & media files: ${MediaUtils.formatFileSize(cacheBytes)}\nCached media items in memory: ${mediaCache.size}") },
            confirmButton = {
                Button(onClick = {
                    context.cacheDir.listFiles()?.forEach { it.delete() }
                    showStorageSheet = false
                    onShowMessage("Local media cache cleared")
                }) { Text("Clear Cache") }
            },
            dismissButton = { TextButton(onClick = { showStorageSheet = false }) { Text("Close") } }
        )
    }

    if (showHelpSheet) {
        AlertDialog(
            onDismissRequest = { showHelpSheet = false },
            title = { Text("Help & Support") },
            text = { Text("Chatting App v1.0\nConnect Beyond Messages.\n\n• Real-time Firestore messaging & media sync\n• Google Sign-In via Android Credential Manager\n• QR Code friend discovery & CameraX video calling\n\nOfficial APK Releases:\n$apkReleasesUrl") },
            confirmButton = { TextButton(onClick = { showHelpSheet = false }) { Text("OK") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account?") },
            text = { Text("This removes your profile and private settings from Chatting App and signs you out. This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    onDeleteAccount()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun QrScannerScreen(
    onScannedUid: (String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var manualCode by remember { mutableStateOf("") }
    var alreadyDetected by remember { mutableStateOf(false) }

    val camPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPerm = granted
    }

    val infinite = rememberInfiniteTransition(label = "laser")
    val laserProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "laserY"
    )

    Box(modifier = Modifier.fillMaxSize().background(LightBackground)) {
        if (hasCameraPerm) {
            val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                update = { previewView ->
                    val cameraFuture = ProcessCameraProvider.getInstance(context)
                    cameraFuture.addListener({
                        val provider = cameraFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val scanner = BarcodeScanning.getClient()
                        val analysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !alreadyDetected) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val match = barcodes.firstNotNullOfOrNull { bc ->
                                            bc.rawValue?.let { QrCodeUtils.parseUserIdFromQr(it) }
                                        }
                                        if (match != null && !alreadyDetected) {
                                            alreadyDetected = true
                                            onScannedUid(match)
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LightSurface.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        "Scan Friend QR Code",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (hasCameraPerm) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .border(3.dp, SkyPrimary, RoundedCornerShape(24.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .offset(y = (220 * laserProgress + 12).dp)
                            .height(3.dp)
                            .background(SkyPrimary)
                    )
                }
            } else {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Camera Permission Needed", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Allow camera access to scan a friend's QR code.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { camPermLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Enable Camera")
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        placeholder = { Text("Or paste chattingapp://user/...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = manualCode.isNotBlank(),
                        onClick = {
                            val parsed = QrCodeUtils.parseUserIdFromQr(manualCode)
                            if (parsed != null) onScannedUid(parsed)
                        }
                    ) { Text("Find") }
                }
            }
        }
    }
}
