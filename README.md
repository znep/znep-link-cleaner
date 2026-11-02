# ZnepLinkCleaner (`com.znep.linkcleaner`)

> **[ VIBE-CODED PROOF OF CONCEPT ]**
> **ZnepLinkCleaner** is a 100% vibe-coded proof of concept Android application and web portal built iteratively through natural-language prompts. Every prompt that modified the application is recorded in `ReleaseNotes.kt`, the in-app **Protection / Rules** tab, and on the public web portal (`https://linkcleaner.znep.com` / `https://ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app/`).

## Standing Maintenance Rules (Mandatory)
1. **Always Include Full Release History & Prompts**: Every modification to the app must add a new version entry to `ReleaseNotes.kt` and `download_portal/index.html`, including the exact `userPrompt` that triggered the change, while preserving all prior releases (`v1.0.0` – `v12.0`+).
2. **Preserve Vibe-Coded Proof of Concept Messaging**: Maintain clear messaging in the app and on the webpage that this project is a vibe-coded proof of concept.
3. **Keep Public Web Portal Online (`ais-dev`)**: Ensure Nginx (PID 5) serves `/app/applet/download_portal` directly and `server.py` runs on port 3000 with the Lua auth-bypass watchdog active (`https://ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app/`).

## Building the Project

### Command Line
```bash
gradle :app:assembleDebug
```
The compiled APK is generated at `app/build/outputs/apk/debug/app-debug.apk` and published to `download_portal/ZnepLinkCleaner.apk` and `download_portal/ZnepLinkCleaner-v12.0.apk`.
