package com.example.cleaner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Headless WebView-based expander for Facebook share links (Option 1).
 * Spins up a lightweight off-screen WebView to execute Meta's client-side
 * redirect scripts and retrieve the canonical permalink without requiring API keys.
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
        return withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    var isFinished = false
                    var webView: WebView? = null

                    fun finish(result: Result<String>) {
                        if (!isFinished) {
                            isFinished = true
                            try {
                                webView?.stopLoading()
                                webView?.destroy()
                            } catch (_: Exception) {
                                // Ignore cleanup exceptions
                            }
                            webView = null
                            if (continuation.isActive) {
                                continuation.resume(result)
                            }
                        }
                    }

                    try {
                        val view = WebView(context.applicationContext)
                        webView = view
                        val settings = view.settings
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = false // Save bandwidth
                        settings.blockNetworkImage = true         // Save bandwidth
                        settings.userAgentString = USER_AGENT

                        view.webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val nextUrl = request?.url?.toString() ?: return false
                                if (isResolvedPermalink(nextUrl)) {
                                    finish(Result.success(nextUrl))
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, currentUrl: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, currentUrl, favicon)
                                currentUrl?.let {
                                    if (isResolvedPermalink(it)) {
                                        finish(Result.success(it))
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, currentUrl: String?) {
                                super.onPageFinished(view, currentUrl)
                                currentUrl?.let {
                                    if (isResolvedPermalink(it)) {
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
                            try {
                                webView?.stopLoading()
                                webView?.destroy()
                            } catch (_: Exception) {
                                // Ignore cleanup exceptions
                            }
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
