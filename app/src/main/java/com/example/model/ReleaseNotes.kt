package com.example.model

data class ReleaseNote(
    val version: String,
    val buildNumber: Int,
    val releaseDate: String,
    val title: String,
    val highlights: List<String>,
    val technicalDetails: List<String>
)

object ReleaseHistory {
    val RELEASES = listOf(
        ReleaseNote(
            version = "1.8.0",
            buildNumber = 9,
            releaseDate = "November 2, 2026",
            title = "Share Sheet Prominence & In-App Release History",
            highlights = listOf(
                "Direct Share Target Shortcut for prioritized top-row Android share sheet placement",
                "High-priority intent filter configuration (android:priority=100) for text and URLs",
                "Full Release Notes History viewer accessible directly in app and web portal",
                "Interactive Share Sheet Prominence Guide with 1-tap practice pinning tool"
            ),
            technicalDetails = listOf(
                "Added shortcuts.xml share-target and dynamic ShortcutManagerCompat integration",
                "Optimized MIME resolution for both text/plain and text/*",
                "Bumped versionCode to 9 and versionName to 1.8.0"
            )
        ),
        ReleaseNote(
            version = "1.7.0",
            buildNumber = 8,
            releaseDate = "November 2, 2026",
            title = "Facebook Headless WebView Expander & Dual-Tier Warning System",
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
            highlights = listOf(
                "Cyberpunk/retro monospace typography and neon telemetry highlights",
                "Auto-copy clean URL to clipboard immediately upon sharing from external apps",
                "Auto-close CleanLink after resharing via system sheet to return seamlessly to previous app",
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
            highlights = listOf(
                "Material 3 dynamic color scheme with dark/light mode support",
                "Automatic clipboard detection with quick-paste button when URL is copied",
                "Comprehensive database of 45+ known tracking parameter definitions"
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
