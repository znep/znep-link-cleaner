package com.example.cleaner

import android.content.Context
import android.net.Uri
import com.example.model.CleanResult
import com.example.model.ExpansionWarningType
import com.example.model.RedirectHop
import com.example.model.StrippedParam

object LinkCleaner {

    private val URL_REGEX = Regex(
        """(https?://[^\s<>"'\)]+)""",
        RegexOption.IGNORE_CASE
    )

    private val AMAZON_ASIN_REGEX = Regex(
        """(?:/dp/|/gp/product/)([A-Z0-9]{10})(?:/.*|$)""",
        RegexOption.IGNORE_CASE
    )

    val KNOWN_TRACKING_PARAMS = mapOf(
        // Google / Marketing
        "utm_source" to ("Google / Marketing" to "Identifies advertiser or site sending traffic"),
        "utm_medium" to ("Google / Marketing" to "Identifies marketing medium (e.g. cpc, email, banner)"),
        "utm_campaign" to ("Google / Marketing" to "Identifies specific product campaign"),
        "utm_term" to ("Google / Marketing" to "Identifies paid search keywords"),
        "utm_content" to ("Google / Marketing" to "Used for A/B testing ad variations"),
        "utm_id" to ("Google / Marketing" to "Campaign identification number"),
        "utm_reader" to ("Google / Marketing" to "RSS/reader application tag"),
        "utm_place" to ("Google / Marketing" to "Placement location tag"),
        "utm_name" to ("Google / Marketing" to "Campaign label"),
        "gclid" to ("Google Ads" to "Google Click Identifier used to track ad conversion"),
        "gclsrc" to ("Google Ads" to "Google Click Source identifier"),
        "dclid" to ("Google Display" to "DoubleClick Click Identifier"),
        "gad_source" to ("Google Ads" to "Google Ads delivery origin indicator"),
        "gbraid" to ("Google Ads" to "Google Web-to-App measurement identifier"),
        "wbraid" to ("Google Ads" to "Google App-to-Web measurement identifier"),
        "_ga" to ("Google Analytics" to "Google Analytics client persistent ID"),
        "_gl" to ("Google Analytics" to "Cross-domain linking identifier"),
        "gs_l" to ("Google Search" to "Google search telemetry token"),

        // Meta (Facebook & Instagram)
        "fbclid" to ("Meta / Facebook" to "Facebook Click Identifier for user profiling"),
        "mibextid" to ("Meta / Facebook" to "Mobile in-app browser external share tracker"),
        "rdid" to ("Meta / Facebook" to "Referral device telemetry token"),
        "rc" to ("Meta / Facebook" to "Referral content tracking token"),
        "share_url" to ("Meta / Facebook" to "Share telemetry parameter"),
        "__tn__" to ("Meta / Facebook" to "Timeline navigation tracking token"),
        "__cft__" to ("Meta / Facebook" to "Feed interaction tracking token"),
        "fb_action_ids" to ("Meta / Facebook" to "Facebook Graph API action tracking"),
        "fb_action_types" to ("Meta / Facebook" to "Facebook user activity category"),
        "fb_source" to ("Meta / Facebook" to "Origin link tracking"),
        "fb_ref" to ("Meta / Facebook" to "Referral source token"),
        "hpt_id" to ("Meta / Facebook" to "Home page timeline tracking"),
        "fref" to ("Meta / Facebook" to "Feed referral tracking"),
        "hc_location" to ("Meta / Facebook" to "Home container location token"),
        "notif_id" to ("Meta / Facebook" to "Notification telemetry reference ID"),
        "notif_t" to ("Meta / Facebook" to "Notification type tag"),
        "igshid" to ("Instagram" to "Instagram unique user share identifier"),
        "igsh" to ("Instagram" to "Instagram short share telemetry token"),
        "exln" to ("Instagram / Meta" to "Instagram external link share and user session tracking token"),
        "obrf" to ("Instagram / Meta" to "Instagram off-platform browse referral tag"),
        "vrfl" to ("Instagram / Meta" to "Instagram verification referral link token"),
        "srtk" to ("Instagram / Meta" to "Instagram share routing token"),
        "cplk" to ("Instagram / Meta" to "Instagram copy link session tracker"),
        "dlrf" to ("Instagram / Meta" to "Instagram direct link referral tracker"),
        "mdxt" to ("Instagram / Meta" to "Instagram media external tracking token"),
        "__coig_restricted" to ("Instagram / Meta" to "Instagram cross-origin cookie tracking parameter"),
        "btag" to ("Instagram / Meta" to "Meta browser tracking tag"),
        "xmt" to ("Meta / Threads" to "Meta Threads / Instagram cross-app share telemetry token"),
        "slof" to ("Meta / Threads" to "Meta Threads share log parameter"),

        // TikTok
        "_r" to ("TikTok" to "TikTok referral user identifier"),
        "_t" to ("TikTok" to "TikTok timestamp tracking token"),
        "tt_medium" to ("TikTok" to "TikTok marketing medium"),
        "tt_content" to ("TikTok" to "TikTok campaign variant tag"),
        "is_from_webapp" to ("TikTok" to "Device source telemetry"),
        "sender_device" to ("TikTok" to "Sender device type fingerprint"),
        "share_app_id" to ("TikTok" to "Shared client ID"),
        "share_item_id" to ("TikTok" to "Shared item tracking identifier"),
        "share_link_id" to ("TikTok" to "Unique share link tracking token"),
        "checksum" to ("TikTok" to "Payload verification telemetry"),
        "sec_uid" to ("TikTok" to "Encrypted user tracking ID"),
        "ug_source" to ("TikTok" to "User growth referral campaign"),
        "ugbiz_name" to ("TikTok" to "Business analytics tag"),
        "u_code" to ("TikTok" to "TikTok user profile share tracking code"),
        "preview_pb" to ("TikTok" to "TikTok share preview session token"),
        "ug_btm" to ("TikTok" to "TikTok user growth navigation tracking tag"),
        "ttclid" to ("TikTok Ads" to "TikTok Click Identifier for ad conversion"),

        // Twitter / X
        "twclid" to ("Twitter / X" to "Twitter Click Identifier for conversion tracking"),
        "ref_src" to ("Twitter / X" to "Referrer source tracking token"),
        "ref_url" to ("Twitter / X" to "Referrer origin URL token"),

        // YouTube & Spotify
        "si" to ("Media Telemetry" to "Tracking ID embedded into shared URLs"),
        "feature" to ("Media Telemetry" to "UI element that initiated the link share"),
        "app" to ("Media Telemetry" to "Application client variant identifier"),
        "embeds_referring_euri" to ("Media Telemetry" to "External embedding referrer domain"),
        "embeds_referring_origin" to ("Media Telemetry" to "External embedding origin domain"),
        "pp" to ("Media Telemetry" to "Playlist / preview telemetry"),
        "start_radio" to ("YouTube Telemetry" to "Auto-generated YouTube radio mix session flag"),
        "rv" to ("YouTube Telemetry" to "Related video recommendation tracking token"),
        "nd" to ("Spotify" to "New device redirect token"),
        "context" to ("Spotify" to "Playback context identifier"),

        // Amazon & E-Commerce
        "ref_" to ("Amazon" to "Amazon referral tracking code"),
        "ref" to ("Amazon" to "Amazon product browse referral source"),
        "tag" to ("Amazon" to "Amazon Associates affiliate tracking ID"),
        "linkCode" to ("Amazon" to "Amazon affiliate link generation code"),
        "pf_rd_r" to ("Amazon" to "Page framework request tracking"),
        "pf_rd_m" to ("Amazon" to "Page framework marketplace tag"),
        "pf_rd_t" to ("Amazon" to "Page framework template tracker"),
        "pf_rd_i" to ("Amazon" to "Page framework impression ID"),
        "pf_rd_p" to ("Amazon" to "Page framework placement ID"),
        "pf_rd_s" to ("Amazon" to "Page framework slot ID"),
        "pd_rd_r" to ("Amazon" to "Product detail recommendation request ID"),
        "pd_rd_w" to ("Amazon" to "Product detail recommendation widget"),
        "pd_rd_wg" to ("Amazon" to "Product detail recommendation widget group"),
        "crid" to ("Amazon" to "Customer request telemetry ID"),
        "sprefix" to ("Amazon" to "Search auto-complete prefix tracker"),
        "keywords" to ("Amazon" to "Search query tag on product pages"),
        "qid" to ("Amazon" to "Search result timestamp ID"),
        "sr" to ("Amazon" to "Search ranking position telemetry"),
        "dchild" to ("Amazon" to "Device child tracking token"),
        "ascsubtag" to ("Commerce Tracking" to "Sub-publisher affiliate tracking token"),
        "mkevt" to ("eBay" to "eBay marketing event identifier"),
        "mkcid" to ("eBay" to "eBay marketing channel identifier"),
        "mkrid" to ("eBay" to "eBay marketing campaign identifier"),
        "campid" to ("eBay" to "eBay partner network campaign ID"),
        "toolid" to ("eBay" to "eBay partner network tool identifier"),
        "customid" to ("eBay" to "eBay partner network custom tracking token"),
        "spm" to ("AliExpress" to "Site position model telemetry tracker"),
        "scm" to ("AliExpress" to "Smart content matching tracking token"),
        "pdp_npi" to ("AliExpress" to "Product detail page new pricing impression tracking"),

        // Microsoft / Bing & Yandex
        "msclkid" to ("Microsoft Advertising" to "Bing / Microsoft Click Identifier"),
        "yclid" to ("Yandex" to "Yandex Direct Click Identifier"),
        "ymclid" to ("Yandex" to "Yandex Market Click Identifier"),
        "s_kwcid" to ("Adobe Analytics" to "Adobe search keyword click identifier"),
        "ef_id" to ("Adobe Advertising" to "Adobe Advertising Cloud click identifier"),
        "irclickid" to ("Impact Radius" to "Impact affiliate conversion click identifier"),
        "irgwc" to ("Impact Radius" to "Impact gateway click tracking parameter"),
        "sscid" to ("ShareASale" to "ShareASale affiliate tracking identifier"),
        "zanpid" to ("Awin / Zanox" to "Awin affiliate network tracking ID"),

        // Email & Marketing automation
        "mc_cid" to ("Mailchimp" to "Campaign identifier for subscriber tracking"),
        "mc_eid" to ("Mailchimp" to "Unique recipient email tracking ID"),
        "mkt_tok" to ("Marketo" to "Marketo recipient tracking token"),
        "vero_id" to ("Vero" to "Vero email subscriber identity token"),
        "vero_conv" to ("Vero" to "Vero conversion action tracking"),
        "wickedid" to ("Wicked Reports" to "Multi-touch marketing tracking ID"),
        "hsCtaTracking" to ("HubSpot" to "HubSpot call-to-action click tracker"),
        "_hsenc" to ("HubSpot" to "HubSpot encrypted recipient token"),
        "_hsmi" to ("HubSpot" to "HubSpot email message identifier"),
        "ml_subscriber" to ("MailerLite" to "Subscriber tracking ID"),
        "ml_subscriber_hash" to ("MailerLite" to "Hashed subscriber identifier"),
        "spJobID" to ("Acoustic" to "Mailing job tracking identifier"),
        "spMailingID" to ("Acoustic" to "Mailing campaign ID"),
        "spReportId" to ("Acoustic" to "Telemetry reporting identifier"),

        // Analytics & OpenStat
        "pk_campaign" to ("Matomo / Piwik" to "Matomo campaign tracker"),
        "pk_kwd" to ("Matomo / Piwik" to "Matomo keyword tracker"),
        "piwik_campaign" to ("Matomo" to "Piwik campaign identifier"),
        "piwik_kwd" to ("Matomo" to "Piwik keyword identifier"),
        "matomo_campaign" to ("Matomo" to "Matomo campaign parameter"),
        "_openstat" to ("OpenStat" to "Russian marketing tracking token"),

        // Reddit & LinkedIn & Pinterest
        "share_id" to ("Reddit" to "Reddit share link tracking token"),
        "shrid" to ("Reddit" to "Reddit user share identity tracking token"),
        "rdt" to ("Reddit" to "Reddit tracking token"),
        "trackingId" to ("LinkedIn" to "LinkedIn internal event tracking ID"),
        "refId" to ("LinkedIn" to "LinkedIn referral identifier"),
        "midToken" to ("LinkedIn" to "LinkedIn messaging tracking token"),
        "trk" to ("LinkedIn" to "LinkedIn navigation tracker"),
        "trkInfo" to ("LinkedIn" to "LinkedIn telemetry data token"),
        "li_fat_id" to ("LinkedIn Ads" to "LinkedIn First-Party Ad Tracking Identifier"),
        "lipi" to ("LinkedIn" to "LinkedIn page instance telemetry token"),
        "licu" to ("LinkedIn" to "LinkedIn click URL identifier"),
        "mx" to ("Twitter / X" to "Twitter / X marketing telemetry token"),
        "invite_code" to ("Pinterest" to "User invitation tracking token"),

        // Canonical / AMP parameters
        "amp" to ("AMP Canonicalizer" to "Accelerated Mobile Pages wrapper parameter"),
        "outputtype" to ("AMP Canonicalizer" to "AMP output format override parameter")
    )

    fun containsLink(text: String): Boolean {
        return URL_REGEX.containsMatchIn(text)
    }

    fun extractLinks(text: String): List<String> {
        val bounded = text.take(UrlSecurityValidator.MAX_INPUT_LENGTH)
        return URL_REGEX.findAll(bounded).map { it.value.trim() }.toList()
    }

    /**
     * Asynchronously cleans text and expands TikTok, Facebook, and short redirect links.
     */
    suspend fun cleanTextAsync(
        inputText: String,
        context: Context? = null,
        enableFacebookWebView: Boolean = true,
        keepAmazonAffiliate: Boolean = false,
        strictMode: Boolean = false,
        offlineOnlyMode: Boolean = false
    ): CleanResult {
        val trimmed = inputText.trim().take(UrlSecurityValidator.MAX_INPUT_LENGTH)
        val urls = extractLinks(trimmed)

        if (urls.isEmpty()) {
            return CleanResult(
                originalText = trimmed,
                cleanedText = trimmed,
                originalUrl = trimmed,
                cleanedUrl = trimmed,
                domain = "",
                strippedParams = emptyList(),
                bytesSaved = 0,
                percentReduction = 0,
                isEntireTextReplaced = true,
                expandedFromUrl = null,
                warningType = ExpansionWarningType.NONE
            )
        }

        if (offlineOnlyMode) {
            return cleanText(
                inputText = trimmed,
                keepAmazonAffiliate = keepAmazonAffiliate,
                strictMode = strictMode,
                offlineOnlyMode = true
            )
        }

        val primaryRawUrl = urls.first()
        val isFbShare = FacebookWebViewExpander.isFacebookShareLink(primaryRawUrl)

        var expandedPrimary = primaryRawUrl
        var wasExpanded = false
        var warningType = ExpansionWarningType.NONE
        var warningMessage: String? = null
        var blockedRedirectReason: String? = null
        val redirectHops = mutableListOf<RedirectHop>()

        if (isFbShare) {
            if (enableFacebookWebView && context != null) {
                val fbResult = FacebookWebViewExpander.expandFacebookShareLink(context, primaryRawUrl)
                if (fbResult.isSuccess) {
                    val resolved = fbResult.getOrThrow()
                    expandedPrimary = resolved
                    wasExpanded = true
                    redirectHops.add(
                        RedirectHop(
                            step = 1,
                            url = primaryRawUrl,
                            host = UrlSecurityValidator.extractHost(primaryRawUrl),
                            mechanism = "Original"
                        )
                    )
                    redirectHops.add(
                        RedirectHop(
                            step = 2,
                            url = resolved,
                            host = UrlSecurityValidator.extractHost(resolved),
                            mechanism = "Meta WebView Permalink"
                        )
                    )
                } else {
                    val err = fbResult.exceptionOrNull()
                    if (err is SecurityException) {
                        blockedRedirectReason = err.message
                        warningType = ExpansionWarningType.RED_SECURITY_BLOCKED
                        warningMessage = err.message
                    } else {
                        warningType = ExpansionWarningType.RED_FACEBOOK_FAILED
                        warningMessage = "Facebook share link could not be expanded to a permalink (requires login session or post is restricted). Tracking parameters were still purged."
                    }
                }
            } else {
                warningType = ExpansionWarningType.RED_FACEBOOK_FAILED
                warningMessage = "Facebook share link could not be expanded (Deep WebView Expansion is disabled in settings). Tracking parameters were still purged."
            }
        } else if (UrlExpander.shouldExpand(primaryRawUrl)) {
            val detailed = UrlExpander.expandUrlDetailed(primaryRawUrl)
            expandedPrimary = detailed.finalUrl
            wasExpanded = detailed.wasRedirected
            blockedRedirectReason = detailed.blockedReason
            redirectHops.addAll(detailed.hops)

            if (detailed.blockedReason != null) {
                warningType = ExpansionWarningType.RED_SECURITY_BLOCKED
                warningMessage = detailed.blockedReason
            } else if (detailed.failedToRedirect && detailed.isOpaqueShortener) {
                warningType = ExpansionWarningType.ORANGE_UNEXPANDED_SHORTLINK
                warningMessage = "Shortlink could not be expanded via network. Opaque tracking IDs may remain inside the shortener slug. Proceed with caution."
            }
        }

        val cleanedPrimary = cleanSingleUrl(
            rawUrl = expandedPrimary,
            keepAmazonAffiliate = keepAmazonAffiliate,
            strictMode = strictMode,
            externalBlockedReason = blockedRedirectReason
        )
        if (cleanedPrimary.expandedFromUrl != null) {
            wasExpanded = true
            if (redirectHops.isEmpty()) {
                redirectHops.addAll(cleanedPrimary.redirectHops)
            }
        }

        var updatedText = trimmed
        val totalStripped = mutableListOf<StrippedParam>()
        totalStripped.addAll(cleanedPrimary.strippedParams)

        var networkExpansionsUsed = if (isFbShare || UrlExpander.shouldExpand(primaryRawUrl)) 1 else 0

        for (url in urls) {
            val expanded = if (url == primaryRawUrl) {
                expandedPrimary
            } else if (networkExpansionsUsed < UrlSecurityValidator.MAX_NETWORK_EXPANSIONS_PER_INPUT && UrlExpander.shouldExpand(url)) {
                networkExpansionsUsed++
                UrlExpander.expandUrl(url)
            } else {
                UrlExpander.unwrapOfflineRedirects(url)
            }
            val cleaned = if (url == primaryRawUrl) cleanedPrimary else cleanSingleUrl(expanded, keepAmazonAffiliate, strictMode)
            if (url != primaryRawUrl) {
                totalStripped.addAll(cleaned.strippedParams)
            }
            updatedText = updatedText.replace(url, cleaned.cleanedUrl)
        }

        val isSingleUrl = trimmed == primaryRawUrl
        val bytesSaved = (trimmed.length - updatedText.length).coerceAtLeast(0)
        val percent = if (trimmed.isNotEmpty()) {
            ((bytesSaved.toDouble() / trimmed.length.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        val finalWarningType = if (warningType != ExpansionWarningType.NONE) warningType else cleanedPrimary.warningType
        val finalWarningMessage = warningMessage ?: cleanedPrimary.warningMessage

        return CleanResult(
            originalText = trimmed,
            cleanedText = updatedText,
            originalUrl = primaryRawUrl,
            cleanedUrl = cleanedPrimary.cleanedUrl,
            domain = cleanedPrimary.domain,
            strippedParams = totalStripped.distinctBy { it.key },
            bytesSaved = bytesSaved,
            percentReduction = percent,
            isEntireTextReplaced = isSingleUrl,
            expandedFromUrl = if (wasExpanded) primaryRawUrl else null,
            warningType = finalWarningType,
            warningMessage = finalWarningMessage,
            isFacebookShareLink = isFbShare,
            securityAdvisories = cleanedPrimary.securityAdvisories,
            isOfflineDeferredShortlink = false,
            redirectHops = redirectHops
        )
    }

    /**
     * Synchronous cleaning (or Offline-Only Mode cleaning).
     */
    fun cleanText(
        inputText: String,
        keepAmazonAffiliate: Boolean = false,
        strictMode: Boolean = false,
        offlineOnlyMode: Boolean = false
    ): CleanResult {
        val trimmed = inputText.trim().take(UrlSecurityValidator.MAX_INPUT_LENGTH)
        val urls = extractLinks(trimmed)

        if (urls.isEmpty()) {
            return CleanResult(
                originalText = trimmed,
                cleanedText = trimmed,
                originalUrl = trimmed,
                cleanedUrl = trimmed,
                domain = "",
                strippedParams = emptyList(),
                bytesSaved = 0,
                percentReduction = 0,
                isEntireTextReplaced = true,
                expandedFromUrl = null
            )
        }

        val primaryRawUrl = urls.first()
        val offlineDetail = UrlExpander.unwrapOfflineRedirectsDetailed(primaryRawUrl)
        val offlineUnwrapped = offlineDetail.url
        val wasExpanded = offlineUnwrapped != primaryRawUrl
        val cleanedPrimary = cleanSingleUrl(
            rawUrl = offlineUnwrapped,
            keepAmazonAffiliate = keepAmazonAffiliate,
            strictMode = strictMode,
            externalBlockedReason = offlineDetail.blockedReason
        )

        val hops = mutableListOf<RedirectHop>()
        if (wasExpanded) {
            hops.add(
                RedirectHop(
                    step = 1,
                    url = primaryRawUrl,
                    host = UrlSecurityValidator.extractHost(primaryRawUrl),
                    mechanism = "Original"
                )
            )
            hops.add(
                RedirectHop(
                    step = 2,
                    url = offlineUnwrapped,
                    host = UrlSecurityValidator.extractHost(offlineUnwrapped),
                    mechanism = offlineDetail.mechanism ?: "Offline Shim"
                )
            )
        }

        var updatedText = trimmed
        val totalStripped = mutableListOf<StrippedParam>()
        totalStripped.addAll(cleanedPrimary.strippedParams)

        for (url in urls) {
            val unwrapped = if (url == primaryRawUrl) offlineUnwrapped else UrlExpander.unwrapOfflineRedirects(url)
            val cleaned = if (url == primaryRawUrl) cleanedPrimary else cleanSingleUrl(unwrapped, keepAmazonAffiliate, strictMode)
            if (url != primaryRawUrl) {
                totalStripped.addAll(cleaned.strippedParams)
            }
            updatedText = updatedText.replace(url, cleaned.cleanedUrl)
        }

        val isSingleUrl = trimmed == primaryRawUrl
        val bytesSaved = (trimmed.length - updatedText.length).coerceAtLeast(0)
        val percent = if (trimmed.isNotEmpty()) {
            ((bytesSaved.toDouble() / trimmed.length.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        val isFbShare = FacebookWebViewExpander.isFacebookShareLink(primaryRawUrl)
        val needsNetworkExpansion = isFbShare || UrlExpander.shouldExpand(offlineUnwrapped)
        val isDeferred = offlineOnlyMode && needsNetworkExpansion

        val warningType = when {
            cleanedPrimary.warningType != ExpansionWarningType.NONE -> cleanedPrimary.warningType
            isDeferred -> ExpansionWarningType.ORANGE_UNEXPANDED_SHORTLINK
            else -> ExpansionWarningType.NONE
        }
        val warningMessage = when {
            cleanedPrimary.warningMessage != null -> cleanedPrimary.warningMessage
            isDeferred -> "Offline-Only Mode is active: network redirect expansion was skipped to prevent outbound network connections. Tap 'Expand Shortlink Now' below if you trust this link."
            else -> null
        }

        return CleanResult(
            originalText = trimmed,
            cleanedText = updatedText,
            originalUrl = primaryRawUrl,
            cleanedUrl = cleanedPrimary.cleanedUrl,
            domain = cleanedPrimary.domain,
            strippedParams = totalStripped.distinctBy { it.key },
            bytesSaved = bytesSaved,
            percentReduction = percent,
            isEntireTextReplaced = isSingleUrl,
            expandedFromUrl = if (wasExpanded) primaryRawUrl else null,
            warningType = warningType,
            warningMessage = warningMessage,
            isFacebookShareLink = isFbShare,
            securityAdvisories = cleanedPrimary.securityAdvisories,
            isOfflineDeferredShortlink = isDeferred,
            redirectHops = hops
        )
    }

    fun cleanSingleUrl(
        rawUrl: String,
        keepAmazonAffiliate: Boolean = false,
        strictMode: Boolean = false,
        externalBlockedReason: String? = null
    ): CleanResult {
        val trimmedRaw = rawUrl.trim()
        val offlineDetail = UrlExpander.unwrapOfflineRedirectsDetailed(trimmedRaw)
        val unwrappedUrl = offlineDetail.url
        val wasOfflineUnwrapped = unwrappedUrl != trimmedRaw
        val blockedReason = externalBlockedReason ?: offlineDetail.blockedReason

        val (sanitizedUrl, strippedUserInfo) = UrlSecurityValidator.stripUserInfoIfPresent(unwrappedUrl)

        val uri = try {
            Uri.parse(sanitizedUrl)
        } catch (_: Exception) {
            return CleanResult(
                originalText = rawUrl,
                cleanedText = rawUrl,
                originalUrl = rawUrl,
                cleanedUrl = rawUrl,
                domain = "",
                strippedParams = emptyList(),
                bytesSaved = 0,
                percentReduction = 0,
                isEntireTextReplaced = true,
                expandedFromUrl = null
            )
        }

        val host = UrlSecurityValidator.extractHost(sanitizedUrl).ifEmpty { uri.host?.lowercase().orEmpty() }
        val strippedParams = mutableListOf<StrippedParam>()

        if (wasOfflineUnwrapped && offlineDetail.mechanism != null) {
            strippedParams.add(
                StrippedParam(
                    key = "redirect_wrapper",
                    value = UrlSecurityValidator.extractHost(trimmedRaw).ifEmpty { "wrapper" },
                    category = offlineDetail.mechanism,
                    description = "Unwrapped canonical destination URL from redirect/AMP wrapper"
                )
            )
        }

        if (!strippedUserInfo.isNullOrBlank()) {
            strippedParams.add(
                StrippedParam(
                    key = "userInfo@",
                    value = strippedUserInfo,
                    category = "Security / Anti-Phishing",
                    description = "Stripped embedded URL authority credentials (user@host phishing vector)"
                )
            )
        }

        val newUriBuilder = uri.buildUpon().clearQuery()
        val queryParamNames = try {
            uri.queryParameterNames
        } catch (_: Exception) {
            emptySet()
        }

        for (param in queryParamNames) {
            val lowerParam = param.lowercase()
            val values = uri.getQueryParameters(param)
            val firstValue = values.firstOrNull().orEmpty()

            val shouldStrip = isTrackingParam(
                paramName = lowerParam,
                paramValue = firstValue,
                host = host,
                keepAmazonAffiliate = keepAmazonAffiliate,
                strictMode = strictMode
            )

            if (shouldStrip) {
                val info = KNOWN_TRACKING_PARAMS[lowerParam]
                    ?: guessParamCategory(lowerParam, host)
                strippedParams.add(
                    StrippedParam(
                        key = param,
                        value = firstValue,
                        category = info.first,
                        description = info.second
                    )
                )
            } else {
                for (v in values) {
                    newUriBuilder.appendQueryParameter(param, v)
                }
            }
        }

        var cleanedUri = newUriBuilder.build()

        // Strip trailing /amp or /amp/ path segments on news/article URLs
        cleanedUri = cleanAmpPathSuffix(cleanedUri, strippedParams)

        // Condense Amazon paths (/Product-Title/dp/ASIN/ref=... -> /dp/ASIN)
        if (isAmazonHost(host)) {
            cleanedUri = cleanAmazonPath(cleanedUri, strippedParams)
        }

        var cleanedUrlString = cleanedUri.toString()

        if (cleanedUrlString.endsWith("?")) {
            cleanedUrlString = cleanedUrlString.dropLast(1)
        }

        val advisories = UrlSecurityValidator.analyzeSecurityRisks(
            url = cleanedUrlString,
            strippedUserInfo = strippedUserInfo,
            blockedRedirectReason = blockedReason
        )

        val hops = if (wasOfflineUnwrapped) {
            listOf(
                RedirectHop(
                    step = 1,
                    url = trimmedRaw,
                    host = UrlSecurityValidator.extractHost(trimmedRaw),
                    mechanism = "Original"
                ),
                RedirectHop(
                    step = 2,
                    url = unwrappedUrl,
                    host = host,
                    mechanism = offlineDetail.mechanism ?: "Offline Shim"
                )
            )
        } else {
            emptyList()
        }

        val bytesSaved = (rawUrl.length - cleanedUrlString.length).coerceAtLeast(0)
        val percent = if (rawUrl.isNotEmpty()) {
            ((bytesSaved.toDouble() / rawUrl.length.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

        return CleanResult(
            originalText = rawUrl,
            cleanedText = cleanedUrlString,
            originalUrl = rawUrl,
            cleanedUrl = cleanedUrlString,
            domain = host,
            strippedParams = strippedParams,
            bytesSaved = bytesSaved,
            percentReduction = percent,
            isEntireTextReplaced = true,
            expandedFromUrl = if (wasOfflineUnwrapped) trimmedRaw else null,
            warningType = if (blockedReason != null) ExpansionWarningType.RED_SECURITY_BLOCKED else ExpansionWarningType.NONE,
            warningMessage = blockedReason,
            securityAdvisories = advisories,
            redirectHops = hops
        )
    }

    private fun isTrackingParam(
        paramName: String,
        paramValue: String,
        host: String,
        keepAmazonAffiliate: Boolean,
        strictMode: Boolean
    ): Boolean {
        if (keepAmazonAffiliate && (paramName == "tag" || paramName == "linkcode")) {
            return false
        }

        // Only strip outputType when it's an AMP override (outputType=amp)
        if (paramName == "outputtype") {
            return paramValue.equals("amp", ignoreCase = true)
        }

        if (KNOWN_TRACKING_PARAMS.containsKey(paramName)) {
            return true
        }

        // Twitter / X share ID parameter 's', 't', or 'mx'
        if ((host.contains("twitter.com") || host.contains("x.com")) && (paramName == "s" || paramName == "t" || paramName == "mx")) {
            return true
        }

        // Facebook tracking parameters
        if (host.contains("facebook.com") || host.contains("fb.com")) {
            if (paramName == "mibextid" || paramName == "rdid" || paramName == "rc" ||
                paramName == "share_url" || paramName.startsWith("__tn") || paramName.startsWith("__cft")
            ) {
                return true
            }
        }

        // Instagram tracking parameters
        if (host.contains("instagram.com")) {
            if (paramName == "exln" || paramName == "obrf" || paramName == "vrfl" ||
                paramName == "srtk" || paramName == "cplk" || paramName == "dlrf" ||
                paramName == "mdxt" || paramName == "e" || paramName == "__coig_restricted" ||
                paramName == "btag" || paramName == "igsh" || paramName == "igshid"
            ) {
                return true
            }
        }

        // Threads tracking parameters
        if (host.contains("threads.net")) {
            if (paramName == "xmt" || paramName == "slof") {
                return true
            }
        }

        // Reddit tracking parameters
        if (host.contains("reddit.com")) {
            if (paramName == "shrid" || paramName == "share_id" || paramName == "rdt") {
                return true
            }
        }

        // Substack referral tracking
        if (host.contains("substack.com") && paramName == "r") {
            return true
        }

        // TikTok tracking parameters
        if (host.contains("tiktok.com") || host.contains("douyin.com")) {
            if (paramName == "_r" || paramName == "_t" || paramName.startsWith("tt_") ||
                paramName == "is_from_webapp" || paramName == "sender_device" ||
                paramName == "share_app_id" || paramName == "share_item_id" ||
                paramName == "share_link_id" || paramName == "checksum" ||
                paramName == "sec_uid" || paramName.startsWith("ug_") ||
                paramName == "u_code" || paramName == "preview_pb" || paramName == "ttclid"
            ) {
                return true
            }
        }

        // LinkedIn tracking parameters
        if (host.contains("linkedin.com")) {
            if (paramName == "trackingid" || paramName == "refid" || paramName == "midtoken" ||
                paramName == "trk" || paramName == "trkinfo" || paramName == "li_fat_id" ||
                paramName == "lipi" || paramName == "licu"
            ) {
                return true
            }
        }

        // Wildcard prefixes
        if (paramName.startsWith("utm_") ||
            paramName.startsWith("fb_") ||
            paramName.startsWith("pf_rd_") ||
            paramName.startsWith("pd_rd_") ||
            paramName.startsWith("ga_") ||
            paramName.startsWith("_ga") ||
            paramName.startsWith("pk_") ||
            paramName.startsWith("piwik_") ||
            paramName.startsWith("matomo_") ||
            paramName.startsWith("vero_") ||
            paramName.startsWith("ml_") ||
            paramName.startsWith("sp_")
        ) {
            return true
        }

        if (strictMode) {
            if (paramName.length >= 20 || paramName.contains("token") || paramName.contains("track")) {
                return true
            }
        }

        return false
    }

    private fun guessParamCategory(paramName: String, host: String): Pair<String, String> {
        return when {
            paramName.startsWith("utm_") -> "Marketing / UTM" to "Marketing campaign telemetry"
            paramName.startsWith("fb_") || host.contains("facebook.com") -> "Meta / Facebook" to "Facebook tracking token"
            host.contains("instagram.com") -> "Instagram / Meta" to "Instagram share and user tracking token"
            host.contains("threads.net") -> "Meta / Threads" to "Threads share and attribution token"
            paramName.startsWith("pf_rd_") || paramName.startsWith("pd_rd_") -> "Amazon Telemetry" to "Amazon recommendation pipeline tracker"
            paramName.startsWith("ga_") || paramName.startsWith("_ga") -> "Google Analytics" to "Google Analytics client tracker"
            host.contains("tiktok.com") -> "TikTok Telemetry" to "TikTok user share tracker"
            host.contains("reddit.com") -> "Reddit Telemetry" to "Reddit share tracking token"
            host.contains("linkedin.com") -> "LinkedIn Telemetry" to "LinkedIn attribution tracking token"
            host.contains("ebay.") -> "eBay Marketing" to "eBay partner and campaign identifier"
            host.contains("aliexpress.") -> "AliExpress Telemetry" to "AliExpress site position tracker"
            host.contains("amazon.") -> "Amazon Telemetry" to "Amazon product tracking parameter"
            host.contains("youtube.") || host.contains("youtu.be") -> "YouTube Telemetry" to "YouTube playback tracker"
            else -> "Tracking Parameter" to "Third-party telemetry identifier"
        }
    }

    private fun isAmazonHost(host: String): Boolean {
        return host == "amazon.com" ||
            host.endsWith(".amazon.com") ||
            host == "amazon.co.uk" ||
            host.endsWith(".amazon.co.uk") ||
            host == "amazon.de" ||
            host.endsWith(".amazon.de") ||
            host == "amazon.fr" ||
            host == "amazon.ca" ||
            host == "amazon.es" ||
            host == "amazon.it" ||
            host == "amazon.in" ||
            host == "amazon.co.jp" ||
            host == "a.co"
    }

    private fun cleanAmpPathSuffix(uri: Uri, strippedParams: MutableList<StrippedParam>): Uri {
        val path = uri.path ?: return uri
        if (path.length > 5 && (path.endsWith("/amp", ignoreCase = true) || path.endsWith("/amp/", ignoreCase = true))) {
            val trimmedPath = path.trimEnd('/').removeSuffix("/amp").removeSuffix("/AMP")
            if (trimmedPath.isNotEmpty()) {
                strippedParams.add(
                    StrippedParam(
                        key = "/amp",
                        value = path,
                        category = "AMP Canonicalizer",
                        description = "Removed trailing /amp path suffix to restore canonical article URL"
                    )
                )
                return uri.buildUpon().path(trimmedPath).build()
            }
        }
        return uri
    }

    private fun cleanAmazonPath(uri: Uri, strippedParams: MutableList<StrippedParam>): Uri {
        val path = uri.path ?: return uri

        // Condense /Product-Title-Slug/dp/<ASIN>/ref=... or /gp/product/<ASIN>/... to /dp/<ASIN>
        val asinMatch = AMAZON_ASIN_REGEX.find(path)
        if (asinMatch != null) {
            val asin = asinMatch.groupValues[1].uppercase()
            val canonicalPath = "/dp/$asin"
            if (path != canonicalPath && path != "$canonicalPath/") {
                strippedParams.add(
                    StrippedParam(
                        key = "amazon_path_slug",
                        value = path,
                        category = "Amazon Canonicalizer",
                        description = "Condensed verbose product path & ref= slug to canonical /dp/$asin"
                    )
                )
                return uri.buildUpon().path(canonicalPath).build()
            }
        }

        val refIndex = path.indexOf("/ref=")
        if (refIndex > 0) {
            val cleanPath = path.substring(0, refIndex)
            strippedParams.add(
                StrippedParam(
                    key = "ref=",
                    value = path.substring(refIndex),
                    category = "Amazon Telemetry",
                    description = "Stripped embedded /ref= path referral tracker"
                )
            )
            return uri.buildUpon().path(cleanPath).build()
        }
        return uri
    }
}
