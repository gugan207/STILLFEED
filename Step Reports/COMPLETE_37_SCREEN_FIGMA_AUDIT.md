# STILLFEED — COMPLETE 37-SCREEN FIGMA AUDIT

**Repository:** `gugan207/STILLFEED`  
**Working Branch:** `feature/complete-figma-screens`  
**Base Commit:** `bd0d6b14875c49d0d66c0b9af04985205620a48d`  
**Figma File:** [Stillfeed — Widget Design System](https://www.figma.com/design/SW3qMYI0E3pnYALuExsdd8/Stillfeed-%E2%80%94-Widget-Design-System?node-id=31-2) (`SW3qMYI0E3pnYALuExsdd8`)  
**Canvas:** `02 — App Design` (`31:2`)  
**Product Map:** `03 — Final Product Map` (`79:82` / `99:243`)  
**Audit Timestamp:** October 13, 2026  
**Auditor:** Antigravity AI Pair Programmer  

---

## 1. Executive Summary

This comprehensive audit inventories all 37 designs in the STILLFEED design system—spanning numbered screens 01 through 34, Flow 00 authentication states (00A through 00F), and contextual edge variations. Every design has been inspected directly via the Figma MCP against the live Figma document tree and verified against the actual Kotlin/Compose codebase.

### Current Implementation Baseline
- **Implemented & Verified (Screens 00A–00F, 01–11, 15–18):**
  - **Flow 00 Auth:** Sign In, Create Account, Verify Code OTP, Forgot Password, Reset Link Sent, and Deep Link Recovery are fully functional with Supabase GoTrue backend.
  - **Flow 01 Onboarding (Screens 01–05):** Five-stage setup sequence (`Welcome`, `What Stillfeed Blocks`, `Permissions`, `Daily Allowance`, `Setup Complete`) is fully operational.
  - **Flow 02 Core Experience (Screens 06–08):** `Home Day` and `Home Evening` are implemented with time-of-day awareness and hero metric protection. `Screen 08 Blocked Reel` is implemented inside `InterventionActivity`.
  - **Flow 03 Reset/Reflect/Unlock (Screens 09–11):** `Reset Activity` (with 30s timer), `Awareness Cards` (reflective deck), and `Unlock Selection` (5/10/20/30 batches) are implemented inside `InterventionActivity`.
  - **Flow 04 Edge States (Screens 15–18):** `Batch Finished` (Screen 15), `Permission Disabled` (Screen 16), `Offline` (Screen 17), and `First Day` (Screen 18) are implemented as guarded routes.

### Missing & Actionable Screens (Screens 12, 13, 14, 19–34)
- **Screen 12 (Insights):** Partially implemented; needs direct navigation links to Reports (Screens 19, 20, 21) and visual token refinement.
- **Screen 13 (Settings):** Partially implemented as a monolithic list; needs hierarchical navigation links to subpages (Screens 22, 23, 25, 26, 31, 32).
- **Screen 14 (Reel from DM Blocked):** Missing dedicated DM intervention screen (`Hero: "This reel is still paused"`, `CTA: "Back to messages"`).
- **Screens 19, 20, 21 (Reports):** `Weekly Report`, `Monthly Review`, and `Yearly Report` are present as detailed Figma frames in `FLOW 05 — Reports` (`146:35`) but missing in Compose.
- **Screens 22, 23, 24 (Account & Privacy):** `Profile & Security`, `Privacy Center`, and `Data Export` are present as detailed Figma frames in `FLOW 06 — Account / Privacy` (`146:37`) but missing full Compose implementations.
- **Screen 25 (App Lock):** Present as detailed Figma frame in `FLOW 07 — Security / Support` (`130:190`) with Biometric + 4-digit PIN options; missing in Compose.
- **Screens 26, 27, 28, 29 (Support, Deletion, Edit Profile):** Defined as roadmap cards in `03 — Final Product Map` (`99:243`) without separate Figma mockup frames; must be implemented using authentic STILLFEED design tokens and Supabase architecture.
- **Screens 30, 31, 32, 33, 34 (Supporting Settings):** Present as detailed Figma frames in `FLOW 09 — Supporting Settings States` (`153:35`): `Change Password`, `Notifications`, `Appearance`, `Active Sessions`, and `Data Export Ready`.

---

## 2. Complete 37-Design Figma Cross-Check Matrix

| # | Screen Name | Figma Node ID | Figma Frame Type | Kotlin / Compose File | Implementation Status | Navigation Registered | Interactive Reachability | Functional Truth | Visual Match | Action Required |
|---|---|---|---|---|---|---|---|---|---|---|
| **00A** | Sign In | `289:307` | Detailed Frame | `auth/ui/SignInScreen.kt` | IMPLEMENTED | YES (`Screen.Auth`) | YES (Initial unauth) | PASS (Supabase GoTrue) | PASS (Exact Figma tokens) | Reused |
| **00A-L**| Sign In Loading | `289:375` | Detailed Frame | `auth/ui/AuthLoadingSplash.kt` | IMPLEMENTED | YES (AuthGate state) | YES (Cold launch restore) | PASS (Session restore) | PASS (Calm spinner) | Reused |
| **00A-E**| Sign In Error | `289:443` | Detailed Frame | `auth/ui/SignInScreen.kt` | IMPLEMENTED | YES (Inline banner) | YES (On auth error) | PASS (Supabase error) | PASS (Crimson banner) | Reused |
| **00B** | Create Account | `289:517` | Detailed Frame | `auth/ui/CreateAccountScreen.kt` | IMPLEMENTED | YES (Auth navigation) | YES (Toggle from 00A) | PASS (GoTrue signup) | PASS (Exact Figma tokens) | Reused |
| **00B-E**| Create Account Validation | `289:595` | Detailed Frame | `auth/ui/CreateAccountScreen.kt` | IMPLEMENTED | YES (Inline validation) | YES (On form change) | PASS (Complexity check) | PASS (Validation helper) | Reused |
| **00C** | Verify Code OTP | `289:674` | Detailed Frame | `auth/ui/VerifyCodeScreen.kt` | IMPLEMENTED | YES (Auth navigation) | YES (Post-signup) | PASS (GoTrue OTP verify)| PASS (6-digit boxes) | Reused |
| **00C-E**| Verify Code Wrong Code | `289:719` | Detailed Frame | `auth/ui/VerifyCodeScreen.kt` | IMPLEMENTED | YES (Inline alert) | YES (On bad OTP) | PASS (Shake + error) | PASS (Exact styling) | Reused |
| **00D** | Forgot Password | `289:770` | Detailed Frame | `auth/ui/ForgotPasswordScreen.kt` | IMPLEMENTED | YES (Auth navigation) | YES (From 00A CTA) | PASS (Password recovery)| PASS (Exact tokens) | Reused |
| **00E** | Reset Link Sent | `289:808` | Detailed Frame | `auth/ui/ResetLinkSentScreen.kt` | IMPLEMENTED | YES (Auth navigation) | YES (Post-recovery) | PASS (Confirmation) | PASS (Card layout) | Reused |
| **00F** | Signed In Preview | `302:572` | Detailed Frame | `core/navigation/StillfeedNavHost.kt` | IMPLEMENTED | YES (Session gate) | YES (Signed in state) | PASS (User session) | PASS (Transitions to App)| Reused |
| **01** | Welcome | `39:2` | Detailed Frame | `feature/onboarding/steps/WelcomeStep.kt` | IMPLEMENTED | YES (`Screen.Onboarding`) | YES (First-run step 0) | PASS (Step progression) | PASS (Illustration & copy)| Reused |
| **02** | What STILLFEED Blocks | `39:23` | Detailed Frame | `feature/onboarding/steps/WhatStillfeedBlocksStep.kt` | IMPLEMENTED | YES (Onboarding step 1) | YES (Step 0 continue) | PASS (Block explainer) | PASS (Blocked vs Kept) | Reused |
| **03** | Permissions | `39:41` | Detailed Frame | `feature/onboarding/steps/PermissionsStep.kt` | IMPLEMENTED | YES (Onboarding step 2) | YES (Step 1 continue) | PASS (System intents) | PASS (Permission rows) | Reused |
| **04** | Daily Allowance | `39:64` | Detailed Frame | `feature/onboarding/steps/DailyAllowanceStep.kt` | IMPLEMENTED | YES (Onboarding step 3) | YES (Step 2 continue) | PASS (DataStore saved) | PASS (30 reels default) | Reused |
| **05** | Setup Complete | `39:84` | Detailed Frame | `feature/onboarding/steps/SetupCompleteStep.kt` | IMPLEMENTED | YES (Onboarding step 4) | YES (Step 3 continue) | PASS (Finish onboarding)| PASS (Summary card) | Reused |
| **06** | Home (Day) | `39:103` | Detailed Frame | `feature/home/HomeScreen.kt` | IMPLEMENTED | YES (`Screen.Home`) | YES (Main tab) | PASS (UsageAggregator) | PASS (Sun companion) | Reused |
| **07** | Home Evening | `39:137` | Detailed Frame | `feature/home/HomeScreen.kt` | IMPLEMENTED | YES (`Screen.Home`) | YES (Main tab evening) | PASS (Time-aware layout)| PASS (Moon companion) | Reused |
| **08** | Blocked Reel | `39:162` | Detailed Frame | `protection/intervention/ui/Screen08BlockedReel.kt` | IMPLEMENTED | YES (`InterventionActivity`) | YES (On Reel intercept)| PASS (Intervention logic)| PASS (Blocked moment) | Reused |
| **09** | Reset Activity | `64:7` | Detailed Frame | `protection/intervention/ui/Screen09ResetActivity.kt` | IMPLEMENTED | YES (`InterventionActivity`) | YES (From 08 or Home) | PASS (30s timer) | PASS (Reset activities) | Reused |
| **10** | Awareness Cards | `64:46` | Detailed Frame | `protection/intervention/ui/Screen10AwarenessCards.kt` | IMPLEMENTED | YES (`InterventionActivity`) | YES (From 09 completion)| PASS (Card deck nav) | PASS (Reflective cards) | Reused |
| **11** | Unlock | `64:65` | Detailed Frame | `protection/intervention/ui/Screen11UnlockSelection.kt` | IMPLEMENTED | YES (`InterventionActivity`) | YES (From 10 completion)| PASS (Room v3 sessions) | PASS (Batch choices) | Reused |
| **12** | Insights | `70:113` | Detailed Frame | `feature/insights/InsightsScreen.kt` | PARTIAL | YES (`Screen.Insights`) | YES (Main tab) | PASS (Historical data) | PARTIAL (Needs report links) | Wire links to 19, 20, 21 |
| **13** | Settings | `70:149` | Detailed Frame | `feature/settings/SettingsScreen.kt` | PARTIAL | YES (`Screen.Settings`) | YES (Main tab) | PASS (Preferences) | PARTIAL (Monolithic list) | Add subpage navigation |
| **14** | Reel from DM Blocked | `70:184` | Detailed Frame | `protection/intervention/ui/Screen14DmReelBlocked.kt` | MISSING | NO | NO (Generic 08 shown) | PASS (Classifier detects)| MISSING | Create dedicated 14 screen |
| **15** | Daily Limit / Batch Done | `70:198` | Detailed Frame | `feature/edgestates/BatchFinishedScreen.kt` | IMPLEMENTED | YES (`Screen.BatchFinished` & Interv) | YES (On batch exhaust) | PASS (Guarded route) | PASS (Batch finish card) | Reused |
| **16** | Permission Disabled | `70:221` | Detailed Frame | `feature/edgestates/PermissionDisabledScreen.kt` | IMPLEMENTED | YES (`Screen.PermissionDisabled`) | YES (From Home banner) | PASS (Intent launchers) | PASS (Status warning) | Reused |
| **17** | No Internet / Offline | `70:241` | Detailed Frame | `feature/edgestates/OfflineScreen.kt` | IMPLEMENTED | YES (`Screen.Offline`) | YES (From Home offline card)| PASS (Connectivity obs)| PASS (Offline status) | Reused |
| **18** | First Day | `70:261` | Detailed Frame | `feature/edgestates/FirstDayScreen.kt` | IMPLEMENTED | YES (`Screen.FirstDay`) | YES (From Home first day) | PASS (Day-boundary logic)| PASS (Empty state guide)| Reused |
| **19** | Weekly Report | `130:19` | Detailed Frame | `feature/reports/WeeklyReportScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 19 |
| **20** | Monthly Review | `130:55` | Detailed Frame | `feature/reports/MonthlyReviewScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 20 |
| **21** | Yearly Report | `130:85` | Detailed Frame | `feature/reports/YearlyReportScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 21 |
| **22** | Profile & Security | `130:111` | Detailed Frame | `feature/settings/subscreens/ProfileSecurityScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 22 |
| **23** | Privacy Center | `130:139` | Detailed Frame | `feature/settings/subscreens/PrivacyCenterScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 23 |
| **24** | Data Export | `130:172` | Detailed Frame | `feature/settings/subscreens/DataExportScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 24 |
| **25** | App Lock | `130:190` | Detailed Frame | `feature/settings/subscreens/AppLockScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 25 |
| **26** | Help & WhatsApp | `147:30` | Roadmap Card | `feature/settings/subscreens/SupportScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 26 |
| **27** | Delete Account | `147:34` | Roadmap Card | `feature/settings/subscreens/DeleteAccountScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 27 |
| **28** | Deletion Pending | `147:38` | Roadmap Card | `feature/settings/subscreens/DeletionPendingScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 28 |
| **29** | Edit Profile | `153:202` | Roadmap Card | `feature/settings/subscreens/EditProfileScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 29 |
| **30** | Change Password | `153:40` | Detailed Frame | `feature/settings/subscreens/ChangePasswordScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 30 |
| **31** | Notifications | `153:43` | Detailed Frame | `feature/settings/subscreens/NotificationSettingsScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 31 |
| **32** | Appearance | `153:46` | Detailed Frame | `feature/settings/subscreens/AppearanceSettingsScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 32 |
| **33** | Active Sessions | `153:49` | Detailed Frame | `feature/settings/subscreens/ActiveSessionsScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 33 |
| **34** | Data Export Ready | `153:52` | Detailed Frame | `feature/settings/subscreens/DataExportReadyScreen.kt` | MISSING | NO | NO | NOT IMPLEMENTED | MISSING | Create Screen 34 |

---

## 3. Detailed Breakdown of Missing Screens & Exact Design Specifications

### FLOW 04: Screen 14 — Reel from DM Blocked (`70:184`)
- **Figma Hierarchy:**
  - Header: `FROM A MESSAGE` / `EDGE`
  - Hero: `"This reel is still paused."`
  - Body: `"You can keep chatting and viewing Stories. Short video stays blocked, even when it arrives through a DM."`
  - Status Badge: `pause`
  - Block Card: `"Reel blocked"`, `"Your message is still here."`
  - Primary CTA: `"Back to messages"`
  - Note: `"No accidental detours into short video."`
- **Functional Requirements:**
  - Placed in `InterventionActivity` when short video is opened from an Instagram Direct Message conversation (`surfaceCategory == "InstagramDirect"` or `surfaceCategory == "DirectMessage"`).
  - Tapping `"Back to messages"` exits the intervention activity with `finish()`, returning directly to Instagram DM.

### FLOW 05: Reports (Screens 19, 20, 21)
#### Screen 19 — Weekly Report (`130:19`)
- **Header:** `SKILLFEED` · `REPORTS` · `01 / 03`
- **Title:** `"Your week"`, Subtitle: `"What changed this week."`
- **Hero Card:**
  - Headline: Honest reel metric (or `"―"` if real reel counting is unavailable).
  - Subtitle: `"reels watched"`
  - Trend badge: `"↓ 18% vs last week"` (derived from real week-over-week monitored minutes comparison).
  - 7-day pattern bar chart: Mon through Sun.
- **Context Items:**
  - `"Best day · Tuesday"` (lowest usage day).
  - `"Most improved · Saturday"` (biggest reduction day).
- **Navigation:** Accessible from `InsightsScreen` ("Weekly Report" card / banner).

#### Screen 20 — Monthly Review (`130:55`)
- **Header:** `SKILLFEED` · `REPORTS` · `02 / 03`
- **Title:** `"Next month"`, Subtitle: `"Choose a pace that feels realistic."`
- **Monthly Card:** `"12% lower"`, `"Reel use vs recent pattern"`.
- **Allowance Options:**
  - `Gentle`: 5% lower (29 reels)
  - `Balanced`: 10% lower (27 reels)
  - `Focused`: 15% lower (26 reels)
  - `Challenge`: 20% lower (24 reels)
- **Footer Note:** `"Your limit changes once per month, after review."`
- **CTA:** `"Use this limit"` (persists selected daily allowance to `AppPreferencesRepository`).
- **Navigation:** Accessible from `InsightsScreen` ("Monthly Review" card) and `SettingsScreen`.

#### Screen 21 — Yearly Report (`130:85`)
- **Header:** `SKILLFEED` · `REPORTS` · `03 / 03`
- **Title:** `"Your year"`, Subtitle: `"The long-term pattern matters."`
- **Hero Card:**
  - Metric: Monitored total reels/hours with honest fallback.
  - Trend: 12-month summary chart.
  - Highlights: `"Best month · October"`, `"22% below January pattern"`.
  - Insight: `"Second-half usage stayed consistently lower than your first half."`
- **Navigation:** Accessible from `InsightsScreen`.

### FLOW 06: Account & Privacy (Screens 22, 23, 24)
#### Screen 22 — Profile & Security (`130:111`)
- **Header:** `SKILLFEED` · `ACCOUNT` · `01 / 03`
- **Title:** `"Make it yours"`, Subtitle: `"Personal details stay under your control."`
- **Interactive Rows:**
  - `"Your name"` → Navigates to Screen 29 (`Edit Profile`).
  - `"Email"` → Displays current user email from Supabase GoTrue.
  - `"Phone"` → Shows phone number / "Add phone" row.
  - `"Password"` → Navigates to Screen 30 (`Change Password`).
  - `"Sessions"` → Navigates to Screen 33 (`Active Sessions`).
- **CTA:** `"Save changes"`

#### Screen 23 — Privacy Center (`130:139`)
- **Header:** `SKILLFEED` · `PRIVACY` · `01 / 04`
- **Title:** `"Your privacy"`, Subtitle: `"What we store · why we need it · how long we keep it."`
- **Category Rows:**
  - `Account`: `"Name, photo, email, phone"` (Stored in Supabase auth).
  - `Reel usage`: `"Used for awareness + reports"` (Stored locally on-device in Room).
  - `Reports`: `"Weekly · monthly · yearly"` (Computed on-device).
  - `Support`: `"Feedback + support IDs"` (Transient).
- **Destructive & Export CTAs:**
  - `"Download my data"` → Navigates to Screen 24 (`Data Export`).
  - `"Delete my account"` → Navigates to Screen 27 (`Delete Account`).

#### Screen 24 — Data Export (`130:172`)
- **Header:** `SKILLFEED` · `PRIVACY` · `02 / 04`
- **Title:** `"Download your data"`, Subtitle: `"A copy you can keep, review, or share."`
- **Identity Check:** Prompts for account confirmation.
- **Export Options:**
  - Complete data export (ZIP: account, usage events, sessions, preferences).
  - Readable summary (JSON/HTML/PDF overview).
- **CTA:** `"Prepare export"` (triggers `DataExportService`, generates archive, and navigates to Screen 34 `Data Export Ready`).
- **Note:** `"The download link expires after 24 hours."`

### FLOW 07: Security / Support (Screens 25, 26)
#### Screen 25 — App Lock (`130:190`)
- **Header:** `SKILLFEED` · `SECURITY` · `01 / 02`
- **Title:** `"Keep it private"`, Subtitle: `"Use the device security you already trust."`
- **Settings Rows:**
  - `App Lock`: Toggle ON/OFF (persisted in preferences).
  - `Biometric unlock`: Toggle ON/OFF (uses Android `BiometricPrompt`).
  - `4-digit PIN`: Setup/Change PIN row.
  - `Auto-lock`: Timeout picker (`Immediately`, `1 min`, `5 min`).
  - `Smart behavior`: `"Stay unlocked while using. Phone lock or restart locks it."`
- **Recovery Link:** `"Forgot PIN? Recover with your SKILLFEED account →"`

#### Screen 26 — Help & WhatsApp (`147:30` Roadmap Card)
- **Header:** `SKILLFEED` · `SUPPORT`
- **Title:** `"Help & Support"`, Subtitle: `"We're here to help you build a calmer rhythm."`
- **Actions:**
  - `"Chat on WhatsApp"`: Launches `https://wa.me/...` intent.
  - `"Email Support"`: Launches `mailto:support@stillfeed.app` intent.
  - `"FAQ & Guides"`: Common questions on permissions, blocking, and allowances.
  - `"App Version & Diagnostics"`: Device and version information for support inquiries.

### FLOW 08: Deletion (Screens 27, 28)
#### Screen 27 — Delete Account (`147:34` Roadmap Card)
- **Header:** `SKILLFEED` · `ACCOUNT DELETION`
- **Title:** `"Delete your account"`, Subtitle: `"This action is permanent and cannot be undone."`
- **Warning Details:**
  - All profile data and synced preferences will be deleted.
  - Local usage history and unlock sessions on this phone will be cleared.
  - Protection guardrails will be turned off.
- **Confirmation:** Requires typing `"DELETE"` or entering current password.
- **Destructive Button:** `"Permanently delete account"` (executes Supabase deletion + local Room clear, navigates to Screen 28).

#### Screen 28 — Deletion Pending (`147:38` Roadmap Card)
- **Header:** `SKILLFEED` · `ACCOUNT`
- **Title:** `"Account deleted"`, Subtitle: `"Your data has been removed from STILLFEED."`
- **Status Card:** Confirms local storage purged, cloud session invalidated, accessibility protection deactivated.
- **CTA:** `"Exit STILLFEED"` (navigates to Sign In / closes application).

### FLOW 09: Supporting Settings States (Screens 29, 30, 31, 32, 33, 34)
#### Screen 29 — Edit Profile (`153:202` Roadmap Card)
- **Title:** `"Edit Profile"`
- **Fields:** Display name text field, avatar image selector / initials fallback.
- **Action:** `"Save changes"` (updates Supabase `user_metadata` and local cache).

#### Screen 30 — Change Password (`153:40`)
- **Header:** `SKILLFEED` · `Security`
- **Title:** `"Password"`, Subtitle: `"Use a strong password you can remember."`
- **Inputs:** Current password, New password, Confirm password (with password visibility toggles).
- **Strength Indicator:** `"Strong"` (evaluates length 12+, numbers, symbols).
- **CTA:** `"Update password"` (calls Supabase `updateUser` with new password).

#### Screen 31 — Notifications (`153:43`)
- **Header:** `SKILLFEED` · `Settings`
- **Title:** `"Notifications"`, Subtitle: `"Choose what deserves your attention."`
- **Toggles:**
  - `Daily summary`: `"End-of-day usage and reflection"` (Toggle).
  - `Weekly summary`: `"Weekly progress report"` (Toggle).
  - `Activity reminders`: `"Gentle reminders to reset"` (Toggle).
  - `Important account alerts`: `"Security and account changes"` (`ALWAYS ON` badge).
  - `Quiet hours`: `"11:00 PM – 7:00 AM"`.

#### Screen 32 — Appearance (`153:46`)
- **Header:** `SKILLFEED` · `SETTINGS`
- **Title:** `"Appearance"`, Subtitle: `"Choose how Stillfeed should look."`
- **Theme Cards:**
  - `Light`: `"Warm paper"` (`#F7F4EE`)
  - `Dark`: `"Low-light"` (`#1A1918`)
  - `System`: `"Follow device"` (Default selected with `✓`)
- **Note:** `"Changes apply instantly. No save button needed."`

#### Screen 33 — Active Sessions (`153:49`)
- **Header:** `SKILLFEED` · `Security`
- **Title:** `"Active sessions"`, Subtitle: `"See where your account is signed in."`
- **Session List:**
  - `Current device`: `"This device · active now"` (Badge: `ACTIVE`).
  - Prior/other sessions with last active time and `"Log out"` button.
- **Bulk Action:** `"Log out all other devices"`.
- **Note:** `"Logging out another device does not change your current session."`

#### Screen 34 — Data Export Ready (`153:52`)
- **Header:** `SKILLFEED` · `Privacy`
- **Title:** `"Export ready"`, Subtitle: `"Download the files you requested."`
- **Export Badge:** `EXPORT READY` · `"Created just now"` · `"Link expires in 24 hours"`.
- **Downloads:**
  - `Complete data export`: `"ZIP · account · usage · reports · support"` → `"Download"` (Android Share Sheet / file intent).
  - `Readable summary`: `"PDF · easy-to-read overview"` → `"Download"`.
- **Secondary CTA:** `"Generate another export"`.

---

## 4. Implementation Strategy & Architecture

1. **New Feature Packages:**
   - `com.stillfeed.app.feature.reports`: Screens 19 (`WeeklyReportScreen`), 20 (`MonthlyReviewScreen`), 21 (`YearlyReportScreen`).
   - `com.stillfeed.app.feature.settings.subscreens`:
     - Screen 22: `ProfileSecurityScreen.kt`
     - Screen 23: `PrivacyCenterScreen.kt`
     - Screen 24: `DataExportScreen.kt`
     - Screen 25: `AppLockScreen.kt`
     - Screen 26: `SupportScreen.kt`
     - Screen 27: `DeleteAccountScreen.kt`
     - Screen 28: `DeletionPendingScreen.kt`
     - Screen 29: `EditProfileScreen.kt`
     - Screen 30: `ChangePasswordScreen.kt`
     - Screen 31: `NotificationSettingsScreen.kt`
     - Screen 32: `AppearanceSettingsScreen.kt`
     - Screen 33: `ActiveSessionsScreen.kt`
     - Screen 34: `DataExportReadyScreen.kt`
   - `com.stillfeed.app.protection.intervention.ui`:
     - Screen 14: `Screen14DmReelBlocked.kt` wired into `InterventionActivity` for DM short-form attempts.

2. **Navigation Graph Updates (`Screen.kt` & `StillfeedNavHost.kt`):**
   - Register all 19 missing routes.
   - Connect Settings to all sub-destinations.
   - Connect Insights to all 3 report destinations.
   - Connect Privacy Center to Data Export, Data Export Ready, and Delete Account.

3. **Metrics Honesty:**
   - Retain neutral `―` placeholder whenever discrete reel counts cannot be accurately verified. Never fabricate reel counters.

4. **Automated and Device Testing:**
   - Add unit tests for report computation, settings state changes, app lock preferences, password update validation, and data export.
   - Verify on physical Motorola device (`ZA222WTN4H`).
