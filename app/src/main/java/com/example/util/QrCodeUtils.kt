package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeUtils {

    private const val PREFIX = "chattingapp://user/"

    fun buildUserProfileLink(uid: String): String = "$PREFIX$uid"

    fun parseUserIdFromQr(rawValue: String): String? {
        val trimmed = rawValue.trim()
        if (trimmed.startsWith(PREFIX)) {
            val uid = trimmed.removePrefix(PREFIX).trim()
            if (uid.matches(Regex("^[a-zA-Z0-9_\\-]{4,128}$"))) {
                return uid
            }
        }
        if (trimmed.matches(Regex("^[a-zA-Z0-9_\\-]{8,128}$"))) {
            return trimmed
        }
        return null
    }

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = Color.parseColor("#14072E"),
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }
}
