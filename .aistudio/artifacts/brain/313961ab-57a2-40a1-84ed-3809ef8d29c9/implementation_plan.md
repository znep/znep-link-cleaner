# ZnepLinkCleaner — Cyberpunk `Zlc` Quivering Logo & Synchronized Git/GitHub Release History (`v13.0` / Build 13)

This plan updates the **ZnepLinkCleaner** brand mark across the Android launcher icon, in-app Jetpack Compose interface, and web portal (`linkcleaner.znep.com`) to a cyberpunk **`Zlc`** logo with a quivering **`Z`**, AND establishes a permanent synchronization pipeline ensuring that the linked GitHub repository and the portal's published Git repository/bundle are identical mirrors containing the complete prompts and changes for every release (`v1.0.0` through `v13.0`).

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Design, Git/GitHub Sync & Standing Rules**
> - **Confirmed Decision 1 (Cyberpunk `Zlc` Palette)**: **Electric Cyan (`#38BDF8` / `#00F0FF`) and Neon Purple (`#A855F7` / `#C084FC`)** chromatic-aberration glitch layers set against a deep dark navy (`#090D16` / `#0F172A`) background.
> - **Confirmed Decision 2 (`Z` Quiver Mechanics)**: In the Android Compose UI and Web Portal, the large **`Z`** quivers on **tap**, **automatically whenever a link is cleaned/expanded**, and in **subtle periodic glitch bursts every few seconds**, while the smaller **`lc`** stays steady.
> - **Confirmed Decision 3 (Cross-Platform Logo Scope)**: Updated across the **Android Adaptive & Raster Launcher Icons** (`mdpi` through `xxxhdpi`), the **In-App Top Bar**, and the **Web Download Portal + Repo Browser** (`linkcleaner.znep.com`).
> - **Confirmed Decision 4 (Linked GitHub & Published Git Repo Parity + Persistence)**:
>   1. **Full Prompt & Release History in Root `README.md` & `CHANGELOG.md`**: Because AI Studio's linked GitHub integration syncs the workspace repository directly to GitHub, embedding the complete chronological release notes AND triggering user prompts (`v1.0.0` – `v13.0`) in `README.md` and `CHANGELOG.md` ensures the linked GitHub repository landing page and changelog always display every release's prompt and changes.
>   2. **Annotated Git Commits & Tags (`v1.0.0` – `v13.0`)**: Every release tag in `.git` includes the full release title, triggering user prompt, and change summary, and `git.user` is updated to `ZnepLinkCleaner`.
>   3. **1:1 Parity with Published Portal Repo (`repo.html`, `ZnepLinkCleaner.bundle`, `ZnepLinkCleaner-git.zip`, `ZnepLinkCleaner-v13.0-source.zip`)**: The portal's Git Repository Browser (`repo.html`) and downloadable Git bundle/archives are generated directly from the same workspace Git state, displaying the exact same tags, commits, and triggering user prompts for every release.
>   4. **Permanent Rule Persistence (`AGENTS.md`, `README.md`, `ReleaseNotes.kt`)**: Codifies Rule #5 in `AGENTS.md` (and `ReleaseNotes.kt` / `README.md`) requiring every future release to keep the linked GitHub repo (`README.md`, `CHANGELOG.md`, Git commits/tags, and any configured remote) and the published web portal repo (`repo.html`, `.bundle`, `.zip`) in 100% lockstep with all prompts and changes.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. Replaces the chain-link icon with a custom cyberpunk **`Zlc`** monogram (`Z` for Znep + `lc` for LinkCleaner) where the **`Z`** quivers slightly.
  2. Synchronizes the workspace Git repository (synced to the linked GitHub repo) and the web portal's published repository artifacts so both contain the exact same codebase, commit/tag history, and complete prompt-annotated release notes (`v1.0.0` – `v13.0`).
- **Target Audience / Persona**: Privacy-conscious Android users and developers inspecting the vibe-coded proof of concept on GitHub or `linkcleaner.znep.com`.
- **Key Value**: Combines a distinctive cyberpunk brand identity with complete, verifiable prompt-to-commit transparency across the Android app, linked GitHub repository, and web portal.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Home Screen & Share Sheet (`ic_launcher`)**: High-contrast adaptive icon on a deep navy background (`#090D16`) with a sharp diagonal Electric Cyan **`Z`** (with an offset Neon Purple chromatic ghost layer) and a crisp **`lc`** tucked into the lower-right quadrant.
  2. **In-App Top Bar (`ZlcLogo` Composable)**:
     - Every ~4 seconds, the large **`Z`** executes a brief 180ms cyberpunk micro-quiver (rapid ±1.5dp horizontal/vertical jitter + purple/cyan ghost split) before snapping back into alignment.
     - Tapping the logo or cleaning/expanding any link immediately triggers a quiver burst on the **`Z`**.
  3. **Web Portal (`index.html`) & Git Repository Browser (`repo.html`)**:
     - Both pages feature the animated SVG **`Zlc`** emblem (and SVG favicon) where the **`Z`** quivers periodically and on click.
     - `repo.html` displays the full commit and tag log (`v1.0.0` – `v13.0`) including the exact triggering **`> USER PROMPT //`** and change highlights for each release, matching the linked GitHub repository's `README.md` and `CHANGELOG.md`.
- **Visual Identity & Theme**:
  - *Primary Foreground `Z`*: Electric Cyan (`#38BDF8` with `#E0F2FE` highlight)
  - *Quiver / Glitch Ghost `Z`*: Neon Purple (`#A855F7`) offset by `(-2dp, +1dp)`
  - *Subscript `lc`*: Crisp Ice Cyan / White (`#F1F5F9` / `#7DD3FC`)
  - *Background*: Deep Cyber Navy (`#090D16` to `#131C31`)

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Single Source of Truth for GitHub & Portal Git Artifacts**
  - *Chosen Approach*: Keep `ReleaseNotes.kt`, `README.md`, `CHANGELOG.md`, `download_portal/index.html`, `download_portal/repo.html`, annotated Git tags, and `ZnepLinkCleaner.bundle` / `ZnepLinkCleaner-git.zip` strictly synchronized on every release, and enforce this in both `AGENTS.md` and Robolectric unit tests (`ExampleRobolectricTest.kt`).
  - *Why*: Ensures that whether someone views the linked GitHub repository, clones `ZnepLinkCleaner.bundle`, browses `linkcleaner.znep.com/repo.html`, or opens the Android app's Protection tab, they see the exact same repository state, release history, and triggering prompts.
- **Decision 2: Isolate Quiver Motion to the `Z` Glyph**
  - *Chosen Approach*: Animate only the large `Z` (and its Neon Purple chromatic ghost layer) while keeping `lc` stationary.
  - *Why*: Directly fulfills "make the Z quiver slightly" and gives the `Z` a crisp, high-tech reference anchor against the stationary `lc`.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌────────────────────────────────────────────────────────────────────────────┐
│            Zlc LOGO & SYNCHRONIZED GIT / GITHUB RELEASE PIPELINE           │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 1. Cyberpunk 'Zlc' Quivering Logo                                    │  │
│  │    • Large Electric Cyan 'Z' + Neon Purple Glitch Ghost (Quivers)    │  │
│  │    • Smaller steady 'lc' subscript in bottom-right                   │  │
│  │    • Deployed to: Android Adaptive/PNG Icons, Compose UI, Web SVG    │  │
│  └──────────────────────────────────┬───────────────────────────────────┘  │
│                                     │                                      │
│                                     ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 2. Prompt & Release Single Source of Truth (v1.0.0 – v13.0)          │  │
│  │    • In-App: ReleaseNotes.kt (verified by Robolectric unit test)     │  │
│  │    • Linked GitHub Repo: README.md + CHANGELOG.md + Annotated Tags   │  │
│  │    • Web Portal: index.html + repo.html (with prompts per commit)    │  │
│  │    • Published Mirrors: ZnepLinkCleaner.bundle + -git.zip + -src.zip │  │
│  │    • Permanent Standing Rule #5 persisted in AGENTS.md               │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

- **Interactive Component & State Mapping**:
  - **In-App `ZlcLogo` Composable**:
    - Observes `cleanResult` and `isExpanding` to trigger a quiver burst whenever a URL is cleaned, plus a periodic 4s loop and tap handler.
  - **Repository & GitHub Sync Verification**:
    - Updates `AGENTS.md` with Standing Rule #5 (Linked GitHub & Published Repo Parity with Prompts/Changes).
    - Updates `README.md` and creates `CHANGELOG.md` with the full `v1.0.0` – `v13.0` release notes and triggering prompts.
    - Updates `download_portal/repo.html` to include the new `Zlc` quivering logo and all 13 releases (`v1.0.0` – `v13.0`) with their prompts and changes.
    - Adds a Robolectric test assertion verifying that `README.md`, `CHANGELOG.md`, `download_portal/index.html`, and `download_portal/repo.html` all stay in sync with `ReleaseHistory.RELEASES`.
