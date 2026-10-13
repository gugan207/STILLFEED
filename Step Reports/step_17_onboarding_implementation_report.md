# STILLFEED — Step 17: Five-Screen Onboarding Flow Implementation Report

**Date:** October 13, 2026  
**Auditor / Implementer:** Antigravity AI Pair Programmer  
**Figma Source:** [Stillfeed — Widget Design System](https://www.figma.com/design/SW3qMYI0E3pnYALuExsdd8/Stillfeed-%E2%80%94-Widget-Design-System?node-id=31-2)  
**Canvas / Flow:** `02 — App Design`, Flow 01: Onboarding (Section Node `99:220`)  
**Target Hardware:** Physical Motorola Edge 60 Fusion (`ZA222WTN4H`, Android 16 / SDK 36, 1220×2712 @ 480dpi)  
**Repository Branch:** `main` (workspace `d:\SKILLFEED`)  

---

## 1. EXECUTIVE SUMMARY

Step 17 implements the complete five-screen onboarding sequence for STILLFEED, closing the primary gap identified in `Step Reports/step_16_figma_screen_audit_report.md` (Screens 01 through 05). Newly signed-up and newly authenticated users now enter a calm, editorial setup journey that clearly communicates the app's purpose, details what is protected and what remains accessible, guides system permissions without misleading status claims, allows selection of their daily Reels allowance (audited default: 30 reels), and summarizes verified setup before navigating into the Home dashboard.

All 320 unit tests pass (an increase of +14 tests from 306), Android lint completed with 0 errors, and `assembleDebug` built the production-ready debug APK cleanly.

---

## 2. FIGMA NODES REFERENCED AND ALIGNED

Each onboarding screen was directly inspected from the Figma API canvas (`SW3qMYI0E3pnYALuExsdd8`):

| Screen | Figma Node ID | Title / Section Label | Visual Elements & Tokens Aligned |
|---|---|---|---|
| **01 — Welcome** | `39:2` | `GET STARTED` · `01 / 05` | Editorial header (`YOUR ATTENTION IS STILL YOURS`, 42sp headline), calm eye/glasses illustration (`cr=42.5dp`, surface cream fill, sage green stroke/pupils), dark CTA pill button (`START WITH THESE`), and footer reassurance with setup marker (`You can change everything later.`). |
| **02 — What STILLFEED Blocks** | `39:23` | `WHY IT EXISTS` · `02 / 05` | 31sp headline (`Short video is the thing we’re changing.`); `BLOCKED` blush coral badge with card for Reels and Shorts; `KEPT` sage green badge with card for DMs + Stories; boundary explanation note (`Even when a Reel opens from a DM, STILLFEED keeps the boundary.`). |
| **03 — Permissions** | `39:41` | `PROTECT YOUR FEED` · `03 / 05` | 31sp headline (`Protect the apps where scrolling starts.`); separate Permission Row Cards with direct system settings intent launchers for Accessibility Service and Usage Access; live verification of actual grant state; clear explanation of what remains unavailable if denied; protected apps status list (Instagram, YouTube, and Facebook); and privacy statement. |
| **04 — Daily Allowance** | `39:64` | `SET YOUR DEFAULT` · `04 / 05` | 32sp headline (`Choose your starting limit.`); 52sp hero number (`30 reels`) with `Balanced` tier badge; interactive preset chips (15, 20, 30, 45); monthly persistence context cards (`THIS MONTH` 30 reels fixed, `MONTHLY REVIEW` choose again); primary action (`Use this limit`). |
| **05 — Setup Complete** | `39:84` | `READY` · `05 / 05` | Calm glasses illustration (`cr=44.8dp`, 90dp); 35sp headline (`Your phone can feel quieter now.`); summary card displaying the selected allowance (`30 reels today’s allowance`) alongside active hours (`06:00–22:00`); verified protection status badge (`Protected` vs `Permissions pending (paused)`); final action (`Open STILLFEED`). |

---

## 3. ARCHITECTURE, NAVIGATION & PERSISTENCE CHANGES

### 3.1 Persistence Layer (`AppPreferencesDataStore`)
- Added `val DAILY_REELS_ALLOWANCE = intPreferencesKey("daily_reels_allowance")` in `AppPreferencesKeys.kt`.
- Updated `AppPreferences` domain model to hold `val dailyReelsAllowance: Int = 30` (preserving the audited default).
- Extended `AppPreferencesRepository` with default-implemented methods:
  - `suspend fun setOnboardingStep(step: Int)`
  - `suspend fun setDailyReelsAllowance(allowance: Int)`
- Updated `DataStoreAppPreferencesRepository` to persist and map `dailyReelsAllowance` and `onboardingStep` directly to Jetpack DataStore without creating conflicting stores or requiring Room schema migrations.

### 3.2 Navigation Integration (`StillfeedNavHost.kt`)
- Added `Screen.Onboarding` route (`"onboarding"`).
- Injected `AppPreferencesRepository` and resolved `OnboardingViewModel` factory using the dependency container on `StillfeedApplication`.
- **Dynamic Start Destination Resolution:**
  - When authenticated (`AuthState.SignedIn`):
    - If `hasCompletedOnboarding == true`: start destination resolves to `Screen.Home.route`.
    - If `hasCompletedOnboarding == false`: start destination resolves to `Screen.Onboarding.route`.
  - When an explicit `startDestination` is passed (e.g., in previews or tests), the explicit route is honored.
- **Completion Navigation:**
  - Upon calling `viewModel.completeOnboarding()`, `navController.navigate(Screen.Home.route)` is executed with `popUpTo(Screen.Onboarding.route) { inclusive = true }` and `launchSingleTop = true`, removing Onboarding from the backstack so system back on Home exits the app instead of looping back to onboarding.

### 3.3 Back Navigation & Lifecycle Resilience
- `OnboardingScreen` registers a `BackHandler(enabled = currentStep > 0)`:
  - Back press on step 4 returns to step 3, step 3 returns to step 2, etc.
  - Back press on step 0 (Welcome) exits normally or defers to system back.
- Uses `LocalLifecycleOwner` with `LifecycleEventObserver` to trigger `viewModel.refreshPermissions()` on `Lifecycle.Event.ON_RESUME`. When the user switches to Android Settings and returns to STILLFEED, the grant status updates dynamically without needing an app restart.

---

## 4. PERMISSION HANDLING SPECIFICATION

1. **Accessibility Service:**
   - Checked via `AccessibilityPermissionChecker.isAccessibilityPermissionGranted()` (inspects `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`).
   - Launcher: `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
   - Explains: Needed to recognize when Instagram Reels or YouTube Shorts open. If disabled, STILLFEED cannot detect short-form surfaces to pause scrolling.
2. **Usage Access:**
   - Checked via `UsageStatsDataSource.hasUsageStatsPermission()` (inspects `AppOpsManager.OPSTR_GET_USAGE_STATS`).
   - Launcher: `Settings.ACTION_USAGE_ACCESS_SETTINGS` with package URI fallback.
   - Explains: Needed to monitor daily usage rhythms and calculate monitored usage.
3. **Truthful Status Guarantees:**
   - Protection is marked as `isProtectionActive` (`StillfeedStatus.Protected`) **only** when both permissions are verified as granted.
   - Screen 05 displays `Permissions pending (paused)` with `StillfeedStatus.NotConnected` if either permission is missing. Protection in Room DB is NOT enabled prematurely if permissions are missing.

---

## 5. FILES CREATED AND MODIFIED

### Created Files
1. `app/src/main/java/com/stillfeed/app/feature/onboarding/OnboardingScreen.kt` — Master onboarding container with step transitions, back handling, and lifecycle resume observer.
2. `app/src/main/java/com/stillfeed/app/feature/onboarding/OnboardingViewModel.kt` — State management, permission checks, step progression, and completion logic.
3. `app/src/main/java/com/stillfeed/app/feature/onboarding/components/OnboardingCalmIllustration.kt` — Vector canvas drawing of the Figma glasses/calm eyes illustration.
4. `app/src/main/java/com/stillfeed/app/feature/onboarding/steps/WelcomeStep.kt` — Screen 01: Welcome.
5. `app/src/main/java/com/stillfeed/app/feature/onboarding/steps/WhatStillfeedBlocksStep.kt` — Screen 02: What STILLFEED Blocks (Reels/Shorts blocked, DMs/Stories kept).
6. `app/src/main/java/com/stillfeed/app/feature/onboarding/steps/PermissionsStep.kt` — Screen 03: Permissions (Accessibility & Usage Access).
7. `app/src/main/java/com/stillfeed/app/feature/onboarding/steps/DailyAllowanceStep.kt` — Screen 04: Daily Allowance (default 30 reels).
8. `app/src/main/java/com/stillfeed/app/feature/onboarding/steps/SetupCompleteStep.kt` — Screen 05: Setup Complete.
9. `app/src/test/java/com/stillfeed/app/feature/onboarding/OnboardingViewModelTest.kt` — 7 unit tests for ViewModel state, permissions, steps, and allowance.
10. `app/src/test/java/com/stillfeed/app/feature/navigation/OnboardingNavigationTest.kt` — 5 unit tests for route contracts and start-destination resolution.

### Modified Files
1. `app/src/main/java/com/stillfeed/app/domain/model/AppPreferences.kt` — Added `dailyReelsAllowance: Int = 30`.
2. `app/src/main/java/com/stillfeed/app/data/preferences/AppPreferencesDataStore.kt` — Added `DAILY_REELS_ALLOWANCE` preference key.
3. `app/src/main/java/com/stillfeed/app/domain/repository/AppPreferencesRepository.kt` — Added `setOnboardingStep` and `setDailyReelsAllowance`.
4. `app/src/main/java/com/stillfeed/app/data/preferences/DataStoreAppPreferencesRepository.kt` — Implemented data store editing and mapping for new preferences.
5. `app/src/main/java/com/stillfeed/app/core/navigation/StillfeedNavHost.kt` — Integrated Onboarding composable and dynamic start-destination determination.
6. `app/src/test/java/com/stillfeed/app/data/preferences/AppPreferencesRepositoryTest.kt` — Added unit test coverage for `dailyReelsAllowance` and `onboardingStep`.

---

## 6. VERIFICATION COMMANDS AND RESULTS

| Verification Task | Command Executed | Result |
|---|---|---|
| **Kotlin Compilation** | `.\gradlew.bat compileDebugKotlin` | **SUCCESS** (0 compile errors, 0 warnings) |
| **Unit Test Suite** | `.\gradlew.bat testDebugUnitTest --rerun-tasks` | **SUCCESS** (320/320 tests passed, 0 failures, 0 ignored, 29 executed tasks) |
| **Android Lint** | `.\gradlew.bat lintDebug` | **SUCCESS** (0 errors) |
| **Debug APK Build** | `.\gradlew.bat assembleDebug` | **SUCCESS** (`app-debug.apk` built successfully in 46s) |

---

## 7. MANUAL PHYSICAL DEVICE TEST PLAN (Motorola Edge 60 Fusion)

The following tests will be performed on the physical Motorola Edge 60 Fusion (`ZA222WTN4H`, Android 16 / SDK 36):
1. **Fresh Install / New Account Flow:**
   - Sign up with new email or Google account -> Verify app drops directly into Screen 01 (Welcome).
   - Tap "START WITH THESE" -> Step 02 loads with smooth horizontal slide animation.
   - Tap "CONTINUE" -> Step 03 opens with "ENABLE" buttons for Accessibility and Usage Access.
   - Tap "ENABLE" on Accessibility -> Opens system Accessibility settings -> Return to app -> Verify status updates to "Protected" without manual reload.
   - Tap "ENABLE" on Usage Access -> Opens system Usage Access settings -> Return to app -> Verify status updates to "Protected".
   - Step 04 -> Select 20 reels or leave default 30 -> Tap "Use this limit".
   - Step 05 -> Verify summary shows "30 reels today's allowance" and "Protection is active".
   - Tap "Open STILLFEED" -> Transitions to Home screen.
2. **Back Navigation & Persistence:**
   - Press device back button during Steps 02, 03, 04, 05 -> Verify each returns to previous step.
   - Force close app on Step 03 -> Reopen -> Verify app resumes onboarding without dropping user into Home.
   - Once completed, press back on Home -> Verify app exits instead of returning to Onboarding.
3. **Denied Permissions Path:**
   - Complete onboarding with permissions skipped/denied -> Verify Screen 05 shows `Permissions pending (paused)` with `Not connected` badge, and protection is not prematurely claimed as active.

---

## 8. STATUS & NEXT STEPS

- **Implementation Step 17:** Complete.
- **Git Status:** Working tree clean except for uncommitted Step 17 files, ready for review.
- **Step 18:** Core Home & Evening Alignment (`Screens 06 & 07`) will begin following review of this report.
