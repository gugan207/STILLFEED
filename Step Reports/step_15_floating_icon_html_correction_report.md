# STILLFEED — FLOATING ICON: STEP 2 HTML CORRECTION REPORT

**Repository:** `gugan207/STILLFEED`  
**Execution Date:** October 13, 2026  
**Status:** **STEP 2 COMPLETED (HTML Prototype Correction, Safe-Area Alignment & Verification)**  
**Next Milestone:** Awaiting User Review before proceeding to Step 3 (Android Architecture & Overlay Strategy)

---

## 1. Executive Summary & Objective

The objective of **Step 2** is strictly to correct and prepare the floating-icon HTML prototype for future integration into STILLFEED:
1. **HTML-Only Scope:** No Android Kotlin application code was touched. No Android overlay services, WebViews, or rendering libraries were introduced into the Android codebase.
2. **Top-Centre Positioning Fixed:** Eliminated the legacy proportional vertical positioning (`stage.clientHeight * 0.2`) and hardcoded drag bounds (`y0 = 30`). Replaced with deliberate top-centre viewport initialization, dynamic safe-area inset top detection, and responsive bounds clamping.
3. **Existing Visuals & Physics 100% Preserved:** Preserved the pink 3D cartoon brain, glossy eyes, brown pill-shaped counter, extruded orange numbers, glow, dynamic shadow, 5 mood states, spring physics, pointer interactions, and the SVG fallback for non-WebGL devices.
4. **Product Naming Alignment:** Product title and tag reflect `STILLFEED` while preserving full backwards compatibility for `window.SkillfeedIcon` alongside `window.StillfeedIcon`.

---

## 2. Exact Changes to the HTML Prototype

### 2.1 Top-Centre Initialization & Vertical Sizing
- **Previous Implementation:**
  ```javascript
  pos.x = (stage.clientWidth - W) / 2;
  pos.y = Math.max(30, stage.clientHeight * 0.2); // 20% proportional drop down
  clampPos(); place();
  ```
  On a 915px mobile screen or desktop monitor, this dropped the icon 152px–200px down from the top into the upper-middle stage area.
- **Corrected Implementation:**
  ```javascript
  const TOP_SPACING = 20;
  function getSafeTop() {
    const rootPadding = parseFloat(getComputedStyle(document.documentElement).paddingTop) || 0;
    if (rootPadding > 0) return rootPadding;
    const probe = document.createElement('div');
    probe.style.cssText = 'position:fixed;top:0;left:0;height:env(safe-area-inset-top,0px);pointer-events:none;visibility:hidden;';
    document.body.appendChild(probe);
    const h = probe.offsetHeight || 0;
    probe.remove();
    return h;
  }
  function getInitialTop() { return getSafeTop() + TOP_SPACING; }
  function getCenteredX() { return (stage.clientWidth - W) / 2; }
  ```
  The icon is now deliberately positioned at `safeTop + 20px` and horizontally centered at `(stage.clientWidth - 360) / 2`.

### 2.2 Safe-Area Inset & Drag Bounds Clamping
- **Previous Implementation:**
  ```javascript
  function bounds() {
    const bar = $('.bar').offsetHeight;
    return { x0: -40, x1: stage.clientWidth - W + 40, y0: 30, y1: Math.max(30, stage.clientHeight - bar - H + 20) };
  }
  ```
  Hardcoded `y0: 30` allowed users to drag the icon directly underneath mobile notches, dynamic islands, or system status bars when safe-area insets exceeded 30px (e.g. 47px–59px).
- **Corrected Implementation:**
  ```javascript
  function bounds() {
    const bar = $('.bar').offsetHeight;
    const safeTop = getSafeTop();
    const y0 = safeTop + 16;
    const y1 = Math.max(y0, stage.clientHeight - bar - H + 20);
    return {
      x0: -40,
      x1: stage.clientWidth - W + 40,
      y0: y0,
      y1: y1
    };
  }
  ```
  `y0` dynamically accounts for `safeTop + 16px`. Since `.glow` starts at `top: 14px` within the container, the glow and brain can never overlap the browser or system safe area.

### 2.3 Responsive Resize Handling & Default Centering
- **Previous Implementation:**
  `ResizeObserver` only clamped existing `pos.x` and `pos.y`. Resizing the window or changing device orientation broke horizontal centering.
- **Corrected Implementation:**
  Introduced `let userMoved = false;` tracking:
  - If the user has not manually dragged the icon (`!userMoved`), any viewport resize re-calculates `getCenteredX()` and `getInitialTop()`.
  - If the user has manually dragged the icon (`userMoved = true`), resize clamps the user's custom position within the new stage bounds so the icon is never pushed off-screen.
  - Clicking `Reset` or calling `SkillfeedIcon.resetPosition()` resets `userMoved = false` and returns the icon to the top-centre.

### 2.4 Tag Styling & Product Identity
- Updated `.tag` in CSS:
  ```css
  .tag {
    position: absolute;
    left: 20px;
    top: calc(18px + env(safe-area-inset-top, 0px));
    font-size: 12px;
    font-weight: 600;
    letter-spacing: .14em;
    text-transform: uppercase;
    color: var(--muted);
    pointer-events: none;
    z-index: 2;
  }
  ```
  Added `top: calc(18px + env(safe-area-inset-top, 0px))` and `pointer-events: none` so the tag never intercepts touches or clips beneath system insets.
- Product label is `STILLFEED / Floating icon`.
- Added alias: `window.StillfeedIcon = window.SkillfeedIcon`.

---

## 3. Browser Verification & Test Results

A full automated verification test suite was executed using Chrome Headless via the Chrome DevTools Protocol (CDP):

| Test Item | Target / Scenario | Measured Result | Verdict |
|---|---|---|:---:|
| **Initial Position (Mobile)** | 412x915 viewport | `pos.x = 72px` (exact center), `pos.y = 20px` (top spacing) | **PASS** |
| **Responsive Centering (360px)** | 360x800 viewport | `pos.x = 0px` (exact center), `pos.y = 20px` | **PASS** |
| **Responsive Centering (390px)** | 390x800 viewport (iPhone 14) | `pos.x = 15px` (exact center), `pos.y = 20px` | **PASS** |
| **Responsive Centering (480px)** | 480x800 viewport | `pos.x = 60px` (exact center), `pos.y = 20px` | **PASS** |
| **Responsive Centering (768px)** | 768x800 viewport (Tablet) | `pos.x = 204px` (exact center), `pos.y = 20px` | **PASS** |
| **Responsive Centering (1024px)** | 1024x768 viewport (Desktop) | `pos.x = 332px` (exact center), `pos.y = 20px` | **PASS** |
| **Responsive Centering (1440px)** | 1440x800 viewport (Widescreen) | `pos.x = 540px` (exact center), `pos.y = 20px` | **PASS** |
| **Safe-Area Inset Handling** | Simulated 47px `safe-area-inset-top` | `pos.y = 67px` (`47 + 20`), `y0 = 63px` (`47 + 16`) | **PASS** |
| **Drag & Bounds Clamping** | `moveTo(100, -50)` (out of bounds) | Clamped to `pos.y = 16px` (`safeTop + 16`) | **PASS** |
| **Reset Interaction** | `resetPosition()` after drag | Re-centered to `pos.x = 332px`, `pos.y = 20px`, `isMoved = false` | **PASS** |
| **Mood Transitions** | Counts: 0, 7, 8, 15, 23, 30, 35 | Fresh → Fresh → Calm → Tired → Sad → Worn → Worn | **PASS** |
| **SVG Flat Fallback** | Fallback when WebGL is unavailable | SVG pill, brain `#a39ac0`, mouth `M78 94q8 -8 16 0`, count 30 rendered | **PASS** |
| **Prefers Reduced Motion** | `prefers-reduced-motion: reduce` | Instant snap, animations halted, count updates cleanly | **PASS** |
| **Console & JS Errors** | Full inspection via CDP console logs | **0 console errors, 0 exceptions** | **PASS** |

---

## 4. File Locations & Canonical Source

Two synchronized copies of the corrected prototype have been produced and verified:

1. **Repository Canonical Reference:**
   `design-reference/skillfeed-floating-icon.html`  
   *(This is the canonical version tracked in version control for future Android asset staging or reference).*

2. **Local Export Delivery:**
   `C:\Users\gugan\Downloads\Skillfeed Floating Icon - Fixed.html`  
   *(Identical file hash: `0E1DA99681736FF302710DF757C7BCDF39DC2D2437EE94C312F7605C64D0879A`).*

3. **Original Reference Preserved:**
   `C:\Users\gugan\Downloads\Skillfeed Floating Icon.html`  
   *(Untouched and preserved in its original location).*

---

## 5. Remaining Limitations & Considerations for Step 3

1. **External Dependency (CDN Three.js):** The prototype currently loads Three.js r128 via `cdnjs.cloudflare.com`. For Android offline resilience, Three.js should either be bundled locally in `app/src/main/assets/` or evaluated against a native Canvas/Compose implementation.
2. **WebGL Overlay Overhead:** If deployed as an Android `SYSTEM_ALERT_WINDOW` overlay, running a continuous WebGL render loop in a floating WebView consumes GPU and battery resources. The existing `IntersectionObserver` pause logic and `prefers-reduced-motion` will need to be bridged with Android lifecycle events.
3. **Allowance Source of Truth:** Daily allowance (default 30 reels in prototype) and monitored usage count must be bound to `UsageRepository` and `UsageAggregator` via the bridge contract outlined in Step 1.

---

## 6. Next Steps

Step 2 HTML Correction is **complete and verified**.

**STOPPED AS INSTRUCTED.** Step 3 (Android overlay architecture or in-app integration) will not commence until this report is reviewed.
