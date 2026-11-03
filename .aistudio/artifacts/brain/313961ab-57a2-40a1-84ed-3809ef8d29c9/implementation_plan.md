# ZnepLinkCleaner (`v16.0`) — Untrusted Link Security Hardening Plan

## 1. SSRF, Private IP & Scheme Lockdown in `UrlExpander.kt`
- **Strict Scheme Allowlist**: Reject any redirect hop (`Location` header) or offline-unwrapped target (`l.facebook.com`, `google.com/url`, `out.reddit.com`) that does not use `https://` or `http://` (blocking `intent://`, `javascript:`, `file://`, `content://`, `market://`, and `data:` schemes).
- **Private / Loopback IP & SSRF Protection**:
  - Implement a custom OkHttp `Dns` and host validator (`UrlSecurityValidator`) that resolves hostnames and blocks connections or redirects to loopback (`127.0.0.0/8`, `::1`, `localhost`), RFC 1918 private networks (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), link-local/cloud metadata (`169.254.0.0/16`, `fe80::/10`), multicast, and `.local`/`.internal` hosts.
- **Resource & DoS Guardrails**:
  - Cap input processing length and maximum URLs expanded per operation (max 3 network expansions per shared text) and close response bodies immediately on every `3xx` hop.

## 2. Headless `WebView` Sandboxing in `FacebookWebViewExpander.kt`
- **Hardened `WebSettings`**:
  - Explicitly disable local file and content provider access (`allowFileAccess = false`, `allowContentAccess = false`, `saveFormData = false`) and enforce `mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW`.
- **Zero-Cookie & Ephemeral Storage Isolation**:
  - Disable cookies (`CookieManager.getInstance().setAcceptCookie(false)`) and wipe `view.clearCache(true)` and `WebStorage.getInstance().deleteAllData()` upon completion or cancellation so Meta cannot correlate expansions across sessions.
- **Strict Domain Allowlist in `shouldOverrideUrlLoading` & `shouldInterceptRequest`**:
  - Block any top-level navigation or subresource request that is not `https://` or does not belong to an official Facebook domain (`facebook.com`, `*.facebook.com`, `fb.com`, `fb.me`, `fb.watch`, `*.fbcdn.net`), preventing arbitrary third-party JavaScript execution if a share link redirects off-platform.

## 3. Phishing, Homograph, `user@host`, Cleartext & Executable Download Detection
- **`UrlSecurityValidator.analyzeSecurityRisks(url)`**:
  - Inspect every cleaned/expanded URL for deceptive or dangerous patterns:
    1. **Embedded Credentials (`user:pass@host`)**: Strip or flag URLs using `@` authority spoofing (e.g., `https://google.com@evil.com`).
    2. **IDN Homograph / Punycode (`xn--` or mixed-script hostnames)**: Flag domains using Punycode or non-ASCII confusable characters.
    3. **Unencrypted Cleartext (`http://`) or Raw IP Destination**: Warn when the final destination uses unencrypted HTTP or a raw IP literal instead of a domain name.
    4. **Dangerous Executable / Package Payload (`.apk`, `.exe`, `.msi`, `.scr`, `.jar`, `.sh`, `.bat`)**: Warn when a link points directly to an installable binary or script.
    5. **Blocked SSRF / Unsafe Redirect Target**: Alert the user in red if a shortlink attempted to redirect to a private IP or non-HTTP(S) scheme.
- **UI Warning Banner & Safe Browser Launch**:
  - Render a high-visibility security warning banner in `CleanResultCard.kt` listing any detected URL security advisories.
  - Harden `MainViewModel.openInBrowser()` to require `https`/`http` and `Intent.CATEGORY_BROWSABLE`.

## 4. Offline-Only Expansion Mode (Canary Token / Zero-Click IP Protection)
- **Automatic by Default + Offline-Only Toggle**:
  - Keep automatic shortlink expansion enabled by default, and add an **"Offline-Only Mode (No Network Expansion)"** switch in the **Protection / Rules** tab (`PreferencesManager.kt`, `MainViewModel.kt`, `RulesScreen.kt`).
  - When Offline-Only Mode is enabled, ZnepLinkCleaner strips all query tracking parameters and unwraps offline link shims (`l.facebook.com`, `google.com/url`) with zero network traffic, and shows an **"Offline Mode — Tap to Expand Shortlink"** action button on `CleanResultCard` when the user wants to resolve a shortlink on demand.

## 5. Verification & Release (`v16.0` / Build 16)
- Add unit tests in `ExampleRobolectricTest.kt` covering SSRF/private-IP blocking, scheme smuggling rejection, homograph/`user@host`/`.apk` risk detection, and Offline-Only mode.
- Bump to **`v16.0` (Build 16)**, record the triggering user prompt in `ReleaseNotes.kt`, `README.md`, `CHANGELOG.md`, `download_portal/index.html`, and `download_portal/repo.html`, commit `v16.0` as a separate prompt-annotated Git commit on `main`, and update all web portal artifacts.
