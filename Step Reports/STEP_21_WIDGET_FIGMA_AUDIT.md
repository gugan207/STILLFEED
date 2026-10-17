# STEP 21: STILLFEED Android Home Screen Widgets — Figma Audit Report

**Figma Design Source:**
- **URL:** `https://www.figma.com/design/SW3qMYI0E3pnYALuExsdd8/Stillfeed-%E2%80%94-Widget-Design-System?node-id=31-2`
- **File Key:** `SW3qMYI0E3pnYALuExsdd8`
- **Document Title:** `Stillfeed — Widget Design System`
- **Primary Canvas:** `01 — Widgets` (`214:2`)
- **Inspection Date:** 2026-10-17

---

## 1. Executive Summary & Design System North Star

The STILLFEED widget system is defined in Canvas `01 — Widgets` (`214:2`) under the design philosophy:
> **"Widgets that grow with your day. The environment changes with time. The companion changes with real attention."**
> **Visual rule:** *Environment first · companion second · count last.*

The widgets do not function as cold analytical dashboards or punitive trackers. Instead, they provide calm, editorial, atmospheric windows onto the user's home screen. The backdrop transitions through distinct phases of the day (Morning, Day, Afternoon, Evening, Night), and the flower companion visually responds to attention level and time, offering gentle encouragement without shame.

---

## 2. Complete Figma Widget Inventory

A thorough audit of the entire Figma document identified five genuine widget designs under Canvas `214:2` (`01 — Widgets`), organized across 3 canonical launcher form factors (Compact, Medium, Large) and 5 diurnal/attention themes (Morning, Day, Afternoon, Evening, Night).

| # | Widget Name | Figma Node ID | Parent Canvas / Frame ID | Launcher Size Category | Figma Dimensions | Theme & Light/Dark Variant | Background Color | Companion Mood / Expression |
|---|---|---|---|---|---|---|---|---|
| 1 | **Morning Widget** | `216:2` | `214:2` | Compact (2×2 / 3×2) | 250 × 190 dp | Light / Morning Glow | `#FAF2D1` (Warm Cream) | Fresh, waking bloom |
| 2 | **Day Widget** | `216:33` | `214:2` | Medium (4×2) | 430 × 190 dp | Light / Clear Sky | `#D3EFE8` (Fresh Turquoise) | Calm, peaceful bloom |
| 3 | **Afternoon Widget** | `216:58` | `214:2` | Medium (4×2) | 430 × 190 dp | Warm / Sunset Glow | `#F9D6BF` (Warm Peach) | Centered, steady bloom |
| 4 | **Evening Widget** | `216:81` | `214:2` | Large (3×3 / 4×3 / 4×4) | 360 × 360 dp | Twilight / Lavender Dusk | `#BAA3D8` (Muted Violet) | Mindful, softly concerned brows |
| 5 | **Night Widget** | `216:107` | `214:2` | Large (3×3 / 4×3 / 4×4) | 360 × 360 dp | Dark / Midnight Blue | `#112335` (Deep Navy) | Resting, asleep bloom |

---

## 3. Detailed Specification by Widget Design

### 3.1. Morning Widget (`216:2`)
- **Metadata Label:** `COMPACT · MORNING` (`214:8`)
- **Dimensions:** 250 × 190 dp, Corner Radius: 20 dp
- **Fills:** Background `#FAF2D1` (Solid Cream)
- **Visual Elements:**
  - **Brand Mark:**
    - Logo Leaf (`216:3`): Ellipse 16 × 13 dp, `#1A2121`
    - Wordmark (`216:4`): "Stillfeed", Inter 13sp SemiBold (weight 600), `#1A2121`
  - **Sky & Environment:**
    - Sun (`216:5`): Disc diameter 30 dp, `#FAAB2E`
    - Sun Rays (`216:6`–`216:13`): 8 radial rays in `#FAAB2E`
    - Cloud Cluster (`216:14`–`216:16`): Overlapping ellipses Cloud A (34×20 dp), Cloud B (30×25 dp), Cloud C (24×17 dp) in `#FFFAE8`
  - **Landscape:**
    - Back Hill (`216:17`): Ellipse 240 × 76 dp, `#D4E0AD` (Pale Olive)
    - Front Hill (`216:18`): Ellipse 265 × 68.4 dp, `#9EB887` (Sage Green)
  - **Companion Character (Morning Bloom):**
    - Stem (`216:21`): 5.4 × 16.8 dp, `#47734D`
    - Leaves (`216:19`, `216:20`): Left & Right ellipses, `#47734D`
    - Petals (`216:22`–`216:25`): Top `#FAADA3`, Left/Right/Bottom `#F78C91`
    - Face (`216:26`–`216:29`): Center disc `#FAADA3`, horizontal slit eyes `#1A2121`, tiny mouth `#1A2121`
  - **Attention & Usage Content:**
    - Count (`216:30`): "6" (Inter 40sp Bold, `#1A2121`)
    - Label (`216:31`): "reels today" (Inter 12sp Regular, `#1A2121`)
    - Message (`216:32`): "Your mind looks fresh." (Inter 14sp Regular, `#1A2121`)
- **Interactions:** Tap opens STILLFEED Home.

---

### 3.2. Day Widget (`216:33`)
- **Metadata Label:** `MEDIUM · DAY` (`214:40`)
- **Dimensions:** 430 × 190 dp, Corner Radius: 20 dp
- **Fills:** Background `#D3EFE8` (Fresh Turquoise)
- **Visual Elements:**
  - **Brand Mark:** Logo Leaf (`216:34`) + "Stillfeed" (`216:35`) Inter 13sp SemiBold `#1A2121`
  - **Sky & Environment:**
    - Left Cloud Formation (`216:36`–`216:38`): `#FFFDF4`
    - Right Cloud Formation (`216:39`–`216:41`): `#FFFDF4`
  - **Landscape:**
    - Back Hill (`216:42`): Ellipse 412.8 × 76 dp, `#ADCCB7`
    - Front Hill (`216:43`): Ellipse 455.8 × 68.4 dp, `#7AA88E`
  - **Companion Character (Daytime Bloom):**
    - Stem & Leaves (`216:44`–`216:46`): `#44724F`
    - Petals (`216:47`–`216:50`): `#F9B5A8` & `#EF9699`
    - Face (`216:51`–`216:54`): Center `#F9B5A8`, serene eyes `#1A2121`, mouth `#1A2121`
  - **Attention & Usage Content:**
    - Count (`216:55`): "9" (Inter 40sp Bold, `#1A2121`)
    - Label (`216:56`): "reels today" (Inter 12sp Regular, `#1A2121`)
    - Message (`216:57`): "Nice and slow. You’re doing well." (Inter 14sp Regular, `#1A2121`)
- **Interactions:** Tap opens STILLFEED Home.

---

### 3.3. Afternoon Widget (`216:58`)
- **Metadata Label:** `MEDIUM · AFTERNOON` (`214:66`)
- **Dimensions:** 430 × 190 dp, Corner Radius: 20 dp
- **Fills:** Background `#F9D6BF` (Warm Peach)
- **Visual Elements:**
  - **Brand Mark:** Logo Leaf (`216:59`) + "Stillfeed" (`216:60`) Inter 13sp SemiBold `#1A2121`
  - **Sky & Environment:**
    - Afternoon Sun (`216:61`): Disc 34 × 34 dp, `#F9AF66` (Warm Amber)
    - Cloud Cluster (`216:62`–`216:64`): Overlapping clouds in `#FFEAD6`
  - **Landscape:**
    - Back Hill (`216:65`): Ellipse 412.8 × 76 dp, `#E2A891`
    - Front Hill (`216:66`): Ellipse 455.8 × 68.4 dp, `#AD7A72`
  - **Companion Character (Afternoon Bloom):**
    - Stem & Leaves (`216:67`–`216:69`): `#546D47`
    - Petals (`216:70`–`216:73`): `#F9999B` & `#E87591`
    - Face (`216:74`–`216:77`): Center `#F9999B`, calm eyes `#1A2121`, mouth `#1A2121`
  - **Attention & Usage Content:**
    - Count (`216:78`): "16" (Inter 40sp Bold, `#1A2121`)
    - Label (`216:79`): "reels today" (Inter 12sp Regular, `#1A2121`)
    - Message (`216:80`): "You’ve been here a while." (Inter 14sp Regular, `#1A2121`)
- **Interactions:** Tap opens STILLFEED Home / Insights.

---

### 3.4. Evening Widget (`216:81`)
- **Metadata Label:** `LARGE · EVENING` (`214:90`)
- **Dimensions:** 360 × 360 dp, Corner Radius: 20 dp
- **Fills:** Background `#BAA3D8` (Twilight Lavender)
- **Visual Elements:**
  - **Brand Mark:** Logo Leaf (`216:82`) + "Stillfeed" (`216:83`) Inter 13sp SemiBold `#1A2121`
  - **Sky & Environment:**
    - Setting Sun (`216:84`): Disc 38 × 38 dp, `#F7A072` (Sunset Coral)
    - Cloud Cluster (`216:85`–`216:87`): Lilac clouds in `#EDDDE6`
  - **Editorial Headline:**
    - Headline (`216:103`): "Your attention is still yours." (Inter 24sp SemiBold, `#1A2121`)
  - **Landscape:**
    - Back Hill (`216:88`): Ellipse 345.6 × 144 dp, `#825999` (Plum)
    - Front Hill (`216:89`): Ellipse 381.6 × 129.6 dp, `#4F3F68` (Deep Aubergine)
  - **Companion Character (Evening Reflective Bloom):**
    - Stem & Leaves (`216:90`–`216:92`): `#385647`
    - Petals (`216:93`–`216:96`): `#CC7087` & `#AA567F`
    - Face & Brows (`216:97`–`216:102`): Softly angled eyebrows (`216:100`, `216:101`) indicating mindful pause, calm eyes, mouth
  - **Attention & Usage Content:**
    - Count (`216:104`): "28" (Inter 52sp Bold, `#1A2121`)
    - Label (`216:105`): "reels today" (Inter 13sp Regular, `#1A2121`)
    - Message (`216:106`): "Take a breath. You can still reset." (Inter 15sp Regular, `#1A2121`)
- **Interactions:** Tap opens STILLFEED Reset Activity / Home.

---

### 3.5. Night Widget (`216:107`)
- **Metadata Label:** `LARGE · NIGHT` (`214:116`)
- **Dimensions:** 360 × 360 dp, Corner Radius: 20 dp
- **Fills:** Background `#112335` (Deep Midnight Blue)
- **Visual Elements:**
  - **Brand Mark:** Logo Leaf (`216:108`) `#F9F7EF` + "Stillfeed" (`216:109`) Inter 13sp SemiBold `#F9F7EF` (Light Paper)
  - **Sky & Environment:**
    - Full Moon (`216:110`): Disc 34 × 34 dp, `#F4E0A0` (Pale Butter Gold)
    - 5 Twinkling Stars (`216:111`–`216:115`): 4 × 4 dp dots in `#D1D8E0` (Silver Star)
  - **Editorial Headline:**
    - Headline (`216:129`): "Your attention is still yours." (Inter 24sp SemiBold, `#F9F7EF`)
  - **Landscape:**
    - Back Hill (`216:116`): Ellipse 345.6 × 144 dp, `#143A4F` (Teal Midnight)
    - Front Hill (`216:117`): Ellipse 381.6 × 129.6 dp, `#1C495B` (Slate Teal)
  - **Companion Character (Night Sleeping Bloom):**
    - Stem & Leaves (`216:118`–`216:120`): `#38686B`
    - Petals (`216:121`–`216:124`): `#9B77C6` & `#7A59AA` (Night Iris / Indigo)
    - Face (`216:125`–`216:128`): Sleeping horizontal eye slits and resting mouth `#1A2121`
  - **Attention & Usage Content:**
    - Count (`216:130`): "34" (Inter 52sp Bold, `#F9F7EF`)
    - Label (`216:131`): "reels today" (Inter 13sp Regular, `#F9F7EF`)
    - Message (`216:132`): "Rest well. You’ve done enough." (Inter 15sp Regular, `#F9F7EF`)
- **Interactions:** Tap opens STILLFEED Home.

---

## 4. Empty, Initial & Unavailable States Policy

Strictly obeying STILLFEED's Data Honesty and Calm Product Principles:
1. **Never Fabricate Reel Counts:** When exact reel count is not verifiable from actual system data, the widget renders an em-dash (`—`) as the count, with "reels today" and true monitored minutes (e.g. `0m monitored` or `15m monitored`).
2. **Fresh / Zero Usage State:**
   - Count: `0` (or `—` if unverifiable)
   - Message: `"Your mind looks fresh."`
   - Companion in Morning / Day cheerful bloom state.
3. **Protection Paused / Permission Missing State:**
   - The widget maintains its calm visual integrity and displays a discreet status note: `"Protection paused · Tap to resume"`.
4. **Offline State:**
   - All widget data is read directly from local Room entities (`usage_events`, `protection_state`) and DataStore preferences. No network calls are required for home-screen rendering. When offline, all local state displays seamlessly.
5. **Security & App Lock:**
   - Tap PendingIntents target `MainActivity` directly with clean flags (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`). If App Lock is enabled in STILLFEED, `MainActivity` immediately prompts for App Lock PIN / Biometric challenge before disclosing sensitive screens.
