# STILLFEED — Step 18: Home (Day) & Home (Evening) Implementation Report

**Date:** October 13, 2026  
**Auditor / Implementer:** Antigravity AI Pair Programmer  
**Repository:** `gugan207/STILLFEED`  
**Branch:** `main`  
**Project Directory:** `D:\SKILLFEED`  
**Target Hardware:** Motorola Edge 60 Fusion (`ZA222WTN4H`, Android 16 / SDK 36, 1220×2712 @ 480dpi)

---

## 1. EXISTING CODE INSPECTED

Before authoring code, the repository state, navigation architecture, data flow, and visual definitions were inspected:

1. **Reports & Audit Foundations:**
   - `Step Reports/step_16_figma_screen_audit_report.md`: Inspected Flow 02 (Core Experience) specifications, Screen 06 (`39:103`), and Screen 07 (`39:137`).
   - `Step Reports/step_17_onboarding_implementation_report.md`: Inspected persisted allowance default (30 reels) and Onboarding-to-Home navigation gateway.
2. **Current Home & Awareness Implementation:**
   - `app/src/main/java/com/stillfeed/app/feature/home/HomeScreen.kt`: Legacy layout displayed static "MONITORED USAGE TODAY", "PROTECTED APPS", and embedded `AwarenessMonthlyCard`.
   - `app/src/main/java/com/stillfeed/app/feature/awareness/AwarenessViewModel.kt`: Handled monthly awareness percentages; lacked time-of-day awareness, yesterday comparisons, and weekly rhythm state.
3. **Usage Aggregation Layer:**
   - `app/src/main/java/com/stillfeed/app/usage/aggregation/UsageAggregator.kt`: Provides `getTodayUsage()` and `getHistoricalDailySummaries(startMillis, endMillis)`. Confirmed that usage data is tracked strictly in millisecond durations and integer session counts; verified that individual watched Reel counts are **not** present in the database.
   - `app/src/main/java/com/stillfeed/app/usage/aggregation/DailyUsageSummary.kt`: Data model with `totalMonitoredMillis`, `instagramUsageMillis`, and `youtubeUsageMillis`.
4. **Preferences & Navigation:**
   - `app/src/main/java/com/stillfeed/app/domain/repository/AppPreferencesRepository.kt`: Provides `dailyReelsAllowance` Flow.
   - `app/src/main/java/com/stillfeed/app/core/navigation/Screen.kt` & `StillfeedNavHost.kt`: Navigation routes and tab controllers.

---

## 2. FIGMA NODES USED

Canvas: `02 — App Design` (Node ID `31:2`) from Figma project `Stillfeed — Widget Design System` (`SW3qMYI0E3pnYALuExsdd8`).

| Screen | Figma Node ID | Geometry | Visual Elements | Exact Styles & Fills |
|---|---|---|---|---|
| **Screen 06 — Home (Day)** | `39:103` | 390×844dp, cr=28 | - Brand Wordmark + Section Header `TODAY` + Step `01 / 03`<br>- SuperDisplay Hero Counter (86sp)<br>- Mood Badge capsule: `YOUR MIND LOOKS FRESH`<br>- Sun Companion Graphic<br>- Context Card: `A LITTLE CONTEXT`<br>- Yesterday comparison + Delta badge<br>- `YOUR WEEK` 7-day mini-rhythm bar chart<br>- CTA Button: `Reset activity`<br>- Bottom Navigation (Home selected) | - Background: `#F5F0E7`<br>- SuperDisplay: 86sp w600 `#1F2421`<br>- Mood Pill: fill `#7D927C`, text `#1F2421`<br>- Sun Companion: Gold `#E8C86B` circle (36dp) + inset `#F5F0E7` circle (18.72dp)<br>- Card: `#FBF8F2` fill, `#D9D4CA` border, cr=26dp<br>- Delta: `#7D927C` 14sp w600<br>- Bars: 22dp width, cr=8dp, `#7D927C` (history), `#E8C86B` (today)<br>- CTA: `#1F2421` fill, `#FBF8F2` text, cr=28dp |
| **Screen 07 — Home (Evening)** | `39:137` | 390×844dp, cr=28 | - Brand Wordmark + Section Header `EVENING` + Step `02 / 03`<br>- SuperDisplay Hero Counter (86sp)<br>- Mood Badge capsule: `MAKE A LITTLE SPACE`<br>- Crescent Moon Companion Graphic<br>- Context Card: `TONIGHT`<br>- Subtitle: "A reset is available whenever you want it."<br>- Reflection copy: "Keep the rest of your evening yours."<br>- CTA Button: `Reset activity`<br>- Bottom Navigation (Home selected) | - Background: `#F5F0E7`<br>- SuperDisplay: 86sp w600 `#1F2421`<br>- Mood Pill: fill `#E8C86B`, text `#1F2421`<br>- Moon Companion: Lavender `#B7AEC8` disc (36dp) + offset `#F5F0E7` circle mask (shifted +9dp, -2.88dp)<br>- Card: `#FBF8F2` fill, `#D9D4CA` border, cr=26dp<br>- Reflection: 17sp w600 `#1F2421`<br>- CTA: `#1F2421` fill, `#FBF8F2` text, cr=28dp |

---

## 3. FILES CREATED AND MODIFIED

### Created Files:
1. `app/src/main/java/com/stillfeed/app/feature/home/components/HomeCompanionGraphic.kt`:
   - `DaySunCompanion()`: Renders the Figma Day sun icon using an outer `#E8C86B` gold circle and an inset `#F5F0E7` paper circle.
   - `EveningMoonCompanion()`: Renders the Figma Evening crescent moon using a `#B7AEC8` lavender disc and a shifted background mask circle.
2. `app/src/main/java/com/stillfeed/app/feature/home/components/HomeWeeklyRhythmChart.kt`:
   - 7-bar Monday–Sunday mini-rhythm chart matching Figma dimensions (22dp bar width, 8dp corner radius, 72dp max track, `#7D927C` historical bars, `#E8C86B` today highlight).
3. `app/src/test/java/com/stillfeed/app/feature/home/HomePresentationTest.kt`:
   - 7 unit tests verifying daytime/evening time boundary transitions, daily allowance persistence across time shifts, absence of fabricated reel counts, yesterday comparison calculation, and 7-day weekly rhythm bars.

### Modified Files:
1. `app/src/main/java/com/stillfeed/app/core/navigation/Screen.kt`:
   - Added `data object ResetActivity : Screen("reset_activity")` to establish navigation route contract for Step 19.
2. `app/src/main/java/com/stillfeed/app/feature/awareness/AwarenessViewModel.kt`:
   - Added state fields to `AwarenessUiState`: `isEvening`, `dailyReelsAllowance`, `yesterdayUsageMinutes`, `yesterdayUsageDifferenceMinutes`, `isReelsCountAvailable`, `watchedReelsToday`, and `weeklyRhythm`.
   - Injected optional `appPreferencesRepository` and `clock` (defaulting to system clock for production, injectable for unit tests).
   - In `loadMonthlyState()`, computed local time Day/Evening boundary (`hour >= 18 || hour < 6`), queried yesterday usage summary, and calculated 7-day Monday–Sunday rhythm.
   - Added `observePreferences()` to reflect persisted daily allowance changes in real time.
3. `app/src/main/java/com/stillfeed/app/feature/home/HomeScreen.kt`:
   - Replaced legacy layout with Figma Screen 06 (Day) and Screen 07 (Evening) implementations.
   - Integrated `StillfeedScreenHeader`, `superDisplay` hero number, mood capsules, companion graphics, adaptive context cards, weekly mini-rhythm, protection status warning banner, and `Reset activity` CTA.
4. `app/src/main/java/com/stillfeed/app/core/navigation/StillfeedNavHost.kt`:
   - Passed `appPreferencesRepository` to `AwarenessViewModel.provideFactory`.
   - Wired `onResetActivity` parameter.
5. `app/src/test/java/com/stillfeed/app/feature/navigation/HomeNavigationTest.kt`:
   - Added route validation for `Screen.ResetActivity`.

---

## 4. HOME DAY IMPLEMENTATION (SCREEN 06)

Screen 06 renders automatically when device-local time is between **06:00 and 17:59**:
- **Brand Header:** `StillfeedScreenHeader` rendering `STILLFEED` wordmark (15sp Bold), `TODAY` section tag (10sp SemiBold), and `01 / 03` step index on top right.
- **Hero Counter:** Displays verified monitored app time (`reels watched today · 42m monitored app time`). In accordance with data contracts, displays `—` for watched reels until single-reel interception is implemented in Step 21.
- **Companion Mood Badge & Sun Graphic:**
  - Capsule pill: `#7D927C` (Sage Green) with text `"YOUR MIND LOOKS FRESH"` (10sp SemiBold `#1F2421`).
  - Right graphic: `DaySunCompanion()` (36dp gold ring with 18.72dp centered paper core).
- **A Little Context Card:**
  - Card: 26dp rounded corners, `#FBF8F2` cream fill, 1dp `#D9D4CA` border.
  - Section header: `"A LITTLE CONTEXT"`.
  - Headline: Dynamically adapts to usage ("You’re using short video less than yesterday." when usage decreased; "A gentle check-in on today's rhythm." when increased; "Steady attention rhythm today." when consistent).
  - Metrics row: Displays `$todayFormatted today · $yesterdayFormatted yesterday`.
  - Delta badge: Displays `"↓ Xm less"` in `#7D927C` or `"↑ Xm more"` in `#6D746E`.
  - Allowance caption: `"Allowance: 30 reels · Monitored in app time"`.
  - Mini-Rhythm Chart: Embedded `HomeWeeklyRhythmChart` with 7 bars (M, T, W, T, F, S, S), highlighting today in `#E8C86B`.
- **Reset Activity Action:** Full-width `#1F2421` pill button (56dp height, 28dp radius) labeled `"Reset activity"`.

---

## 5. HOME EVENING IMPLEMENTATION (SCREEN 07)

Screen 07 renders automatically when device-local time is between **18:00 and 05:59**:
- **Brand Header:** Section label changes to `EVENING` and step index displays `02 / 03`.
- **Hero Counter:** Consistently displays monitored duration and persisted allowance.
- **Companion Mood Badge & Moon Graphic:**
  - Capsule pill: `#E8C86B` (Gold) with text `"MAKE A LITTLE SPACE"` (10sp SemiBold `#1F2421`).
  - Right graphic: `EveningMoonCompanion()` (36dp lavender crescent moon).
- **Tonight Card:**
  - Headline: `"You’ve been here a while."` (22sp SemiBold `#1F2421`).
  - Subtitle: `"A reset is available whenever you want it."` (13sp Regular `#6D746E`).
  - Metrics: `"$todayFormatted monitored app time · Allowance: 30 reels"`.
- **Evening Reflection Copy:** Prominent calm message below the card: `"Keep the rest of your evening yours."` (17sp SemiBold `#1F2421`).
- **Reset Activity Action:** Full-width `#1F2421` pill button labeled `"Reset activity"`.

---

## 6. USAGE METRIC HANDLING AND LIMITATIONS

### Critical Data Architecture Finding:
The STILLFEED database records accessibility session intervals and blocking events:
$$\text{monitoredMillis} = \sum \text{durationMillis of closed sessions}$$
The codebase currently does **not** track individual scroll gestures or distinguish between 1 Reel watched for 60 seconds vs 4 Reels watched for 15 seconds each.

### Enforced Rules:
1. **Zero Conversion:** We do NOT multiply or divide minutes to fabricate a Reel count (e.g., assuming 1 reel = 45 seconds).
2. **Zero False Equivalence:** We do NOT treat blocked interventions as watched Reels.
3. **Zero Counter Spoofing:** We do NOT increment a Reel counter on arbitrary accessibility events.
4. **Honest UI Representation:**
   - Hero number displays `—` (em-dash) when `isReelsCountAvailable == false`.
   - Subtitle clearly reads: `"reels watched today · 42m monitored app time"`.
   - Daily allowance selected during Onboarding (default 30 reels) is persisted in `AppPreferences` and displayed as context: `"Allowance: 30 reels · Monitored in app time"`.
5. **Contract for Step 21 Overlay:**
   - Single-reel interception requires structural UI tree diffing or vertical fling gesture listening in `StillfeedAccessibilityService`. This contract will be implemented during Step 21 without compromising the current clean data model.

---

## 7. NAVIGATION CHANGES

1. **Screen.kt:**
   - Declared `data object ResetActivity : Screen("reset_activity")`.
2. **HomeScreen Contract:**
   - Added `onResetActivity: () -> Unit = {}` callback.
   - When clicked, surfaces an in-app calm Snackbar notification (`"Reset activity will be available in the next update."`) while executing the callback.
   - Prevents dead-end navigation or broken blank screens while keeping the contract ready for Step 19.
3. **Preserved Navigation:**
   - Onboarding → Home transition remains operational.
   - Home ↔ Insights ↔ Settings bottom navigation tabs operate with single-top and save-state restoration.
   - Settings ↔ AwarenessSelection navigation remains operational.

---

## 8. AUTOMATED TEST RESULTS

All compilation, lint, and test verification tasks completed successfully:

```
> Task :app:compileDebugKotlin
BUILD SUCCESSFUL in 2m 11s

> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 4m 50s
Total Test Suites: 47
Total Unit Tests: 328 (328 passed, 0 failed, 0 skipped)

> Task :app:lintDebug
BUILD SUCCESSFUL in 2m 42s
Lint Errors: 0

> Task :app:assembleDebug
BUILD SUCCESSFUL in 2m 5s
Output: app-debug.apk generated cleanly
```

### New Tests Added in `HomePresentationTest.kt`:
1. `dayPresentation_whenClockIsDaytime_isEveningIsFalse`: Passed.
2. `eveningPresentation_whenClockIsEvening_isEveningIsTrue`: Passed.
3. `lateNightPresentation_whenClockIsLateNight_isEveningIsTrue`: Passed.
4. `boundaryTransitions_dayAndEveningExactBoundaries`: Passed (05:59 evening, 06:00 day, 17:59 day, 18:00 evening).
5. `dailyAllowance_reflectsPersistedPreferencesAndDoesNotResetOnTimeShift`: Passed (persists 45 across day/evening switch).
6. `usageData_unavailableReelCountContractIsHonored`: Passed (`isReelsCountAvailable == false`, `watchedReelsToday == null`).
7. `yesterdayComparison_calculatesDifferencesAccurately`: Passed (yesterday 50m, today 30m, delta -20m).
8. `weeklyRhythm_builds7DaysWithCorrectTodayHighlight`: Passed (7 bars M–S with Tuesday highlighted).

---

## 9. PHYSICAL-DEVICE VERIFICATION STATUS

- **Target Device:** Motorola Edge 60 Fusion (`ZA222WTN4H`, Android 16 / SDK 36).
- **Runtime ADB Probe:** Checked `adb devices`. The physical device was not attached to the host during this step session.
- **Status:** Automated test suite and build validation passed with 100% success. Physical on-device deployment is ready to run upon handset USB/wireless connection.

---

## 10. REMAINING ISSUES AND STEP 19 DEPENDENCIES

1. **Step 19 Dependencies:**
   - Implement `Screen 08 — Blocked Reel` (Figma Node `39:162`).
   - Implement `Screen 09 — Reset Activity` (Figma Node `64:7`) with 30s pause, breathing exercise, and reflection.
   - Implement `Screen 10 — Awareness Cards` (Figma Node `64:46`) card deck.
   - Implement `Screen 11 — Unlock` (Figma Node `64:65`) for batch allowance unlocks (10, 15, 20 reels).
   - Wire `onResetActivity` on `HomeScreen` directly into `ResetActivityScreen`.

---

## 11. COMMIT AND SUBMISSION NOTICE

In accordance with instructions:
- Application code changes remain uncommitted in the local working tree.
- Only this markdown report (`Step Reports/step_18_home_evening_implementation_report.md`) is staged, committed, and pushed to GitHub on branch `main`.
- STOPPED after Step 18 for review.
