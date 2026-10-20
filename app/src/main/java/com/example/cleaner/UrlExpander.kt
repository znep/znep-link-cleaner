package com.example.cleaner

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

object UrlExpander {

    val SUPPORTED_SHORTENER_DOMAINS = listOf(
        "vt.tiktok.com", "vm.tiktok.com", "tiktok.com/t/", "v.douyin.com",
        "fb.me", "m.me", "fb.watch", "ig.me", "l.facebook.com", "lm.facebook.com",
        "a.co", "amzn.to", "amzn.eu", "amzn.asia",
        "t.co", "lnkd.in", "youtu.be",
        "spotify.link", "spoti.fi", "redd.it", "pin.it",
        "bit.ly", "tinyurl.com", "t.ly", "is.gd", "v.gd", "rb.gy",
        "buff.ly", "ow.ly", "rebrand.ly", "cutt.ly", "shorturl.at", "git.io", "qrco.de", "goo.gl"
    )

    data class DetailedExpansion(
        val finalUrl: String,
        val wasRedirected: Boolean,
        val isOpaqueShortener: Boolean,
        val failedToRedirect: Boolean
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    /**
     * Checks whether a URL is from a known shortener or redirect domain
     * that should be expanded via HTTP.
     */
    fun shouldExpand(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase().orEmpty()
            val path = uri.path?.lowercase().orEmpty()

            // TikTok short link domains
            if (host == "vt.tiktok.com" ||
                host == "vm.tiktok.com" ||
                (host.contains("tiktok.com") && path.startsWith("/t/")) ||
                host == "v.douyin.com"
            ) {
                return true
            }

            // Facebook / Instagram / Meta short links
            if (host == "fb.me" || host == "m.me" || host == "fb.watch" || host == "ig.me") {
                return true
            }

            // Amazon Shorteners
            if (host == "a.co" || host == "amzn.to" || host == "amzn.eu" || host == "amzn.asia") {
                return true
            }

            // Social & Media shorteners
            if (host == "t.co" || host == "lnkd.in" || host == "youtu.be" ||
                host == "spotify.link" || host == "spoti.fi" ||
                host == "redd.it" || host == "pin.it"
            ) {
                return true
            }

            // General shorteners
            if (host == "bit.ly" || host == "tinyurl.com" || host == "t.ly" ||
                host == "is.gd" || host == "v.gd" || host == "rb.gy" ||
                host == "buff.ly" || host == "ow.ly" || host == "rebrand.ly" ||
                host == "cutt.ly" || host == "shorturl.at" || host == "git.io" ||
                host == "qrco.de" || host == "goo.gl"
            ) {
                return true
            }

            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Identifies whether a URL uses an opaque shortener service where
     * tracking identifiers may be encapsulated within the slug itself.
     */
    fun isOpaqueShortener(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase().orEmpty()
            val path = uri.path.orEmpty()
            if (path.length <= 1) return false

            host == "bit.ly" || host == "tinyurl.com" || host == "t.ly" ||
            host == "is.gd" || host == "v.gd" || host == "rb.gy" ||
            host == "buff.ly" || host == "ow.ly" || host == "rebrand.ly" ||
            host == "cutt.ly" || host == "shorturl.at" || host == "git.io" ||
            host == "qrco.de" || host == "goo.gl" || host == "t.co" ||
            host == "lnkd.in" || host == "youtu.be" || host == "spotify.link" ||
            host == "spoti.fi" || host == "redd.it" || host == "pin.it" ||
            host == "vt.tiktok.com" || host == "vm.tiktok.com" ||
            host == "fb.me" || host == "fb.watch" || host == "m.me" ||
            host == "a.co" || host == "amzn.to"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Expands a short link by following HTTP 301/302 redirects up to maxRedirects.
     */
    suspend fun expandUrl(url: String, maxRedirects: Int = 6): String {
        return expandUrlDetailed(url, maxRedirects).finalUrl
    }

    suspend fun expandUrlDetailed(url: String, maxRedirects: Int = 6): DetailedExpansion = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        val offline = unwrapOfflineRedirects(trimmed)
        val isOpaque = isOpaqueShortener(trimmed)

        if (!shouldExpand(offline)) {
            return@withContext DetailedExpansion(
                finalUrl = offline,
                wasRedirected = offline != trimmed,
                isOpaqueShortener = isOpaque,
                failedToRedirect = false
            )
        }

        var currentUrl = offline
        var redirectCount = 0

        try {
            while (redirectCount < maxRedirects) {
                val request = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    if (code in 300..399) {
                        val location = response.header("Location")
                        if (!location.isNullOrBlank()) {
                            val resolvedUri = Uri.parse(currentUrl).buildUpon()
                            val nextUri = Uri.parse(location)
                            currentUrl = if (nextUri.isAbsolute) {
                                location
                            } else {
                                val scheme = resolvedUri.build().scheme ?: "https"
                                val host = resolvedUri.build().host ?: ""
                                "$scheme://$host$location"
                            }
                            redirectCount++
                        } else {
                            return@withContext DetailedExpansion(
                                finalUrl = currentUrl,
                                wasRedirected = currentUrl != trimmed,
                                isOpaqueShortener = isOpaque,
                                failedToRedirect = currentUrl == trimmed
                            )
                        }
                    } else {
                        return@withContext DetailedExpansion(
                            finalUrl = currentUrl,
                            wasRedirected = currentUrl != trimmed,
                            isOpaqueShortener = isOpaque,
                            failedToRedirect = currentUrl == trimmed
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Network error
        }

        val wasRedirected = currentUrl != trimmed
        DetailedExpansion(
            finalUrl = currentUrl,
            wasRedirected = wasRedirected,
            isOpaqueShortener = isOpaque,
            failedToRedirect = !wasRedirected
        )
    }

    /**
     * Handles link shims that can be unwrapped instantly without network
     * (e.g. l.facebook.com/l.php?u=... or google.com/url?q=...).
     */
    fun unwrapOfflineRedirects(url: String): String {
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase().orEmpty()

            // Facebook link shim
            if ((host == "l.facebook.com" || host == "lm.facebook.com" || host.contains("facebook.com")) &&
                uri.path?.contains("/l.php") == true
            ) {
                val u = uri.getQueryParameter("u")
                if (!u.isNullOrBlank() && u.startsWith("http")) {
                    return URLDecoder.decode(u, StandardCharsets.UTF_8.name())
                }
            }

            // Google redirect
            if (host.contains("google.") && uri.path?.contains("/url") == true) {
                val q = uri.getQueryParameter("q") ?: uri.getQueryParameter("url")
                if (!q.isNullOrBlank() && q.startsWith("http")) {
                    return URLDecoder.decode(q, StandardCharsets.UTF_8.name())
                }
            }

            // Reddit outbound
            if (host.contains("out.reddit.com")) {
                val target = uri.getQueryParameter("url")
                if (!target.isNullOrBlank() && target.startsWith("http")) {
                    return URLDecoder.decode(target, StandardCharsets.UTF_8.name())
                }
            }
        } catch (_: Exception) {
            // Keep original
        }
        return url
    }
}
