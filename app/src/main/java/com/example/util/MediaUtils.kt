package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max

data class EncodedMediaPayload(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val base64Data: String
)

object MediaUtils {

    private const val MAX_BASE64_LENGTH = 360_000

    fun compressImageUriToPayload(context: Context, uri: Uri): EncodedMediaPayload? {
        return try {
            val resolver = context.contentResolver
            val meta = queryUriMetadata(context, uri)
            val input = resolver.openInputStream(uri) ?: return null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return null

            val maxDim = 720
            val scale = minOf(
                maxDim.toFloat() / max(1, original.width),
                maxDim.toFloat() / max(1, original.height),
                1f
            )
            val targetW = max(1, (original.width * scale).toInt())
            val targetH = max(1, (original.height * scale).toInt())
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }

            var quality = 80
            var bytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 15
            } while (bytes.size * 4 / 3 > MAX_BASE64_LENGTH && quality >= 25)

            if (scaled != original) scaled.recycle()
            original.recycle()

            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (b64.length > MAX_BASE64_LENGTH) return null

            EncodedMediaPayload(
                fileName = meta.first.ifBlank { "photo_${System.currentTimeMillis()}.jpg" },
                mimeType = "image/jpeg",
                sizeBytes = bytes.size.toLong(),
                base64Data = b64
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodeSmallFileUriToPayload(context: Context, uri: Uri): EncodedMediaPayload? {
        return try {
            val (name, size) = queryUriMetadata(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = stream.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > 260_000) break
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            } ?: ByteArray(0)

            val b64 = if (bytes.isNotEmpty() && bytes.size <= 260_000) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                // Large file metadata fallback token so file preview & download state still sync safely
                Base64.encodeToString("META:$name:$size:$mimeType".toByteArray(), Base64.NO_WRAP)
            }

            EncodedMediaPayload(
                fileName = name.ifBlank { "attachment_${System.currentTimeMillis()}" },
                mimeType = mimeType,
                sizeBytes = if (size > 0) size else bytes.size.toLong(),
                base64Data = b64
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodeAudioFileToPayload(file: File): EncodedMediaPayload? {
        return try {
            if (!file.exists()) return null
            val bytes = file.readBytes()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (b64.length > MAX_BASE64_LENGTH) return null
            EncodedMediaPayload(
                fileName = file.name,
                mimeType = "audio/mp4",
                sizeBytes = bytes.size.toLong(),
                base64Data = b64
            )
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64Data: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToTempFile(context: Context, fileName: String, base64Data: String): File? {
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            val file = File(context.cacheDir, safeName)
            file.writeBytes(bytes)
            file
        } catch (_: Exception) {
            null
        }
    }

    fun queryUriMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "file"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: "file"
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        return name to size
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)
    }
}

class VoiceAudioController(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L
    private var player: MediaPlayer? = null

    fun startRecording(): Boolean {
        return try {
            stopRecording()
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(48_000)
            mr.setAudioSamplingRate(22_050)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            outputFile = file
            startTimeMs = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
            false
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        val file = outputFile
        val durationSec = max(1, ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt())
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            file to durationSec
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
            null to 0
        }
    }

    fun playBase64Audio(fileName: String, base64Data: String, onComplete: () -> Unit) {
        stopPlayback()
        val file = MediaUtils.decodeBase64ToTempFile(context, fileName, base64Data) ?: run {
            onComplete()
            return
        }
        try {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    onComplete()
                    stopPlayback()
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            onComplete()
            stopPlayback()
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release()
        player = null
    }
}

object NotificationHelper {
    const val CHANNEL_MESSAGES = "chatting_app_messages"
    const val CHANNEL_CALLS = "chatting_app_calls"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages & Social Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New messages, friend requests, stories, and channel updates"
            }
            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Voice & Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming audio and video call alerts"
            }
            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        channelId: String,
        title: String,
        body: String
    ) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }
}

class BackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.ensureChannels(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "chatting_app_bg_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
