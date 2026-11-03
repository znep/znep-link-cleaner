# ZnepLinkCleaner — Permanent Agent & Project Standing Instructions

> [!CRITICAL]
> **MANDATORY STANDING INSTRUCTIONS FOR ALL FUTURE TURNS & COMPACTIONS**
> These rules were explicitly established by the project owner (Marc Slemko) and **MUST** be obeyed in every future session:

1. **Vibe-Coded Proof of Concept Messaging**:
   - Always keep clear, visible messaging in both the Android app UI (`CleanScreen.kt`, `RulesScreen.kt`) and on the public web portal (`download_portal/index.html`, `download_portal/repo.html`, `README.md`, `CHANGELOG.md`) that **ZnepLinkCleaner** is a **"Vibe-Coded Proof of Concept"** built iteratively via natural-language prompts.

2. **Full Release Notes History + Triggering User Prompts**:
   - Always include the **complete release notes history** (`v1.0.0` through the latest version) in the Android app (`ReleaseNotes.kt` / `RulesScreen.kt`), the web portal (`download_portal/index.html` and `download_portal/repo.html`), and the Git repository documentation (`README.md` and `CHANGELOG.md`).
   - Every release note entry **MUST** include the `userPrompt` that modified the application. (Do not include conversational prompts or pure documentation/standing-rule edits that did not modify the app itself).
   - Never truncate or remove older releases when adding a new release.

3. **Brand, Logo & Domain Identity (`znep.com`)**:
   - App Name: **`ZnepLinkCleaner`** (synced in `metadata.json`, `res/values/strings.xml`, and `settings.gradle.kts`).
   - Android Package (`applicationId`): **`com.znep.linkcleaner`** (keeping internal `namespace = "com.example"` for source directories).
   - Canonical Web Portal Subdomain: **`https://linkcleaner.znep.com`** (Remove internal DNS setup instructions from the public webpage).
   - Logo Identity: Cyberpunk **`Zlc`** monogram (large Electric Cyan **`Z`** with Neon Purple chromatic glitch + smaller steady **`lc`** subscript), powered by the **5-Mode Randomized Cyberpunk `Z` Glitch & Stunt Engine** (`SEISMIC_JIGGLE`, `SCANLINE_SLICE`, `CYBER_FLIP`, `VOLTAGE_SURGE`, `QUANTUM_BLINK`) in both the Compose UI (`ZlcLogo.kt`) and the web portal (`download_portal/index.html`, `download_portal/repo.html`), while keeping the **`lc`** subscript steady.

4. **Public Web Portal Availability & GitHub Pages (`gh-pages`)**:
   - Canonical production hosting for **`https://linkcleaner.znep.com`** is served via **GitHub Pages** (`gh-pages` branch + `download_portal/CNAME` + `download_portal/.nojekyll` + `.github/workflows/deploy-pages.yml`).
   - Also ensure Nginx (`PID 5`) serves `/app/applet/download_portal` directly in the workspace container with `absolute_redirect off; server_name_in_redirect off;` and `/app/applet/server.py` running as a daemon on port 3000, with the background watchdog keeping `do return end` at the top of `/etc/nginx/user_auth_verification.lua` so the preview URL never reverts to 403 Forbidden or 502 warmup.html.

5. **Linked Public GitHub Repository (`https://github.com/znep/znep-link-cleaner`), Releases & Per-Release Commit Push (MANDATORY)**:
   - Public GitHub Repository: **`https://github.com/znep/znep-link-cleaner`** (must always be linked in the Android app UI and both linked & embedded via the live interactive GitHub Commit Explorer on `download_portal/index.html` and `download_portal/repo.html`).
   - Every new app version published to the website **MUST** be committed as a **separate Git commit** on `main` whose commit message body explicitly includes `User Prompt:` (the exact prompt that triggered the release) and `Changes:`, tagged with annotated tag `v<version>`, and pushed to `origin` (`https://github.com/znep/znep-link-cleaner.git`) via `scripts/sync_github.py` / `server.py`.
   - Large binary release artifacts (`ZnepLinkCleaner.apk`, `ZnepLinkCleaner.bundle`, `ZnepLinkCleaner-git.zip`, and `ZnepLinkCleaner-v<version>-source.zip`) **MUST** be uploaded to **GitHub Releases** (`https://github.com/znep/znep-link-cleaner/releases/download/v<version>/...`) via `scripts/sync_github.py` so the Git repository and `gh-pages` branch remain lightweight and within GitHub size limits.
   - The workspace Git repository, the `gh-pages` branch, and the published repository metadata on the web portal (`download_portal/repo.html`, `download_portal/git_commits.json`, and the GitHub Release archives) **MUST always be a 1:1 copy of each other**.
   - Non-app documentation or standing-instruction updates (such as editing `AGENTS.md`) should be committed and pushed to `main` (and synced to `gh-pages` / `git_commits.json`) without bumping `versionCode` unless the Android app itself is modified.

6. **Truthful Status Reporting & Zero-Bloat UX Philosophy**:
   - **Never Falsely Report `[ CLEANED ]`**: Always distinguish modified URLs (`[ CLEANED ]`) from already-clean inputs (`[ ✓ UNCHANGED // ALREADY CLEAN ]` via `result.isUnchanged`, `0 B CHANGED` pill, and `"VERIFIED CLEAN LINK // IDENTICAL TO INPUT"` header) and failed/blocked expansions (`[ UNCHANGED // SEE ADVISORY ]` when expansion or security checks fail without changing the URL). Users must never have to visually diff two identical strings or be misled when an expansion fails.
   - **Zero-Bloat UX**: Avoid unnecessary screens, modals, or toggles. Favor automatic pipeline normalization (Google AMP unwrapping, Amazon `/dp/<ASIN>` path condensation, `fb://native_post/<base64>` offline permalink decoding), native OS hooks (in-place `ACTION_PROCESS_TEXT` text replacement when `EXTRA_PROCESS_TEXT_READONLY == false`), and collapsible progressive disclosure (Redirect Hop Chain inspector).

7. **Untrusted Link Security Invariants**:
   - Always preserve `UrlSecurityValidator` SSRF and DNS-rebinding protection (`SafePublicDns` blocking loopback, RFC1918 private LAN, link-local `169.254.0.0/16`, CGNAT `100.64.0.0/10`, multicast, and IPv6 ULA/site-local ranges on every redirect hop).
   - Enforce strict `http://` and `https://` scheme validation before any network socket connection, while supporting safe offline extraction of embedded `https://` permalinks from `intent://` and `fb://native_post/<base64>` deep links.
   - Keep `FacebookWebViewExpander` strictly sandboxed (`allowFileAccess = false`, `allowContentAccess = false`, `domStorageEnabled = false`, `CookieManager.acceptCookie = false`, restricted exclusively to official HTTPS Meta domains) and preserve anti-phishing advisories (`user@host` credential stripping, Punycode `xn--` homograph alerts, and high-risk executable extension warnings).
