# ZnepLinkCleaner — Permanent Agent & Project Standing Instructions

> [!CRITICAL]
> **MANDATORY STANDING INSTRUCTIONS FOR ALL FUTURE TURNS & COMPACTIONS**
> These rules were explicitly established by the project owner (Marc Slemko) and **MUST** be obeyed in every future session:

1. **Vibe-Coded Proof of Concept Messaging**:
   - Always keep clear, visible messaging in both the Android app UI (`CleanScreen.kt`, `RulesScreen.kt`) and on the public web portal (`download_portal/index.html`, `download_portal/repo.html`, `README.md`, `CHANGELOG.md`) that **ZnepLinkCleaner** is a **"Vibe-Coded Proof of Concept"** built iteratively via natural-language prompts.

2. **Full Release Notes History + Triggering User Prompts**:
   - Always include the **complete release notes history** (`v1.0.0` through the latest version) in the Android app (`ReleaseNotes.kt` / `RulesScreen.kt`), the web portal (`download_portal/index.html` and `download_portal/repo.html`), and the Git repository documentation (`README.md` and `CHANGELOG.md`).
   - Every release note entry **MUST** include the `userPrompt` that modified the application. (Do not include conversational prompts that did not modify the app itself).
   - Never truncate or remove older releases when adding a new release.

3. **Brand, Logo & Domain Identity (`znep.com`)**:
   - App Name: **`ZnepLinkCleaner`** (synced in `metadata.json`, `res/values/strings.xml`, and `settings.gradle.kts`).
   - Android Package (`applicationId`): **`com.znep.linkcleaner`** (keeping internal `namespace = "com.example"` for source directories).
   - Canonical Web Portal Subdomain: **`https://linkcleaner.znep.com`** (Remove internal DNS setup instructions from the public webpage).
   - Logo Identity: Cyberpunk **`Zlc`** monogram (large Electric Cyan **`Z`** with Neon Purple chromatic glitch + smaller steady **`lc`** subscript), where the **`Z`** quivers slightly in the Compose UI (`ZlcLogo.kt`) and web portal (`index.html`, `repo.html`).

4. **Public Web Portal Availability (Zero 403/502 Reverts)**:
   - Always ensure Nginx (`PID 5`) serves `/app/applet/download_portal` directly with `absolute_redirect off; server_name_in_redirect off;` and `/app/applet/server.py` is running as a daemon on port 3000.
   - Ensure the background watchdog in `server.py` keeps `do return end` at the top of `/etc/nginx/user_auth_verification.lua` so the public URL never reverts to 403 Forbidden or 502 warmup.html.

5. **Linked GitHub Repository & Published Portal Repo Parity (MANDATORY)**:
   - The workspace Git repository (which syncs to the linked GitHub repository) and the published repository on the web portal (`download_portal/repo.html`, `download_portal/ZnepLinkCleaner.bundle`, `download_portal/ZnepLinkCleaner-git.zip`, and `download_portal/ZnepLinkCleaner-v<version>-source.zip`) **MUST always be a 1:1 copy of each other**.
   - Every release **MUST** update `README.md`, `CHANGELOG.md`, `download_portal/repo.html`, and annotated Git commits/tags (`v1.0.0` through latest) with the exact **user prompts and changes for each release**, then regenerate `ZnepLinkCleaner.bundle` and `ZnepLinkCleaner-git.zip` from `HEAD` (and push to `origin` if a remote token is configured).
