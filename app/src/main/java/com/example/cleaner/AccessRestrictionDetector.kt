package com.example.cleaner

import com.example.model.AccessAssessment
import com.example.model.AccessRestrictionType
import com.example.model.DetectionConfidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Hybrid paywall, region-lock, and access-block detector:
 * - Stage A (Offline, 0ms): Matches against a curated registry of known hard/metered paywalled publishers.
 * - Stage B (Online, bounded): Performs a single SSRF-safe HTTP GET (max 96 KB HTML read, max 3 hops)
 *   inspecting HTTP status codes (402/451/403), redirect-to-subscribe paths, Schema.org JSON-LD
 *   paywall markup, paywall vendor scripts, and region-lock copy, while opportunistically extracting
 *   the article title.
 */
object AccessRestrictionDetector {

    private const val MAX_PROBE_BYTES = 96 * 1024L
    private const val MAX_REDIRECT_HOPS = 3

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    /**
     * Curated offline registry of known paywalled / metered news & publication domains.
     * Maps base domain -> concise publisher label.
     */
    val KNOWN_PAYWALLED_PUBLISHERS: Map<String, String> = mapOf(
        "nytimes.com" to "The New York Times",
        "wsj.com" to "Wall Street Journal",
        "ft.com" to "Financial Times",
        "bloomberg.com" to "Bloomberg",
        "economist.com" to "The Economist",
        "washingtonpost.com" to "The Washington Post",
        "theatlantic.com" to "The Atlantic",
        "newyorker.com" to "The New Yorker",
        "wired.com" to "WIRED",
        "latimes.com" to "Los Angeles Times",
        "bostonglobe.com" to "Boston Globe",
        "barrons.com" to "Barron's",
        "businessinsider.com" to "Business Insider",
        "telegraph.co.uk" to "The Telegraph",
        "thetimes.co.uk" to "The Times",
        "thetimes.com" to "The Times",
        "theinformation.com" to "The Information",
        "hbr.org" to "Harvard Business Review",
        "technologyreview.com" to "MIT Technology Review",
        "medium.com" to "Medium",
        "seekingalpha.com" to "Seeking Alpha",
        "statnews.com" to "STAT News",
        "forbes.com" to "Forbes",
        "lefigaro.fr" to "Le Figaro",
        "lemonde.fr" to "Le Monde",
        "spiegel.de" to "Der Spiegel",
        "nikkei.com" to "Nikkei Asia",
        "scmp.com" to "South China Morning Post",
        "afr.com" to "Australian Financial Review",
        "theglobeandmail.com" to "The Globe and Mail",
        "thestar.com" to "Toronto Star",
        "haaretz.com" to "Haaretz",
        "irishtimes.com" to "The Irish Times",
        "smh.com.au" to "Sydney Morning Herald",
        "theage.com.au" to "The Age",
        "nzherald.co.nz" to "NZ Herald",
        "chicagotribune.com" to "Chicago Tribune",
        "sfchronicle.com" to "San Francisco Chronicle",
        "seattletimes.com" to "The Seattle Times",
        "dallasnews.com" to "Dallas Morning News",
        "inquirer.com" to "Philadelphia Inquirer",
        "foreignpolicy.com" to "Foreign Policy",
        "foreignaffairs.com" to "Foreign Affairs",
        "nature.com" to "Nature",
        "science.org" to "Science",
        "spectator.co.uk" to "The Spectator",
        "newstatesman.com" to "New Statesman",
        "stratechery.com" to "Stratechery",
        "adweek.com" to "Adweek",
        "vanityfair.com" to "Vanity Fair"
    )

    private val JSON_LD_NOT_FREE_REGEX = Regex(
        """"isAccessibleForFree"\s*:\s*(?:false|"false"|'false')""",
        RegexOption.IGNORE_CASE
    )

    private val JSON_LD_PAYWALL_PART_REGEX = Regex(
        """"@type"\s*:\s*"WebPageElement"[^}]{0,300}"isAccessibleForFree"\s*:\s*(?:false|"false")""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val PAYWALL_VENDOR_MARKERS = listOf(
        "tinypass.com" to "Piano/Tinypass Paywall",
        "piano.io" to "Piano Paywall",
        "cdn.pelcro.com" to "Pelcro Paywall",
        "zephr.com" to "Zephr Paywall",
        "poool.fr" to "Poool Paywall",
        "evolok.net" to "Evolok Paywall",
        "memberful.com" to "Memberful Paywall"
    )

    private val PAYWALL_COPY_MARKERS = listOf(
        "subscribe to continue reading",
        "subscribe to read the full article",
        "this article is for subscribers",
        "this content is exclusive to subscribers",
        "already a subscriber? sign in",
        "you have reached your article limit",
        "you've reached your limit of free articles",
        "create a free account to continue reading",
        "class=\"paywall",
        "id=\"paywall",
        "data-testid=\"paywall"
    )

    private val REGION_LOCK_MARKERS = listOf(
        "not available in your country",
        "not available in your region",
        "not available in your location",
        "unavailable in your country",
        "unavailable in your region",
        "unavailable due to legal reasons",
        "unavailable in most european countries",
        "this site is currently unavailable to visitors from the european",
        "content is geo-blocked",
        "451 unavailable for legal reasons"
    )

    private val WALL_REDIRECT_PATH_SEGMENTS = listOf(
        "/subscribe", "/subscription", "/paywall", "/membership", "/offer"
    )

    private val LOGIN_REDIRECT_PATH_SEGMENTS = listOf(
        "/login", "/signin", "/sign-in", "/account/login", "/register"
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(UrlSecurityValidator.SafePublicDns)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    /**
     * Stage A: Instant offline heuristic check against known paywalled publishers.
     * Safe to call on any thread and works in Offline-Only Mode.
     */
    fun assessOffline(url: String, offlineOnlyMode: Boolean = false): AccessAssessment {
        val trimmed = url.trim()
        if (!UrlSecurityValidator.isSafeHttpUrl(trimmed)) {
            return AccessAssessment(
                targetUrl = trimmed,
                restrictionType = AccessRestrictionType.NONE,
                isChecking = false,
                wasOfflineOnly = offlineOnlyMode
            )
        }

        val host = UrlSecurityValidator.extractHost(trimmed)
        val matchedEntry = findKnownPublisher(host)
        val path = extractPath(trimmed)

        if (matchedEntry != null && isLikelyArticleOrGatedPath(path)) {
            return AccessAssessment(
                targetUrl = trimmed,
                restrictionType = AccessRestrictionType.PAYWALL,
                confidence = DetectionConfidence.OFFLINE_LIKELY,
                signals = listOf("Known Publisher: ${matchedEntry.second}"),
                isChecking = !offlineOnlyMode,
                wasOfflineOnly = offlineOnlyMode
            )
        }

        return AccessAssessment(
            targetUrl = trimmed,
            restrictionType = AccessRestrictionType.NONE,
            confidence = DetectionConfidence.OFFLINE_LIKELY,
            signals = emptyList(),
            isChecking = !offlineOnlyMode,
            wasOfflineOnly = offlineOnlyMode
        )
    }

    /**
     * Stage A + Stage B: Runs offline assessment followed by a bounded SSRF-safe live HTTP/HTML probe
     * (skipped when offlineOnlyMode is true or when the target URL fails security checks).
     */
    suspend fun assessLive(url: String, offlineOnlyMode: Boolean = false): AccessAssessment =
        withContext(Dispatchers.IO) {
            val trimmed = url.trim()
            val offlineBase = assessOffline(trimmed, offlineOnlyMode = offlineOnlyMode)

            if (offlineOnlyMode || UrlSecurityValidator.validateRedirectTarget(trimmed) != null) {
                return@withContext offlineBase.copy(isChecking = false)
            }

            try {
                var currentUrl = trimmed
                var hops = 0
                var finalStatusCode = 0
                var htmlSnippet = ""

                while (hops <= MAX_REDIRECT_HOPS) {
                    if (UrlSecurityValidator.validateRedirectTarget(currentUrl) != null) {
                        return@withContext offlineBase.copy(isChecking = false)
                    }

                    val request = Request.Builder()
                        .url(currentUrl)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .get()
                        .build()

                    var nextUrl: String? = null
                    client.newCall(request).execute().use { response ->
                        finalStatusCode = response.code
                        if (finalStatusCode in 300..399) {
                            val location = response.header("Location")?.trim()
                            if (!location.isNullOrBlank()) {
                                val resolved = UrlExpander.resolveRedirectLocation(currentUrl, location)
                                if (UrlSecurityValidator.validateRedirectTarget(resolved) == null) {
                                    nextUrl = resolved
                                }
                            }
                        } else {
                            val contentType = response.header("Content-Type").orEmpty().lowercase()
                            val isReadableMarkup = contentType.isEmpty() ||
                                contentType.contains("text/html") ||
                                contentType.contains("application/xhtml") ||
                                contentType.contains("text/plain")
                            if (isReadableMarkup) {
                                htmlSnippet = readBoundedBody(response, MAX_PROBE_BYTES)
                            }
                        }
                    }

                    if (nextUrl != null && hops < MAX_REDIRECT_HOPS) {
                        currentUrl = nextUrl!!
                        hops++
                    } else {
                        break
                    }
                }

                analyzeHttpAndHtmlResponse(
                    originalUrl = trimmed,
                    statusCode = finalStatusCode,
                    finalUrl = currentUrl,
                    htmlBody = htmlSnippet,
                    offlineBase = offlineBase
                )
            } catch (_: Exception) {
                // Network timeout / unreachable: fall back gracefully to Stage A offline result
                offlineBase.copy(isChecking = false)
            }
        }

    /**
     * Pure, unit-testable analyzer combining Stage A offline match with Stage B HTTP status,
     * redirect destination, JSON-LD schema, vendor signatures, region-lock copy, and title extraction.
     */
    internal fun analyzeHttpAndHtmlResponse(
        originalUrl: String,
        statusCode: Int,
        finalUrl: String,
        htmlBody: String,
        offlineBase: AccessAssessment
    ): AccessAssessment {
        val signals = LinkedHashSet<String>()
        signals.addAll(offlineBase.signals)

        var restrictionType = offlineBase.restrictionType
        var confidence = offlineBase.confidence

        val lowerHtml = htmlBody.lowercase()
        val finalPath = extractPath(finalUrl).lowercase()
        val originalPath = extractPath(originalUrl).lowercase()

        // 1. Check HTTP Status Codes
        when (statusCode) {
            451 -> {
                restrictionType = AccessRestrictionType.REGION_LOCKED
                confidence = DetectionConfidence.HIGH
                signals.add("HTTP 451 Legal / Geo-Block")
            }
            402 -> {
                restrictionType = AccessRestrictionType.PAYWALL
                confidence = DetectionConfidence.HIGH
                signals.add("HTTP 402 Payment Required")
            }
            403 -> {
                if (restrictionType == AccessRestrictionType.NONE) {
                    restrictionType = AccessRestrictionType.ACCESS_BLOCKED
                    confidence = DetectionConfidence.MEDIUM
                } else {
                    confidence = DetectionConfidence.HIGH
                }
                signals.add("HTTP 403 Access Blocked")
            }
        }

        // 2. Check Redirect to Paywall / Login Gate
        if (finalPath != originalPath) {
            if (WALL_REDIRECT_PATH_SEGMENTS.any { finalPath.contains(it) }) {
                restrictionType = AccessRestrictionType.PAYWALL
                confidence = DetectionConfidence.HIGH
                signals.add("Redirected to Subscribe / Paywall Gate")
            } else if (LOGIN_REDIRECT_PATH_SEGMENTS.any { finalPath.contains(it) }) {
                if (restrictionType == AccessRestrictionType.NONE) {
                    restrictionType = AccessRestrictionType.REGISTRATION_WALL
                    confidence = DetectionConfidence.HIGH
                }
                signals.add("Redirected to Login Wall")
            }
        }

        // 3. Check Region-Lock / Geo-Block HTML Copy
        val matchedGeo = REGION_LOCK_MARKERS.firstOrNull { lowerHtml.contains(it) }
        if (matchedGeo != null) {
            restrictionType = AccessRestrictionType.REGION_LOCKED
            confidence = DetectionConfidence.HIGH
            signals.add("Geo-Block / Region Lock Copy")
        }

        // 4. Check Schema.org JSON-LD Paywall Markup
        if (JSON_LD_NOT_FREE_REGEX.containsMatchIn(htmlBody) ||
            JSON_LD_PAYWALL_PART_REGEX.containsMatchIn(htmlBody)
        ) {
            if (restrictionType != AccessRestrictionType.REGION_LOCKED) {
                restrictionType = AccessRestrictionType.PAYWALL
            }
            confidence = DetectionConfidence.HIGH
            signals.add("JSON-LD isAccessibleForFree=false")
        }

        // 5. Check Paywall Vendor Scripts
        val matchedVendor = PAYWALL_VENDOR_MARKERS.firstOrNull { (needle, _) -> lowerHtml.contains(needle) }
        if (matchedVendor != null) {
            if (restrictionType == AccessRestrictionType.NONE || restrictionType == AccessRestrictionType.ACCESS_BLOCKED) {
                restrictionType = AccessRestrictionType.PAYWALL
            }
            confidence = DetectionConfidence.HIGH
            signals.add(matchedVendor.second)
        }

        // 6. Check Explicit Paywall Overlay / Copy Markers
        if (PAYWALL_COPY_MARKERS.any { lowerHtml.contains(it) }) {
            if (restrictionType == AccessRestrictionType.NONE || restrictionType == AccessRestrictionType.ACCESS_BLOCKED) {
                restrictionType = AccessRestrictionType.PAYWALL
            }
            confidence = DetectionConfidence.HIGH
            signals.add("Paywall Overlay / Meter Markup")
        }

        val extractedTitle = ArchiveShareFormatter.extractArticleTitle(htmlBody)

        return AccessAssessment(
            targetUrl = originalUrl,
            restrictionType = restrictionType,
            confidence = confidence,
            signals = signals.toList(),
            detectedTitle = extractedTitle,
            isChecking = false,
            wasOfflineOnly = false
        )
    }

    internal fun findKnownPublisher(host: String): Pair<String, String>? {
        val cleanHost = host.trim().lowercase().removePrefix("www.")
        if (cleanHost.isBlank()) return null
        for ((domain, name) in KNOWN_PAYWALLED_PUBLISHERS) {
            if (cleanHost == domain || cleanHost.endsWith(".$domain")) {
                return domain to name
            }
        }
        return null
    }

    private fun extractPath(url: String): String {
        return try {
            URI(url.trim()).path.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun isLikelyArticleOrGatedPath(path: String): Boolean {
        val normalized = path.trim().trimEnd('/')
        if (normalized.isEmpty() || normalized == "/") return false
        // Skip bare single-word section homepages like "/world" or "/business" unless clearly gated
        val segments = normalized.split('/').filter { it.isNotBlank() }
        if (segments.size == 1 && segments[0].length < 14 && !segments[0].contains('-')) {
            val seg = segments[0].lowercase()
            return seg == "subscribe" || seg == "paywall" || seg == "login"
        }
        return true
    }

    private fun readBoundedBody(response: okhttp3.Response, maxBytes: Long): String {
        return try {
            response.body?.source()?.let { source ->
                source.request(maxBytes)
                source.buffer.clone().readString(
                    minOf(source.buffer.size, maxBytes),
                    StandardCharsets.UTF_8
                )
            }.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}
