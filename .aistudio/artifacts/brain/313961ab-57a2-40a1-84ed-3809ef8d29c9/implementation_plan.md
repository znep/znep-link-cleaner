# ZnepLinkCleaner — System-Wide Text Selection & Smart Link Menu Expansion

Expand **ZnepLinkCleaner (`Zlc`)** so it surfaces reliably in Android's primary floating text selection bar, rich-text editors, and Smart Link selection menus, while gracefully catching partial word selections (such as bare `"https"` highlights) with an instant explanatory Toast instead of hijacking the screen.

## User Review & Critical Decisions

> [!IMPORTANT]
> All core UX and menu behavior decisions have been confirmed and are ready for implementation upon your approval.

- **Confirmed Decision 1 — Menu Integrations**: Broaden `ACTION_PROCESS_TEXT` to cover all `text/*` MIME types (`text/plain`, `text/html`, `text/uri-list`, `text/*`) with high priority (`100`), and register an `ACTION_VIEW` (`http` / `https`, `CATEGORY_BROWSABLE`) link handler so Android's `TextClassifier` Smart Selection and link-tap menus can route URLs directly to ZnepLinkCleaner.
- **Confirmed Decision 2 — Compact Floating Toolbar Label (`Zlc`)**: Use **`Zlc`** as the dedicated `ACTION_PROCESS_TEXT` action label via an activity alias so Android's horizontal floating toolbar fits the action on the primary bar instead of relegating 15-character `"ZnepLinkCleaner"` to the `⋮` overflow menu.
- **Confirmed Decision 3 — Partial Selection (`"https"`) Guard**: When an editable text field tokenizes a URL at `:` and only sends `"https"` (or any text without a complete URL) to `ACTION_PROCESS_TEXT`, show a helpful Toast explaining how to highlight the full URL (drag handles or tap Select All), return `RESULT_CANCELED` so the text remains untouched, and stay inside the current app without opening the full ZnepLinkCleaner window.

---

## 1. Overview & Core Concept

- **What It Does**: Elevates ZnepLinkCleaner from a buried overflow-menu action into a first-class system link utility across Android text inputs, rich-text editors, and Smart Selection link menus, while eliminating the frustrating failure mode where double-tapping a URL in an input box only sends `"https"`.
- **Target Audience / Persona**: Privacy-conscious Android users cleaning tracking parameters in-place inside messaging apps (Google Messages, Signal, WhatsApp), email clients (Gmail), note apps (Keep, Obsidian), and browsers.
- **Key Value**: Faster one-tap access (`Zlc` fits on the primary floating toolbar), broader app compatibility (`text/*` + `http`/`https` link intent routing), and zero disruptive context switches when Android's word selection stops at `://`.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **In-Place Editable URL Cleaning (`ACTION_PROCESS_TEXT` + Complete URL)**:
     - User selects a full URL inside an editable field (e.g., via *Select All* or dragging selection handles) and taps **`Zlc`** in the floating toolbar.
     - ZnepLinkCleaner strips tracking parameters in-place, replaces the highlighted text directly inside the host app, shows a confirmation Toast (`"Link cleaned in-place (-42B)"` or `"Link already clean (unchanged)"`), and never leaves the host app.
  2. **Partial Selection Guard (`ACTION_PROCESS_TEXT` + `"https"` or Fragment)**:
     - User double-taps a URL in an input box and Android only highlights `"https"`.
     - User taps **`Zlc`**. Instead of opening the ZnepLinkCleaner UI with `"https"`, the host app stays open with its text untouched and displays a clear Toast: *"Only 'https' was selected — drag selection handles across the full URL or tap Select All"*.
  3. **Smart Link Selection & Direct Link Handling (`ACTION_VIEW` for `http`/`https`)**:
     - User highlights a URL in read-only text (triggering Android `TextClassifier` Smart Selection) or selects ZnepLinkCleaner from an "Open link with" prompt.
     - ZnepLinkCleaner opens directly into the Cyberpunk Telemetry Cleaner dashboard, automatically expands any shortlinks/redirects, purges trackers, and presents the verified result card.
- **Visual Identity & Theme**:
  - Preserves the **Cyberpunk Telemetry Cleaner** aesthetic (`Electric Cyan #00F0FF`, `Neon Purple #BC13FE`, `Deep Void #07090E`, and the 5-Mode Randomized Cyberpunk `Z` Glitch & Stunt Engine).
  - Updates the in-app **Rules & System Hooks** documentation card to document the new **`Zlc`** floating toolbar shortcut, partial-selection guidance, and `http`/`https` link handler support, alongside full **v22.0** release notes across the app, GitHub repository, and web portal.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dedicated Activity Alias for `ACTION_PROCESS_TEXT` with Label `"Zlc"`**
  - *Chosen Approach*: Expose `ACTION_PROCESS_TEXT` through an exported `<activity-alias>` labeled **`Zlc`** while keeping the main launcher label as **`ZnepLinkCleaner`**.
  - *Why*: Android's floating text selection toolbar (`ActionMode`) allocates strictly limited pixel width before pushing actions into the `⋮` overflow menu. A 3-character **`Zlc`** label maximizes primary-toolbar visibility without renaming the home screen app title.
  - *Alternatives Considered*: Changing `app_name` globally (rejected because it violates brand identity rules).
- **Decision 2: Non-Disruptive Toast on Partial `"https"` Selections**
  - *Chosen Approach*: Detect scheme-only selections (`http`, `https`, `http://`, `https://`, `fb`, `fb://`) or non-link text during `ACTION_PROCESS_TEXT`, return `Activity.RESULT_CANCELED`, and show a targeted instructional Toast before immediately finishing the activity.
  - *Why*: Previously, when `LinkCleaner.containsLink("https")` returned `false`, the activity fell through to launching the full UI with `"https"` in the input box—forcing the user out of their messaging app with a broken string. Returning `RESULT_CANCELED` with a Toast keeps the user right where they were typing.
- **Decision 3: `ACTION_VIEW` (`http`/`https`) Link Handler Integration**
  - *Chosen Approach*: Register a browsable `http` / `https` `<intent-filter>` and extract `intent.dataString` in `MainActivity`.
  - *Why*: Android's Smart TextClassifier inspects apps registered for `ACTION_VIEW` on `http`/`https` URLs when populating smart link actions in text selection menus and browser long-press sheets.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:
```
┌───────────────────────────────────────────────────────────────────────────┐
│                         Android OS Text & Link Hooks                      │
├─────────────────────────┬─────────────────────────┬───────────────────────┤
│   ACTION_PROCESS_TEXT   │       ACTION_VIEW       │      ACTION_SEND      │
│  Alias Label: "Zlc"     │  Schemes: http / https  │   System Share Sheet  │
│  MIME: text/plain,text/*│  CATEGORY_BROWSABLE     │   Direct Share Target │
└────────────┬────────────┴────────────┬────────────┴───────────┬───────────┘
             │                         │                        │
             ▼                         ▼                        ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      MainActivity Intent Router                           │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ 1. ACTION_PROCESS_TEXT:                                             │  │
│  │    ├─ Is partial scheme ("https", "http://") or no valid link?      │  │
│  │    │   └─► Toast guidance + setResult(RESULT_CANCELED) + finish()   │  │
│  │    ├─ Editable (READONLY == false) + contains valid link:           │  │
│  │    │   └─► Synchronous clean -> setResult(RESULT_OK) + Toast + exit │  │
│  │    └─ Read-Only (READONLY == true) + contains valid link:           │  │
│  │        └─► Open ZnepLinkCleaner UI & run full async clean pipeline  │  │
│  │                                                                     │  │
│  │ 2. ACTION_VIEW (intent.dataString):                                 │  │
│  │    └─► Forward URL to MainViewModel.processInputText() -> Full UI   │  │
│  │                                                                     │  │
│  │ 3. ACTION_SEND (EXTRA_TEXT):                                        │  │
│  │    └─► Forward shared text to MainViewModel.processInputText()      │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────┬────────────────────────────────────┘
                                       │
                                       ▼
┌───────────────────────────────────────────────────────────────────────────┐
│             LinkCleaner & UrlSecurityValidator Pipeline (v22.0)           │
│  • Partial Scheme Detector (isPartialUrlSelection)                        │
│  • Offline Tracker Purge + AMP/Amazon Canonicalization + Hop Inspector    │
│  • SSRF-Guarded Async Shortlink & Sandboxed Meta WebView Expansion        │
└───────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - `LinkCleaner.isPartialUrlSelection(text: String): Boolean`: Identifies bare scheme/prefix fragments (`http`, `https`, `http:`, `https:`, `http://`, `https://`, `fb`, `fb://`, `www`) commonly produced when Android's word boundary tokenizer splits a URL at punctuation.
  - `ReleaseHistory.RELEASES`: Adds **v22.0 (Build 22)** with the triggering user prompt and technical changelog while preserving `v1.0.0` through `v21.0` across the Android app, GitHub repository (`README.md`, `CHANGELOG.md`), and web portal (`index.html`, `repo.html`, `git_commits.json`).
- **Interactive Component & State Mapping**:
  - **`ACTION_PROCESS_TEXT` Handler**: Inspects `EXTRA_PROCESS_TEXT` and `EXTRA_PROCESS_TEXT_READONLY`. When `LinkCleaner.containsLink(text)` is false, checks `LinkCleaner.isPartialUrlSelection(text)` to show a specific `"Only '<selection>' was selected — drag selection handles across the full URL or tap Select All"` Toast (or a general `"No complete URL in selection — highlight the full link first"` Toast), returns `RESULT_CANCELED`, and calls `finish()` without calling `setContent`.
  - **`ACTION_VIEW` Handler**: Reads `intent.dataString`, bounds length via `UrlSecurityValidator.MAX_INPUT_LENGTH`, and invokes `viewModel.processInputText(url, isSharedIntent = true)` so the user immediately sees the cleaned URL and stripped tracker telemetry.
