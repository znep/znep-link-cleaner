# ZnepLinkCleaner (`com.znep.linkcleaner`) — v13.0 (Build 13)

> **[ VIBE-CODED PROOF OF CONCEPT ]**
> **ZnepLinkCleaner** is a 100% vibe-coded proof of concept Android application and web portal (`https://linkcleaner.znep.com`) built iteratively through natural-language prompts in Google AI Studio. Every prompt that modified the application and its corresponding changes are permanently recorded in this `README.md`, `CHANGELOG.md`, `ReleaseNotes.kt`, the in-app **Protection / Rules** tab, and on the public web portal (`index.html` & `repo.html`).

---

## Standing Maintenance Rules (Mandatory)
1. **Always Include Full Release History & Prompts**: Every modification to the app must add a new version entry to `ReleaseNotes.kt`, `README.md`, `CHANGELOG.md`, `download_portal/index.html`, and `download_portal/repo.html`, including the exact `userPrompt` that triggered the change, while preserving all prior releases (`v1.0.0` – `v13.0`+).
2. **Preserve Vibe-Coded Proof of Concept Messaging**: Maintain clear messaging in the app, GitHub repository, and web portal that this project is a vibe-coded proof of concept.
3. **Keep Public Web Portal Online**: Ensure Nginx (PID 5) serves `/app/applet/download_portal` directly with `absolute_redirect off; server_name_in_redirect off;` and `server.py` runs on port 3000 with the Lua auth-bypass watchdog active.
4. **Linked GitHub & Published Portal Repository Parity**: Ensure the published Git repository on the web portal (`repo.html`, `ZnepLinkCleaner.bundle`, `ZnepLinkCleaner-git.zip`, `ZnepLinkCleaner-v13.0-source.zip`) is an exact copy of the linked GitHub repository, updated with the prompts and changes for every release.

---

## Complete Release Notes & Vibe-Coding Prompt History (`v1.0.0` – `v13.0`)

### `v13.0` (Build 13) — November 2, 2026
**Cyberpunk Quivering 'Zlc' Logo & Linked GitHub / Portal Repository Sync**
- **User Prompt**:
  > *"Update the app logo to be a large Z with a smaller \"lc\". Where possible make the Z quiver slightly. It should be cyberpunk inspired but not too complicated. / Also ensure the GitHub repo published is a copy of the linked GitHub repo and the linked GitHub is updated with the prompts and changes for each release. Persist this request."*
- **Changes**:
  - Redesigned app logo to a cyberpunk `Zlc` monogram (large Electric Cyan `Z` with Neon Purple chromatic glitch + smaller steady `lc`).
  - Added interactive & periodic quiver animation to the `Z` in the Android Compose UI (quivers on tap, on link clean/expand, and in periodic bursts) and on the web portal.
  - Updated Android adaptive launcher icons (`ic_launcher_foreground.xml` / `ic_launcher_background.xml`) and all 5 density PNG mipmaps (`mdpi`–`xxxhdpi`).
  - Synchronized the workspace Git repository (`README.md`, `CHANGELOG.md`, annotated Git tags) with the published web portal repo (`repo.html`, `ZnepLinkCleaner.bundle`, `ZnepLinkCleaner-git.zip`).
  - Persisted standing rule in `AGENTS.md`, `README.md`, and unit tests requiring every release's prompt and changes to be synced across the linked GitHub repo and the published portal repo.

### `v12.0` (Build 12) — November 2, 2026
**Google Play Publishing Version Bump, Direct Nginx Portal Hardening & Cloudflare Reverse-Proxy Support**
- **User Prompt**:
  > *"The website is down again. / Publishing to Google Play finished. Please bump the version code: increment `versionCode` by 1 in the app build.gradle(.kts) file, and set `versionName` to \"<new versionCode>.0\". / Remove the dns setup instructions from the website, they are only for me not the public. Also, the option A cloud flare setup is redirecting me to the run.app name not proxying. Note I'm on a free cloud flare plan."*
- **Changes**:
  - Bumped `versionCode` to `12` and `versionName` to `"12.0"` following Google Play publishing.
  - Removed internal DNS setup instructions from the public web portal.
  - Configured Nginx with `absolute_redirect off` and `server_name_in_redirect off` so proxied requests via `linkcleaner.znep.com` never redirect to the underlying container hostname.
  - Hardened public web portal by configuring Nginx (PID 5) to serve `/app/applet/download_portal` directly with zero upstream dependency + self-healing `server.py` daemon.

### `v2.0.0` (Build 11) — November 2, 2026
**ZnepLinkCleaner Rebrand, znep.com Domain & Prompt-Tracked PoC**
- **User Prompt**:
  > *"Before we go further, I want to rename the app. The name should still indicate what it does but be unique in a way that might be legally protectable in the future. Perhaps include \"znep\"... Choose ZnepLinkCleaner. I own znep.com. The app can live under that name, and you can give me DNS entries to setup make linkcleaner.znep.com resolve to the website. Don't worry about backwards compatibility. Ensure you are recording prompts as part of the release notes and have clear messaging the app is a vibe coded proof of concept, and that this instruction sticks in the future."*
- **Changes**:
  - Full rebrand to `ZnepLinkCleaner` across Android UI, system Share Sheet targets, and web portal.
  - Migrated Android `applicationId` to authoritative domain package `com.znep.linkcleaner`.
  - Added prominent "Vibe-Coded Proof of Concept" banner and messaging in-app and on `linkcleaner.znep.com`.
  - Added verbatim/reconstructed User Prompts to every release note entry (`v1.0.0` – `v2.0.0`).
  - Removed legacy `CleanLink` artifacts for a clean break under the `ZnepLinkCleaner` brand.

### `v1.9.0` (Build 10) — November 2, 2026
**Instagram exln Stripping & Enhanced Multi-Platform Tracking Purge**
- **User Prompt**:
  > *"Instagram links still have an exln param that looks like tracking. Should it be removed? If so, update the app and also look for other missing tracking params."*
- **Changes**:
  - Stripped Instagram `exln`, `obrf`, `vrfl`, `srtk`, `cplk`, `dlrf`, and `mdxt` user share tracking tokens.
  - Added Meta Threads `xmt` cross-app telemetry and `slof` share log parameter removal.
  - Purged Reddit user share identity tracking token `shrid`.
  - Added TikTok user profile tracking (`u_code`), preview session (`preview_pb`), and ad click (`ttclid`) removal.
  - Added LinkedIn first-party ad tracking (`li_fat_id`), page instance (`lipi`), and click identifier (`licu`) stripping.
  - Added e-commerce & affiliate parameter coverage for eBay (`mkevt`, `mkcid`, `campid`), AliExpress (`spm`, `scm`), and Impact/Awin.

### `v1.8.0` (Build 9) — November 2, 2026
**Share Sheet Prominence, Public Portal Watchdog & Full Release History**
- **User Prompt**:
  > *"The website is no longer public, ensure it remains public and analyze why previous attempts reverted. Always include the full release notes history on the webpage and within the app somewhere. You don't need to include prompts that don't modify the app itself. In addition, provide tips on how to make the app show up earlier and more prominently in the share dialogue. Modify the app if possible to help."*
- **Changes**:
  - Direct Share Target Shortcut (`shortcuts.xml`) for prioritized top-row Android share sheet placement.
  - High-priority intent filter configuration (`android:priority=100`) for text and URLs.
  - Full Release Notes History viewer accessible directly in app and web portal.
  - Interactive Share Sheet Prominence Guide with 1-tap practice pinning tool.
  - Permanent Nginx auth-bypass watchdog in `server.py` to keep the web portal public 24/7.

### `v1.7.0` (Build 8) — November 2, 2026
**Facebook Headless WebView Expander & Dual-Tier Warning System**
- **User Prompt**:
  > *"Add Option 1 (on-device headless Android WebView resolution) for Facebook share links (/share/p/, /share/r/, etc.) to resolve client-side JS redirects to canonical permalinks. Implement a red warning when Facebook share links fail to expand and an orange advisory for unexpanded shorteners. Include technical details for Facebook share links in the app without cluttering the main screen, and provide a config setting to disable Option 1 (enabled by default)."*
- **Changes**:
  - On-device headless Android WebView expander (Option 1) for Facebook `/share/p/` & `/share/r/` links.
  - Red Warning alert when Facebook permalinks cannot be resolved (private/login gate).
  - Orange Advisory alert for unexpanded opaque shorteners (`bit.ly`, `t.co`, `vt.tiktok.com`).
  - Protection setting to toggle Facebook WebView expansion (enabled by default).
  - Facebook Share Links Architecture technical deep dive in Rules tab.

### `v1.6.0` (Build 7) — November 1, 2026
**Architecture Documentation & Web Portal Infrastructure**
- **User Prompt**:
  > *"Create a public download portal webpage hosting the signed APK, an offline Git bundle, a source archive, an interactive Git repository browser, and a visual system architecture diagram."*
- **Changes**:
  - Architectural topology diagram generation and offline vector assets.
  - Standalone interactive Git Repository Browser (`repo.html`).
  - Web Download Portal with direct APK and Git bundle downloads.

### `v1.5.0` (Build 6) — October 30, 2026
**Retro Monospace Aesthetics & System Share Flow**
- **User Prompt**:
  > *"Restyle the UI with a sleek retro/cyberpunk monospace terminal aesthetic, add auto-copy on share and auto-close after reshare options, and design a custom adaptive launcher icon."*
- **Changes**:
  - Cyberpunk/retro monospace typography and neon telemetry highlights.
  - Auto-copy clean URL to clipboard immediately upon sharing from external apps.
  - Auto-close app after resharing via system sheet to return seamlessly to previous app.
  - Custom adaptive launcher icon with laser-clean vector branding.

### `v1.4.0` (Build 5) — October 28, 2026
**Strict Cleaning Mode & Amazon Affiliate Preservation**
- **User Prompt**:
  > *"Add a Protection / Rules tab with a toggle to preserve Amazon creator affiliate tags while stripping tracking parameters, plus a Strict Cleaning Mode for aggressive heuristic token removal."*
- **Changes**:
  - Strict mode to purge non-standard tokens, unknown query arguments, and custom tracking hashes.
  - Option to preserve Amazon creator affiliate tags (`tag=...`) while purging all tracking telemetry.
  - Protection / Rules tab with granular user preference switches.

### `v1.3.0` (Build 4) — October 26, 2026
**Room SQLite Local History & Privacy Telemetry**
- **User Prompt**:
  > *"Add a local Room database to record cleaned link history on-device and show privacy stats (links cleaned, trackers removed, and bytes saved)."*
- **Changes**:
  - Persistent cleaning history saved securely on-device using Room SQLite.
  - Analytics dashboard: total links cleaned, stripped parameters count, and estimated bytes saved.
  - Individual history item deletion and one-tap history wipe.

### `v1.2.0` (Build 3) — October 24, 2026
**Shortener Traversal & Redirect Engine**
- **User Prompt**:
  > *"Support expanding shortened links like TikTok (vt.tiktok.com), Amazon (a.co, amzn.to), bit.ly, and t.co by following HTTP redirects before cleaning the destination URL."*
- **Changes**:
  - Network redirect traversal for TikTok (`vt.tiktok.com`, `vm.tiktok.com`, `/t/`).
  - YouTube short link canonicalization (`youtu.be` with `si` parameter stripping).
  - Amazon shortlink expansion (`a.co`, `amzn.to`).
  - Support for `bit.ly`, `tinyurl.com`, `t.co`, and 30+ major redirect services.

### `v1.1.0` (Build 2) — October 22, 2026
**Material 3 Dynamic Theming & Clipboard Detection**
- **User Prompt**:
  > *"Add Material 3 theming, automatic clipboard link detection with a quick-paste button, and a searchable dictionary of known tracking parameters."*
- **Changes**:
  - Material 3 dynamic color scheme with dark/light mode support.
  - Automatic clipboard detection with quick-paste button when URL is copied.
  - Comprehensive database of known tracking parameter definitions.

### `v1.0.0` (Build 1) — October 20, 2026
**Initial Release — Pure On-Device Link Sanitizer**
- **User Prompt**:
  > *"Build an Android app that receives shared links from the Android Share Sheet, strips tracking parameters like utm_source, fbclid, and gclid, and lets you copy or reshare the clean URL."*
- **Changes**:
  - Offline tracker stripping for Google Analytics (`utm_*`), Facebook (`fbclid`), Google Ads (`gclid`), and Twitter (`ref_src`).
  - Clean URL output with one-tap copy and system share button.
  - Instant share-intent ingestion via Android `SEND` action.

---

## Building the Project

```bash
gradle :app:assembleDebug
```
The compiled APK is generated at `app/build/outputs/apk/debug/app-debug.apk` and published to `download_portal/ZnepLinkCleaner.apk` and `download_portal/ZnepLinkCleaner-v13.0.apk`.
