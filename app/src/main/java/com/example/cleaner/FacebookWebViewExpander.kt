package com.example.cleaner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.coroutines.resume

/**
 * Hardened headless WebView-based expander for Facebook share links (Option 1).
 * Spins up an isolated off-screen WebView locked strictly to official HTTPS Facebook
 * domains, with file/content access disabled, third-party cookies blocked, and
 * storage cleared upon teardown.
 *
 * Also intercepts and decodes Facebook native Android app deep-link redirects
 * (such as `fb://native_post/<base64>?wtsid=...`, `fb://facewebmodal/f?href=...`,
 * `fb://reel/...`, and `intent://...#Intent;scheme=fb;...`) directly into canonical
 * `https://www.facebook.com/...` permalinks without triggering false-positive
 * security firewall blocks.
 */
object FacebookWebViewExpander {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val GRAPHQL_STORY_TOKEN_REGEX = Regex(
        """S:_I(\d+):([A-Za-z0-9_.-]+)(?::[A-Za-z0-9_.-]+)?"""
    )
    private val COLON_PAIR_ID_REGEX = Regex(
        """(?:story|post|feedback)[:_](\d+):(\d+)""",
        RegexOption.IGNORE_CASE
    )
    private val SINGLE_STORY_ID_REGEX = Regex(
        """(?:story|post|feedback)[:_](\d{8,})""",
        RegexOption.IGNORE_CASE
    )

    fun isFacebookShareLink(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase().orEmpty()
            val path = uri.path?.lowercase().orEmpty()

            (host == "facebook.com" || host.endsWith(".facebook.com") || host == "fb.com" || host == "fb.me") &&
                (path.startsWith("/share/p/") ||
                 path.startsWith("/share/v/") ||
                 path.startsWith("/share/r/") ||
                 path.startsWith("/share/g/") ||
                 path.startsWith("/share/"))
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks whether a URI is a Facebook native app deep-link scheme (`fb://`, `fb-messenger://`,
     * `fb-pma://`, or an Android `intent://` targeting Facebook).
     */
    fun isFacebookNativeAppDeepLink(url: String): Boolean {
        val trimmed = url.trim().lowercase()
        if (trimmed.startsWith("fb://") ||
            trimmed.startsWith("fb-messenger://") ||
            trimmed.startsWith("fb-pma://") ||
            trimmed.startsWith("fb-work://")
        ) {
            return true
        }
        if (trimmed.startsWith("intent://")) {
            return trimmed.contains("scheme=fb") ||
                trimmed.contains("package=com.facebook.") ||
                trimmed.contains("facebook.com")
        }
        return false
    }

    /**
     * Extracts a canonical `https://www.facebook.com/...` permalink from a Facebook native
     * deep-link URI (such as `fb://native_post/UzpfSTEwMDA2NTU0MDM4NTAxOToxNDMzNTc5NTM1NTAzMzc5OjE0MzM1Nzk1MzU1MDMzNzk=?wtsid=...`,
     * `fb://facewebmodal/f?href=...`, `fb://story/?story_fbid=...&id=...`, or `intent://...`).
     *
     * @param preserveQuery If true, appends any query parameters from the `fb://` deep link
     *   (such as `wtsid=...`) onto the resolved HTTPS URL so `LinkCleaner` can strip and log them.
     */
    fun extractPermalinkFromFacebookDeepLink(
        rawUri: String,
        preserveQuery: Boolean = false
    ): String? {
        val trimmed = rawUri.trim()
        if (!isFacebookNativeAppDeepLink(trimmed)) return null

        try {
            // 1. Unwrap Android intent:// URIs if present
            if (trimmed.lowercase().startsWith("intent://")) {
                val intentIdx = trimmed.indexOf("#Intent;")
                if (intentIdx != -1) {
                    val fragment = trimmed.substring(intentIdx + 8)
                    for (part in fragment.split(";")) {
                        if (part.startsWith("S.browser_fallback_url=")) {
                            val encodedFallback = part.removePrefix("S.browser_fallback_url=")
                            val fallbackUrl = URLDecoder.decode(encodedFallback, StandardCharsets.UTF_8.name()).trim()
                            if (UrlSecurityValidator.isAllowedFacebookWebViewUrl(fallbackUrl) &&
                                isResolvedPermalink(fallbackUrl)
                            ) {
                                return fallbackUrl
                            }
                        }
                    }
                    if (fragment.lowercase().contains("scheme=fb")) {
                        val fbEquivalent = "fb://" + trimmed.substring("intent://".length, intentIdx)
                        return extractPermalinkFromFacebookDeepLink(fbEquivalent, preserveQuery)
                    }
                }
                return null
            }

            val uri = Uri.parse(trimmed)
            val host = uri.host?.lowercase().orEmpty()
            val path = uri.path.orEmpty()
            val querySuffix = if (preserveQuery && !uri.encodedQuery.isNullOrBlank()) {
                "?${uri.encodedQuery}"
            } else {
                ""
            }

            // 2. fb://native_post/<base64_payload>?wtsid=...
            if (host == "native_post" || path.startsWith("/native_post/")) {
                val rawToken = if (host == "native_post") {
                    path.removePrefix("/").trim()
                } else {
                    path.removePrefix("/native_post/").trim()
                }
                val decodedPermalink = decodeNativePostBase64Token(rawToken)
                if (decodedPermalink != null) {
                    return attachQueryParameters(decodedPermalink, querySuffix)
                }
            }

            // 3. fb://facewebmodal/f?href=... or fb://faceweb/f?href=...
            if (host == "facewebmodal" || host == "faceweb") {
                val href = uri.getQueryParameter("href")
                if (!href.isNullOrBlank()) {
                    val normalized = normalizeFacebookHref(href)
                    if (normalized != null &&
                        UrlSecurityValidator.isAllowedFacebookWebViewUrl(normalized) &&
                        isResolvedPermalink(normalized)
                    ) {
                        return attachQueryParameters(normalized, querySuffix)
                    }
                }
            }

            // 4. fb://story/?story_fbid=<postId>&id=<actorId> or fb://post/<postId>
            if (host == "story" || host == "post" || host == "native_story") {
                val storyFbid = uri.getQueryParameter("story_fbid")
                    ?: uri.getQueryParameter("fbid")
                    ?: path.removePrefix("/").takeIf { it.all(Char::isDigit) && it.length >= 6 }
                val actorId = uri.getQueryParameter("id")
                if (!storyFbid.isNullOrBlank() && !actorId.isNullOrBlank()) {
                    return attachQueryParameters(
                        "https://www.facebook.com/$actorId/posts/$storyFbid",
                        querySuffix
                    )
                } else if (!storyFbid.isNullOrBlank()) {
                    return attachQueryParameters(
                        "https://www.facebook.com/story.php?story_fbid=$storyFbid",
                        querySuffix
                    )
                }
            }

            // 5. fb://reel/<id>, fb://video/<id>, fb://photo/<id>, fb://group/<id>
            val cleanSegment = path.removePrefix("/").substringBefore("/").trim()
            val paramId = uri.getQueryParameter("id") ?: uri.getQueryParameter("fbid") ?: cleanSegment
            if (paramId.isNotBlank() && paramId.all { it.isLetterOrDigit() || it == '_' || it == '-' }) {
                when (host) {
                    "reel", "reels" ->
                        return attachQueryParameters("https://www.facebook.com/reel/$paramId", querySuffix)
                    "video", "videos", "watch" ->
                        return attachQueryParameters("https://www.facebook.com/watch/?v=$paramId", querySuffix)
                    "photo", "photos" ->
                        return attachQueryParameters("https://www.facebook.com/photo.php?fbid=$paramId", querySuffix)
                    "group", "groups" ->
                        return attachQueryParameters("https://www.facebook.com/groups/$paramId", querySuffix)
                }
            }
        } catch (_: Exception) {
            return null
        }

        return null
    }

    internal fun decodeNativePostBase64Token(rawToken: String): String? {
        if (rawToken.isBlank()) return null
        val urlDecoded = try {
            URLDecoder.decode(rawToken, StandardCharsets.UTF_8.name()).trim()
        } catch (_: Exception) {
            rawToken.trim()
        }

        val candidates = mutableListOf(urlDecoded)
        val decodedString = decodeBase64Flexible(urlDecoded)
        if (!decodedString.isNullOrBlank()) {
            candidates.add(0, decodedString)
        }

        for (candidate in candidates) {
            // Format 1: S:_I100065540385019:1433579535503379:1433579535503379
            val storyMatch = GRAPHQL_STORY_TOKEN_REGEX.find(candidate)
            if (storyMatch != null) {
                val actorId = storyMatch.groupValues[1]
                val postId = storyMatch.groupValues[2]
                if (actorId.isNotBlank() && postId.isNotBlank()) {
                    return "https://www.facebook.com/$actorId/posts/$postId"
                }
            }

            // Format 2: story:100065540385019:1433579535503379
            val pairMatch = COLON_PAIR_ID_REGEX.find(candidate)
            if (pairMatch != null) {
                val actorId = pairMatch.groupValues[1]
                val postId = pairMatch.groupValues[2]
                return "https://www.facebook.com/$actorId/posts/$postId"
            }

            // Format 3: feedback:1433579535503379
            val singleMatch = SINGLE_STORY_ID_REGEX.find(candidate)
            if (singleMatch != null) {
                val postId = singleMatch.groupValues[1]
                return "https://www.facebook.com/story.php?story_fbid=$postId"
            }
        }

        return null
    }

    private fun decodeBase64Flexible(input: String): String? {
        val cleaned = input.trim().substringBefore("?").substringBefore("#")
        if (cleaned.isEmpty()) return null
        val normalized = cleaned.replace('-', '+').replace('_', '/')
        val padded = when (normalized.length % 4) {
            2 -> "$normalized=="
            3 -> "$normalized="
            else -> normalized
        }
        return try {
            val bytes = Base64.getDecoder().decode(padded)
            String(bytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeFacebookHref(rawHref: String): String? {
        val decoded = try {
            URLDecoder.decode(rawHref, StandardCharsets.UTF_8.name()).trim()
        } catch (_: Exception) {
            rawHref.trim()
        }
        return when {
            decoded.startsWith("https://") -> decoded
            decoded.startsWith("http://") -> "https://" + decoded.removePrefix("http://")
            decoded.startsWith("/") -> "https://www.facebook.com$decoded"
            else -> null
        }
    }

    private fun attachQueryParameters(baseUrl: String, querySuffix: String): String {
        val rawQuery = querySuffix.removePrefix("?").trim()
        if (rawQuery.isEmpty()) return baseUrl
        return if (baseUrl.contains("?")) {
            "$baseUrl&$rawQuery"
        } else {
            "$baseUrl?$rawQuery"
        }
    }

    suspend fun expandFacebookShareLink(
        context: Context,
        url: String,
        timeoutMs: Long = 6500L
    ): Result<String> {
        if (!UrlSecurityValidator.isAllowedFacebookWebViewUrl(url)) {
            return Result.failure(
                SecurityException("Blocked non-HTTPS or non-Facebook URL from loading in WebView.")
            )
        }

        return withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    var isFinished = false
                    var webView: WebView? = null

                    fun cleanupWebView(target: WebView?) {
                        try {
                            target?.stopLoading()
                            target?.clearHistory()
                            target?.clearCache(true)
                            target?.loadUrl("about:blank")
                            WebStorage.getInstance().deleteAllData()
                            CookieManager.getInstance().removeAllCookies(null)
                            target?.destroy()
                        } catch (_: Exception) {
                            // Ignore cleanup exceptions
                        }
                    }

                    fun finish(result: Result<String>) {
                        if (!isFinished) {
                            isFinished = true
                            cleanupWebView(webView)
                            webView = null
                            if (continuation.isActive) {
                                continuation.resume(result)
                            }
                        }
                    }

                    try {
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(false)

                        val view = WebView(context.applicationContext)
                        webView = view
                        cookieManager.setAcceptThirdPartyCookies(view, false)

                        val settings = view.settings
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        @Suppress("DEPRECATION")
                        settings.allowFileAccessFromFileURLs = false
                        @Suppress("DEPRECATION")
                        settings.allowUniversalAccessFromFileURLs = false
                        @Suppress("DEPRECATION")
                        settings.saveFormData = false
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.loadsImagesAutomatically = false // Save bandwidth
                        settings.blockNetworkImage = true         // Save bandwidth
                        settings.userAgentString = USER_AGENT

                        view.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val nextUrl = request?.url?.toString() ?: return true

                                // 1. Check if this is a Facebook native app deep link (e.g. fb://native_post/...)
                                if (isFacebookNativeAppDeepLink(nextUrl)) {
                                    val extracted = extractPermalinkFromFacebookDeepLink(nextUrl, preserveQuery = true)
                                    if (extracted != null) {
                                        finish(Result.success(extracted))
                                    }
                                    // Always return true to suppress launching the external Facebook app,
                                    // without treating Facebook's own fb:// scheme as an SSRF attack.
                                    return true
                                }

                                // 2. Block any genuine external / non-HTTPS redirect outside Facebook
                                if (!UrlSecurityValidator.isAllowedFacebookWebViewUrl(nextUrl)) {
                                    finish(
                                        Result.failure(
                                            SecurityException("Blocked WebView navigation to external or non-HTTPS URL: $nextUrl")
                                        )
                                    )
                                    return true
                                }

                                if (isResolvedPermalink(nextUrl)) {
                                    finish(Result.success(nextUrl))
                                    return true
                                }
                                return false
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString().orEmpty()
                                if (isFacebookNativeAppDeepLink(reqUrl)) {
                                    val extracted = extractPermalinkFromFacebookDeepLink(reqUrl, preserveQuery = true)
                                    if (extracted != null) {
                                        Handler(Looper.getMainLooper()).post {
                                            finish(Result.success(extracted))
                                        }
                                    }
                                    return WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        204,
                                        "Suppressed native app scheme",
                                        emptyMap(),
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                if (!UrlSecurityValidator.isAllowedFacebookWebViewUrl(reqUrl)) {
                                    return WebResourceResponse(
                                        "text/plain",
                                        "UTF-8",
                                        403,
                                        "Forbidden by ZnepLinkCleaner WebView Sandbox",
                                        emptyMap(),
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, currentUrl: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, currentUrl, favicon)
                                currentUrl?.let {
                                    if (isFacebookNativeAppDeepLink(it)) {
                                        val extracted = extractPermalinkFromFacebookDeepLink(it, preserveQuery = true)
                                        if (extracted != null) {
                                            finish(Result.success(extracted))
                                        }
                                        return
                                    }
                                    if (!UrlSecurityValidator.isAllowedFacebookWebViewUrl(it)) {
                                        finish(
                                            Result.failure(
                                                SecurityException("Blocked WebView page load outside Facebook HTTPS sandbox.")
                                            )
                                        )
                                    } else if (isResolvedPermalink(it)) {
                                        finish(Result.success(it))
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, currentUrl: String?) {
                                super.onPageFinished(view, currentUrl)
                                currentUrl?.let {
                                    if (isFacebookNativeAppDeepLink(it)) {
                                        val extracted = extractPermalinkFromFacebookDeepLink(it, preserveQuery = true)
                                        if (extracted != null) {
                                            finish(Result.success(extracted))
                                            return
                                        }
                                    }
                                    if (UrlSecurityValidator.isAllowedFacebookWebViewUrl(it) && isResolvedPermalink(it)) {
                                        finish(Result.success(it))
                                        return
                                    }
                                }

                                // Inspect canonical / App Links meta tags in the loaded DOM as fallback
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                      var out = [];
                                      var al = document.querySelector('meta[property="al:android:url"]');
                                      if (al && al.content) out.push(al.content);
                                      var can = document.querySelector('link[rel="canonical"]');
                                      if (can && can.href) out.push(can.href);
                                      var og = document.querySelector('meta[property="og:url"]');
                                      if (og && og.content) out.push(og.content);
                                      return JSON.stringify(out);
                                    })();
                                    """.trimIndent()
                                ) { jsonResult ->
                                    if (isFinished || jsonResult.isNullOrBlank() || jsonResult == "null") return@evaluateJavascript
                                    try {
                                        val raw = if (jsonResult.startsWith("\"") && jsonResult.endsWith("\"")) {
                                            jsonResult.substring(1, jsonResult.length - 1)
                                                .replace("\\\"", "\"")
                                                .replace("\\\\", "\\")
                                        } else {
                                            jsonResult
                                        }
                                        val arr = JSONArray(raw)
                                        for (i in 0 until arr.length()) {
                                            val candidate = arr.optString(i).trim()
                                            if (candidate.isEmpty()) continue
                                            if (isFacebookNativeAppDeepLink(candidate)) {
                                                val extracted = extractPermalinkFromFacebookDeepLink(candidate, preserveQuery = true)
                                                if (extracted != null) {
                                                    finish(Result.success(extracted))
                                                    return@evaluateJavascript
                                                }
                                            } else if (UrlSecurityValidator.isAllowedFacebookWebViewUrl(candidate) &&
                                                isResolvedPermalink(candidate)
                                            ) {
                                                finish(Result.success(candidate))
                                                return@evaluateJavascript
                                            }
                                        }
                                    } catch (_: Exception) {
                                        // Ignore JSON parse errors
                                    }
                                }
                            }
                        }

                        view.loadUrl(url)
                    } catch (e: Exception) {
                        finish(Result.failure(e))
                    }

                    continuation.invokeOnCancellation {
                        Handler(Looper.getMainLooper()).post {
                            cleanupWebView(webView)
                            webView = null
                        }
                    }
                }
            }
        } ?: Result.failure(Exception("Facebook expansion timed out or hit authentication wall."))
    }

    /**
     * Checks if a navigated URL represents a resolved canonical post/video/reel
     * rather than an unresolved /share/ slug or a login redirect.
     */
    fun isResolvedPermalink(url: String): Boolean {
        if (!UrlSecurityValidator.isAllowedFacebookWebViewUrl(url)) return false
        val uri = try { Uri.parse(url) } catch (_: Exception) { return false }
        val path = uri.path?.lowercase().orEmpty()

        // Still on the unresolved share endpoint
        if (path.startsWith("/share/")) return false

        // Redirected to login wall, checkpoint, or empty root
        if (path.startsWith("/login") || path.startsWith("/checkpoint") || path == "/" || path.isEmpty()) {
            return false
        }

        // Canonical destination patterns
        if (path.contains("/posts/") ||
            path.contains("/reel/") ||
            path.contains("/videos/") ||
            path.contains("/story.php") ||
            path.contains("/permalink.php") ||
            path.contains("/photo.php") ||
            path.contains("/watch/") ||
            path.contains("/groups/")
        ) {
            return true
        }

        val host = uri.host?.lowercase().orEmpty()
        return (host.contains("facebook.com") || host.contains("fb.watch")) && path.length > 5
    }
}
