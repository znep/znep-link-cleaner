package com.example.cleaner

import com.example.model.AccessRestrictionType
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Extracts clean article titles from HTML metadata, determines whether an article title
 * is already represented in the URL path slug, and builds the structured clipboard/share
 * bundle containing the labeled original link, archival link, and conditional title.
 */
object ArchiveShareFormatter {

    private val OG_TITLE_REGEX = Regex(
        """<meta[^>]+(?:property|name)\s*=\s*["'](?:og:title|twitter:title)["'][^>]+content\s*=\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE
    )

    private val OG_TITLE_CONTENT_FIRST_REGEX = Regex(
        """<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]+(?:property|name)\s*=\s*["'](?:og:title|twitter:title)["']""",
        RegexOption.IGNORE_CASE
    )

    private val JSON_LD_HEADLINE_REGEX = Regex(
        """"headline"\s*:\s*"((?:\\.|[^"\\]){4,260})"""",
        RegexOption.IGNORE_CASE
    )

    private val HTML_TITLE_REGEX = Regex(
        """<title[^>]*>(.*?)</title>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val NUMERIC_ENTITY_REGEX = Regex("""&#(\d{2,6});""")
    private val HEX_ENTITY_REGEX = Regex("""&#x([0-9a-fA-F]{2,6});""")
    private val WHITESPACE_REGEX = Regex("""\s+""")
    private val NON_ALNUM_REGEX = Regex("""[^a-z0-9]+""")

    private val GENERIC_BLOCK_TITLES = listOf(
        "subscribe",
        "subscription",
        "sign in",
        "log in",
        "login",
        "access denied",
        "just a moment",
        "attention required",
        "403 forbidden",
        "404 not found",
        "451 unavailable",
        "unavailable for legal reasons",
        "are you a robot",
        "robot or human",
        "verify you are human",
        "security check",
        "page not found",
        "captcha",
        "wayback machine",
        "internet archive",
        "archive.today",
        "archive.ph",
        "archive.is",
        "perma.cc",
        "ghostarchive",
        "please enable js",
        "cloudflare"
    )

    private val STOP_WORDS = setOf(
        "the", "and", "for", "that", "this", "with", "from", "into", "over", "under",
        "about", "after", "before", "between", "through", "during", "without", "within",
        "your", "our", "their", "its", "are", "was", "were", "has", "have", "had",
        "will", "would", "could", "should", "may", "might", "can", "not", "but",
        "how", "why", "what", "when", "where", "who", "new", "more", "most", "out"
    )

    private val GENERIC_URL_SEGMENTS = setOf(
        "article", "articles", "news", "story", "stories", "content", "post", "posts",
        "index", "amp", "html", "htm", "php", "aspx", "jsp", "www", "en", "us", "uk",
        "world", "business", "technology", "tech", "politics", "opinion", "markets",
        "science", "health", "culture", "lifestyle", "sports", "archive", "web"
    )

    /**
     * Extracts and sanitizes a readable article title from an HTML snippet.
     * Rejects generic paywall/login/CAPTCHA/archive-shell titles.
     */
    fun extractArticleTitle(html: String): String? {
        if (html.isBlank()) return null

        val rawCandidates = listOfNotNull(
            OG_TITLE_REGEX.find(html)?.groupValues?.getOrNull(1),
            OG_TITLE_CONTENT_FIRST_REGEX.find(html)?.groupValues?.getOrNull(1),
            JSON_LD_HEADLINE_REGEX.find(html)?.groupValues?.getOrNull(1)?.let { unescapeJsonString(it) },
            HTML_TITLE_REGEX.find(html)?.groupValues?.getOrNull(1)
        )

        for (raw in rawCandidates) {
            val cleaned = sanitizeTitle(raw)
            if (cleaned != null) return cleaned
        }
        return null
    }

    /**
     * Sanitizes a candidate title string (from HTML or API metadata like Perma.cc),
     * stripping publisher suffixes, control characters, and generic wall/error placeholders.
     */
    fun sanitizeTitle(rawTitle: String?): String? {
        if (rawTitle.isNullOrBlank()) return null

        var text = decodeHtmlEntities(rawTitle)
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()

        if (text.length < 4) return null

        // Strip trailing publisher suffix like " - The New York Times" or " | Financial Times"
        text = stripPublisherSuffix(text)
        if (text.length < 4) return null

        val lower = text.lowercase()
        if (GENERIC_BLOCK_TITLES.any { lower == it || lower.startsWith("$it ") || lower.contains("access denied") || lower.contains("just a moment") || lower.contains("verify you are human") || lower.contains("attention required") }) {
            return null
        }

        return text.take(160).trim()
    }

    /**
     * Returns true if the extracted article title is already clearly represented in the URL path slug
     * (for example, "/2026/11/10/fed-holds-interest-rates-steady.html"), and false for opaque URLs
     * (such as "/content/8a9f21c0-3b4e" or "/articles/12345678").
     */
    fun isTitleRepresentedInUrl(title: String, url: String): Boolean {
        val cleanTitle = sanitizeTitle(title) ?: return true
        val path = try {
            val rawPath = URI(url.trim()).rawPath.orEmpty()
            URLDecoder.decode(rawPath, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            url.substringAfter("://", url).substringAfter("/", "").substringBefore("?").substringBefore("#")
        }

        if (path.isBlank() || path == "/") return false

        val cleanPath = path
            .replace(Regex("""\.(html?|php|aspx?|jsp|cfm)$""", RegexOption.IGNORE_CASE), "")
            .lowercase()

        val urlTokens = NON_ALNUM_REGEX.split(cleanPath)
            .filter { token ->
                token.length >= 3 &&
                    !token.all { it.isDigit() } &&
                    token !in GENERIC_URL_SEGMENTS
            }
            .toSet()

        if (urlTokens.isEmpty()) return false

        val titleTokens = NON_ALNUM_REGEX.split(cleanTitle.lowercase())
            .filter { token ->
                token.length >= 3 &&
                    !token.all { it.isDigit() } &&
                    token !in STOP_WORDS
            }
            .distinct()

        if (titleTokens.isEmpty()) return false

        val matchedCount = titleTokens.count { titleWord ->
            urlTokens.any { urlWord ->
                urlWord == titleWord ||
                    (titleWord.length >= 5 && urlWord.startsWith(titleWord.take(5))) ||
                    (urlWord.length >= 5 && titleWord.startsWith(urlWord.take(5)))
            }
        }

        return if (titleTokens.size <= 2) {
            matchedCount == titleTokens.size
        } else {
            val ratio = matchedCount.toDouble() / titleTokens.size.toDouble()
            matchedCount >= 3 && ratio >= 0.60
        }
    }

    /**
     * Formats the clipboard / share bundle with:
     * 1. Extracted article title (ONLY when available and NOT already represented in the URL path)
     * 2. Original cleaned URL explicitly labeled as Paywalled / Region-Locked / Blocked
     * 3. Archival URL labeled with its archive provider name
     */
    fun formatClipboardBundle(
        originalUrl: String,
        archiveUrl: String,
        providerName: String,
        restrictionType: AccessRestrictionType,
        title: String?
    ): String {
        val label = when (restrictionType) {
            AccessRestrictionType.PAYWALL -> "Paywalled"
            AccessRestrictionType.REGION_LOCKED -> "Region-Locked"
            AccessRestrictionType.ACCESS_BLOCKED -> "Blocked"
            AccessRestrictionType.REGISTRATION_WALL -> "Registration-Gated"
            AccessRestrictionType.NONE -> "Paywalled"
        }

        val sanitizedTitle = sanitizeTitle(title)
        val includeTitle = sanitizedTitle != null && !isTitleRepresentedInUrl(sanitizedTitle, originalUrl)

        return buildString {
            if (includeTitle) {
                appendLine(sanitizedTitle)
            }
            appendLine("Original ($label): ${originalUrl.trim()}")
            append("Archive ($providerName): ${archiveUrl.trim()}")
        }
    }

    /**
     * Picks the best non-generic title between an archive snapshot's title and the original site's title.
     * Prefers the archive title when available (since original pages often return login/wall titles),
     * falling back to the live probe's title.
     */
    fun selectBestTitle(archiveTitle: String?, originalProbeTitle: String?): String? {
        return sanitizeTitle(archiveTitle) ?: sanitizeTitle(originalProbeTitle)
    }

    private fun stripPublisherSuffix(title: String): String {
        val separators = listOf(" | ", " - ", " — ", " – ", " :: ")
        for (sep in separators) {
            val idx = title.lastIndexOf(sep)
            if (idx >= 16) {
                val suffix = title.substring(idx + sep.length).trim()
                if (suffix.length in 2..42) {
                    return title.substring(0, idx).trim()
                }
            }
        }
        return title
    }

    private fun unescapeJsonString(raw: String): String {
        return raw
            .replace("\\\"", "\"")
            .replace("\\/", "/")
            .replace("\\n", " ")
            .replace("\\t", " ")
            .replace("\\\\", "\\")
    }

    private fun decodeHtmlEntities(input: String): String {
        var result = input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&rsquo;", "'")
            .replace("&lsquo;", "'")
            .replace("&rdquo;", "\"")
            .replace("&ldquo;", "\"")
            .replace("&hellip;", "...")

        result = NUMERIC_ENTITY_REGEX.replace(result) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 32..0x10FFFF) {
                String(Character.toChars(code))
            } else {
                match.value
            }
        }

        result = HEX_ENTITY_REGEX.replace(result) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null && code in 32..0x10FFFF) {
                String(Character.toChars(code))
            } else {
                match.value
            }
        }
        return result
    }
}
