package com.xiaoquexing.app.ui.share

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShareIntentTest {
    @Test
    fun imageShareIntent_isSendJpeg() {
        val intent = ShareViewModel.imageShareIntent(Uri.parse("content://xqx/share.jpg"), "今天很好")
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/jpeg", intent.type)
        assertEquals("今天很好", intent.getStringExtra(Intent.EXTRA_TEXT))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
