package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cleaner.LinkCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CleanLink", appName)
    }

    @Test
    fun `strip utm parameters from marketing link`() {
        val input = "https://example.com/article?utm_source=twitter&utm_medium=social&utm_campaign=launch"
        val result = LinkCleaner.cleanText(input)
        assertEquals("https://example.com/article", result.cleanedUrl)
        assertEquals(3, result.strippedParams.size)
    }

    @Test
    fun `strip facebook fbclid and mibextid`() {
        val input = "https://example.com/shop?id=123&fbclid=IwAR123456789&mibextid=ZbWKwL"
        val result = LinkCleaner.cleanText(input)
        assertEquals("https://example.com/shop?id=123", result.cleanedUrl)
        assertTrue(result.strippedParams.any { it.key == "fbclid" })
        assertTrue(result.strippedParams.any { it.key == "mibextid" })
    }

    @Test
    fun `unwrap facebook link shim and clean destination`() {
        val input = "https://l.facebook.com/l.php?u=https%3A%2F%2Fexample.com%2Fstory%3Futm_source%3Dfb&h=AT1234"
        val result = LinkCleaner.cleanText(input)
        assertEquals("https://example.com/story", result.cleanedUrl)
        assertTrue(result.strippedParams.any { it.key == "utm_source" })
    }

    @Test
    fun `strip tiktok share parameters`() {
        val input = "https://www.tiktok.com/@creator/video/7345678901234567890?_r=1&_t=8Wxyz&is_from_webapp=1&sender_device=pc&share_app_id=1233&share_link_id=abc123xyz"
        val result = LinkCleaner.cleanText(input)
        assertEquals("https://www.tiktok.com/@creator/video/7345678901234567890", result.cleanedUrl)
        assertTrue(result.strippedParams.any { it.key == "_r" })
        assertTrue(result.strippedParams.any { it.key == "_t" })
        assertTrue(result.strippedParams.any { it.key == "share_link_id" })
    }

    @Test
    fun `strip youtube si tracking while keeping video parameter`() {
        val input = "https://youtu.be/dQw4w9WgXcQ?si=abcdef12345&feature=share"
        val result = LinkCleaner.cleanText(input)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", result.cleanedUrl)
        assertTrue(result.strippedParams.any { it.key == "si" })
    }

    @Test
    fun `clean embedded link in message text`() {
        val input = "Hey check this out: https://example.com/post?utm_source=slack it is awesome!"
        val result = LinkCleaner.cleanText(input)
        assertEquals("https://example.com/post", result.cleanedUrl)
        assertEquals("Hey check this out: https://example.com/post it is awesome!", result.cleanedText)
        assertFalse(result.isEntireTextReplaced)
    }

    @Test
    fun `identify facebook share links correctly`() {
        assertTrue(com.example.cleaner.FacebookWebViewExpander.isFacebookShareLink("https://www.facebook.com/share/p/18V4XJ9abc/?mibextid=wwXIfr"))
        assertTrue(com.example.cleaner.FacebookWebViewExpander.isFacebookShareLink("https://facebook.com/share/r/7ZkX9abc/"))
        assertTrue(com.example.cleaner.FacebookWebViewExpander.isFacebookShareLink("https://m.facebook.com/share/v/8YwP2xyz/"))
        assertFalse(com.example.cleaner.FacebookWebViewExpander.isFacebookShareLink("https://facebook.com/zuck/posts/12345"))
    }

    @Test
    fun `facebook share link purges query trackers and triggers red warning when webview disabled`() = kotlinx.coroutines.runBlocking {
        val input = "https://www.facebook.com/share/p/18V4XJ9abc/?mibextid=wwXIfr"
        val result = LinkCleaner.cleanTextAsync(input, context = null, enableFacebookWebView = false)
        assertEquals("https://www.facebook.com/share/p/18V4XJ9abc/", result.cleanedUrl)
        assertEquals(com.example.model.ExpansionWarningType.RED_FACEBOOK_FAILED, result.warningType)
        assertTrue(result.strippedParams.any { it.key == "mibextid" })
    }
}
