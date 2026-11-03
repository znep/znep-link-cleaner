package com.example.cleaner

import android.net.Uri
import com.example.model.RedirectHop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
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

    private val AMP_CDN_REGEX = Regex(
        """^https?://[a-z0-9.-]+\.cdn\.ampproject\.org/[vc]/(s/)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val GOOGLE_AMP_PATH_REGEX = Regex(
        """^/amp/(s/)?(.+)$""",
        RegexOption.IGNORE_CASE
    )

    data class DetailedExpansion(
        val finalUrl: String,
        val wasRedirected: Boolean,
        val isOpaqueShortener: Boolean,
        val failedToRedirect: Boolean,
        val blockedReason: String? = null,
        val hops: List<RedirectHop> = emptyList()
    )

    data class OfflineUnwrapResult(
        val url: String,
        val blockedReason: String? = null,
        val mechanism: String? = null
    )

    private val client = OkHttpClient.Builder()
        .dns(UrlSecurityValidator.SafePublicDns)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
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
     * Expands a short link by following HTTP 301/302 redirects up to maxRedirects,
     * enforcing SSRF and scheme security checks at every hop.
     */
    suspend fun expandUrl(url: String, maxRedirects: Int = 6): String {
        return expandUrlDetailed(url, maxRedirects).finalUrl
    }

    suspend fun expandUrlDetailed(url: String, maxRedirects: Int = 6): DetailedExpansion = withContext(Dispatchers.IO) {
        val trimmed = url.trim()
        val hops = mutableListOf<RedirectHop>()
        hops.add(
            RedirectHop(
                step = 1,
                url = trimmed,
                host = UrlSecurityValidator.extractHost(trimmed),
                mechanism = "Original"
            )
        )

        val offlineResult = unwrapOfflineRedirectsDetailed(trimmed)
        val offline = offlineResult.url
        val isOpaque = isOpaqueShortener(trimmed)

        if (offline != trimmed) {
            hops.add(
                RedirectHop(
                    step = hops.size + 1,
                    url = offline,
                    host = UrlSecurityValidator.extractHost(offline),
                    mechanism = offlineResult.mechanism ?: "Offline Shim"
                )
            )
        }

        if (offlineResult.blockedReason != null) {
            return@withContext DetailedExpansion(
                finalUrl = offline,
                wasRedirected = false,
                isOpaqueShortener = isOpaque,
                failedToRedirect = true,
                blockedReason = offlineResult.blockedReason,
                hops = hops
            )
        }

        val initialBlockReason = UrlSecurityValidator.validateRedirectTarget(offline)
        if (initialBlockReason != null) {
            return@withContext DetailedExpansion(
                finalUrl = offline,
                wasRedirected = false,
                isOpaqueShortener = isOpaque,
                failedToRedirect = true,
                blockedReason = initialBlockReason,
                hops = hops
            )
        }

        if (!shouldExpand(offline)) {
            return@withContext DetailedExpansion(
                finalUrl = offline,
                wasRedirected = offline != trimmed,
                isOpaqueShortener = isOpaque,
                failedToRedirect = false,
                blockedReason = null,
                hops = if (hops.size > 1) hops else emptyList()
            )
        }

        var currentUrl = offline
        var redirectCount = 0
        var blockedReason: String? = null

        try {
            while (redirectCount < maxRedirects) {
                val hopBlockReason = UrlSecurityValidator.validateRedirectTarget(currentUrl)
                if (hopBlockReason != null) {
                    blockedReason = hopBlockReason
                    break
                }

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
                            val rawCandidateUrl = resolveRedirectLocation(currentUrl, location.trim())
                            val candidateUrl = if (FacebookWebViewExpander.isFacebookNativeAppDeepLink(rawCandidateUrl)) {
                                FacebookWebViewExpander.extractPermalinkFromFacebookDeepLink(
                                    rawCandidateUrl,
                                    preserveQuery = true
                                ) ?: rawCandidateUrl
                            } else {
                                rawCandidateUrl
                            }
                            val candidateBlockReason = UrlSecurityValidator.validateRedirectTarget(candidateUrl)
                            if (candidateBlockReason != null) {
                                blockedReason = candidateBlockReason
                                return@withContext DetailedExpansion(
                                    finalUrl = currentUrl,
                                    wasRedirected = currentUrl != trimmed,
                                    isOpaqueShortener = isOpaque,
                                    failedToRedirect = true,
                                    blockedReason = candidateBlockReason,
                                    hops = if (hops.size > 1) hops else emptyList()
                                )
                            }
                            currentUrl = candidateUrl
                            redirectCount++
                            hops.add(
                                RedirectHop(
                                    step = hops.size + 1,
                                    url = currentUrl,
                                    host = UrlSecurityValidator.extractHost(currentUrl),
                                    mechanism = if (candidateUrl != rawCandidateUrl) "Meta Native Deep-Link Decode" else "HTTP $code"
                                )
                            )
                        } else {
                            return@withContext DetailedExpansion(
                                finalUrl = currentUrl,
                                wasRedirected = currentUrl != trimmed,
                                isOpaqueShortener = isOpaque,
                                failedToRedirect = currentUrl == trimmed,
                                blockedReason = null,
                                hops = if (hops.size > 1) hops else emptyList()
                            )
                        }
                    } else {
                        return@withContext DetailedExpansion(
                            finalUrl = currentUrl,
                            wasRedirected = currentUrl != trimmed,
                            isOpaqueShortener = isOpaque,
                            failedToRedirect = currentUrl == trimmed,
                            blockedReason = null,
                            hops = if (hops.size > 1) hops else emptyList()
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            blockedReason = e.message ?: "Blocked unsafe network redirect."
        } catch (e: java.net.UnknownHostException) {
            if (e.message?.contains("Blocked") == true) {
                blockedReason = e.message
            }
        } catch (_: Exception) {
            // Network timeout or connection failure
        }

        val wasRedirected = currentUrl != trimmed
        DetailedExpansion(
            finalUrl = currentUrl,
            wasRedirected = wasRedirected,
            isOpaqueShortener = isOpaque,
            failedToRedirect = !wasRedirected || blockedReason != null,
            blockedReason = blockedReason,
            hops = if (hops.size > 1) hops else emptyList()
        )
    }

    internal fun resolveRedirectLocation(baseUrl: String, location: String): String {
        return try {
            val nextUri = Uri.parse(location)
            if (nextUri.isAbsolute) {
                location
            } else {
                val resolved = URI(baseUrl).resolve(location).toString()
                resolved
            }
        } catch (_: Exception) {
            val baseUri = Uri.parse(baseUrl)
            val scheme = baseUri.scheme ?: "https"
            val host = baseUri.host ?: ""
            if (location.startsWith("/")) {
                "$scheme://$host$location"
            } else {
                "$scheme://$host/$location"
            }
        }
    }

    /**
     * Handles link shims and AMP cache wrappers that can be unwrapped instantly without network
     * (e.g. l.facebook.com/l.php?u=..., google.com/url?q=..., *.cdn.ampproject.org/v/s/...),
     * while validating that the unwrapped target is a safe public HTTP/HTTPS URL.
     */
    fun unwrapOfflineRedirects(url: String): String {
        return unwrapOfflineRedirectsDetailed(url).url
    }

    fun unwrapOfflineRedirectsDetailed(url: String): OfflineUnwrapResult {
        try {
            val trimmed = url.trim()

            // 0. Facebook Native Deep-Link Unwrap (fb://native_post/<base64>, fb://facewebmodal/..., intent://...)
            if (FacebookWebViewExpander.isFacebookNativeAppDeepLink(trimmed)) {
                val decodedFb = FacebookWebViewExpander.extractPermalinkFromFacebookDeepLink(
                    trimmed,
                    preserveQuery = true
                )
                if (decodedFb != null) {
                    return OfflineUnwrapResult(url = decodedFb, mechanism = "Meta Native Deep-Link Decode")
                }
            }

            // 1. Google AMP Project CDN Wrapper (*.cdn.ampproject.org/v/s/<target>)
            val ampCdnMatch = AMP_CDN_REGEX.matchEntire(trimmed)
            if (ampCdnMatch != null) {
                val isHttps = !ampCdnMatch.groupValues[1].isNullOrEmpty()
                val rest = ampCdnMatch.groupValues[2]
                if (rest.isNotBlank()) {
                    val candidate = (if (isHttps) "https://" else "http://") + rest
                    val blockReason = UrlSecurityValidator.validateRedirectTarget(candidate)
                    return if (blockReason == null) {
                        OfflineUnwrapResult(url = candidate, mechanism = "AMP Cache Unwrap")
                    } else {
                        OfflineUnwrapResult(url = trimmed, blockedReason = blockReason)
                    }
                }
            }

            val uri = Uri.parse(trimmed)
            val host = uri.host?.lowercase().orEmpty()
            val path = uri.path.orEmpty()

            // 2. Google AMP Viewer Path (www.google.com/amp/s/<target>)
            if (host.contains("google.") && path.lowercase().startsWith("/amp/")) {
                val googleAmpMatch = GOOGLE_AMP_PATH_REGEX.matchEntire(path)
                if (googleAmpMatch != null) {
                    val isHttps = !googleAmpMatch.groupValues[1].isNullOrEmpty()
                    val restPath = googleAmpMatch.groupValues[2]
                    val querySuffix = uri.encodedQuery?.let { "?$it" }.orEmpty()
                    val candidate = (if (isHttps) "https://" else "http://") + restPath + querySuffix
                    val blockReason = UrlSecurityValidator.validateRedirectTarget(candidate)
                    return if (blockReason == null) {
                        OfflineUnwrapResult(url = candidate, mechanism = "AMP Viewer Unwrap")
                    } else {
                        OfflineUnwrapResult(url = trimmed, blockedReason = blockReason)
                    }
                }
            }

            // 3. Facebook link shim
            if ((host == "l.facebook.com" || host == "lm.facebook.com" || host.contains("facebook.com")) &&
                path.contains("/l.php")
            ) {
                val u = uri.getQueryParameter("u")
                if (!u.isNullOrBlank()) {
                    val decoded = URLDecoder.decode(u, StandardCharsets.UTF_8.name()).trim()
                    val blockReason = UrlSecurityValidator.validateRedirectTarget(decoded)
                    return if (blockReason == null) {
                        OfflineUnwrapResult(url = decoded, mechanism = "Facebook Shim Unwrap")
                    } else {
                        OfflineUnwrapResult(url = trimmed, blockedReason = blockReason)
                    }
                }
            }

            // 4. Google redirect
            if (host.contains("google.") && path.contains("/url")) {
                val q = uri.getQueryParameter("q") ?: uri.getQueryParameter("url")
                if (!q.isNullOrBlank()) {
                    val decoded = URLDecoder.decode(q, StandardCharsets.UTF_8.name()).trim()
                    val blockReason = UrlSecurityValidator.validateRedirectTarget(decoded)
                    return if (blockReason == null) {
                        OfflineUnwrapResult(url = decoded, mechanism = "Google Shim Unwrap")
                    } else {
                        OfflineUnwrapResult(url = trimmed, blockedReason = blockReason)
                    }
                }
            }

            // 5. Reddit outbound
            if (host.contains("out.reddit.com")) {
                val target = uri.getQueryParameter("url")
                if (!target.isNullOrBlank()) {
                    val decoded = URLDecoder.decode(target, StandardCharsets.UTF_8.name()).trim()
                    val blockReason = UrlSecurityValidator.validateRedirectTarget(decoded)
                    return if (blockReason == null) {
                        OfflineUnwrapResult(url = decoded, mechanism = "Reddit Shim Unwrap")
                    } else {
                        OfflineUnwrapResult(url = trimmed, blockedReason = blockReason)
                    }
                }
            }
        } catch (_: Exception) {
            // Keep original
        }
        return OfflineUnwrapResult(url = url)
    }
}
