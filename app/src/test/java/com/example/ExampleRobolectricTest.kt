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
        assertEquals("ZnepLinkCleaner", appName)
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

    @Test
    fun `strip instagram exln and related meta trackers`() {
        val input = "https://www.instagram.com/reel/C3_test/?igsh=MTNmeXFuZzF3ejJveA==&exln=MTNmeXFuZzF3ejJveA==&obrf=1&vrfl=abc"
        val result = LinkCleaner.cleanText(input)
        assertEquals("https://www.instagram.com/reel/C3_test/", result.cleanedUrl)
        assertTrue(result.strippedParams.any { it.key == "exln" })
        assertTrue(result.strippedParams.any { it.key == "igsh" })
        assertTrue(result.strippedParams.any { it.key == "obrf" })
        assertTrue(result.strippedParams.any { it.key == "vrfl" })
    }

    @Test
    fun `strip threads xmt and slof trackers`() {
        val input = "https://www.threads.net/@user/post/DFxyz123/?xmt=AQG1234567890&slof=1"
        val result = LinkCleaner.cleanText(input)
        assertEquals("https://www.threads.net/@user/post/DFxyz123/", result.cleanedUrl)
        assertTrue(result.strippedParams.any { it.key == "xmt" })
        assertTrue(result.strippedParams.any { it.key == "slof" })
    }

    @Test
    fun `strip reddit shrid and tiktok u_code`() {
        val redditInput = "https://www.reddit.com/r/androiddev/comments/12345/awesome_tool/?shrid=abc12345&share_id=def678"
        val redditResult = LinkCleaner.cleanText(redditInput)
        assertEquals("https://www.reddit.com/r/androiddev/comments/12345/awesome_tool/", redditResult.cleanedUrl)
        assertTrue(redditResult.strippedParams.any { it.key == "shrid" })

        val tiktokInput = "https://www.tiktok.com/@creator/video/987654321?u_code=d8f7e6d5&preview_pb=0&ttclid=E_1234"
        val tiktokResult = LinkCleaner.cleanText(tiktokInput)
        assertEquals("https://www.tiktok.com/@creator/video/987654321", tiktokResult.cleanedUrl)
        assertTrue(tiktokResult.strippedParams.any { it.key == "u_code" })
        assertTrue(tiktokResult.strippedParams.any { it.key == "ttclid" })
    }

    @Test
    fun `block SSRF private and loopback IPs and non-http schemes`() {
        assertTrue(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("127.0.0.1"))
        assertTrue(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("10.0.0.1"))
        assertTrue(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("172.16.5.4"))
        assertTrue(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("192.168.1.1"))
        assertTrue(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("169.254.169.254"))
        assertTrue(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("localhost"))
        assertTrue(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("router.local"))
        assertTrue(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("[::1]"))
        assertFalse(com.example.cleaner.UrlSecurityValidator.isPrivateOrLoopbackHost("example.com"))

        // Offline shim attempting to redirect to private LAN IP or javascript: URI must be blocked
        val ssrfShim = "https://l.facebook.com/l.php?u=http%3A%2F%2F169.254.169.254%2Flatest%2Fmeta-data%2F"
        val ssrfResult = LinkCleaner.cleanText(ssrfShim)
        assertEquals(com.example.model.ExpansionWarningType.RED_SECURITY_BLOCKED, ssrfResult.warningType)
        assertTrue(ssrfResult.securityAdvisories.any { it.code == "BLOCKED_UNSAFE_REDIRECT" })

        val jsShim = "https://www.google.com/url?q=javascript%3Aalert(1)"
        val jsResult = LinkCleaner.cleanText(jsShim)
        assertEquals(com.example.model.ExpansionWarningType.RED_SECURITY_BLOCKED, jsResult.warningType)
    }

    @Test
    fun `strip user@host credentials and detect homograph and apk phishing risks`() {
        val spoofInput = "https://google.com:login@evil-phish.example.com/update.apk?utm_source=phish"
        val spoofResult = LinkCleaner.cleanText(spoofInput)
        assertEquals("https://evil-phish.example.com/update.apk", spoofResult.cleanedUrl)
        assertTrue(spoofResult.strippedParams.any { it.key == "userInfo@" })
        assertTrue(spoofResult.securityAdvisories.any { it.code == "EMBEDDED_CREDENTIALS_SPOOF" })
        assertTrue(spoofResult.securityAdvisories.any { it.code == "EXECUTABLE_DOWNLOAD_RISK" })

        val homographInput = "http://xn--pple-43d.com/login?fbclid=123"
        val homographResult = LinkCleaner.cleanText(homographInput)
        assertEquals("http://xn--pple-43d.com/login", homographResult.cleanedUrl)
        assertTrue(homographResult.securityAdvisories.any { it.code == "IDN_HOMOGRAPH_RISK" })
        assertTrue(homographResult.securityAdvisories.any { it.code == "CLEARTEXT_HTTP" })
    }

    @Test
    fun `enforce Facebook WebView HTTPS domain sandbox and Offline-Only mode`() = kotlinx.coroutines.runBlocking {
        assertTrue(com.example.cleaner.UrlSecurityValidator.isAllowedFacebookWebViewUrl("https://www.facebook.com/share/p/18V4XJ9abc/"))
        assertTrue(com.example.cleaner.UrlSecurityValidator.isAllowedFacebookWebViewUrl("https://m.facebook.com/story.php?story_fbid=123&id=456"))
        assertFalse(com.example.cleaner.UrlSecurityValidator.isAllowedFacebookWebViewUrl("http://www.facebook.com/share/p/18V4XJ9abc/"))
        assertFalse(com.example.cleaner.UrlSecurityValidator.isAllowedFacebookWebViewUrl("https://evil-site.com/share/p/18V4XJ9abc/"))
        assertFalse(com.example.cleaner.UrlSecurityValidator.isAllowedFacebookWebViewUrl("file:///android_asset/exploit.html"))

        val shortlink = "https://bit.ly/3xyzabc?utm_source=test"
        val offlineResult = LinkCleaner.cleanTextAsync(shortlink, offlineOnlyMode = true)
        assertTrue(offlineResult.isOfflineDeferredShortlink)
        assertEquals(com.example.model.ExpansionWarningType.ORANGE_UNEXPANDED_SHORTLINK, offlineResult.warningType)
        assertEquals("https://bit.ly/3xyzabc", offlineResult.cleanedUrl)
    }

    @Test
    fun `detect unchanged clean links vs modified links`() {
        val cleanInput = "https://znep.com/projects/linkcleaner"
        val cleanRes = LinkCleaner.cleanText(cleanInput)
        assertTrue(cleanRes.isUnchanged)
        assertEquals(cleanInput, cleanRes.cleanedUrl)
        assertEquals(0, cleanRes.bytesSaved)

        val dirtyInput = "https://znep.com/projects/linkcleaner?utm_source=newsletter"
        val dirtyRes = LinkCleaner.cleanText(dirtyInput)
        assertFalse(dirtyRes.isUnchanged)
        assertEquals("https://znep.com/projects/linkcleaner", dirtyRes.cleanedUrl)
    }

    @Test
    fun `unwrap Google AMP cache and condense Amazon ASIN paths with hop trace`() {
        val ampInput = "https://www-theverge-com.cdn.ampproject.org/v/s/www.theverge.com/2026/11/02/tech-news/amp?utm_source=twitter&amp=1"
        val ampRes = LinkCleaner.cleanText(ampInput)
        assertEquals("https://www.theverge.com/2026/11/02/tech-news", ampRes.cleanedUrl)
        assertFalse(ampRes.isUnchanged)
        assertTrue(ampRes.redirectHops.size >= 2)
        assertEquals("AMP Cache Unwrap", ampRes.redirectHops.last().mechanism)

        val amznInput = "https://www.amazon.com/Apple-AirPods-Wireless-Earbuds-Charging/dp/B08N5WRWNW/ref=sr_1_1?crid=289EHQ99W&keywords=airpods"
        val amznRes = LinkCleaner.cleanText(amznInput)
        assertEquals("https://www.amazon.com/dp/B08N5WRWNW", amznRes.cleanedUrl)
        assertFalse(amznRes.isUnchanged)
        assertTrue(amznRes.strippedParams.any { it.key == "amazon_path_slug" })
    }

    @Test
    fun `decode facebook native_post base64 deep link and strip wtsid tracker`() {
        val fbDeepLink = "fb://native_post/UzpfSTEwMDA2NTU0MDM4NTAxOToxNDMzNTc5NTM1NTAzMzc5OjE0MzM1Nzk1MzU1MDMzNzk=?wtsid=wt_0DfZb2qGHBljgYsBz"
        val extracted = com.example.cleaner.FacebookWebViewExpander.extractPermalinkFromFacebookDeepLink(
            fbDeepLink,
            preserveQuery = true
        )
        assertEquals(
            "https://www.facebook.com/100065540385019/posts/1433579535503379?wtsid=wt_0DfZb2qGHBljgYsBz",
            extracted
        )

        val cleaned = LinkCleaner.cleanText(fbDeepLink)
        assertEquals("https://www.facebook.com/100065540385019/posts/1433579535503379", cleaned.cleanedUrl)
        assertTrue(cleaned.strippedParams.any { it.key == "wtsid" })
        assertEquals(com.example.model.ExpansionWarningType.NONE, cleaned.warningType)
    }

    @Test
    fun `detect partial scheme selections like https and handle ACTION_PROCESS_TEXT gracefully`() {
        assertTrue(LinkCleaner.isPartialUrlSelection("https"))
        assertTrue(LinkCleaner.isPartialUrlSelection("http"))
        assertTrue(LinkCleaner.isPartialUrlSelection("https:"))
        assertTrue(LinkCleaner.isPartialUrlSelection("https://"))
        assertTrue(LinkCleaner.isPartialUrlSelection("fb://"))
        assertTrue(LinkCleaner.isPartialUrlSelection("www."))
        assertFalse(LinkCleaner.isPartialUrlSelection("https://example.com?utm_source=test"))
        assertFalse(LinkCleaner.isPartialUrlSelection("hello world"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Zlc", context.getString(R.string.process_text_label))
        assertEquals("Zlc Clean", context.getString(R.string.link_handler_label))

        // Partial "https" selection in ACTION_PROCESS_TEXT should cancel in-place replacement, show Toast, and finish immediately
        val partialIntent = android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_PROCESS_TEXT, "https")
            putExtra(android.content.Intent.EXTRA_PROCESS_TEXT_READONLY, false)
        }
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java, partialIntent).use { controller ->
            controller.create()
            val activity = controller.get()
            assertTrue(activity.isFinishing)
            val shadowActivity = org.robolectric.Shadows.shadowOf(activity)
            assertEquals(android.app.Activity.RESULT_CANCELED, shadowActivity.resultCode)
            val toastText = org.robolectric.shadows.ShadowToast.getTextOfLatestToast()
            assertTrue(toastText != null && toastText.contains("Only 'https' was selected"))
        }

        // Complete URL selection in editable ACTION_PROCESS_TEXT should return RESULT_OK with cleaned URL
        val fullUrlIntent = android.content.Intent(android.content.Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/html"
            putExtra(android.content.Intent.EXTRA_PROCESS_TEXT, "https://example.com/page?utm_source=newsletter&fbclid=123")
            putExtra(android.content.Intent.EXTRA_PROCESS_TEXT_READONLY, false)
        }
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java, fullUrlIntent).use { controller ->
            controller.create()
            val activity = controller.get()
            assertTrue(activity.isFinishing)
            val shadowActivity = org.robolectric.Shadows.shadowOf(activity)
            assertEquals(android.app.Activity.RESULT_OK, shadowActivity.resultCode)
            assertEquals(
                "https://example.com/page",
                shadowActivity.resultIntent.getStringExtra(android.content.Intent.EXTRA_PROCESS_TEXT)
            )
        }
    }

    @Test
    fun `detect and clean reddit opaque s share links and preserve comment thread paths`() {
        val subredditShare = "https://www.reddit.com/r/Seattle/s/XXXXXXXXXX"
        val userShare = "https://www.reddit.com/u/spez/s/YYYYYYYYYY"
        val userFullShare = "https://old.reddit.com/user/SeattleMod/s/ZZZZZZZZZZ/"
        val canonicalPost = "https://www.reddit.com/r/Seattle/comments/1abc234/sunset_over_puget_sound_from_kerry_park/"

        assertTrue(com.example.cleaner.UrlExpander.isRedditOpaqueShareLink(subredditShare))
        assertTrue(com.example.cleaner.UrlExpander.isRedditOpaqueShareLink(userShare))
        assertTrue(com.example.cleaner.UrlExpander.isRedditOpaqueShareLink(userFullShare))
        assertFalse(com.example.cleaner.UrlExpander.isRedditOpaqueShareLink(canonicalPost))

        assertTrue(com.example.cleaner.UrlExpander.shouldExpand(subredditShare))
        assertTrue(com.example.cleaner.UrlExpander.shouldExpand(userShare))
        assertTrue(com.example.cleaner.UrlExpander.isOpaqueShortener(subredditShare))
        assertTrue(com.example.cleaner.UrlExpander.isOpaqueShortener(userShare))
        assertFalse(com.example.cleaner.UrlExpander.shouldExpand(canonicalPost))

        // Verify HTML canonical fallback extraction for Reddit /s/ pages returning 200 HTML
        val mockHtml = """
            <html><head>
            <link rel="canonical" href="https://www.reddit.com/r/Seattle/comments/1abc234/sunset_over_puget_sound_from_kerry_park/?%24deep_link=true&amp;correlation_id=9f8e7d6c-5b4a-3210&amp;ref=share&amp;ref_source=link" />
            </head></html>
        """.trimIndent()
        val extractedCanonical = com.example.cleaner.UrlExpander.extractCanonicalFromHtmlBody(subredditShare, mockHtml)
        assertEquals(
            "https://www.reddit.com/r/Seattle/comments/1abc234/sunset_over_puget_sound_from_kerry_park/?%24deep_link=true&correlation_id=9f8e7d6c-5b4a-3210&ref=share&ref_source=link",
            extractedCanonical
        )

        // Verify post permalink after /s/ redirect strips all Reddit & Branch.io deep-link telemetry
        val redirectedPostUrl =
            "https://www.reddit.com/r/Seattle/comments/1abc234/sunset_over_puget_sound_from_kerry_park/?%24deep_link=true&correlation_id=9f8e7d6c-5b4a-3210&ref=share&ref_campaign=share_button&ref_source=link&share_id=XyZ987654&shrid=ab12cd34&_branch_match_id=123456789&utm_source=share&utm_medium=android_app"
        val cleanedPost = LinkCleaner.cleanText(redirectedPostUrl)
        assertEquals(
            "https://www.reddit.com/r/Seattle/comments/1abc234/sunset_over_puget_sound_from_kerry_park/",
            cleanedPost.cleanedUrl
        )
        assertTrue(cleanedPost.strippedParams.any { it.key == "\$deep_link" })
        assertTrue(cleanedPost.strippedParams.any { it.key == "correlation_id" })
        assertTrue(cleanedPost.strippedParams.any { it.key == "ref" })
        assertTrue(cleanedPost.strippedParams.any { it.key == "ref_source" })
        assertTrue(cleanedPost.strippedParams.any { it.key == "_branch_match_id" })

        // Verify comment thread permalink preserves readable path + comment ID while stripping tracking params
        val redirectedCommentUrl =
            "https://www.reddit.com/r/Seattle/comments/1abc234/sunset_over_puget_sound_from_kerry_park/k9xyz12/?%24deep_link=true&correlation_id=11223344&ref=share&ref_source=link&share_id=abc999&utm_name=androidcss"
        val cleanedComment = LinkCleaner.cleanText(redirectedCommentUrl)
        assertEquals(
            "https://www.reddit.com/r/Seattle/comments/1abc234/sunset_over_puget_sound_from_kerry_park/k9xyz12/",
            cleanedComment.cleanedUrl
        )
    }

    @Test
    fun `detect paywalled publishers offline and analyze live HTTP and HTML paywall or region-lock signals`() {
        val nytArticle = "https://www.nytimes.com/2026/11/10/technology/ai-chips-energy-grid.html"
        val offlineNyt = com.example.cleaner.AccessRestrictionDetector.assessOffline(nytArticle, offlineOnlyMode = true)
        assertTrue(offlineNyt.isRestricted)
        assertEquals(com.example.model.AccessRestrictionType.PAYWALL, offlineNyt.restrictionType)
        assertEquals(com.example.model.DetectionConfidence.OFFLINE_LIKELY, offlineNyt.confidence)
        assertTrue(offlineNyt.signals.any { it.contains("The New York Times") })

        // Bare homepage should not falsely flag offline
        val nytHome = com.example.cleaner.AccessRestrictionDetector.assessOffline("https://www.nytimes.com/", offlineOnlyMode = true)
        assertFalse(nytHome.isRestricted)

        // Live HTTP 451 + geo-block HTML should detect REGION_LOCKED with HIGH confidence
        val baseNone = com.example.cleaner.AccessRestrictionDetector.assessOffline("https://example-news.com/story/123")
        val geoResult = com.example.cleaner.AccessRestrictionDetector.analyzeHttpAndHtmlResponse(
            originalUrl = "https://example-news.com/story/123",
            statusCode = 451,
            finalUrl = "https://example-news.com/story/123",
            htmlBody = "<html><head><title>451 Unavailable For Legal Reasons</title></head><body>This content is not available in your region.</body></html>",
            offlineBase = baseNone
        )
        assertEquals(com.example.model.AccessRestrictionType.REGION_LOCKED, geoResult.restrictionType)
        assertEquals(com.example.model.DetectionConfidence.HIGH, geoResult.confidence)
        assertTrue(geoResult.signals.any { it.contains("451") })
        assertEquals(null, geoResult.detectedTitle) // Generic error title rejected

        // JSON-LD isAccessibleForFree=false + Piano script + valid og:title
        val jsonLdHtml = """
            <html><head>
            <meta property="og:title" content="Inside the Next Wave of Chip Manufacturing - Financial Times" />
            <script type="application/ld+json">{"@type":"NewsArticle","isAccessibleForFree":false,"headline":"Inside the Next Wave of Chip Manufacturing"}</script>
            <script src="https://cdn.tinypass.com/api/tinypass.min.js"></script>
            </head><body><p>Subscribe to continue reading.</p></body></html>
        """.trimIndent()
        val paywallResult = com.example.cleaner.AccessRestrictionDetector.analyzeHttpAndHtmlResponse(
            originalUrl = "https://www.ft.com/content/8a9f21c0-3b4e",
            statusCode = 200,
            finalUrl = "https://www.ft.com/content/8a9f21c0-3b4e",
            htmlBody = jsonLdHtml,
            offlineBase = com.example.cleaner.AccessRestrictionDetector.assessOffline("https://www.ft.com/content/8a9f21c0-3b4e")
        )
        assertEquals(com.example.model.AccessRestrictionType.PAYWALL, paywallResult.restrictionType)
        assertEquals(com.example.model.DetectionConfidence.HIGH, paywallResult.confidence)
        assertEquals("Inside the Next Wave of Chip Manufacturing", paywallResult.detectedTitle)
        assertTrue(paywallResult.signals.any { it.contains("JSON-LD") })
    }

    @Test
    fun `format archive clipboard bundle with paywall label and conditional title based on URL representation`() {
        val opaqueFtUrl = "https://www.ft.com/content/8a9f21c0-3b4e"
        val waybackFtUrl = "https://web.archive.org/web/20261112000000/$opaqueFtUrl"
        val opaqueTitle = "Inside the Next Wave of Chip Manufacturing"

        assertFalse(com.example.cleaner.ArchiveShareFormatter.isTitleRepresentedInUrl(opaqueTitle, opaqueFtUrl))
        val opaqueBundle = com.example.cleaner.ArchiveShareFormatter.formatClipboardBundle(
            originalUrl = opaqueFtUrl,
            archiveUrl = waybackFtUrl,
            providerName = "Wayback Machine",
            restrictionType = com.example.model.AccessRestrictionType.PAYWALL,
            title = opaqueTitle
        )
        assertEquals(
            "Inside the Next Wave of Chip Manufacturing\n" +
                "Original (Paywalled): $opaqueFtUrl\n" +
                "Archive (Wayback Machine): $waybackFtUrl",
            opaqueBundle
        )

        // When URL slug already conveys the title, omit the redundant title line
        val descriptiveUrl = "https://www.nytimes.com/2026/11/10/technology/ai-chips-energy-grid.html"
        val descriptiveTitle = "AI Chips and the Energy Grid"
        assertTrue(com.example.cleaner.ArchiveShareFormatter.isTitleRepresentedInUrl(descriptiveTitle, descriptiveUrl))
        val descriptiveBundle = com.example.cleaner.ArchiveShareFormatter.formatClipboardBundle(
            originalUrl = descriptiveUrl,
            archiveUrl = "https://archive.ph/AbCdE",
            providerName = "Archive.today",
            restrictionType = com.example.model.AccessRestrictionType.PAYWALL,
            title = descriptiveTitle
        )
        assertEquals(
            "Original (Paywalled): $descriptiveUrl\n" +
                "Archive (Archive.today): https://archive.ph/AbCdE",
            descriptiveBundle
        )

        // Region-locked label formatting
        val regionBundle = com.example.cleaner.ArchiveShareFormatter.formatClipboardBundle(
            originalUrl = "https://news.example.com/a/99182",
            archiveUrl = "https://perma.cc/W5MF-N9EV",
            providerName = "Perma.cc",
            restrictionType = com.example.model.AccessRestrictionType.REGION_LOCKED,
            title = "European Privacy Law Ruling Explained"
        )
        assertTrue(regionBundle.startsWith("European Privacy Law Ruling Explained\nOriginal (Region-Locked): "))
    }

    @Test
    fun `parse archive API payloads and validate full article vs paywalled stub vs CAPTCHA`() {
        val cdxJson = """
            [
              ["timestamp","original","statuscode","mimetype","length"],
              ["20261101100000","https://www.ft.com/content/8a9f21c0","200","text/html","800"],
              ["20261112143000","https://www.ft.com/content/8a9f21c0","200","text/html","24500"]
            ]
        """.trimIndent()
        val wayback = com.example.cleaner.ArchiveLookupEngine.parseWaybackCdxJson(cdxJson)
        assertEquals("20261112143000", wayback?.timestamp)
        assertEquals("2026-11-12", wayback?.formattedDate)
        assertEquals("https://web.archive.org/web/20261112143000/https://www.ft.com/content/8a9f21c0", wayback?.viewerUrl)

        val permaJson = """
            {
              "objects": [
                {
                  "guid": "W5MF-N9EV",
                  "url": "https://www.ft.com/content/8a9f21c0",
                  "creation_timestamp": "2026-11-12T10:15:00Z",
                  "title": "Inside Chip Manufacturing | Financial Times"
                }
              ]
            }
        """.trimIndent()
        val perma = com.example.cleaner.ArchiveLookupEngine.parsePermaCcJson(permaJson, "https://ft.com/content/8a9f21c0")
        assertEquals("https://perma.cc/W5MF-N9EV", perma?.archiveUrl)
        assertEquals("Inside Chip Manufacturing", perma?.title)

        val ghostHtml = """<html><body><a href="/archive/xYz123">Snapshot</a></body></html>"""
        assertEquals(
            "https://ghostarchive.org/archive/xYz123",
            com.example.cleaner.ArchiveLookupEngine.parseGhostarchiveSearchHtml(ghostHtml)
        )

        // Stage 2 heuristic: full article (>= 250 words) -> VERIFIED_FULL_ARTICLE
        val longBody = (1..280).joinToString(" ") { "journalism" }
        val fullEval = com.example.cleaner.ArchiveLookupEngine.evaluateArchivedHtml(
            "<html><head><title>Deep Investigative Report</title></head><body><article><p>$longBody</p></article></body></html>"
        )
        assertEquals(com.example.model.ArchiveValidationStatus.VERIFIED_FULL_ARTICLE, fullEval.status)
        assertEquals("Deep Investigative Report", fullEval.extractedTitle)

        // Stage 2 heuristic: short paywalled stub -> SNAPSHOT_PARTIAL_OR_PAYWALLED
        val stubEval = com.example.cleaner.ArchiveLookupEngine.evaluateArchivedHtml(
            "<html><body><p>Subscribe to continue reading this exclusive article.</p></body></html>"
        )
        assertEquals(com.example.model.ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED, stubEval.status)

        // Stage 2 heuristic: CAPTCHA wall -> UNVALIDATED_CAPTCHA_OR_LOOKUP
        val captchaEval = com.example.cleaner.ArchiveLookupEngine.evaluateArchivedHtml(
            "<html><head><title>Just a moment...</title></head><body><div class='cf-challenge'>Verify you are human</div></body></html>"
        )
        assertEquals(com.example.model.ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP, captchaEval.status)

        // Trust rank ordering invariant
        val ranks = com.example.model.ArchiveProvider.entries.map { it.trustRank }
        assertEquals(listOf(1, 2, 3, 4), ranks)
    }

    @Test
    fun `verify full release notes history completeness and prompts`() {
        val releases = com.example.model.ReleaseHistory.RELEASES
        assertEquals(24, releases.size)
        assertEquals("24.0", releases.first().version)
        assertEquals("1.0.0", releases.last().version)
        assertEquals("https://github.com/znep/znep-link-cleaner", com.example.model.ReleaseHistory.GITHUB_REPO_URL)
        assertTrue(releases.all { it.highlights.isNotEmpty() })
        assertTrue(releases.all { it.userPrompt.isNotBlank() })
        assertTrue(com.example.model.ReleaseHistory.VIBE_CODED_POC_NOTICE.contains("VIBE-CODED PROOF OF CONCEPT"))

        // Verify linked GitHub files (README.md, CHANGELOG.md) and published portal files (index.html, repo.html) stay in sync
        val rootDir = java.io.File("..").canonicalFile
        val syncFiles = listOf(
            java.io.File(rootDir, "README.md"),
            java.io.File(rootDir, "CHANGELOG.md"),
            java.io.File(rootDir, "download_portal/index.html"),
            java.io.File(rootDir, "download_portal/repo.html")
        )
        for (file in syncFiles) {
            if (file.exists()) {
                val content = file.readText()
                assertTrue(
                    "File ${file.name} is missing public GitHub repository URL",
                    content.contains("https://github.com/znep/znep-link-cleaner")
                )
                for (release in releases) {
                    assertTrue(
                        "File ${file.name} is missing version ${release.version}",
                        content.contains(release.version)
                    )
                }
            }
        }
    }
}
