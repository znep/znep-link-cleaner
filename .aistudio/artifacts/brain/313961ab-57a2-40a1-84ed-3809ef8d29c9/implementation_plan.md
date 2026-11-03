# ZnepLinkCleaner v18.0 — Unchanged Link Indicator, Randomized Cyberpunk `Z` Glitch Engine, Zero-Bloat OS Integration, Canonical URL Normalization & Redirect Hop Trace

Deliver five high-leverage, zero-bloat improvements: (1) an unmistakable **"Unchanged / Already Clean"** status indicator so users never have to visually diff two identical URLs, (2) a **Randomized Cyberpunk `Z` Glitch & Stunt Engine** that makes the `Z` in `Zlc` jiggle more vigorously and perform random visual effects, (3) in-place Android text selection replacement (`ACTION_PROCESS_TEXT` write-back), (4) canonical URL normalization (Google AMP unwrapping and minimal Amazon `/dp/<ASIN>` path condensation), and (5) a collapsible redirect hop chain inspector.

## User Review & Critical Decisions

> [!IMPORTANT]
> The plan has been updated to include the **Randomized Cyberpunk `Z` Glitch & Stunt Engine** alongside the **Unchanged / Already Clean** status indicator and the three core technical enhancements. Approve the plan when ready to build.

- **Confirmed Addition 1 — Randomized Cyberpunk `Z` Glitch & Stunt Engine (`ZlcLogo.kt` + Web Portal)**: Upgrades the `Z` in the `Zlc` monogram from a mild uniform quiver to a dynamic multi-mode cyberpunk animation engine that triggers at randomized intervals (`1.4s–3.4s`), on link cleaning, and immediately on tap—while keeping the `lc` subscript steady:
  - *Seismic Tri-Chromatic Jiggle*: Vigorous multi-axis jitter (`±3.8dp`) splitting the `Z` into Electric Cyan (`#38BDF8`), Neon Purple (`#A855F7`), and Hot Magenta (`#EC4899`) chromatic ghosts.
  - *Horizontal Scanline Slice Tear*: Slices the `Z` into upper, middle, and lower horizontal bands using `clipRect`, shearing the middle diagonal sideways while a bright laser scanline sweeps vertically down the badge.
  - *Kinetic Snap-Flip & Skew*: Rapidly flips/skews the `Z` horizontally or rotates it with an electric afterimage before snapping back to center.
  - *High-Voltage Neon Overdrive & Sparks*: Strobes the `Z` between hyper-white, cyan, and neon purple while rendering miniature geometric spark particles off the `Z` vertices.
- **Confirmed Addition 2 — Explicit "Unchanged / Already Clean" Indicator**: Detects `result.isUnchanged` and displays a distinct `[ ✓ UNCHANGED // ALREADY CLEAN ]` badge, a `0 B CHANGED` pill, and `"VERIFIED CLEAN LINK // IDENTICAL TO INPUT"` so the user never has to visually compare two identical URLs.
- **Feature 3 — In-Place Text Selection Write-Back (`ACTION_PROCESS_TEXT`)**: Highlighting a URL inside an editable text field (Signal, WhatsApp, Gmail, Notes) and tapping **ZnepLinkCleaner** in Android's floating text toolbar sanitizes and replaces the highlighted selection *in-place* via `EXTRA_PROCESS_TEXT` without switching apps.
- **Feature 4 — Canonical URL Normalization (AMP & Amazon `/dp/ASIN`)**: Unwraps Google AMP cache URLs (`*.cdn.ampproject.org/v/s/...` and `/amp/` suffixes) back to the canonical publisher URL, and condenses verbose Amazon product URLs (`/Long-Product-Slug/dp/B08N5WRWNW/ref=...`) down to `/dp/<ASIN>`.
- **Feature 5 — Collapsible Redirect Hop Chain Inspector**: Records each step during offline shim unwrapping and HTTP `301`/`302` expansion, displaying a single compact badge (`N HOPS`) only when a link was redirected, which expands on tap to reveal the full intermediate hop chain.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. **Unmistakable Unchanged-Link State**: Clearly distinguishes modified/purged links (`[ CLEANED ]` + `-X%` reduction) from links that required zero modification (`[ ✓ UNCHANGED // ALREADY CLEAN ]` + `"IDENTICAL TO INPUT"`), eliminating manual character-by-character comparison.
  2. **Randomized Cyberpunk `Z` Stunt Engine**: Gives the `Zlc` brand mark an unmistakable, living cyberpunk personality with randomized jitter bursts, scanline slice tears, chromatic splits, snap-flips, and spark discharges in both the Android app (`ZlcLogo.kt`) and the web portal (`index.html`, `repo.html`).
  3. **In-Place `ACTION_PROCESS_TEXT` Replacement**: Turns ZnepLinkCleaner into an invisible inline sanitizer inside any Android messaging or notes app whenever text is selected in an editable input field.
  4. **Canonical URL Simplifier**: Strips structural URL bloat beyond query parameters—specifically Google AMP CDN wrappers, AMP path suffixes, and verbose Amazon SEO product slugs (`/Title-Slug/dp/ASIN` and `/gp/product/ASIN`).
  5. **Collapsible Redirect Hop Trace**: Captures the exact sequence of intermediate URLs and HTTP status codes traversed during link unwrapping and shortlink expansion, presenting it behind a zero-clutter collapsible section on the result card.
- **Target Audience / Persona**: Privacy-conscious Android users who want immediate visual clarity on whether a link was modified, expressive cyberpunk polish, one-tap inline cleaning inside messaging apps, and full transparency into redirect chains.
- **Key Value**: Saves cognitive effort when checking already-clean links, makes the `Zlc` logo fun and reactive, eliminates app-switching friction when composing messages, shortens Amazon and AMP links to their cleanest canonical form, and exposes hidden multi-hop tracker networks without cluttering the UI.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Instant "Unchanged / Already Clean" Recognition**:
     - User pastes or shares a URL that has no trackers, redirect shims, or canonical bloat (e.g., `https://znep.com/projects`).
     - Instead of saying `[ CLEANED ]` and `PURGED LINK //`, the result card header immediately displays `[ ✓ UNCHANGED // ALREADY CLEAN ]` (or `[ UNCHANGED // SEE ADVISORY ]` if an expansion/security warning applies) alongside a `0 B CHANGED` pill.
     - The URL box header reads `"VERIFIED CLEAN LINK // IDENTICAL TO INPUT"` with a concise note: `"Link is already clean — no tracking parameters, redirect wrappers, or path bloat found."`
  2. **Living Cyberpunk `Zlc` Logo**:
     - While viewing the app or web portal, the `Z` in `Zlc` randomly performs one of five visual stunts every `1.4s–3.4s` (and immediately whenever a link is cleaned or the user taps the logo): vigorous tri-color RGB jiggle, horizontal CRT scanline slice tear, horizontal snap-flip, neon voltage strobe with corner sparks, or quantum double-ghost blink.
  3. **Inline Editable Text Cleaning (Zero UI Switch)**:
     - User highlights a messy link inside a chat composer (e.g., Signal or Gmail) and taps **ZnepLinkCleaner** in the Android floating text selection menu.
     - ZnepLinkCleaner detects `EXTRA_PROCESS_TEXT_READONLY == false`, runs synchronous link sanitization + canonicalization + offline shim unwrapping, returns the cleaned string via `RESULT_OK`, and finishes immediately so the highlighted text is replaced in-place inside the host app.
  4. **Canonical AMP & Amazon Cleaning**:
     - User shares or pastes `https://www-theverge-com.cdn.ampproject.org/v/s/www.theverge.com/2026/11/02/tech-news/amp?utm_source=twitter` or `https://www.amazon.com/Sony-WH-1000XM5-Canceling-Headphones-Hands-Free/dp/B09XS7JWHH/ref=sr_1_3?crid=123&qid=456`.
     - Result card outputs the crisp canonical URL (`https://www.theverge.com/2026/11/02/tech-news` or `https://www.amazon.com/dp/B09XS7JWHH`) and records a `"Canonical Simplification"` entry in the stripped/normalized breakdown.
  5. **Collapsible Redirect Hop Inspector**:
     - When a shortlink (`vt.tiktok.com`, `amzn.to`, `bit.ly`, `l.facebook.com`) is unwrapped or expanded, the collapsible telemetry breakdown shows the step-by-step hop timeline (`1. Original` -> `2. HTTP 301` -> `3. Final`) with each intermediate hostname and URL.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Retains ZnepLinkCleaner's cyberpunk-inspired Material 3 dark/dynamic palette (`Electric Cyan #38BDF8`, `Neon Purple #A855F7`, `Hot Magenta #EC4899`, `Emerald Verification #10B981` for unchanged clean state) with strict restraint against layout bloat.
  - *Component Styling & Layout*:
    - Zero new tabs, dialogs, or settings switches required.
    - All `Zlc` visual stunts are self-contained inside the `32.dp` / `34.dp` `ZlcLogo` Canvas and matching SVG/CSS animation engine on the web portal, so layout bounds never shift.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Self-Contained Canvas Stunts for `ZlcLogo`**
  - *Chosen Approach*: Implement all five random `Z` glitch modes inside `ZlcLogo`'s `Canvas` (using `clipRect` for scanline slice shearing, `withTransform` for snap-flips/rotations, and dual chromatic ghost paths) while keeping the outer `Box` dimensions and `lc` subscript fixed.
  - *Why*: Delivers dramatic, varied cyberpunk motion without shifting surrounding TopAppBar text or hurting layout performance.
- **Decision 2: Explicit `isUnchanged` State vs. Generic `[ CLEANED ]` Badge**
  - *Chosen Approach*: Compute `val isUnchanged: Boolean` directly on `CleanResult` (`originalText == cleanedText && originalUrl == cleanedUrl && strippedParams.isEmpty() && expandedFromUrl == null`) and switch the top HUD badge (`[ ✓ UNCHANGED // ALREADY CLEAN ]`), the delta badge (`0 B CHANGED`), and the URL box header (`VERIFIED CLEAN LINK // IDENTICAL TO INPUT`).
  - *Why*: Users should never have to visually scan two long URLs character-by-character to figure out whether ZnepLinkCleaner modified the link.
- **Decision 3: Synchronous Cleaning for In-Place `ACTION_PROCESS_TEXT` Write-Back**
  - *Chosen Approach*: When `EXTRA_PROCESS_TEXT_READONLY == false`, execute instant synchronous sanitization (`LinkCleaner.cleanText`, which includes offline redirect shim unwrapping, canonical AMP/Amazon normalization, and parameter stripping) and return `RESULT_OK` immediately without launching the Compose UI.
  - *Why*: Android text selection replacements must feel instantaneous (<50ms) so the user's keyboard and cursor inside the host app never flicker or lose focus.
- **Decision 4: Always-On Canonical AMP & Amazon `/dp/ASIN` Normalization**
  - *Chosen Approach*: Build AMP cache unwrapping and Amazon `/dp/<ASIN>` path condensation directly into the core `UrlExpander` / `LinkCleaner` pipeline and report the normalization in `strippedParams` (`category = "Canonical Simplification"`).
  - *Why*: Users never want Google AMP CDN wrappers or 80-character SEO keyword slugs in shared Amazon links, and recording the simplification inside `strippedParams` ensures `isUnchanged` accurately becomes `false` whenever a URL path is canonicalized.
- **Decision 5: Progressive Disclosure for Redirect Hop Chains**
  - *Chosen Approach*: Store the ordered `List<RedirectHop>` on `CleanResult` and render it inside the collapsible telemetry section on `CleanResultCard`.
  - *Why*: Keeps the result card compact by default while giving technical users full visibility into multi-hop redirects on a single tap.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         Android OS Intent Entry                           │
│  ┌───────────────────────────────────┐ ┌────────────────────────────────┐ │
│  │ ACTION_PROCESS_TEXT (Editable)    │ │ ACTION_SEND / Read-Only / App  │ │
│  │ (EXTRA_PROCESS_TEXT_READONLY=false│ │ Launch                         │ │
│  └─────────────────┬─────────────────┘ └────────────────┬───────────────┘ │
└────────────────────┼────────────────────────────────────┼─────────────────┘
                     │ Instant Sync Write-Back            │ Async Pipeline
                     ▼                                    ▼
┌────────────────────────────────────┐   ┌──────────────────────────────────┐
│ LinkCleaner.cleanText()            │   │ MainViewModel.processInputText() │
│  • Offline Shim Unwrap             │   │  • LinkCleaner.cleanTextAsync()  │
│  • AMP Cache Unwrap                │   │  • UrlExpander (HTTP 301/302 +   │
│  • Amazon /dp/<ASIN> Canonicalizer │   │    SafePublicDns + Hop Recorder) │
│  • Tracker Query Purging           │   │  • Canonical Path Simplifier     │
└─────────────────┬──────────────────┘   └────────────────┬─────────────────┘
                  │                                       │
                  ▼                                       ▼
┌────────────────────────────────────┐   ┌──────────────────────────────────┐
│ setResult(RESULT_OK, EXTRA_TEXT)   │   │ MainScreen & CleanResultCard     │
│ + finish() (Zero UI Switch)        │   │  • ZlcLogo (5 Random Glitch      │
└────────────────────────────────────┘   │    Stunts: Jiggle, Slice, Flip,  │
                                         │    Voltage Sparks, Quantum Blink)│
                                         │  • [ CLEANED ] vs                │
                                         │    [ ✓ UNCHANGED // ALREADY CLEAN│
                                         │  • Collapsible Hop Trace List    │
                                         └──────────────────────────────────┘
```

- **Data Model & State**:
  - `RedirectHop`:
    - `step: Int` (1-indexed hop order)
    - `url: String` (the intermediate or final URL at this step)
    - `host: String` (extracted lowercase hostname)
    - `mechanism: String` (e.g., `"Original"`, `"Offline Shim"`, `"AMP Unwrap"`, `"HTTP 301"`, `"HTTP 302"`, `"WebView Permalink"`, `"Final"`)
  - `CleanResult`:
    - Add `val redirectHops: List<RedirectHop> = emptyList()`
    - Add computed property `val isUnchanged: Boolean get() = originalText == cleanedText && originalUrl == cleanedUrl && strippedParams.isEmpty() && expandedFromUrl == null`
- **Interactive Component & State Mapping**:
  - **`ZlcLogo` Randomized Stunt Engine**:
    - Defines `enum class ZGlitchMode { SEISMIC_JIGGLE, SCANLINE_SLICE, CYBER_FLIP, VOLTAGE_SURGE, QUANTUM_BLINK }`.
    - Runs a coroutine loop with randomized delays (`Random.nextLong(1400L, 3400L)`) and immediate trigger on `triggerKey` or user tap, picking a random `ZGlitchMode` and animating frame-by-frame offsets, horizontal slice shear (`sliceOffset`), scale/rotation (`zScaleX`, `zRotation`), scanline Y position (`scanlineY`), and spark alpha (`sparkAlpha`).
    - Mirrors the randomized multi-mode `Z` glitch behavior in `download_portal/index.html` and `download_portal/repo.html` via a lightweight JS interval + click listener on the `.zlc-logo-badge` SVG.
  - **Unchanged Link UI State (`CleanResultCard`)**:
    - When `result.isUnchanged` is `true` and `result.warningType == ExpansionWarningType.NONE`:
      - Top badge displays `[ ✓ UNCHANGED // ALREADY CLEAN ]` (`testTag = "status_badge_unchanged"`) in emerald/teal styling.
      - Right delta badge displays `0 B CHANGED`.
      - URL box header displays `"VERIFIED CLEAN LINK // IDENTICAL TO INPUT"` and a concise note `"Link is already clean — no tracking parameters, redirect wrappers, or path bloat found."`
    - When `result.isUnchanged` is `false`:
      - Top badge displays `[ CLEANED ]` (`testTag = "status_badge_cleaned"`), right badge shows `-${result.percentReduction}%`, and URL box header displays `"PURGED LINK //"`.
  - **Editable `ACTION_PROCESS_TEXT` Handler**: Reads `Intent.EXTRA_PROCESS_TEXT` and `Intent.EXTRA_PROCESS_TEXT_READONLY`. When `readOnly == false` and `LinkCleaner.containsLink(text)` is true, computes `val result = LinkCleaner.cleanText(text, ...)`, sets `Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, result.cleanedText)` with `setResult(Activity.RESULT_OK, ...)`, and calls `finish()` before `setContent` renders.
  - **Canonical AMP & Amazon Normalizer**:
    - AMP Cache: Unwraps `https://<subdomain>.cdn.ampproject.org/v/s/<target>` and `https://www.google.<tld>/amp/s/<target>`, strips trailing `/amp` or `/amp/` path segments and `amp=1` / `outputType=amp` query parameters, and records a `StrippedParam(key = "amp_wrapper", ...)` entry.
    - Amazon ASIN: Condenses `/.../dp/<ASIN>/...` and `/gp/product/<ASIN>/...` on recognized Amazon domains to `/dp/<ASIN>` and records a `StrippedParam(key = "amazon_path_slug", ...)` entry when path segments were removed.
  - **Redirect Hop Trace UI**: Inside `CleanResultCard`, when `result.redirectHops.size > 1`, displays the hop count pill (`"${result.redirectHops.size - 1} HOPS"`) and renders the ordered hop timeline inside the collapsible telemetry details section.
