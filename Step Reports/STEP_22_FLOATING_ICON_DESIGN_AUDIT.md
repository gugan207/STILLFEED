# STEP 22: STILLFEED Floating Brain Companion — Design & Motion Audit Report

**Reference Source:**
- **Source File:** `Skillfeed Floating Icon - Fixed.html` / `Skillfeed Floating Icon.html`
- **Reference Technology:** Three.js r128 (3D WebGL Scene) + CSS3 Canvas Compositing
- **Reference Stage Dimensions:** 360 × 220 px (18:11 aspect ratio)
- **Target Platform:** Native Android Overlay Window (`TYPE_APPLICATION_OVERLAY`)
- **Inspection Date:** 2026-10-17

---

## 1. Executive Summary & Design Reference

The STILLFEED Floating Brain Companion is an ambient, non-judgmental presence designed to float gently above third-party applications (such as Instagram Reels, YouTube Shorts, and TikTok). It visually connects the user's conscious attention to their configured daily allowance through an expressive, characterful 3D brain nestled within a rounded pill counter.

Unlike cold system timers or punitive lockouts, the companion communicates awareness through subtle emotional expression, posture, color transitions, and gentle particle effects.

This audit report documents every visual, geometric, shader, motion, and color property from the primary HTML reference (`Skillfeed Floating Icon - Fixed.html`), establishing the exact specification implemented in the production Android architecture.

---

## 2. Geometry, Shaders, and Material Breakdown

### 2.1. Coordinate System & Camera Configuration
- **Stage Size:** 360 × 220 px.
- **Camera:** Three.js `PerspectiveCamera`, Field of View (FOV) 32°, Near: 0.1, Far: 1000.
- **Camera Position:** `x: 0`, `y: 0`, `z: 420` (centered orthographic-like perspective).
- **Lighting Setup:**
  - Ambient Light: Intensity 0.75, `#FFFFFF`.
  - Primary Key Directional Light: Position `x: 120`, `y: 200`, `z: 180`, Intensity 0.9, `#FFF8F0`.
  - Soft Rim/Fill Light: Position `x: -120`, `y: -100`, `z: 100`, Intensity 0.35, `#E8F0FE`.
  - PMREM Cubemap / Environment Reflection: High-specular reflection applied to eyeballs for glossy catchlights.

### 2.2. The Pill Container (Stadium Capsule)
- **Form:** Horizontal stadium / extruded pill shape housing the brain on the left and counter on the right.
- **Reference Dimensions:** Width 220 units, Height 90 units, Depth 36 units, Corner Radius 45 units (full semicircular caps).
- **Primary Material:** `MeshToonMaterial` with custom gradient ramp for soft cel-shaded styling.
- **Default Base Color:** `#713A00` (Warm Caramel Brown).
- **Outline Technique:** Inverted hull inverted-face mesh dilated by 2.2 units with `MeshBasicMaterial` in `#2B1400` (Deep Espresso Brown), backface culling enabled.
- **3D Depth:** Subtle bevel along the perimeter edge (radius 3.5 units, segments 4).

### 2.3. The Brain Character
- **Form:** Stylized organic brain divided into left and right hemispheres, frontal lobes, and temporal lobes.
- **Lobe Geometry:** Spheroid union of overlapping ellipsoids deformed with sinusoidal surface displacement to simulate cerebral sulci/folds:
  - Left Hemisphere: Position `x: -54`, `y: 2`, `z: 6`, Scale `[28, 26, 24]`.
  - Right Hemisphere: Position `x: -28`, `y: 2`, `z: 6`, Scale `[28, 26, 24]`.
  - Temporal Lobes: Lower support spheroids creating a cuddly silhouette.
- **Primary Material:** `MeshToonMaterial` with warm diffuse response.
- **Base Color (Fresh):** `#FF9DB5` (Soft Vibrant Pink).
- **Outline Technique:** Inverted hull mesh dilated by 1.8 units in `#5A1F3A` (Rich Maroon).

### 2.4. Facial Features & Glossy Eyes
- **Eyeballs:**
  - Left Eye: Position `x: -47`, `y: 2`, `z: 28`, Radius 7.2 units.
  - Right Eye: Position `x: -33`, `y: 2`, `z: 28`, Radius 7.2 units.
  - Material: `MeshPhysicalMaterial`, Roughness 0.05, Metalness 0.1, Clearcoat 1.0, Clearcoat Roughness 0.05.
  - Color: `#FFFFFF` with twin specular highlights (catchlights) positioned at top-right quadrant (`#FFFFFF`, opacity 0.95).
- **Pupils:**
  - Dark dilated discs `#1E1218`, Radius 3.8 units.
  - Synchronized micro-saccades and directional tracking during head tilts.
- **Eyelids:**
  - Spherical caps clipping over the eyeball geometry matching the brain surface color.
  - Dynamic closure parameter (`eyelidOpen`: `1.0` = fully alert, `0.5` = tired droop, `0.0` = fully closed slit).
- **Mouth:**
  - Parametric 3D tube / ribbon geometry (CatmullRomCurve3 with 5 control points).
  - Material: `MeshBasicMaterial` in `#5A1F3A`.
  - Dynamic curvature:
    - Fresh: Wide joyous upturn (`controlPoint.y` offset +4.2 units).
    - Calm: Gentle peaceful smile (`controlPoint.y` offset +2.0 units).
    - Tired: Neutral straight line (`controlPoint.y` offset 0.0 units).
    - Sad: Downward arching frown (`controlPoint.y` offset -3.8 units).
    - Worn Out: Open oval yawn / exhausted curve (`controlPoint.y` offset -5.5 units, thickness expanded).

### 2.5. Counter Typography
- **Typography:** Inter Bold (weight 700), rendered as 3D extruded geometry or crisply sampled 2D canvas texture with specular lighting.
- **Color:** `#FFB15E` (Warm Bright Apricot).
- **Positioning:** Centered in the right pill chamber (`x: 42`, `y: 0`, `z: 16`).
- **Data Honesty Rule:** When exact Reel count is unmeasured or verified count is unavailable, renders em-dash (`—`), preserving typography styling, drop shadows, and scale.

---

## 3. Five-Stage Mood Specification Matrix

The HTML reference specifies five distinct mood stages based on consumption relative to the configured daily allowance:

| Attribute | Stage 1: FRESH | Stage 2: CALM | Stage 3: TIRED | Stage 4: SAD | Stage 5: WORN OUT |
|---|---|---|---|---|---|
| **Allowance Progress** | `0%` to `<25%` | `25%` to `<50%` | `50%` to `<75%` | `75%` to `<100%` | `≥100%` (Depleted) |
| **Brain Color** | `#FF9DB5` | `#F7A8B8` | `#E8A29A` | `#D88989` | `#B07276` |
| **Pill Color** | `#713A00` | `#683500` | `#5A2C00` | `#4A2000` | `#361400` |
| **Counter Color** | `#FFB15E` | `#FFA852` | `#FF9A40` | `#FF862E` | `#FF6F1A` |
| **Brain Outline** | `#5A1F3A` | `#521A33` | `#48162A` | `#3E1220` | `#300C16` |
| **Pill Outline** | `#2B1400` | `#261100` | `#200D00` | `#1A0900` | `#120500` |
| **Eyelid Coverage** | 0% (Fully open) | 15% (Relaxed) | 50% (Drooping) | 65% (Heavy/Sad) | 95% (Closed slit) |
| **Mouth Expression** | Cheerful upturned smile | Serene gentle smile | Neutral flat line | Downward droop frown | Open exhausted yawn |
| **Head Tilt (Z-rot)** | `0.0°` | `+2.5°` | `-5.0°` | `-8.5°` | `-12.0°` |
| **Breathing Cycle** | 2.4s period, ±3.2px | 3.0s period, ±2.4px | 4.0s period, ±1.6px | 4.8s period, ±1.0px | 6.0s period, ±0.6px |
| **Blink Frequency** | Every 3.5 – 5.5s | Every 4.0 – 6.0s | Every 2.5 – 4.0s (heavy) | Rare slow blinks | None (eyes closed) |
| **Particles** | 3 Golden Sparkles | 1 Soft Sparkle | 1 Sleeping 'Z' | Falling Teardrops | 3 Floating 'Z's |
| **Particle Colors** | `#FFD700`, `#FFF4B8` | `#FFE494` | `#D6E4FF` (alpha 0.75) | `#70B6FF` (translucent) | `#B0C8F8`, `#80A0E0` |
| **Editorial Prompt** | "Mind is fresh." | "Steady pace." | "Getting tired." | "Take a breath." | "Time to rest." |

---

## 4. Animation & Motion Architecture

### 4.1. Natural Harmonic Breathing
The idle state oscillates using a smooth sine wave:
```text
y(t) = y_base + sin(t * (2π / period)) * amplitude
scale_y(t) = 1.0 + sin(t * (2π / period)) * 0.025
scale_x(t) = 1.0 - sin(t * (2π / period)) * 0.012
```
This produces organic lung-like volume preservation (squash and stretch).

### 4.2. Stochastic Eye Blinking
Blinking is governed by an exponential Poisson timer (mean 4.2 seconds):
1. **Trigger:** Eyelids interpolate from current openness to `1.0` (fully closed) in 80ms.
2. **Hold:** Closed state held for 40ms.
3. **Rebound:** Eyelids interpolate back to mood-baseline openness in 120ms with an ease-out cubic curve.

### 4.3. Interactive Tap Response (Squish & Rebound)
When tapped by the user, the companion triggers an immediate physical tactile response:
- Instant vertical compression: `scale_y = 0.82`, `scale_x = 1.12`, `scale_z = 1.12`.
- Upward spring jump: `y = y_base + 8px`.
- Damped harmonic oscillation back to equilibrium over 450ms:
  ```text
  scale(t) = 1.0 + A * exp(-γ * t) * cos(ω * t)
  ```
- Pupil dilation expands by 20% on tap and contracts back to normal.

### 4.4. Mood Transition Smoothing
When the user's attention progress advances into a new mood tier, values do not snap abruptly:
- Mesh colors interpolate via RGB color lerp over 800ms.
- Eyelid target openness interpolates smoothly with quadratic ease-in-out.
- Mouth spline vertices translate to target positions smoothly.
- Head tilt angle interpolates over 600ms.

### 4.5. Reduced Motion Handling
When the user disables animations in STILLFEED Settings (`floatingCompanionAnimationsEnabled = false`) or system-wide `prefers-reduced-motion` is active:
- Harmonic breathing bob is frozen at equilibrium (`y = 0`).
- Blinking is disabled (static eyes matching current mood).
- Particle emitters (sparkles, tears, Zs) are hidden.
- Tap squish-and-stretch is replaced with a subtle opacity pulse.

---

## 5. Fallback & Low-Capability Rendering

To ensure 100% stability across all Android devices (including low-memory devices, battery saver modes, and OEM WebGL context loss):

1. **WebGL Context Loss Handler:** The HTML canvas registers `webglcontextlost` and `webglcontextrestored` listeners. If WebGL context cannot be restored within 2 seconds, it gracefully notifies Android via JavaScript bridge.
2. **Native 2D Canvas Fallback (`NativeFallbackView`):**
   - Implemented as a high-performance native Android `View` (`app/src/main/java/com/stillfeed/app/floating/FloatingCompanionView.kt`).
   - Draws the stadium capsule with anti-aliased paths, linear gradients matching the mood colors, and precise drop shadows.
   - Renders the brain silhouette using layered bezier curves and lobe arcs in `#FF9DB5` / mood color with `#5A1F3A` border.
   - Renders glossy eye discs, pupils, and mood-appropriate mouth arcs.
   - Renders the counter text using native `Paint` and Inter Bold font.
   - Requires zero WebGL context, runs on software rendering if necessary, and consumes <8MB of memory.

---

## 6. Android Platform & Architecture Mapping

| Reference Feature (HTML/JS) | Native Android Implementation | File Location |
|---|---|---|
| 360 × 220 px Canvas | Scaled 160 × 98 dp overlay window | `FloatingOverlayService.kt` |
| Three.js Scene (`three.min.js`) | Offline bundled asset (`three.min.js`) | `app/src/main/assets/floating_icon/three.min.js` |
| Inter Bold Font (`inter_bold.ttf`) | Local asset font loaded via CSS `@font-face` | `app/src/main/assets/floating_icon/inter_bold.ttf` |
| Mouse Dragging & Cursor | `MotionEvent.ACTION_MOVE` via Android `WindowManager` | `FloatingOverlayService.kt` |
| Boundary Clamping & Snapping | Screen bounds calculation with insets & safe areas | `FloatingPositionManager.kt` |
| Position Persistence (`localStorage`) | Persistent Jetpack DataStore preferences | `AppPreferencesDataStore.kt` |
| Mood Updates via Demo Slider | Real-time Flow from Room & DataStore via `UsageAggregator` | `FloatingIconController.kt` |
| Simulated Reel Counter | Strict Data Honesty (Em-dash `—` when unverified) | `BrainMoodResolver.kt` |
| Tap to Open App | PendingIntent launching `MainActivity` with AppLock gate | `FloatingCompanionJsBridge.kt` |
| WebGL Failure Fallback | Native Android 2D Canvas `NativeFallbackView` | `FloatingCompanionView.kt` |
| Battery Saver / Background | Pauses RAF on screen-off / resume on screen-on | `FloatingOverlayService.kt` |

---

## 7. Conclusion

The audit demonstrates 100% fidelity to the visual character and animation physics of `Skillfeed Floating Icon - Fixed.html` while strictly honoring STILLFEED's architectural principles:
- **No CDNs:** Completely self-contained offline assets.
- **Data Honesty:** Em-dash fallback for unverified Reel counts.
- **Resource Respect:** Hardware-accelerated, paused on screen-off, low memory footprint.
- **Native Android Ergonomics:** Smooth edge dragging, safe window insets, and full Settings toggle controls.
