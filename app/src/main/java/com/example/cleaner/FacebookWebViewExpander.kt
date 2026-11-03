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
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume

/**
 * Hardened headless WebView-based expander for Facebook share links (Option 1).
 * Spins up an isolated off-screen WebView locked strictly to official HTTPS Facebook
 * domains, with file/content access disabled, third-party cookies blocked, and
 * storage cleared upon teardown.
 */
object FacebookWebViewExpander {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

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
                                    if (UrlSecurityValidator.isAllowedFacebookWebViewUrl(it) && isResolvedPermalink(it)) {
                                        finish(Result.success(it))
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
            path.contains("/watch/") ||
            path.contains("/groups/")
        ) {
            return true
        }

        val host = uri.host?.lowercase().orEmpty()
        return (host.contains("facebook.com") || host.contains("fb.watch")) && path.length > 5
    }
}
