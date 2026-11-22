package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.MediaUtils
import com.example.util.QrCodeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches Chatting App`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Chatting App", appName)
  }

  @Test
  fun `qr profile deep link round trips user id`() {
    val uid = "user_abc123XYZ"
    val link = QrCodeUtils.buildUserProfileLink(uid)
    assertEquals("chattingapp://user/$uid", link)
    assertEquals(uid, QrCodeUtils.parseUserIdFromQr(link))
    assertEquals(uid, QrCodeUtils.parseUserIdFromQr("  $uid  "))
    assertNull(QrCodeUtils.parseUserIdFromQr("https://example.com/not-a-user"))
  }

  @Test
  fun `formatFileSize formats bytes kilobytes and megabytes`() {
    assertEquals("512 B", MediaUtils.formatFileSize(512))
    assertEquals("2.0 KB", MediaUtils.formatFileSize(2048))
  }
}
