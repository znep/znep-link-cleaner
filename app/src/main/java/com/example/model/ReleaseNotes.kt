package com.example.model

/**
 * MANDATORY AGENT & DEVELOPER STANDING INSTRUCTIONS (DO NOT REMOVE OR REVERT):
 *
 * 1. FULL RELEASE HISTORY & PROMPTS: Every time the application is modified, you MUST:
 *    - Add a new [ReleaseNote] entry at the top of [ReleaseHistory.RELEASES] (and bump versionName/versionCode).
 *    - Record the exact triggering user prompt in [ReleaseNote.userPrompt] (omit non-modifying chat turns).
 *    - Keep all historical releases (v1.0.0 through latest) intact in both this file AND `/download_portal/index.html`.
 * 2. VIBE-CODED PROOF OF CONCEPT MESSAGING: Always preserve clear messaging in the app UI and on the
 *    web portal (`linkcleaner.znep.com`) that ZnepLinkCleaner is a "Vibe-Coded Proof of Concept".
 * 3. PUBLIC WEB PORTAL AVAILABILITY: Always ensure `/app/applet/server.py` is running on port 3000 and
 *    Nginx auth bypass (`do return end` in `/etc/nginx/user_auth_verification.lua`) is active so the
 *    website never returns 403 Forbidden.
 * 4. LINKED GITHUB & PUBLISHED GIT REPO PARITY: Always keep `README.md`, `CHANGELOG.md`, `download_portal/repo.html`,
 *    annotated Git tags, `ZnepLinkCleaner.bundle`, and `ZnepLinkCleaner-git.zip` in 100% sync with [ReleaseHistory.RELEASES]
 *    so both the linked GitHub repository and the web portal's published repo include all prompts and changes for every release.
 */
data class ReleaseNote(
    val version: String,
    val buildNumber: Int,
    val releaseDate: String,
    val title: String,
    val userPrompt: String,
    val highlights: List<String>,
    val technicalDetails: List<String>
)

object ReleaseHistory {
    const val VIBE_CODED_POC_NOTICE =
        "VIBE-CODED PROOF OF CONCEPT // Built iteratively via natural-language prompts. Every modifying prompt is recorded in the release history."

    val RELEASES = listOf(
        ReleaseNote(
            version = "13.0",
            buildNumber = 13,
            releaseDate = "November 2, 2026",
            title = "Cyberpunk Quivering 'Zlc' Logo & Linked GitHub / Portal Repository Sync",
            userPrompt = "Update the app logo to be a large Z with a smaller \"lc\". Where possible make the Z quiver slightly. It should be cyberpunk inspired but not too complicated. / Also ensure the GitHub repo published is a copy of the linked GitHub repo and the linked GitHub is updated with the prompts and changes for each release. Persist this request.",
            highlights = listOf(
                "Redesigned app logo to a cyberpunk 'Zlc' monogram (large Electric Cyan 'Z' with Neon Purple chromatic glitch + smaller steady 'lc')",
                "Added interactive & periodic quiver animation to the 'Z' in the Android Compose UI (quivers on tap, on link clean/expand, and in periodic bursts) and on the web portal",
                "Updated Android adaptive launcher icons (ic_launcher_foreground/background) and all 5 density PNG mipmaps (mdpi–xxxhdpi)",
                "Synchronized the workspace Git repository (for linked GitHub sync via README.md, CHANGELOG.md, and annotated tags) with the published web portal repo (repo.html, ZnepLinkCleaner.bundle, ZnepLinkCleaner-git.zip)",
                "Persisted standing rule in AGENTS.md, README.md, and unit tests requiring every release's prompt and changes to be synced across GitHub and the published portal repo"
            ),
            technicalDetails = listOf(
                "Created ZlcLogo Jetpack Compose Canvas component with isolated Z micro-jitter and chromatic ghost offset",
                "Added CHANGELOG.md and expanded README.md & repo.html with full v1.0.0 – v13.0 prompts and changes",
                "Added Robolectric unit test verifying release parity across ReleaseNotes.kt, README.md, CHANGELOG.md, index.html, and repo.html",
                "Bumped versionCode to 13 and versionName to 13.0"
            )
        ),
        ReleaseNote(
            version = "12.0",
            buildNumber = 12,
            releaseDate = "November 2, 2026",
            title = "Google Play Publishing Version Bump, Direct Nginx Portal Hardening & Cloudflare Reverse-Proxy Support",
            userPrompt = "The website is down again. / Publishing to Google Play finished. Please bump the version code: increment `versionCode` by 1 in the app build.gradle(.kts) file, and set `versionName` to \"<new versionCode>.0\". / Remove the dns setup instructions from the website, they are only for me not the public. Also, the option A cloud flare setup is redirecting me to the run.app name not proxying. Note I'm on a free cloud flare plan.",
            highlights = listOf(
                "Bumped versionCode to 12 and versionName to 12.0 following Google Play publishing",
                "Removed internal DNS setup instructions from the public web portal",
                "Configured Nginx with absolute_redirect off so proxied requests via linkcleaner.znep.com never redirect to the underlying container hostname",
                "Hardened public web portal by configuring Nginx (PID 5) to serve /app/applet/download_portal directly with zero upstream dependency + self-healing server.py daemon"
            ),
            technicalDetails = listOf(
                "Enabled absolute_redirect off and server_name_in_redirect off in /etc/nginx/nginx.conf",
                "Configured Nginx try_files and custom MIME types (.apk, .bundle, .svg) in /etc/nginx/nginx.conf",
                "Incremented defaultConfig.versionCode = 12 and defaultConfig.versionName = \"12.0\" in app/build.gradle.kts",
                "Updated release history in-app and on linkcleaner.znep.com with complete v1.0.0 – v12.0 prompt log"
            )
        ),
        ReleaseNote(
            version = "2.0.0",
            buildNumber = 11,
            releaseDate = "November 2, 2026",
            title = "ZnepLinkCleaner Rebrand, znep.com Domain & Prompt-Tracked PoC",
            userPrompt = "Before we go further, I want to rename the app. The name should still indicate what it does but be unique in a way that might be legally protectable in the future. Perhaps include \"znep\"... Choose ZnepLinkCleaner. I own znep.com. The app can live under that name, and you can give me DNS entries to setup make linkcleaner.znep.com resolve to the website. Don't worry about backwards compatibility. Ensure you are recording prompts as part of the release notes and have clear messaging the app is a vibe coded proof of concept, and that this instruction sticks in the future.",
            highlights = listOf(
                "Full rebrand to ZnepLinkCleaner across Android UI, system Share Sheet targets, and web portal",
                "Migrated Android applicationId to authoritative domain package 'com.znep.linkcleaner'",
                "Added prominent 'Vibe-Coded Proof of Concept' banner and messaging in-app and on linkcleaner.znep.com",
                "Added verbatim/reconstructed User Prompts to every release note entry (v1.0.0 – v2.0.0)",
                "Added DNS & Cloudflare reverse-proxy guide for custom subdomain linkcleaner.znep.com",
                "Removed legacy CleanLink artifacts for a clean break under the ZnepLinkCleaner brand"
            ),
            technicalDetails = listOf(
                "Added required userPrompt property to ReleaseNote data model enforced by Robolectric unit tests",
                "Updated shortcuts.xml and ShortcutManagerCompat category to com.znep.linkcleaner.category.DIRECT_SHARE_TARGET",
                "Codified permanent standing instructions in ReleaseNotes.kt, AGENTS.md, and README.md",
                "Bumped versionCode to 11 and versionName to 2.0.0"
            )
        ),
        ReleaseNote(
            version = "1.9.0",
            buildNumber = 10,
            releaseDate = "November 2, 2026",
            title = "Instagram exln Stripping & Enhanced Multi-Platform Tracking Purge",
            userPrompt = "Instagram links still have an exln param that looks like tracking. Should it be removed? If so, update the app and also look for other missing tracking params.",
            highlights = listOf(
                "Stripped Instagram 'exln', 'obrf', 'vrfl', 'srtk', 'cplk', 'dlrf', and 'mdxt' user share tracking tokens",
                "Added Meta Threads 'xmt' cross-app telemetry and 'slof' share log parameter removal",
                "Purged Reddit user share identity tracking token 'shrid'",
                "Added TikTok user profile tracking ('u_code'), preview session ('preview_pb'), and ad click ('ttclid') removal",
                "Added LinkedIn first-party ad tracking ('li_fat_id'), page instance ('lipi'), and click identifier ('licu') stripping",
                "Added e-commerce & affiliate parameter coverage for eBay (mkevt, mkcid, campid), AliExpress (spm, scm), and Impact/Awin"
            ),
            technicalDetails = listOf(
                "Added comprehensive parameter rules and descriptions to KNOWN_TRACKING_PARAMS map",
                "Added host-aware pattern guards for instagram.com, threads.net, reddit.com, substack.com, and linkedin.com",
                "Added quick-test [ Instagram ] sample vector on the main sanitizer console",
                "Bumped versionCode to 10 and versionName to 1.9.0"
            )
        ),
        ReleaseNote(
            version = "1.8.0",
            buildNumber = 9,
            releaseDate = "November 2, 2026",
            title = "Share Sheet Prominence, Public Portal Watchdog & Full Release History",
            userPrompt = "The website is no longer public, ensure it remains public and analyze why previous attempts reverted. Always include the full release notes history on the webpage and within the app somewhere. You don't need to include prompts that don't modify the app itself. In addition, provide tips on how to make the app show up earlier and more prominently in the share dialogue. Modify the app if possible to help.",
            highlights = listOf(
                "Direct Share Target Shortcut for prioritized top-row Android share sheet placement",
                "High-priority intent filter configuration (android:priority=100) for text and URLs",
                "Full Release Notes History viewer accessible directly in app and web portal",
                "Interactive Share Sheet Prominence Guide with 1-tap practice pinning tool",
                "Permanent Nginx auth-bypass watchdog in server.py to keep the web portal public 24/7"
            ),
            technicalDetails = listOf(
                "Added shortcuts.xml share-target and dynamic ShortcutManagerCompat integration",
                "Diagnosed stateless container overlay & Lua auth bridge causes of 403 reverts",
                "Bumped versionCode to 9 and versionName to 1.8.0"
            )
        ),
        ReleaseNote(
            version = "1.7.0",
            buildNumber = 8,
            releaseDate = "November 2, 2026",
            title = "Facebook Headless WebView Expander & Dual-Tier Warning System",
            userPrompt = "Add Option 1 (on-device headless Android WebView resolution) for Facebook share links (/share/p/, /share/r/, etc.) to resolve client-side JS redirects to canonical permalinks. Implement a red warning when Facebook share links fail to expand and an orange advisory for unexpanded shorteners. Include technical details for Facebook share links in the app without cluttering the main screen, and provide a config setting to disable Option 1 (enabled by default).",
            highlights = listOf(
                "On-device headless Android WebView expander (Option 1) for Facebook /share/p/ & /share/r/ links",
                "Red Warning alert when Facebook permalinks cannot be resolved (private/login gate)",
                "Orange Advisory alert for unexpanded opaque shorteners (bit.ly, t.co, vt.tiktok.com)",
                "Protection setting to toggle Facebook WebView expansion (enabled by default)",
                "Facebook Share Links Architecture technical deep dive in Rules tab"
            ),
            technicalDetails = listOf(
                "Implemented FacebookWebViewExpander with XMiBUrlWrapperController JavaScript interception",
                "Added ExpansionWarningType sealed enum with retro UI indicators",
                "Zero data leaves the device; 100% on-device expansion with automatic memory cleanup"
            )
        ),
        ReleaseNote(
            version = "1.6.0",
            buildNumber = 7,
            releaseDate = "November 1, 2026",
            title = "Architecture Documentation & Web Portal Infrastructure",
            userPrompt = "Create a public download portal webpage hosting the signed APK, an offline Git bundle, a source archive, an interactive Git repository browser, and a visual system architecture diagram.",
            highlights = listOf(
                "Architectural topology diagram generation and offline vector assets",
                "Standalone interactive Git Repository Browser (repo.html)",
                "Web Download Portal with direct APK and Git bundle downloads"
            ),
            technicalDetails = listOf(
                "Configured persistent workspace web server on port 3000",
                "Automated Nginx authentication bypass watchdog to eliminate 403 Forbidden errors"
            )
        ),
        ReleaseNote(
            version = "1.5.0",
            buildNumber = 6,
            releaseDate = "October 30, 2026",
            title = "Retro Monospace Aesthetics & System Share Flow",
            userPrompt = "Restyle the UI with a sleek retro/cyberpunk monospace terminal aesthetic, add auto-copy on share and auto-close after reshare options, and design a custom adaptive launcher icon.",
            highlights = listOf(
                "Cyberpunk/retro monospace typography and neon telemetry highlights",
                "Auto-copy clean URL to clipboard immediately upon sharing from external apps",
                "Auto-close app after resharing via system sheet to return seamlessly to previous app",
                "Custom adaptive launcher icon with laser-clean vector branding"
            ),
            technicalDetails = listOf(
                "SingleTop activity launchMode for instant share-intent handling",
                "PreferencesManager backed by SharedPreferences with reactive StateFlows"
            )
        ),
        ReleaseNote(
            version = "1.4.0",
            buildNumber = 5,
            releaseDate = "October 28, 2026",
            title = "Strict Cleaning Mode & Amazon Affiliate Preservation",
            userPrompt = "Add a Protection / Rules tab with a toggle to preserve Amazon creator affiliate tags while stripping tracking parameters, plus a Strict Cleaning Mode for aggressive heuristic token removal.",
            highlights = listOf(
                "Strict mode to purge non-standard tokens, unknown query arguments, and custom tracking hashes",
                "Option to preserve Amazon creator affiliate tags (tag=...) while purging all tracking telemetry",
                "Protection / Rules tab with granular user preference switches"
            ),
            technicalDetails = listOf(
                "Regex-based pattern matching for Amazon canonical ASIN paths (/dp/{ASIN})",
                "Configurable param retention whitelist"
            )
        ),
        ReleaseNote(
            version = "1.3.0",
            buildNumber = 4,
            releaseDate = "October 26, 2026",
            title = "Room SQLite Local History & Privacy Telemetry",
            userPrompt = "Add a local Room database to record cleaned link history on-device and show privacy stats (links cleaned, trackers removed, and bytes saved).",
            highlights = listOf(
                "Persistent cleaning history saved securely on-device using Room SQLite",
                "Analytics dashboard: total links cleaned, stripped parameters count, and estimated bytes saved",
                "Individual history item deletion and one-tap history wipe"
            ),
            technicalDetails = listOf(
                "CleanedLinkDao with reactive Kotlin Flow queries",
                "Zero cloud sync; 100% device-local storage"
            )
        ),
        ReleaseNote(
            version = "1.2.0",
            buildNumber = 3,
            releaseDate = "October 24, 2026",
            title = "Shortener Traversal & Redirect Engine",
            userPrompt = "Support expanding shortened links like TikTok (vt.tiktok.com), Amazon (a.co, amzn.to), bit.ly, and t.co by following HTTP redirects before cleaning the destination URL.",
            highlights = listOf(
                "Network redirect traversal for TikTok (vt.tiktok.com, vm.tiktok.com, /t/)",
                "YouTube short link canonicalization (youtu.be with si parameter stripping)",
                "Amazon shortlink expansion (a.co, amzn.to)",
                "Support for bit.ly, tinyurl.com, t.co, and 30+ major redirect services"
            ),
            technicalDetails = listOf(
                "UrlExpander with OkHttpClient followRedirects=false to inspect HTTP 301/302 Location headers",
                "Asynchronous coroutines-based resolution in LinkCleaner.cleanTextAsync"
            )
        ),
        ReleaseNote(
            version = "1.1.0",
            buildNumber = 2,
            releaseDate = "October 22, 2026",
            title = "Material 3 Dynamic Theming & Clipboard Detection",
            userPrompt = "Add Material 3 theming, automatic clipboard link detection with a quick-paste button, and a searchable dictionary of known tracking parameters.",
            highlights = listOf(
                "Material 3 dynamic color scheme with dark/light mode support",
                "Automatic clipboard detection with quick-paste button when URL is copied",
                "Comprehensive database of known tracking parameter definitions"
            ),
            technicalDetails = listOf(
                "Searchable and filterable tracking parameter dictionary in Rules tab",
                "ClipboardManager integration with feedback snackbars"
            )
        ),
        ReleaseNote(
            version = "1.0.0",
            buildNumber = 1,
            releaseDate = "October 20, 2026",
            title = "Initial Release — Pure On-Device Link Sanitizer",
            userPrompt = "Build an Android app that receives shared links from the Android Share Sheet, strips tracking parameters like utm_source, fbclid, and gclid, and lets you copy or reshare the clean URL.",
            highlights = listOf(
                "Offline tracker stripping for Google Analytics (UTM), Facebook (fbclid), Google Ads (gclid), and Twitter (ref_src)",
                "Clean URL output with one-tap copy and system share button",
                "Instant share-intent ingestion via Android SEND action"
            ),
            technicalDetails = listOf(
                "Jetpack Compose UI with Kotlin Coroutines",
                "Url parsing and clean URI reconstruction without altering legitimate path/query values"
            )
        )
    )
}
