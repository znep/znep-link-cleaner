# ZnepLinkCleaner — Live `ais-dev` Endpoint Alignment & Portal DNS Update

This plan updates all Cloud Run endpoint references and DNS configuration instructions across the **ZnepLinkCleaner** web portal and documentation so they point exclusively to the live, publicly accessible **`ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app`** endpoint instead of the unprovisioned `ais-pre-...` hostname.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Why `ais-dev` Works (`HTTP 200 OK`) and `ais-pre` Returns `404 Not Found`**
> - In Google AI Studio, Android workspaces run inside the **`ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app`** Cloud Run service. Because our Nginx direct static file configuration and Lua auth bypass (`do return end`) run inside this container, **`https://ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app/`** is 100% public and serves the portal (`HTTP/2 200`, 30,948 bytes) without authentication.
> - The `ais-pre-...` hostname is only provisioned for Web applets, so Google Frontend returns `404` for that hostname on Android projects.

- **Confirmed Approach 1 (Endpoint Standardization)**: Replace every `ais-pre-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app` reference in the web portal DNS setup guide (`linkcleaner.znep.com` CNAME, Cloudflare Origin Rule/Worker, and URL redirect target) and project documentation with the live `ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app` endpoint.
- **Confirmed Approach 2 (Standing Rules Compliance)**: Keep `versionCode = 12` / `versionName = "12.0"` (or update the v12.0 release entry with this prompt), preserve all 12 releases (`v1.0.0` – `v12.0`) with their triggering `userPrompt` records, and maintain prominent **"Vibe-Coded Proof of Concept"** messaging across both the Android app and web portal.

---

## 1. Overview & Core Concept

- **What It Does**: Ensures visitors and DNS rules for `https://linkcleaner.znep.com` route to the verified, active Cloud Run container (`https://ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app/`) and explains why `ais-dev` is the authoritative origin for Android projects in AI Studio.
- **Target Audience / Persona**: Project owner (`znep.com`) configuring DNS records and users downloading `ZnepLinkCleaner.apk` or inspecting the Git repository.
- **Key Value**: Eliminates dead `ais-pre` links and guarantees `linkcleaner.znep.com` resolves cleanly to the live portal.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Direct Portal Access**: Opening `https://ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app/` (or `https://linkcleaner.znep.com` once pointed to `ais-dev-...`) immediately loads the dark-mode **ZnepLinkCleaner v12.0 (Build 12)** portal served directly by Nginx.
  2. **Copy-Paste DNS Setup**: The DNS section on the portal provides ready-to-copy Cloudflare CNAME / Worker and standard Registrar Redirect snippets pre-populated with `ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app`, plus a clickable live mirror badge.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Retro-futuristic monospace terminal aesthetic (`#090d16` background, `#121826` cards, `#38bdf8` cyan and `#4ade80` neon green accents, `#a855f7` Vibe-Coded PoC callouts).
  - *Typography & Hierarchy*: Monospace badges and code blocks paired with high-contrast sans-serif headings.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Use `ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app` as the Canonical Origin**
  - *Chosen Approach*: Point all DNS documentation, copyable Worker scripts, and live portal links to `ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app`.
  - *Why*: Verified live via external HTTPS check (`HTTP/2 200 OK`), whereas `ais-pre-...` is not provisioned by Cloud Run for Android containers (`HTTP/2 404`).
- **Decision 2: Record Prompt & Explanation in Release History (`v12.0`)**
  - *Chosen Approach*: Document the `ais-dev` vs. `ais-pre` resolution in the `v12.0` release notes (and `warmup.html` fallback copy) so the explanation is preserved in both the app and web portal.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

```
┌───────────────────────────────────────────────────────────────────────────┐
│                   PUBLIC REQUEST & DNS ROUTING TOPOLOGY                   │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  User Browser ──► https://linkcleaner.znep.com                            │
│                        │                                                  │
│                        ▼ (CNAME / Worker / Redirect)                      │
│        https://ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app│
│                        │                                                  │
│                        ▼                                                  │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ Cloud Run Container (Port 8080 — Nginx Master PID 5)                │  │
│  │  • Lua Auth Bypass: `do return end` (Public HTTP/2 200 OK)          │  │
│  │  • Direct Static Root: `/app/applet/download_portal`                │  │
│  │  • Fallback Daemon: `server.py` (Port 3000) + `warmup.html` mirror  │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
```

- **Interactive Component & State Mapping**:
  - Update the Web Portal (`index.html` and `warmup.html` mirror) DNS cards and live mirror link to `https://ais-dev-27fpm5txvzhwtlauz5vnvv-285289174725.us-east1.run.app`.
  - Update `ReleaseNotes.kt` and `README.md` so all references match `ais-dev-...` and `v12.0`.
  - Rebuild the APK and Git bundle so the downloadable `ZnepLinkCleaner.apk` (`v12.0`) and `ZnepLinkCleaner.bundle` contain the updated documentation.
