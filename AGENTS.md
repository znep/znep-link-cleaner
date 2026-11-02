# ZnepLinkCleaner — Permanent Agent & Project Standing Instructions

> [!CRITICAL]
> **MANDATORY STANDING INSTRUCTIONS FOR ALL FUTURE TURNS & COMPACTIONS**
> These rules were explicitly established by the project owner (Marc Slemko) and **MUST** be obeyed in every future session:

1. **Vibe-Coded Proof of Concept Messaging**:
   - Always keep clear, visible messaging in both the Android app UI (`CleanScreen.kt`, `RulesScreen.kt`) and on the public web portal (`download_portal/index.html`) that **ZnepLinkCleaner** is a **"Vibe-Coded Proof of Concept"** built iteratively via natural-language prompts.

2. **Full Release Notes History + Triggering User Prompts**:
   - Always include the **complete release notes history** (`v1.0.0` through the latest version) in both the Android app (`ReleaseNotes.kt` / `RulesScreen.kt`) and on the web portal (`download_portal/index.html`).
   - Every release note entry **MUST** include the `userPrompt` that modified the application. (Do not include conversational prompts that did not modify the app itself).
   - Never truncate or remove older releases when adding a new release.

3. **Brand & Domain Identity (`znep.com`)**:
   - App Name: **`ZnepLinkCleaner`** (synced in `metadata.json`, `res/values/strings.xml`, and `settings.gradle.kts`).
   - Android Package (`applicationId`): **`com.znep.linkcleaner`** (keeping internal `namespace = "com.example"` for source directories).
   - Canonical Web Portal Subdomain: **`https://linkcleaner.znep.com`**.
   - Do not re-introduce legacy `CleanLink` filenames or branding.

4. **Public Web Portal Availability (Zero 403 Forbidden Reverts)**:
   - Always ensure `/app/applet/server.py` is running as a daemon on port 3000 serving `/app/applet/download_portal`.
   - Ensure the background watchdog in `server.py` keeps `do return end` at the top of `/etc/nginx/user_auth_verification.lua` so the public URL never reverts to 403 Forbidden.
