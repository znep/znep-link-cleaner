# Changelog — ZnepLinkCleaner (`com.znep.linkcleaner`)

> **VIBE-CODED PROOF OF CONCEPT**
> Every release of **ZnepLinkCleaner** ([https://linkcleaner.znep.com](https://linkcleaner.znep.com) | Public GitHub: **[https://github.com/znep/znep-link-cleaner](https://github.com/znep/znep-link-cleaner)**) is built iteratively via natural-language prompts. This changelog records every functional release (`v1.0.0` through `v20.0`), the exact user prompt that modified the application, and the resulting engineering changes.

---

## [v20.0] — Build 20 (November 3, 2026)
### Facebook Native Deep-Link (`fb://native_post`) Base64 Permalink Decoder, `wtsid` Tracker Purge & GitHub Pages/Releases Hosting
- **Triggering User Prompt**:
  > *"Does it make sense to setup the git repository so I can host the website on GitHub pages? Or something? Explain each option presented. / This happened trying to expand a Facebook link from a copy from a text field."*
- **Highlights**:
  - Decoded Facebook native Android app deep-link redirects (`fb://native_post/<base64>?wtsid=...`, `fb://facewebmodal/f?href=...`, `fb://story/...`, and `intent://...#Intent;scheme=fb;...`) directly into canonical `https://www.facebook.com/<actorId>/posts/<postId>` permalinks by unpacking Facebook's Base64 GraphQL `S:_I<actorId>:<postId>` token.
  - Eliminated false-positive `"Security Firewall // Unsafe Redirect Blocked"` errors when Facebook mobile web pages attempt to launch the native `fb://` app scheme during headless WebView expansion.
  - Added DOM meta-tag fallback extraction (`al:android:url`, `link[rel=canonical]`, `og:url`) in `FacebookWebViewExpander` and added `wtsid`, `_rdc`, and `_rdr` to `KNOWN_TRACKING_PARAMS`.
  - Deduplicated stacked security advisory banners and clarified the output URL box header when warnings are present in `CleanResultCard`.
  - Configured GitHub Pages static deployment (`gh-pages` + `CNAME` `linkcleaner.znep.com`) and automated GitHub Releases binary asset publishing via the GitHub REST API.

## [v19.0] — Build 19 (November 2, 2026)
### Google Play Publishing Version Bump (v19.0 / Build 19)
- **Triggering User Prompt**:
  > *"Publishing to Google Play finished. Please bump the version code: increment `versionCode` by 1 in the app build.gradle(.kts) file, and set `versionName` to \"<new versionCode>.0\"."*
- **Highlights**:
  - Incremented `versionCode` from `18` to `19` and updated `versionName` to `"19.0"` following Google Play publishing completion for `v18.0`.
  - Synchronized `v19.0` release notes and triggering prompt across the Android app (`ReleaseNotes.kt`), linked GitHub repository (`README.md` & `CHANGELOG.md`), and web portal (`index.html` & `repo.html`).
  - Committed `v19.0` as a separate prompt-annotated commit on `main`, tagged `v19.0`, and pushed to `https://github.com/znep/znep-link-cleaner`.

## [v18.0] — Build 18 (November 2, 2026)
### Unchanged Link Indicator, Randomized Cyberpunk 'Z' Glitch Engine, In-Place Text Replacement, Canonical AMP/Amazon Simplifier & Redirect Hop Trace
- **Triggering User Prompt**:
  > *"Give me some ideas for useful features to add that are technically solid. This could include ux changes, but the less ux bloat the better. / Additionally, indicate clearly if the link is unchanged instead of requiring the user to compare the two versions of it. / Also make the Z jiggle more or do other cool visual things randomly."*
- **Highlights**:
  - Added an unmistakable **`[ ✓ UNCHANGED // ALREADY CLEAN ]`** emerald status badge, `0 B CHANGED` pill, and `"VERIFIED CLEAN LINK // IDENTICAL TO INPUT"` notice on `CleanResultCard` so users never have to visually diff two identical URLs.
  - Upgraded the `Zlc` logo with a **5-mode Randomized Cyberpunk `Z` Glitch & Stunt Engine** (Seismic Tri-Chromatic Jiggle, Horizontal CRT Scanline Slice Tear, Kinetic Snap-Flip, High-Voltage Neon Overdrive with corner sparks, and Quantum Blink) in both the Android UI (`ZlcLogo.kt`) and web portal (`index.html`, `repo.html`).
  - Added zero-UI **In-Place Android Text Selection Replacement (`ACTION_PROCESS_TEXT` write-back)**: highlighting a dirty URL inside any editable text field (Signal, WhatsApp, Gmail, Notes) and tapping **ZnepLinkCleaner** replaces the link in-place without switching apps.
  - Added **Canonical URL Normalization**: unwraps Google AMP cache/viewer URLs (`*.cdn.ampproject.org/v/s/...` and `/amp` suffixes) and condenses verbose Amazon product paths (`/Product-Title-Slug/dp/ASIN/ref=...`) down to canonical `/dp/<ASIN>` permalinks.
  - Added a collapsible **Redirect Hop Chain Inspector** that records every intermediate redirect hop (`Offline Shim`, `AMP Cache Unwrap`, `HTTP 301`/`302`, `Meta WebView`) and displays the step-by-step trace on demand.

## [v17.0] — Build 17 (November 2, 2026)
### Google Play Publishing Version Bump (v17.0 / Build 17) & Tagged GitHub Release Sync
- **Triggering User Prompt**:
  > *"Publishing to Google Play finished. Please bump the version code: increment `versionCode` by 1 in the app build.gradle(.kts) file, and set `versionName` to \"<new versionCode>.0\"."*
- **Highlights**:
  - Incremented `versionCode` from `16` to `17` and updated `versionName` to `"17.0"` following Google Play publishing.
  - Synchronized `v17.0` release notes and triggering prompt across the Android app (`ReleaseNotes.kt`), linked GitHub repository (`README.md` & `CHANGELOG.md`), and web portal (`index.html` & `repo.html`).
  - Committed `v17.0` as a separate prompt-annotated commit on `main`, tagged `v17.0`, and pushed to `https://github.com/znep/znep-link-cleaner`.

## [v16.0] — Build 16 (November 2, 2026)
### Untrusted Link Security Hardening, SSRF Firewall, WebView Sandbox & Anti-Phishing Advisories
- **Triggering User Prompt**:
  > *"Analyze the app for security concerns including, but not limited to, dangers of processing untrusted links. Recommend improvements. [Selected: Block private IPs and non-HTTPS schemes in UrlExpander; Lock down Facebook WebView domains, cookies, and file access; Detect homograph, user@host, HTTP, and APK phishing risks; Expand automatically by default with a toggle for Offline-Only mode]"*
- **Highlights**:
  - Implemented `UrlSecurityValidator` and `SafePublicDns` in `UrlExpander` to block SSRF redirects against loopback, RFC1918 LAN, link-local, CGNAT, and cloud metadata IPs (`127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.169.254`, IPv6 `::1`/`fe80::`).
  - Enforced strict `http`/`https` scheme validation across all redirect hops, offline link shims, and browser intent launches (blocking `javascript:`, `intent:`, `file:`, `content:`, and `data:` URI injection).
  - Hardened `FacebookWebViewExpander` sandbox: locked navigation and subresource requests strictly to HTTPS official Meta domains, disabled file/content/DOM storage and cookies, and wiped cache/storage on teardown.
  - Added anti-phishing & homograph detection that strips deceptive `user:pass@host` credentials and alerts on Punycode (`xn--`) lookalike domains, `.apk`/`.exe` downloads, raw IPs, and cleartext HTTP links.
  - Added **Offline-Only Mode** toggle in Protection settings to prevent automatic outbound network requests on untrusted shortlinks, with an on-demand **"Expand Shortlink Now"** button.

## [v15.0] — Build 15 (November 2, 2026)
### Public GitHub Repo Integration, Live Embedded Commit Explorer & Per-Release Prompt-Annotated Git Commits
- **Triggering User Prompt**:
  > *"I had to manually push changes to GitHub from the UI and it didn't include the prompt in the commit message as requested. I want every new version published to the website to also be pushed to GitHub as a separate commit. Also note the GitHub repo is public and should be linked to and embedded if possible."*
- **Highlights**:
  - Linked and embedded the public GitHub repository (`https://github.com/znep/znep-link-cleaner`) across the web portal (`index.html` & `repo.html`) and Android app UI (`CleanScreen.kt` & `RulesScreen.kt`).
  - Added a live interactive **GitHub Repository & Commit Explorer** widget on `linkcleaner.znep.com` that streams real-time commit metadata and displays expandable prompt-annotated commit messages for all releases (`v1.0.0` – `v15.0`).
  - Rebuilt and standardized all release Git commits (`v1.0.0` through `v15.0`) as separate commits on `main` whose commit message bodies explicitly include the triggering `User Prompt:` and `Changes:`.
  - Added automated GitHub remote push daemon (`scripts/sync_github.py` + `server.py` `.env` watcher) so every new version published to the website is pushed as a separate prompt-annotated commit to `https://github.com/znep/znep-link-cleaner`.

## [v14.0] — Build 14 (November 2, 2026)
### Google Play Publishing Version Bump (v14.0 / Build 14)
- **Triggering User Prompt**:
  > *"Publishing to Google Play finished. Please bump the version code: increment `versionCode` by 1 in the app build.gradle(.kts) file, and set `versionName` to \"<new versionCode>.0\"."*
- **Highlights**:
  - Incremented `versionCode` from `13` to `14` and updated `versionName` to `"14.0"` following Google Play publishing.
  - Synchronized `v14.0` release notes and triggering prompt across the Android app (`ReleaseNotes.kt`), linked GitHub repository (`README.md` & `CHANGELOG.md`), and web portal (`index.html` & `repo.html`).
  - Repackaged `ZnepLinkCleaner.apk` (`v14.0`), `ZnepLinkCleaner.bundle`, `ZnepLinkCleaner-git.zip`, and `ZnepLinkCleaner-v14.0-source.zip`.

## [v13.0] — Build 13 (November 2, 2026)
### Cyberpunk Quivering 'Zlc' Logo & Linked GitHub / Portal Repository Sync
- **Triggering User Prompt**:
  > *"Update the app logo to be a large Z with a smaller \"lc\". Where possible make the Z quiver slightly. It should be cyberpunk inspired but not too complicated. / Also ensure the GitHub repo published is a copy of the linked GitHub repo and the linked GitHub is updated with the prompts and changes for each release. Persist this request."*
- **Highlights**:
  - Redesigned app logo to a cyberpunk `Zlc` monogram (large Electric Cyan `Z` with Neon Purple chromatic glitch + smaller steady `lc`).
  - Added interactive & periodic quiver animation to the `Z` in the Android Compose UI (quivers on tap, on link clean/expand, and in periodic bursts) and on the web portal.
  - Updated Android adaptive launcher icons (`ic_launcher_foreground.xml` / `ic_launcher_background.xml`) and all 5 density PNG mipmaps (`mdpi`–`xxxhdpi`).
  - Synchronized the workspace Git repository (`README.md`, `CHANGELOG.md`, annotated Git tags) with the published web portal repo (`repo.html`, `ZnepLinkCleaner.bundle`, `ZnepLinkCleaner-git.zip`).
  - Persisted standing rule in `AGENTS.md`, `README.md`, and unit tests requiring every release's prompt and changes to be synced across GitHub and the published portal repo.

## [v12.0] — Build 12 (November 2, 2026)
### Google Play Publishing Version Bump, Direct Nginx Portal Hardening & Cloudflare Reverse-Proxy Support
- **Triggering User Prompt**:
  > *"The website is down again. / Publishing to Google Play finished. Please bump the version code: increment `versionCode` by 1 in the app build.gradle(.kts) file, and set `versionName` to \"<new versionCode>.0\". / Remove the dns setup instructions from the website, they are only for me not the public. Also, the option A cloud flare setup is redirecting me to the run.app name not proxying. Note I'm on a free cloud flare plan."*
- **Highlights**:
  - Bumped `versionCode` to `12` and `versionName` to `"12.0"` following Google Play publishing.
  - Removed internal DNS setup instructions from the public web portal.
  - Configured Nginx with `absolute_redirect off` and `server_name_in_redirect off` so proxied requests via `linkcleaner.znep.com` never redirect to the underlying container hostname.
  - Hardened public web portal by configuring Nginx (PID 5) to serve `/app/applet/download_portal` directly with zero upstream dependency + self-healing `server.py` daemon.

## [v2.0.0] — Build 11 (November 2, 2026)
### ZnepLinkCleaner Rebrand, znep.com Domain & Prompt-Tracked PoC
- **Triggering User Prompt**:
  > *"Before we go further, I want to rename the app. The name should still indicate what it does but be unique in a way that might be legally protectable in the future. Perhaps include \"znep\"... Choose ZnepLinkCleaner. I own znep.com. The app can live under that name, and you can give me DNS entries to setup make linkcleaner.znep.com resolve to the website. Don't worry about backwards compatibility. Ensure you are recording prompts as part of the release notes and have clear messaging the app is a vibe coded proof of concept, and that this instruction sticks in the future."*
- **Highlights**:
  - Full rebrand to `ZnepLinkCleaner` across Android UI, system Share Sheet targets, and web portal.
  - Migrated Android `applicationId` to authoritative domain package `com.znep.linkcleaner`.
  - Added prominent "Vibe-Coded Proof of Concept" banner and messaging in-app and on `linkcleaner.znep.com`.
  - Added verbatim/reconstructed User Prompts to every release note entry (`v1.0.0` – `v2.0.0`).
  - Removed legacy `CleanLink` artifacts for a clean break under the `ZnepLinkCleaner` brand.

## [v1.9.0] — Build 10 (November 2, 2026)
### Instagram exln Stripping & Enhanced Multi-Platform Tracking Purge
- **Triggering User Prompt**:
  > *"Instagram links still have an exln param that looks like tracking. Should it be removed? If so, update the app and also look for other missing tracking params."*
- **Highlights**:
  - Stripped Instagram `exln`, `obrf`, `vrfl`, `srtk`, `cplk`, `dlrf`, and `mdxt` user share tracking tokens.
  - Added Meta Threads `xmt` cross-app telemetry and `slof` share log parameter removal.
  - Purged Reddit user share identity tracking token `shrid`.
  - Added TikTok user profile tracking (`u_code`), preview session (`preview_pb`), and ad click (`ttclid`) removal.
  - Added LinkedIn first-party ad tracking (`li_fat_id`), page instance (`lipi`), and click identifier (`licu`) stripping.
  - Added e-commerce & affiliate parameter coverage for eBay (`mkevt`, `mkcid`, `campid`), AliExpress (`spm`, `scm`), and Impact/Awin.

## [v1.8.0] — Build 9 (November 2, 2026)
### Share Sheet Prominence, Public Portal Watchdog & Full Release History
- **Triggering User Prompt**:
  > *"The website is no longer public, ensure it remains public and analyze why previous attempts reverted. Always include the full release notes history on the webpage and within the app somewhere. You don't need to include prompts that don't modify the app itself. In addition, provide tips on how to make the app show up earlier and more prominently in the share dialogue. Modify the app if possible to help."*
- **Highlights**:
  - Direct Share Target Shortcut (`shortcuts.xml`) for prioritized top-row Android share sheet placement.
  - High-priority intent filter configuration (`android:priority=100`) for text and URLs.
  - Full Release Notes History viewer accessible directly in app and web portal.
  - Interactive Share Sheet Prominence Guide with 1-tap practice pinning tool.
  - Permanent Nginx auth-bypass watchdog in `server.py` to keep the web portal public 24/7.

## [v1.7.0] — Build 8 (November 2, 2026)
### Facebook Headless WebView Expander & Dual-Tier Warning System
- **Triggering User Prompt**:
  > *"Add Option 1 (on-device headless Android WebView resolution) for Facebook share links (/share/p/, /share/r/, etc.) to resolve client-side JS redirects to canonical permalinks. Implement a red warning when Facebook share links fail to expand and an orange advisory for unexpanded shorteners. Include technical details for Facebook share links in the app without cluttering the main screen, and provide a config setting to disable Option 1 (enabled by default)."*
- **Highlights**:
  - On-device headless Android WebView expander (Option 1) for Facebook `/share/p/` & `/share/r/` links.
  - Red Warning alert when Facebook permalinks cannot be resolved (private/login gate).
  - Orange Advisory alert for unexpanded opaque shorteners (`bit.ly`, `t.co`, `vt.tiktok.com`).
  - Protection setting to toggle Facebook WebView expansion (enabled by default).
  - Facebook Share Links Architecture technical deep dive in Rules tab.

## [v1.6.0] — Build 7 (November 1, 2026)
### Architecture Documentation & Web Portal Infrastructure
- **Triggering User Prompt**:
  > *"Create a public download portal webpage hosting the signed APK, an offline Git bundle, a source archive, an interactive Git repository browser, and a visual system architecture diagram."*
- **Highlights**:
  - Architectural topology diagram generation and offline vector assets.
  - Standalone interactive Git Repository Browser (`repo.html`).
  - Web Download Portal with direct APK and Git bundle downloads.

## [v1.5.0] — Build 6 (October 30, 2026)
### Retro Monospace Aesthetics & System Share Flow
- **Triggering User Prompt**:
  > *"Restyle the UI with a sleek retro/cyberpunk monospace terminal aesthetic, add auto-copy on share and auto-close after reshare options, and design a custom adaptive launcher icon."*
- **Highlights**:
  - Cyberpunk/retro monospace typography and neon telemetry highlights.
  - Auto-copy clean URL to clipboard immediately upon sharing from external apps.
  - Auto-close app after resharing via system sheet to return seamlessly to previous app.
  - Custom adaptive launcher icon with laser-clean vector branding.

## [v1.4.0] — Build 5 (October 28, 2026)
### Strict Cleaning Mode & Amazon Affiliate Preservation
- **Triggering User Prompt**:
  > *"Add a Protection / Rules tab with a toggle to preserve Amazon creator affiliate tags while stripping tracking parameters, plus a Strict Cleaning Mode for aggressive heuristic token removal."*
- **Highlights**:
  - Strict mode to purge non-standard tokens, unknown query arguments, and custom tracking hashes.
  - Option to preserve Amazon creator affiliate tags (`tag=...`) while purging all tracking telemetry.
  - Protection / Rules tab with granular user preference switches.

## [v1.3.0] — Build 4 (October 26, 2026)
### Room SQLite Local History & Privacy Telemetry
- **Triggering User Prompt**:
  > *"Add a local Room database to record cleaned link history on-device and show privacy stats (links cleaned, trackers removed, and bytes saved)."*
- **Highlights**:
  - Persistent cleaning history saved securely on-device using Room SQLite.
  - Analytics dashboard: total links cleaned, stripped parameters count, and estimated bytes saved.
  - Individual history item deletion and one-tap history wipe.

## [v1.2.0] — Build 3 (October 24, 2026)
### Shortener Traversal & Redirect Engine
- **Triggering User Prompt**:
  > *"Support expanding shortened links like TikTok (vt.tiktok.com), Amazon (a.co, amzn.to), bit.ly, and t.co by following HTTP redirects before cleaning the destination URL."*
- **Highlights**:
  - Network redirect traversal for TikTok (`vt.tiktok.com`, `vm.tiktok.com`, `/t/`).
  - YouTube short link canonicalization (`youtu.be` with `si` parameter stripping).
  - Amazon shortlink expansion (`a.co`, `amzn.to`).
  - Support for `bit.ly`, `tinyurl.com`, `t.co`, and 30+ major redirect services.

## [v1.1.0] — Build 2 (October 22, 2026)
### Material 3 Dynamic Theming & Clipboard Detection
- **Triggering User Prompt**:
  > *"Add Material 3 theming, automatic clipboard link detection with a quick-paste button, and a searchable dictionary of known tracking parameters."*
- **Highlights**:
  - Material 3 dynamic color scheme with dark/light mode support.
  - Automatic clipboard detection with quick-paste button when URL is copied.
  - Comprehensive database of known tracking parameter definitions.

## [v1.0.0] — Build 1 (October 20, 2026)
### Initial Release — Pure On-Device Link Sanitizer
- **Triggering User Prompt**:
  > *"Build an Android app that receives shared links from the Android Share Sheet, strips tracking parameters like utm_source, fbclid, and gclid, and lets you copy or reshare the clean URL."*
- **Highlights**:
  - Offline tracker stripping for Google Analytics (`utm_*`), Facebook (`fbclid`), Google Ads (`gclid`), and Twitter (`ref_src`).
  - Clean URL output with one-tap copy and system share button.
  - Instant share-intent ingestion via Android `SEND` action.
