package com.example.cleaner

import android.net.Uri
import com.example.model.SecurityAdvisory
import com.example.model.SecuritySeverity
import okhttp3.Dns
import java.net.IDN
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException

/**
 * Security validation, SSRF prevention, scheme allowlisting, WebView domain lockdown,
 * and anti-phishing / homograph detection for untrusted URLs.
 */
object UrlSecurityValidator {

    const val MAX_INPUT_LENGTH = 8192
    const val MAX_NETWORK_EXPANSIONS_PER_INPUT = 3

    private val ALLOWED_HTTP_SCHEMES = setOf("http", "https")

    private val DANGEROUS_EXTENSIONS = listOf(
        ".apk", ".exe", ".msi", ".scr", ".bat", ".cmd", ".dex", ".jar", ".sh", ".vbs"
    )

    private val IPV4_REGEX = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

    /**
     * Custom OkHttp Dns resolver that blocks DNS rebinding and SSRF against loopback,
     * link-local, RFC1918 private LAN, Carrier-Grade NAT, and cloud metadata IP addresses.
     */
    object SafePublicDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (isPrivateOrLoopbackHost(hostname)) {
                throw UnknownHostException("Blocked non-public or loopback hostname: $hostname")
            }
            val addresses = Dns.SYSTEM.lookup(hostname)
            for (address in addresses) {
                if (isPrivateOrLoopbackAddress(address)) {
                    throw UnknownHostException(
                        "Blocked SSRF attempt: $hostname resolved to non-public IP ${address.hostAddress}"
                    )
                }
            }
            return addresses
        }
    }

    /**
     * Validates that a URL uses strictly http:// or https://, has a non-empty host,
     * and contains no control characters.
     */
    fun isSafeHttpUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || trimmed.any { it.code < 0x20 || it.code == 0x7F }) {
            return false
        }
        return try {
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.lowercase() ?: return false
            scheme in ALLOWED_HTTP_SCHEMES && host.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Validates a redirect target or offline-unwrapped URL before following or accepting it.
     * Returns null if safe, or a descriptive security block reason if unsafe.
     */
    fun validateRedirectTarget(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || trimmed.any { it.code < 0x20 || it.code == 0x7F }) {
            return "Blocked malformed redirect target containing control characters."
        }
        val uri = try {
            Uri.parse(trimmed)
        } catch (_: Exception) {
            return "Blocked unparseable redirect URL."
        }

        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme !in ALLOWED_HTTP_SCHEMES) {
            val displayScheme = if (scheme.isNotEmpty()) "$scheme://" else "unknown scheme"
            return "Blocked unsafe non-HTTP redirect scheme ($displayScheme)."
        }

        val host = extractHost(trimmed)
        if (host.isBlank()) {
            return "Blocked redirect target with missing hostname."
        }

        if (isPrivateOrLoopbackHost(host)) {
            return "Blocked SSRF redirect to private, loopback, or local network host ($host)."
        }

        return null
    }

    /**
     * Checks if a hostname string is a known local, loopback, link-local, metadata,
     * or RFC1918 private network literal.
     */
    fun isPrivateOrLoopbackHost(rawHost: String): Boolean {
        val host = rawHost.trim().lowercase().removePrefix("[").removeSuffix("]")
        if (host.isEmpty()) return true

        if (host == "localhost" ||
            host == "ip6-localhost" ||
            host == "ip6-loopback" ||
            host.endsWith(".localhost") ||
            host.endsWith(".local") ||
            host.endsWith(".internal") ||
            host.endsWith(".home.arpa") ||
            host.endsWith(".lan")
        ) {
            return true
        }

        // Check IPv4 literal ranges
        val ipv4Match = IPV4_REGEX.matchEntire(host)
        if (ipv4Match != null) {
            val parts = ipv4Match.groupValues.drop(1).mapNotNull { it.toIntOrNull() }
            if (parts.size == 4 && parts.all { it in 0..255 }) {
                val a = parts[0]
                val b = parts[1]
                return when {
                    a == 0 -> true                           // 0.0.0.0/8
                    a == 10 -> true                          // 10.0.0.0/8 Private
                    a == 100 && b in 64..127 -> true         // 100.64.0.0/10 CGNAT
                    a == 127 -> true                         // 127.0.0.0/8 Loopback
                    a == 169 && b == 254 -> true             // 169.254.0.0/16 Link-local / Cloud Metadata
                    a == 172 && b in 16..31 -> true          // 172.16.0.0/12 Private
                    a == 192 && b == 168 -> true             // 192.168.0.0/16 Private
                    a >= 224 -> true                         // 224.0.0.0/4 Multicast & Reserved
                    else -> false
                }
            }
            return true // Invalid IPv4 octets
        }

        // Check IPv6 literal ranges
        if (host.contains(":")) {
            if (host == "::1" || host == "::" || host == "0:0:0:0:0:0:0:1" || host == "0:0:0:0:0:0:0:0") {
                return true
            }
            if (host.startsWith("fe80:") ||
                host.startsWith("fc") ||
                host.startsWith("fd") ||
                host.startsWith("ff")
            ) {
                return true
            }
            if (host.startsWith("::ffff:")) {
                val mappedIpv4 = host.removePrefix("::ffff:")
                return isPrivateOrLoopbackHost(mappedIpv4)
            }
        }

        return false
    }

    /**
     * Checks a resolved InetAddress against loopback, private, link-local, CGNAT, and multicast ranges.
     */
    fun isPrivateOrLoopbackAddress(address: InetAddress): Boolean {
        if (address.isLoopbackAddress ||
            address.isSiteLocalAddress ||
            address.isLinkLocalAddress ||
            address.isAnyLocalAddress ||
            address.isMulticastAddress
        ) {
            return true
        }

        val bytes = address.address
        if (address is Inet4Address && bytes.size == 4) {
            val a = bytes[0].toInt() and 0xFF
            val b = bytes[1].toInt() and 0xFF
            if (a == 0 ||
                a == 10 ||
                (a == 100 && b in 64..127) ||
                a == 127 ||
                (a == 169 && b == 254) ||
                (a == 172 && b in 16..31) ||
                (a == 192 && b == 168) ||
                a >= 224
            ) {
                return true
            }
        } else if (address is Inet6Address && bytes.size == 16) {
            val firstByte = bytes[0].toInt() and 0xFF
            // Unique local fc00::/7
            if ((firstByte and 0xFE) == 0xFC) {
                return true
            }
        }

        return false
    }

    /**
     * Strictly validates whether a URL is allowed to load inside the headless Facebook WebView.
     * Enforces HTTPS and official Meta/Facebook domains only.
     */
    fun isAllowedFacebookWebViewUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!isSafeHttpUrl(trimmed)) return false
        return try {
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme != "https") return false

            val host = extractHost(trimmed)
            if (host.isBlank() || isPrivateOrLoopbackHost(host)) return false

            host == "facebook.com" ||
                host.endsWith(".facebook.com") ||
                host == "fb.com" ||
                host.endsWith(".fb.com") ||
                host == "fb.me" ||
                host == "fb.watch" ||
                host == "fbcdn.net" ||
                host.endsWith(".fbcdn.net") ||
                host == "fbsbx.com" ||
                host.endsWith(".fbsbx.com")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Detects and strips embedded userInfo credentials (`https://user:pass@host/...`)
     * which are commonly used in deceptive domain spoofing attacks (e.g. `https://trusted.com@evil.com`).
     * Returns Pair(sanitizedUrl, strippedUserInfoOrNull).
     */
    fun stripUserInfoIfPresent(rawUrl: String): Pair<String, String?> {
        val trimmed = rawUrl.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd == -1) return trimmed to null

        val authorityStart = schemeEnd + 3
        val authorityEnd = trimmed.indexOfAny(charArrayOf('/', '?', '#'), authorityStart)
            .let { if (it == -1) trimmed.length else it }

        val authority = trimmed.substring(authorityStart, authorityEnd)
        val atIndex = authority.lastIndexOf('@')
        if (atIndex != -1) {
            val userInfo = authority.substring(0, atIndex)
            val hostPort = authority.substring(atIndex + 1)
            val sanitized = trimmed.substring(0, authorityStart) + hostPort + trimmed.substring(authorityEnd)
            return sanitized to userInfo
        }
        return trimmed to null
    }

    /**
     * Extracts the lowercase hostname from a URL, stripping any userInfo@ or :port.
     */
    fun extractHost(url: String): String {
        val (sanitized, _) = stripUserInfoIfPresent(url)
        return try {
            val jUri = URI(sanitized)
            val host = jUri.host
            if (!host.isNullOrBlank()) {
                host.lowercase()
            } else {
                Uri.parse(sanitized).host?.lowercase().orEmpty()
            }
        } catch (_: Exception) {
            try {
                Uri.parse(sanitized).host?.lowercase().orEmpty()
            } catch (_: Exception) {
                ""
            }
        }
    }

    /**
     * Analyzes a URL for security, phishing, homograph, cleartext, and malware risks.
     */
    fun analyzeSecurityRisks(
        url: String,
        strippedUserInfo: String? = null,
        blockedRedirectReason: String? = null
    ): List<SecurityAdvisory> {
        val advisories = mutableListOf<SecurityAdvisory>()

        if (!blockedRedirectReason.isNullOrBlank()) {
            advisories.add(
                SecurityAdvisory(
                    code = "BLOCKED_UNSAFE_REDIRECT",
                    severity = SecuritySeverity.HIGH,
                    title = "Unsafe Redirect Blocked (SSRF / Scheme Guard)",
                    description = blockedRedirectReason
                )
            )
        }

        if (!strippedUserInfo.isNullOrBlank()) {
            advisories.add(
                SecurityAdvisory(
                    code = "EMBEDDED_CREDENTIALS_SPOOF",
                    severity = SecuritySeverity.HIGH,
                    title = "Embedded URL Credentials Stripped (user@host)",
                    description = "Removed deceptive '$strippedUserInfo@' prefix from the URL authority, which is frequently used to disguise phishing domains."
                )
            )
        }

        val trimmed = url.trim()
        val uri = try { Uri.parse(trimmed) } catch (_: Exception) { null }
        val scheme = uri?.scheme?.lowercase().orEmpty()
        val host = extractHost(trimmed)
        val path = uri?.path?.lowercase().orEmpty()

        if (host.isNotBlank()) {
            // Check IDN Homograph / Punycode (xn--) or non-ASCII characters in hostname
            val asciiHost = try {
                IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).lowercase()
            } catch (_: Exception) {
                host
            }
            val hasNonAscii = host.any { it.code > 127 }
            val hasPunycode = host.contains("xn--") || asciiHost.contains("xn--")
            if (hasNonAscii || hasPunycode) {
                advisories.add(
                    SecurityAdvisory(
                        code = "IDN_HOMOGRAPH_RISK",
                        severity = SecuritySeverity.HIGH,
                        title = "Internationalized Domain (IDN Homograph Risk)",
                        description = "Hostname ($asciiHost) uses Punycode or non-ASCII characters that may visually impersonate a trusted brand."
                    )
                )
            }

            // Check raw IP literal hostname
            val cleanHost = host.removePrefix("[").removeSuffix("]")
            if (IPV4_REGEX.matches(cleanHost) || cleanHost.contains(":")) {
                val isPrivate = isPrivateOrLoopbackHost(cleanHost)
                advisories.add(
                    SecurityAdvisory(
                        code = if (isPrivate) "PRIVATE_IP_HOST" else "RAW_IP_HOST",
                        severity = if (isPrivate) SecuritySeverity.HIGH else SecuritySeverity.MEDIUM,
                        title = if (isPrivate) "Private / Local Network IP Address" else "Raw IP Address Hostname",
                        description = if (isPrivate) {
                            "URL points directly to a non-public local or loopback IP ($cleanHost)."
                        } else {
                            "URL uses a raw numeric IP address ($cleanHost) instead of a verified domain name."
                        }
                    )
                )
            }
        }

        // Check dangerous executable / package file extensions
        val matchedExt = DANGEROUS_EXTENSIONS.firstOrNull { ext ->
            path.endsWith(ext)
        }
        if (matchedExt != null) {
            advisories.add(
                SecurityAdvisory(
                    code = "EXECUTABLE_DOWNLOAD_RISK",
                    severity = SecuritySeverity.HIGH,
                    title = "Direct Executable / Package Link ($matchedExt)",
                    description = "This URL points directly to an installable package or executable ($matchedExt). Do not install files from untrusted sources."
                )
            )
        }

        // Check unencrypted cleartext HTTP
        if (scheme == "http") {
            advisories.add(
                SecurityAdvisory(
                    code = "CLEARTEXT_HTTP",
                    severity = SecuritySeverity.MEDIUM,
                    title = "Unencrypted HTTP Connection",
                    description = "This link uses cleartext http:// instead of https:// and is vulnerable to network interception."
                )
            )
        }

        return advisories
    }
}
