# ZnepLinkCleaner — Complete Rebrand, `znep.com` Domain & Vibe-Coded PoC Plan (v2.0.0)

Rebrand the Android application, package identity (`com.znep.linkcleaner`), and public web portal (`linkcleaner.znep.com`) to **ZnepLinkCleaner** with zero legacy `CleanLink` baggage, add prominent **"Vibe-Coded Proof of Concept"** messaging across both the app and website, and record the **triggering user prompt** inside every release note entry (`v1.0.0` through `v2.0.0`) with permanent architectural guardrails so this rule sticks across all future sessions.

### User Review & Critical Decisions

> [!IMPORTANT]
> This revised plan incorporates all of your latest instructions directly:

1. **No Backwards Compatibility**: Remove old `CleanLink` filenames, aliases, and references completely. Exclusively publish and serve **`ZnepLinkCleaner.apk`**, **`ZnepLinkCleaner-v2.0.0.apk`**, **`ZnepLinkCleaner.bundle`**, **`ZnepLinkCleaner-git.zip`**, and **`ZnepLinkCleaner-v2.0.0-source.zip`** under the **`com.znep.linkcleaner`** package ID.
2. **User Prompts Recorded in Release Notes (`userPrompt` field)**: Every release entry (`v1.0.0` through `v2.0.0`) in `ReleaseNotes.kt`, the in-app Release History viewer (`RulesScreen.kt`), and the web portal (`index.html`) will include the verbatim/reconstructed **User Prompt** that triggered that app modification (excluding non-modifying conversational prompts).
3. **Clear "Vibe-Coded Proof of Concept" Messaging**:
   - **In-App**: Prominent badge/banner in the header and Protection/About screen stating that **ZnepLinkCleaner** is a *"100% Vibe-Coded Proof of Concept"* with transparent prompt-driven release history.
   - **Web Portal (`linkcleaner.znep.com`)**: Prominent callout banner in the hero section explaining that the entire Android app, headless WebView expander, and web portal were built iteratively as a **Vibe-Coded Proof of Concept**, with every modifying prompt documented in the release notes below.
4. **Permanent Standing Instructions (Compaction-Proof)**:
   - Write a dedicated `AGENTS.md` / `README.md` standing rules section and prominent KDoc headers inside `ReleaseNotes.kt` and `download_portal/index.html` mandating that **every future app modification MUST**:
     1. Increment the version and add a new `ReleaseNote` entry at the top of `ReleaseHistory.RELEASES` and `download_portal/index.html`.
     2. Record the exact `userPrompt` that modified the app.
     3. Preserve the complete historical changelog (`v1.0.0`+) and the **"Vibe-Coded Proof of Concept"** disclaimer in both the app and web portal.

---

### 1. Overview & Core Concept

- **What It Does**: **ZnepLinkCleaner** (`com.znep.linkcleaner`) is a vibe-coded proof-of-concept Android utility that strips invasive tracking parameters (`exln`, `igsh`, `fbclid`, `mibextid`, `xmt`, `shrid`, `u_code`, `li_fat_id`, `utm_*`, `gclid`, etc.), unwraps redirect shorteners, and resolves Facebook `/share/` links via an on-device headless WebView.
- **Target Audience / Persona**: Privacy-conscious Android users and developers exploring what a real-world, prompt-engineered ("vibe-coded") Android application looks like from initial prompt (`v1.0.0`) to `v2.0.0`.
- **Key Value**: Full transparency—showing both the working Android privacy tool and the exact natural-language prompts that built each capability over time.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Sanitizer Console & Vibe-Coded Banner**: The main screen features the **ZnepLinkCleaner** title, a `[ VIBE-CODED PROOF OF CONCEPT ]` badge, quick-test chips (including the new `[ Instagram ]` `exln` sample), and instant copy/share actions.
  2. **In-App Prompt & Release History (`RulesScreen.kt`)**: Each release card displays its version chip (`v2.0.0` down to `v1.0.0`), date, a styled **`> PROMPT:`** quote box showing the user request that drove that release, feature highlights, and technical details.
  3. **Public Web Portal (`linkcleaner.znep.com`)**: Displays the **ZnepLinkCleaner v2.0.0** header, the **Vibe-Coded Proof of Concept** banner, direct downloads (`ZnepLinkCleaner.apk`, `ZnepLinkCleaner.bundle`, `ZnepLinkCleaner-v2.0.0-source.zip`), Share Sheet prominence tips, DNS setup instructions for `linkcleaner.znep.com`, and the full prompt-annotated release history.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Retro-futuristic cybersecurity terminal with monospace prompt callouts (`> PROMPT: "..."`), neon cyan/emerald accents, and semantic Red/Orange warning banners.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Clean Break from `CleanLink` Naming**
  - *Chosen Approach*: Delete old `CleanLink*.apk`, `CleanLink.bundle`, and `CleanLink*.zip` files from `download_portal/` and replace all references with **ZnepLinkCleaner** (`com.znep.linkcleaner`).
  - *Why*: Eliminates legacy clutter and aligns 100% with your `znep.com` domain.
- **Decision 2: First-Class `userPrompt` Field in `ReleaseNote` Data Model**
  - *Chosen Approach*: Add `val userPrompt: String` as a required property on `ReleaseNote` in `ReleaseNotes.kt` and assert via Robolectric unit test (`ExampleRobolectricTest.kt`) that `releases.all { it.userPrompt.isNotBlank() }`.
  - *Why*: Making `userPrompt` a required constructor parameter enforced by unit tests guarantees that future code changes cannot compile or pass tests without recording the user prompt!

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:
```
┌───────────────────────────────────────────────────────────────────────────┐
│                Custom Subdomain: https://linkcleaner.znep.com             │
│         [ VIBE-CODED PROOF OF CONCEPT — FULL PROMPT & RELEASE LOG ]       │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│              Public Web Portal & Watchdog (Port 8080 -> 3000)             │
│  • index.html & repo.html (ZnepLinkCleaner v2.0.0 + Prompts Per Release)  │
│  • ZnepLinkCleaner.apk, ZnepLinkCleaner.bundle, diagram.svg               │
│  • DNS Configuration Guide (CNAME & Cloudflare Host Header Rule)          │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │
                                      ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                   ZnepLinkCleaner Android App (v2.0.0)                    │
│                     applicationId: com.znep.linkcleaner                   │
├───────────────────────┬───────────────────────────┬───────────────────────┤
│   Standing Rules &    │    Sanitization Engine    │    Jetpack Compose    │
│   Prompt Changelog    │  • LinkCleaner (exln,     │  • Vibe-Coded PoC     │
│  • ReleaseNotes.kt    │    xmt, shrid, u_code...) │    Banner & Header    │
│    (userPrompt field) │  • UrlExpander            │  • CleanScreen        │
│  • README.md /        │  • FacebookWebViewExpander│  • RulesScreen Prompt │
│    AGENTS.md Rules    │                           │    Release Cards      │
└───────────────────────┴───────────────────────────┴───────────────────────┘
```

- **Interactive Component & State Mapping**:
  - **Enforced Data Model (`ReleaseNotes.kt`)**: Update `ReleaseNote` to require `userPrompt: String`, populate all 11 releases (`v1.0.0` – `v2.0.0`) with their corresponding prompts, and add a `MANDATORY AGENT INSTRUCTION` KDoc block.
  - **Persistence of Instructions (`README.md` & `AGENTS.md`)**: Document the permanent rules (always keep portal public, always include full release history + modifying prompts in app and webpage, always keep Vibe-Coded PoC messaging) so any future session or compaction reads and obeys them.
  - **DNS Setup Delivery**: Provide copy-pasteable DNS records (standard CNAME + Cloudflare Origin Rule / Worker config) so `linkcleaner.znep.com` resolves seamlessly.
