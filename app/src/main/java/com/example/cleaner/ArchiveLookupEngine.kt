package com.example.cleaner

import com.example.model.ArchiveCandidate
import com.example.model.ArchiveProvider
import com.example.model.ArchiveValidationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/**
 * Trust-ordered archival link discovery & two-stage validation engine:
 *  #1 Wayback Machine (Internet Archive) — CDX JSON / Availability API + `id_` raw HTML check
 *  #2 Perma.cc (Harvard Law Library)     — Public JSON API + snapshot HTML check
 *  #3 Archive.today (archive.ph)         — `/newest/` lookup with honest CAPTCHA/rate-limit handling
 *  #4 Ghostarchive (ghostarchive.org)    — Search extraction + snapshot HTML check
 *
 * Stage 1 confirms whether a concrete archived snapshot exists (or is CAPTCHA-gated).
 * Stage 2 fetches a strictly bounded 128 KB slice of the archived HTML and runs a readable
 * full-article heuristic to verify substantial non-paywalled content.
 */
object ArchiveLookupEngine {

    private const val MAX_STAGE1_HTML_BYTES = 96 * 1024L
    private const val MAX_STAGE2_HTML_BYTES = 128 * 1024L
    private const val MIN_FULL_ARTICLE_WORDS = 250
    private const val MIN_PARTIAL_ARTICLE_WORDS = 80

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val SCRIPT_STYLE_BLOCK_REGEX = Regex(
        """<(script|style|noscript|svg|template|iframe)[^>]*>.*?</\1>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HTML_COMMENT_REGEX = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)
    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    private val WORD_REGEX = Regex("""[A-Za-z]{2,}""")

    private val CAPTCHA_MARKERS = listOf(
        "cf-challenge",
        "challenge-platform",
        "just a moment...",
        "verify you are human",
        "g-recaptcha",
        "hcaptcha",
        "one more step",
        "please complete the security check",
        "too many requests"
    )

    private val ARCHIVE_PAYWALL_BLOCK_MARKERS = listOf(
        "subscribe to continue reading",
        "subscribe to read the full article",
        "this article is for subscribers",
        "you have reached your article limit",
        "you've reached your limit of free articles",
        "create a free account to continue reading",
        "already a subscriber? sign in"
    )

    private val GHOSTARCHIVE_RESULT_REGEX = Regex(
        """href\s*=\s*["'](https?://ghostarchive\.org/archive/[A-Za-z0-9_-]+|/archive/[A-Za-z0-9_-]+)["']""",
        RegexOption.IGNORE_CASE
    )

    private val ARCHIVE_TODAY_ALLOWED_HOSTS = setOf(
        "archive.today", "archive.ph", "archive.is", "archive.md", "archive.li", "archive.vn", "archive.fo"
    )

    data class ContentHeuristicResult(
        val status: ArchiveValidationStatus,
        val wordCount: Int,
        val extractedTitle: String?,
        val detail: String
    )

    data class WaybackCapture(
        val timestamp: String,
        val originalUrl: String,
        val byteLength: Long
    ) {
        val viewerUrl: String
            get() = "https://web.archive.org/web/$timestamp/$originalUrl"
        val rawHtmlUrl: String
            get() = "https://web.archive.org/web/${timestamp}id_/$originalUrl"
        val formattedDate: String
            get() = formatWaybackTimestamp(timestamp)
    }

    data class PermaCapture(
        val guid: String,
        val archiveUrl: String,
        val createdAtDisplay: String?,
        val title: String?
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(UrlSecurityValidator.SafePublicDns)
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    /**
     * Queries all 4 curated archive services concurrently, invoking [onProviderUpdate] as each
     * provider resolves, and returns the completed list sorted strictly by [ArchiveProvider.trustRank].
     */
    suspend fun lookupAllArchives(
        cleanedUrl: String,
        onProviderUpdate: (ArchiveCandidate) -> Unit = {}
    ): List<ArchiveCandidate> = withContext(Dispatchers.IO) {
        val trimmed = cleanedUrl.trim()
        if (UrlSecurityValidator.validateRedirectTarget(trimmed) != null) {
            return@withContext ArchiveProvider.entries.map { provider ->
                ArchiveCandidate(
                    provider = provider,
                    archiveUrl = "",
                    validationStatus = ArchiveValidationStatus.ERROR,
                    statusDetail = "Blocked unsafe or non-public URL"
                )
            }
        }

        coroutineScope {
            val tasks = listOf(
                async {
                    lookupWaybackMachine(trimmed).also(onProviderUpdate)
                },
                async {
                    lookupPermaCc(trimmed).also(onProviderUpdate)
                },
                async {
                    lookupArchiveToday(trimmed).also(onProviderUpdate)
                },
                async {
                    lookupGhostarchive(trimmed).also(onProviderUpdate)
                }
            )
            tasks.awaitAll().sortedBy { it.provider.trustRank }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // #1 WAYBACK MACHINE (Internet Archive — Tier 1 Institutional Non-Profit)
    // ─────────────────────────────────────────────────────────────────────────────

    private fun lookupWaybackMachine(targetUrl: String): ArchiveCandidate {
        val fallbackSearchUrl = "https://web.archive.org/web/*/$targetUrl"
        val encoded = urlEncode(targetUrl)

        return try {
            val cdxUrl = "https://web.archive.org/cdx/search/cdx?url=$encoded&output=json" +
                "&fl=timestamp,original,statuscode,mimetype,length" +
                "&filter=statuscode:200&filter=mimetype:text/html&collapse=digest&limit=-5"

            val cdxResp = fetchText(cdxUrl, maxBytes = MAX_STAGE1_HTML_BYTES)
            var bestCapture = if (cdxResp.code == 200) parseWaybackCdxJson(cdxResp.body) else null

            // Fallback to Wayback Availability JSON API if CDX returned nothing or timed out
            if (bestCapture == null) {
                val availUrl = "https://archive.org/wayback/available?url=$encoded"
                val availResp = fetchText(availUrl, maxBytes = 32 * 1024L)
                if (availResp.code == 200) {
                    bestCapture = parseWaybackAvailabilityJson(availResp.body, targetUrl)
                }
            }

            if (bestCapture == null) {
                return ArchiveCandidate(
                    provider = ArchiveProvider.WAYBACK_MACHINE,
                    archiveUrl = fallbackSearchUrl,
                    validationStatus = ArchiveValidationStatus.NOT_ARCHIVED,
                    statusDetail = "No HTTP 200 HTML capture found in Wayback index",
                    isDirectLookupFallback = true
                )
            }

            // Stage 2: Bounded raw HTML (`id_`) full-article verification
            val rawHtmlResp = fetchText(bestCapture.rawHtmlUrl, maxBytes = MAX_STAGE2_HTML_BYTES, followSafeRedirects = true)
            if (rawHtmlResp.code == 200 && rawHtmlResp.body.isNotBlank()) {
                val heuristic = evaluateArchivedHtml(rawHtmlResp.body)
                ArchiveCandidate(
                    provider = ArchiveProvider.WAYBACK_MACHINE,
                    archiveUrl = bestCapture.viewerUrl,
                    validationStatus = heuristic.status,
                    capturedAtDisplay = bestCapture.formattedDate,
                    estimatedWordCount = heuristic.wordCount.takeIf { it > 0 },
                    extractedTitle = heuristic.extractedTitle,
                    statusDetail = heuristic.detail
                )
            } else {
                ArchiveCandidate(
                    provider = ArchiveProvider.WAYBACK_MACHINE,
                    archiveUrl = bestCapture.viewerUrl,
                    validationStatus = ArchiveValidationStatus.SNAPSHOT_UNVERIFIED,
                    capturedAtDisplay = bestCapture.formattedDate,
                    statusDetail = "Snapshot indexed (${bestCapture.formattedDate}); full-text check timed out"
                )
            }
        } catch (_: Exception) {
            ArchiveCandidate(
                provider = ArchiveProvider.WAYBACK_MACHINE,
                archiveUrl = fallbackSearchUrl,
                validationStatus = ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP,
                statusDetail = "Wayback API temporarily unreachable — tap for direct lookup",
                isDirectLookupFallback = true
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // #2 PERMA.CC (Harvard Law Library Innovation Lab — Tier 1 Academic)
    // ─────────────────────────────────────────────────────────────────────────────

    private fun lookupPermaCc(targetUrl: String): ArchiveCandidate {
        val fallbackUrl = "https://perma.cc/"
        val encoded = urlEncode(targetUrl)

        return try {
            val apiUrl = "https://api.perma.cc/v1/public/archives/?url=$encoded&limit=5"
            val apiResp = fetchText(apiUrl, maxBytes = MAX_STAGE1_HTML_BYTES)

            if (apiResp.code != 200) {
                return ArchiveCandidate(
                    provider = ArchiveProvider.PERMA_CC,
                    archiveUrl = fallbackUrl,
                    validationStatus = ArchiveValidationStatus.NOT_ARCHIVED,
                    statusDetail = "No matching public Perma.cc record found"
                )
            }

            val capture = parsePermaCcJson(apiResp.body, targetUrl)
                ?: return ArchiveCandidate(
                    provider = ArchiveProvider.PERMA_CC,
                    archiveUrl = fallbackUrl,
                    validationStatus = ArchiveValidationStatus.NOT_ARCHIVED,
                    statusDetail = "No public Perma.cc archive for this URL"
                )

            // Stage 2: Verify archive HTML content
            val htmlResp = fetchText(capture.archiveUrl, maxBytes = MAX_STAGE2_HTML_BYTES, followSafeRedirects = true)
            if (htmlResp.code == 200 && htmlResp.body.isNotBlank()) {
                val heuristic = evaluateArchivedHtml(htmlResp.body)
                val bestTitle = ArchiveShareFormatter.selectBestTitle(capture.title, heuristic.extractedTitle)
                ArchiveCandidate(
                    provider = ArchiveProvider.PERMA_CC,
                    archiveUrl = capture.archiveUrl,
                    validationStatus = heuristic.status,
                    capturedAtDisplay = capture.createdAtDisplay,
                    estimatedWordCount = heuristic.wordCount.takeIf { it > 0 },
                    extractedTitle = bestTitle,
                    statusDetail = heuristic.detail
                )
            } else {
                ArchiveCandidate(
                    provider = ArchiveProvider.PERMA_CC,
                    archiveUrl = capture.archiveUrl,
                    validationStatus = ArchiveValidationStatus.SNAPSHOT_UNVERIFIED,
                    capturedAtDisplay = capture.createdAtDisplay,
                    extractedTitle = capture.title,
                    statusDetail = "Public Perma.cc record found; full-text check skipped"
                )
            }
        } catch (_: Exception) {
            ArchiveCandidate(
                provider = ArchiveProvider.PERMA_CC,
                archiveUrl = fallbackUrl,
                validationStatus = ArchiveValidationStatus.ERROR,
                statusDetail = "Perma.cc API unreachable"
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // #3 ARCHIVE.TODAY (archive.ph — Tier 2 Independent Web Snapshot)
    // ─────────────────────────────────────────────────────────────────────────────

    private fun lookupArchiveToday(targetUrl: String): ArchiveCandidate {
        val directNewestUrl = "https://archive.today/newest/$targetUrl"
        return try {
            val probe = fetchText(directNewestUrl, maxBytes = MAX_STAGE1_HTML_BYTES, followSafeRedirects = false)

            // Concrete redirect to an existing snapshot (e.g. https://archive.ph/AbCdE)
            if (probe.code in 300..399 && !probe.locationHeader.isNullOrBlank()) {
                val resolved = UrlExpander.resolveRedirectLocation(directNewestUrl, probe.locationHeader)
                val host = UrlSecurityValidator.extractHost(resolved)
                val isAllowedArchiveHost = ARCHIVE_TODAY_ALLOWED_HOSTS.any { host == it || host.endsWith(".$it") }
                val isConcreteSnapshot = isAllowedArchiveHost &&
                    !resolved.contains("/submit", ignoreCase = true) &&
                    !resolved.contains("/newest/", ignoreCase = true)

                if (isConcreteSnapshot && UrlSecurityValidator.validateRedirectTarget(resolved) == null) {
                    val snapshotResp = fetchText(resolved, maxBytes = MAX_STAGE2_HTML_BYTES, followSafeRedirects = false)
                    if (snapshotResp.code == 200 && snapshotResp.body.isNotBlank() && !isCaptchaPage(snapshotResp.body)) {
                        val heuristic = evaluateArchivedHtml(snapshotResp.body)
                        return ArchiveCandidate(
                            provider = ArchiveProvider.ARCHIVE_TODAY,
                            archiveUrl = resolved,
                            validationStatus = heuristic.status,
                            estimatedWordCount = heuristic.wordCount.takeIf { it > 0 },
                            extractedTitle = heuristic.extractedTitle,
                            statusDetail = heuristic.detail
                        )
                    }
                    return ArchiveCandidate(
                        provider = ArchiveProvider.ARCHIVE_TODAY,
                        archiveUrl = resolved,
                        validationStatus = ArchiveValidationStatus.SNAPSHOT_UNVERIFIED,
                        statusDetail = "Snapshot redirect confirmed; content check gated by CAPTCHA"
                    )
                }
            }

            if (probe.code == 404) {
                return ArchiveCandidate(
                    provider = ArchiveProvider.ARCHIVE_TODAY,
                    archiveUrl = directNewestUrl,
                    validationStatus = ArchiveValidationStatus.NOT_ARCHIVED,
                    statusDetail = "No snapshot found on Archive.today",
                    isDirectLookupFallback = true
                )
            }

            if (probe.code == 403 || probe.code == 429 || probe.code == 503 || isCaptchaPage(probe.body)) {
                return ArchiveCandidate(
                    provider = ArchiveProvider.ARCHIVE_TODAY,
                    archiveUrl = directNewestUrl,
                    validationStatus = ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP,
                    statusDetail = "CAPTCHA or rate-limit blocked automated verification — tap to open directly",
                    isDirectLookupFallback = true
                )
            }

            if (probe.code == 200 && probe.body.isNotBlank()) {
                val heuristic = evaluateArchivedHtml(probe.body)
                return ArchiveCandidate(
                    provider = ArchiveProvider.ARCHIVE_TODAY,
                    archiveUrl = directNewestUrl,
                    validationStatus = heuristic.status,
                    estimatedWordCount = heuristic.wordCount.takeIf { it > 0 },
                    extractedTitle = heuristic.extractedTitle,
                    statusDetail = heuristic.detail
                )
            }

            ArchiveCandidate(
                provider = ArchiveProvider.ARCHIVE_TODAY,
                archiveUrl = directNewestUrl,
                validationStatus = ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP,
                statusDetail = "Unvalidated direct lookup (HTTP ${probe.code})",
                isDirectLookupFallback = true
            )
        } catch (_: Exception) {
            ArchiveCandidate(
                provider = ArchiveProvider.ARCHIVE_TODAY,
                archiveUrl = directNewestUrl,
                validationStatus = ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP,
                statusDetail = "Automated check unavailable — tap for direct Archive.today lookup",
                isDirectLookupFallback = true
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // #4 GHOSTARCHIVE (ghostarchive.org — Tier 3 Community Archive)
    // ─────────────────────────────────────────────────────────────────────────────

    private fun lookupGhostarchive(targetUrl: String): ArchiveCandidate {
        val searchUrl = "https://ghostarchive.org/search?term=${urlEncode(targetUrl)}"
        return try {
            val searchResp = fetchText(searchUrl, maxBytes = MAX_STAGE1_HTML_BYTES, followSafeRedirects = true)

            if (searchResp.code == 403 || searchResp.code == 429 || isCaptchaPage(searchResp.body)) {
                return ArchiveCandidate(
                    provider = ArchiveProvider.GHOSTARCHIVE,
                    archiveUrl = searchUrl,
                    validationStatus = ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP,
                    statusDetail = "Rate-limited on automated check — tap to search Ghostarchive directly",
                    isDirectLookupFallback = true
                )
            }

            val snapshotUrl = if (searchResp.code == 200) parseGhostarchiveSearchHtml(searchResp.body) else null
            if (snapshotUrl == null) {
                return ArchiveCandidate(
                    provider = ArchiveProvider.GHOSTARCHIVE,
                    archiveUrl = searchUrl,
                    validationStatus = ArchiveValidationStatus.NOT_ARCHIVED,
                    statusDetail = "No matching Ghostarchive snapshot found",
                    isDirectLookupFallback = true
                )
            }

            // Stage 2: Verify Ghostarchive snapshot content
            val htmlResp = fetchText(snapshotUrl, maxBytes = MAX_STAGE2_HTML_BYTES, followSafeRedirects = true)
            if (htmlResp.code == 200 && htmlResp.body.isNotBlank() && !isCaptchaPage(htmlResp.body)) {
                val heuristic = evaluateArchivedHtml(htmlResp.body)
                ArchiveCandidate(
                    provider = ArchiveProvider.GHOSTARCHIVE,
                    archiveUrl = snapshotUrl,
                    validationStatus = heuristic.status,
                    estimatedWordCount = heuristic.wordCount.takeIf { it > 0 },
                    extractedTitle = heuristic.extractedTitle,
                    statusDetail = heuristic.detail
                )
            } else {
                ArchiveCandidate(
                    provider = ArchiveProvider.GHOSTARCHIVE,
                    archiveUrl = snapshotUrl,
                    validationStatus = ArchiveValidationStatus.SNAPSHOT_UNVERIFIED,
                    statusDetail = "Snapshot link discovered; full-text verification unavailable"
                )
            }
        } catch (_: Exception) {
            ArchiveCandidate(
                provider = ArchiveProvider.GHOSTARCHIVE,
                archiveUrl = searchUrl,
                validationStatus = ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP,
                statusDetail = "Ghostarchive check unavailable — tap for direct search",
                isDirectLookupFallback = true
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Stage 2: Full-Article Heuristic Validator & Parsers (internal for unit tests)
    // ─────────────────────────────────────────────────────────────────────────────

    internal fun evaluateArchivedHtml(html: String): ContentHeuristicResult {
        if (isCaptchaPage(html)) {
            return ContentHeuristicResult(
                status = ArchiveValidationStatus.UNVALIDATED_CAPTCHA_OR_LOOKUP,
                wordCount = 0,
                extractedTitle = null,
                detail = "Archive returned a CAPTCHA or bot challenge"
            )
        }

        val extractedTitle = ArchiveShareFormatter.extractArticleTitle(html)
        val lowerHtml = html.lowercase()
        val hasHardPaywallCopy = ARCHIVE_PAYWALL_BLOCK_MARKERS.any { lowerHtml.contains(it) }

        // Strip scripts/styles/comments/tags to estimate visible article text length
        val stripped = SCRIPT_STYLE_BLOCK_REGEX.replace(html, " ")
            .let { HTML_COMMENT_REGEX.replace(it, " ") }
            .let { HTML_TAG_REGEX.replace(it, " ") }

        val wordCount = WORD_REGEX.findAll(stripped).count()

        return when {
            wordCount >= MIN_FULL_ARTICLE_WORDS && !hasHardPaywallCopy -> {
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.VERIFIED_FULL_ARTICLE,
                    wordCount = wordCount,
                    extractedTitle = extractedTitle,
                    detail = "Full article verified (~$wordCount words, no paywall block)"
                )
            }
            wordCount >= MIN_FULL_ARTICLE_WORDS && hasHardPaywallCopy -> {
                // Substantial text alongside a soft subscriber banner/CTA in footer
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.VERIFIED_FULL_ARTICLE,
                    wordCount = wordCount,
                    extractedTitle = extractedTitle,
                    detail = "Substantial article text verified (~$wordCount words)"
                )
            }
            hasHardPaywallCopy -> {
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED,
                    wordCount = wordCount,
                    extractedTitle = extractedTitle,
                    detail = "Archived snapshot appears truncated or paywalled (~$wordCount words)"
                )
            }
            wordCount >= MIN_PARTIAL_ARTICLE_WORDS -> {
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.SNAPSHOT_UNVERIFIED,
                    wordCount = wordCount,
                    extractedTitle = extractedTitle,
                    detail = "Snapshot found (~$wordCount words); may be brief or JS-rendered"
                )
            }
            else -> {
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED,
                    wordCount = wordCount,
                    extractedTitle = extractedTitle,
                    detail = "Snapshot has minimal readable text (~$wordCount words)"
                )
            }
        }
    }

    internal fun parseWaybackCdxJson(jsonText: String): WaybackCapture? {
        return try {
            val root = JSONArray(jsonText.trim())
            if (root.length() < 2) return null

            val header = root.getJSONArray(0)
            val cols = (0 until header.length()).associate { header.getString(it) to it }
            val tsIdx = cols["timestamp"] ?: return null
            val origIdx = cols["original"] ?: return null
            val lenIdx = cols["length"]

            val captures = mutableListOf<WaybackCapture>()
            for (i in 1 until root.length()) {
                val row = root.optJSONArray(i) ?: continue
                val ts = row.optString(tsIdx, "").trim()
                val orig = row.optString(origIdx, "").trim()
                val len = lenIdx?.let { row.optString(it, "0").toLongOrNull() } ?: 0L
                if (ts.length >= 8 && UrlSecurityValidator.isSafeHttpUrl(orig)) {
                    captures.add(WaybackCapture(ts, orig, len))
                }
            }
            if (captures.isEmpty()) return null

            // Prefer substantial HTML captures (> 2.5 KB compressed) choosing the most recent,
            // otherwise fall back to the largest capture available.
            val substantial = captures.filter { it.byteLength >= 2500L }
            if (substantial.isNotEmpty()) {
                substantial.maxByOrNull { it.timestamp }
            } else {
                captures.maxByOrNull { it.byteLength }
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseWaybackAvailabilityJson(jsonText: String, fallbackOriginalUrl: String): WaybackCapture? {
        return try {
            val root = JSONObject(jsonText.trim())
            val closest = root.optJSONObject("archived_snapshots")?.optJSONObject("closest") ?: return null
            if (!closest.optBoolean("available", false)) return null
            val status = closest.optString("status", "")
            if (status.isNotEmpty() && status != "200") return null
            val ts = closest.optString("timestamp", "").trim()
            if (ts.length < 8) return null
            WaybackCapture(
                timestamp = ts,
                originalUrl = fallbackOriginalUrl,
                byteLength = 0L
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parsePermaCcJson(jsonText: String, targetUrl: String): PermaCapture? {
        return try {
            val root = JSONObject(jsonText.trim())
            val objects = root.optJSONArray("objects") ?: return null
            val normalizedTarget = normalizeUrlForComparison(targetUrl)

            for (i in 0 until objects.length()) {
                val obj = objects.optJSONObject(i) ?: continue
                val guid = obj.optString("guid", "").trim()
                val objUrl = obj.optString("url", "").trim()
                if (guid.isEmpty() || objUrl.isEmpty()) continue

                if (normalizeUrlForComparison(objUrl) == normalizedTarget) {
                    val created = obj.optString("creation_timestamp", "").take(10).ifBlank { null }
                    val title = ArchiveShareFormatter.sanitizeTitle(obj.optString("title", ""))
                    return PermaCapture(
                        guid = guid,
                        archiveUrl = "https://perma.cc/$guid",
                        createdAtDisplay = created,
                        title = title
                    )
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseGhostarchiveSearchHtml(html: String): String? {
        val match = GHOSTARCHIVE_RESULT_REGEX.find(html) ?: return null
        val href = match.groupValues[1].trim()
        val resolved = if (href.startsWith("/")) "https://ghostarchive.org$href" else href
        return if (UrlSecurityValidator.isSafeHttpUrl(resolved)) resolved else null
    }

    internal fun isCaptchaPage(html: String): Boolean {
        if (html.isBlank()) return false
        val lower = html.lowercase()
        return CAPTCHA_MARKERS.any { lower.contains(it) }
    }

    internal fun formatWaybackTimestamp(timestamp: String): String {
        return if (timestamp.length >= 8) {
            "${timestamp.substring(0, 4)}-${timestamp.substring(4, 6)}-${timestamp.substring(6, 8)}"
        } else {
            timestamp
        }
    }

    private fun normalizeUrlForComparison(url: String): String {
        return url.trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("#")
            .trimEnd('/')
    }

    private data class FetchResult(
        val code: Int,
        val body: String,
        val locationHeader: String? = null
    )

    private fun fetchText(
        url: String,
        maxBytes: Long,
        followSafeRedirects: Boolean = false
    ): FetchResult {
        var currentUrl = url
        var hops = 0

        while (hops <= 3) {
            if (UrlSecurityValidator.validateRedirectTarget(currentUrl) != null) {
                return FetchResult(code = 0, body = "")
            }

            val request = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json,text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()

            var nextRedirect: String? = null
            var result: FetchResult? = null

            client.newCall(request).execute().use { response ->
                val code = response.code
                val loc = response.header("Location")?.trim()
                if (followSafeRedirects && code in 300..399 && !loc.isNullOrBlank() && hops < 3) {
                    val resolved = UrlExpander.resolveRedirectLocation(currentUrl, loc)
                    if (UrlSecurityValidator.validateRedirectTarget(resolved) == null) {
                        nextRedirect = resolved
                    } else {
                        result = FetchResult(code = code, body = "", locationHeader = loc)
                    }
                } else {
                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    val readable = contentType.isEmpty() ||
                        contentType.contains("json") ||
                        contentType.contains("html") ||
                        contentType.contains("text") ||
                        contentType.contains("xml")
                    val text = if (readable) {
                        try {
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
                    } else {
                        ""
                    }
                    result = FetchResult(code = code, body = text, locationHeader = loc)
                }
            }

            if (nextRedirect != null) {
                currentUrl = nextRedirect!!
                hops++
            } else {
                return result ?: FetchResult(code = 0, body = "")
            }
        }
        return FetchResult(code = 0, body = "")
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }
}
