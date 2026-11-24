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
 * Stage 2 strips navigation/header/footer/modal chrome, inspects hard paywall barriers
 * (such as FT's `o-barrier`, `barrier-grid`, JSON-LD `isAccessibleForFree: false`, and
 * subscription pricing tables), and requires genuine multi-paragraph `<p>` editorial prose
 * before marking a snapshot as [ArchiveValidationStatus.VERIFIED_FULL_ARTICLE].
 */
object ArchiveLookupEngine {

    private const val MAX_STAGE1_HTML_BYTES = 96 * 1024L
    private const val MAX_STAGE2_HTML_BYTES = 128 * 1024L

    // Editorial prose thresholds (measured ONLY on non-boilerplate <p> paragraphs after stripping nav/footer chrome)
    private const val MIN_WORDS_PER_EDITORIAL_PARAGRAPH = 15
    private const val MIN_EDITORIAL_PARAGRAPHS_FULL = 4
    private const val MIN_EDITORIAL_WORDS_FULL = 250
    private const val MIN_EDITORIAL_PARAGRAPHS_WHEN_JSONLD_LOCKED = 6
    private const val MIN_EDITORIAL_WORDS_WHEN_JSONLD_LOCKED = 380
    private const val MIN_EDITORIAL_PARAGRAPHS_WHEN_SOFT_CTA = 8
    private const val MIN_EDITORIAL_WORDS_WHEN_SOFT_CTA = 450
    private const val MIN_EDITORIAL_WORDS_UNVERIFIED = 100

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val NON_CONTENT_BLOCK_REGEX = Regex(
        """<(script|style|noscript|svg|template|iframe|nav|header|footer|aside|form|menu|dialog|select)[^>]*>.*?</\1>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HTML_COMMENT_REGEX = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)
    private val PARAGRAPH_TAG_REGEX = Regex(
        """<p(?:\s[^>]*)?>(.*?)</p>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    private val WORD_REGEX = Regex("""[A-Za-z]{2,}""")
    private val CURRENCY_PRICE_REGEX = Regex("""[$£€]\s?\d+(?:\.\d{1,2})?""")

    private val JSON_LD_NOT_FREE_REGEX = Regex(
        """"isAccessibleForFree"\s*:\s*(?:false|"false"|'false')""",
        RegexOption.IGNORE_CASE
    )

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

    /**
     * Explicit DOM barrier containers that publishers (e.g., Financial Times Origami `o-barrier`,
     * WSJ/NYT gateway barriers) render when replacing the article body with a paywall screen.
     */
    private val HARD_BARRIER_DOM_MARKERS = listOf(
        "o-barrier",
        "barrier-grid",
        "data-trackable=\"barrier\"",
        "data-trackable='barrier'",
        "paywall-barrier",
        "ft-barrier",
        "subscribe-barrier",
        "id=\"barrier",
        "class=\"barrier",
        "gateway-content",
        "snippet--paywall",
        "regwall-container",
        "paywall-wall",
        "piano-paywall"
    )

    /**
     * Subscription barrier / paywall gate copy found inside archived snapshots that only captured
     * the locked view.
     */
    private val ARCHIVE_PAYWALL_BLOCK_MARKERS = listOf(
        "subscribe to unlock",
        "try unlimited access",
        "subscribe to read",
        "subscribe to continue reading",
        "keep reading with a subscription",
        "this article is for subscribers",
        "unlock this article",
        "you have reached your article limit",
        "you've reached your limit of free articles",
        "create a free account to continue",
        "already a subscriber? sign in",
        "log in to continue reading",
        "to continue reading this article",
        "become a subscriber to read",
        "explore our subscription offers",
        "choose your subscription"
    )

    private val PRICING_TIER_BOILERPLATE_PAIR = "standard digital" to "premium digital"

    private val PARAGRAPH_BOILERPLATE_PHRASES = listOf(
        "subscribe",
        "subscription",
        "unlimited access",
        "per month",
        "per week",
        "per year",
        "billed annually",
        "cancel anytime",
        "already a subscriber",
        "sign in",
        "log in",
        "cookie",
        "privacy policy",
        "terms of service",
        "terms & conditions",
        "all rights reserved",
        "standard digital",
        "premium digital",
        "manage cookies",
        "accept all",
        "copyright ©",
        "© the financial times"
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
        val detail: String,
        val editorialParagraphCount: Int = 0
    )

    data class WaybackCapture(
        val timestamp: String,
        val originalUrl: String,
        val byteLength: Long,
        val digest: String = ""
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
                "&fl=timestamp,original,statuscode,mimetype,digest,length" +
                "&filter=statuscode:200&filter=mimetype:text/html&collapse=digest&limit=-5"

            val cdxResp = fetchText(cdxUrl, maxBytes = MAX_STAGE1_HTML_BYTES)
            var candidateCaptures = if (cdxResp.code == 200) parseWaybackCdxCaptures(cdxResp.body) else emptyList()

            // Fallback to Wayback Availability JSON API if CDX returned nothing or timed out
            if (candidateCaptures.isEmpty()) {
                val availUrl = "https://archive.org/wayback/available?url=$encoded"
                val availResp = fetchText(availUrl, maxBytes = 32 * 1024L)
                if (availResp.code == 200) {
                    parseWaybackAvailabilityJson(availResp.body, targetUrl)?.let {
                        candidateCaptures = listOf(it)
                    }
                }
            }

            if (candidateCaptures.isEmpty()) {
                return ArchiveCandidate(
                    provider = ArchiveProvider.WAYBACK_MACHINE,
                    archiveUrl = fallbackSearchUrl,
                    validationStatus = ArchiveValidationStatus.NOT_ARCHIVED,
                    statusDetail = "No HTTP 200 HTML capture found in Wayback index",
                    isDirectLookupFallback = true
                )
            }

            // Stage 2: Inspect newest capture first; if paywalled, try 1 earlier distinct-digest capture
            var firstResult: ArchiveCandidate? = null
            for (capture in candidateCaptures.take(2)) {
                val rawHtmlResp = fetchText(capture.rawHtmlUrl, maxBytes = MAX_STAGE2_HTML_BYTES, followSafeRedirects = true)
                val evaluated = if (rawHtmlResp.code == 200 && rawHtmlResp.body.isNotBlank()) {
                    val heuristic = evaluateArchivedHtml(rawHtmlResp.body)
                    ArchiveCandidate(
                        provider = ArchiveProvider.WAYBACK_MACHINE,
                        archiveUrl = capture.viewerUrl,
                        validationStatus = heuristic.status,
                        capturedAtDisplay = capture.formattedDate,
                        estimatedWordCount = heuristic.wordCount.takeIf { it > 0 },
                        extractedTitle = heuristic.extractedTitle,
                        statusDetail = heuristic.detail
                    )
                } else {
                    ArchiveCandidate(
                        provider = ArchiveProvider.WAYBACK_MACHINE,
                        archiveUrl = capture.viewerUrl,
                        validationStatus = ArchiveValidationStatus.SNAPSHOT_UNVERIFIED,
                        capturedAtDisplay = capture.formattedDate,
                        statusDetail = "Snapshot indexed (${capture.formattedDate}); full-text check timed out"
                    )
                }

                if (evaluated.validationStatus == ArchiveValidationStatus.VERIFIED_FULL_ARTICLE) {
                    return evaluated
                }
                if (firstResult == null) {
                    firstResult = evaluated
                } else if (firstResult.isPaywalledSnapshot && !evaluated.isPaywalledSnapshot) {
                    return evaluated
                }
            }

            firstResult ?: ArchiveCandidate(
                provider = ArchiveProvider.WAYBACK_MACHINE,
                archiveUrl = fallbackSearchUrl,
                validationStatus = ArchiveValidationStatus.NOT_ARCHIVED,
                statusDetail = "No usable Wayback capture found",
                isDirectLookupFallback = true
            )
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
                    statusDetail = "CAPTCHA or rate-limit blocked automated check — tap to open directly",
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
    // Stage 2: Strict Editorial Prose & Paywall Barrier Validator
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

        // 1. Inspect hard paywall barriers before stripping chrome
        val hasHardBarrierDom = HARD_BARRIER_DOM_MARKERS.any { lowerHtml.contains(it) }
        val hasPricingTierPair = lowerHtml.contains(PRICING_TIER_BOILERPLATE_PAIR.first) &&
            lowerHtml.contains(PRICING_TIER_BOILERPLATE_PAIR.second)
        val hasHardPaywallCopy = ARCHIVE_PAYWALL_BLOCK_MARKERS.any { lowerHtml.contains(it) } || hasPricingTierPair
        val hasJsonLdNotFree = JSON_LD_NOT_FREE_REGEX.containsMatchIn(html)

        // 2. Strip non-editorial chrome (<nav>, <header>, <footer>, <aside>, <form>, <script>, <style>, etc.)
        val bodyOnly = stripStructuralChrome(html)

        // 3. Extract ONLY substantive, non-boilerplate <p> editorial paragraphs
        val editorialParagraphs = extractEditorialParagraphs(bodyOnly)
        val editorialParagraphCount = editorialParagraphs.size
        val editorialWordCount = editorialParagraphs.sumOf { countWords(it) }

        return when {
            // Hard barrier DOM container (e.g. FT `o-barrier` / `barrier-grid`) ALWAYS disqualifies as paywalled
            hasHardBarrierDom || hasPricingTierPair -> {
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED,
                    wordCount = editorialWordCount,
                    extractedTitle = extractedTitle,
                    editorialParagraphCount = editorialParagraphCount,
                    detail = "Archived copy still shows a paywall barrier ($editorialParagraphCount editorial paragraphs)"
                )
            }

            // Explicit paywall copy present: only pass if overwhelming genuine article prose exists
            hasHardPaywallCopy -> {
                if (editorialParagraphCount >= MIN_EDITORIAL_PARAGRAPHS_WHEN_SOFT_CTA &&
                    editorialWordCount >= MIN_EDITORIAL_WORDS_WHEN_SOFT_CTA
                ) {
                    ContentHeuristicResult(
                        status = ArchiveValidationStatus.VERIFIED_FULL_ARTICLE,
                        wordCount = editorialWordCount,
                        extractedTitle = extractedTitle,
                        editorialParagraphCount = editorialParagraphCount,
                        detail = "Full article verified ($editorialParagraphCount paragraphs · ~$editorialWordCount editorial words)"
                    )
                } else {
                    ContentHeuristicResult(
                        status = ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED,
                        wordCount = editorialWordCount,
                        extractedTitle = extractedTitle,
                        editorialParagraphCount = editorialParagraphCount,
                        detail = "Archived snapshot is still paywalled ($editorialParagraphCount paragraphs · ~$editorialWordCount words)"
                    )
                }
            }

            // JSON-LD declares isAccessibleForFree=false: require stronger editorial prose proof
            hasJsonLdNotFree -> {
                if (editorialParagraphCount >= MIN_EDITORIAL_PARAGRAPHS_WHEN_JSONLD_LOCKED &&
                    editorialWordCount >= MIN_EDITORIAL_WORDS_WHEN_JSONLD_LOCKED
                ) {
                    ContentHeuristicResult(
                        status = ArchiveValidationStatus.VERIFIED_FULL_ARTICLE,
                        wordCount = editorialWordCount,
                        extractedTitle = extractedTitle,
                        editorialParagraphCount = editorialParagraphCount,
                        detail = "Full article verified ($editorialParagraphCount paragraphs · ~$editorialWordCount editorial words)"
                    )
                } else {
                    ContentHeuristicResult(
                        status = ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED,
                        wordCount = editorialWordCount,
                        extractedTitle = extractedTitle,
                        editorialParagraphCount = editorialParagraphCount,
                        detail = "Archived snapshot appears paywalled (JSON-LD locked · $editorialParagraphCount paragraphs)"
                    )
                }
            }

            // Clean open snapshot: require >= 4 genuine editorial <p> paragraphs & >= 250 editorial words
            editorialParagraphCount >= MIN_EDITORIAL_PARAGRAPHS_FULL &&
                editorialWordCount >= MIN_EDITORIAL_WORDS_FULL -> {
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.VERIFIED_FULL_ARTICLE,
                    wordCount = editorialWordCount,
                    extractedTitle = extractedTitle,
                    editorialParagraphCount = editorialParagraphCount,
                    detail = "Full article verified ($editorialParagraphCount paragraphs · ~$editorialWordCount editorial words)"
                )
            }

            // Brief or JS-rendered snapshot without barrier signals
            editorialParagraphCount >= 2 && editorialWordCount >= MIN_EDITORIAL_WORDS_UNVERIFIED -> {
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.SNAPSHOT_UNVERIFIED,
                    wordCount = editorialWordCount,
                    extractedTitle = extractedTitle,
                    editorialParagraphCount = editorialParagraphCount,
                    detail = "Snapshot found ($editorialParagraphCount paragraphs · ~$editorialWordCount words); may be brief or partial"
                )
            }

            else -> {
                ContentHeuristicResult(
                    status = ArchiveValidationStatus.SNAPSHOT_PARTIAL_OR_PAYWALLED,
                    wordCount = editorialWordCount,
                    extractedTitle = extractedTitle,
                    editorialParagraphCount = editorialParagraphCount,
                    detail = "Archived snapshot lacks full article body ($editorialParagraphCount editorial paragraphs)"
                )
            }
        }
    }

    private fun stripStructuralChrome(html: String): String {
        var cleaned = HTML_COMMENT_REGEX.replace(html, " ")
        // Run twice to catch nested structural blocks cleanly (e.g., <nav> inside <header>)
        repeat(2) {
            cleaned = NON_CONTENT_BLOCK_REGEX.replace(cleaned, " ")
        }
        return cleaned
    }

    private fun extractEditorialParagraphs(bodyHtml: String): List<String> {
        val result = mutableListOf<String>()
        for (match in PARAGRAPH_TAG_REGEX.findAll(bodyHtml)) {
            val rawInner = match.groupValues[1]
            val plain = HTML_TAG_REGEX.replace(rawInner, " ")
                .replace("&nbsp;", " ")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (plain.isEmpty()) continue
            val wordCount = countWords(plain)
            if (wordCount < MIN_WORDS_PER_EDITORIAL_PARAGRAPH) continue

            val lower = plain.lowercase()
            if (PARAGRAPH_BOILERPLATE_PHRASES.any { lower.contains(it) }) continue
            if (CURRENCY_PRICE_REGEX.containsMatchIn(plain) &&
                (lower.contains("trial") || lower.contains("offer") || lower.contains("save") || lower.contains("access"))
            ) {
                continue
            }

            result.add(plain)
        }
        return result
    }

    private fun countWords(text: String): Int = WORD_REGEX.findAll(text).count()

    internal fun parseWaybackCdxJson(jsonText: String): WaybackCapture? {
        return parseWaybackCdxCaptures(jsonText).firstOrNull()
    }

    /**
     * Parses Wayback CDX JSON and returns up to 2 preferred captures with distinct digests:
     * 1) the most recent substantial capture (> 2.5 KB) and
     * 2) an earlier distinct-digest capture as a fallback in case the newest capture hit a paywall.
     */
    internal fun parseWaybackCdxCaptures(jsonText: String): List<WaybackCapture> {
        return try {
            val root = JSONArray(jsonText.trim())
            if (root.length() < 2) return emptyList()

            val header = root.getJSONArray(0)
            val cols = (0 until header.length()).associate { header.getString(it) to it }
            val tsIdx = cols["timestamp"] ?: return emptyList()
            val origIdx = cols["original"] ?: return emptyList()
            val lenIdx = cols["length"]
            val digestIdx = cols["digest"]

            val captures = mutableListOf<WaybackCapture>()
            for (i in 1 until root.length()) {
                val row = root.optJSONArray(i) ?: continue
                val ts = row.optString(tsIdx, "").trim()
                val orig = row.optString(origIdx, "").trim()
                val len = lenIdx?.let { row.optString(it, "0").toLongOrNull() } ?: 0L
                val digest = digestIdx?.let { row.optString(it, "").trim() }.orEmpty()
                if (ts.length >= 8 && UrlSecurityValidator.isSafeHttpUrl(orig)) {
                    captures.add(WaybackCapture(ts, orig, len, digest))
                }
            }
            if (captures.isEmpty()) return emptyList()

            val substantial = captures.filter { it.byteLength >= 2500L }
            val pool = if (substantial.isNotEmpty()) substantial else captures
            val sortedNewestFirst = pool.sortedByDescending { it.timestamp }
            val primary = sortedNewestFirst.first()
            val earlierDistinct = sortedNewestFirst.drop(1).lastOrNull {
                it.digest.isEmpty() || it.digest != primary.digest
            }
            listOfNotNull(primary, earlierDistinct)
        } catch (_: Exception) {
            emptyList()
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
