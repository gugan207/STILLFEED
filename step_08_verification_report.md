# STEP 08 — MONTHLY AWARENESS MODEL VERIFICATION REPORT

---

## Executive Summary

STEP 08 establishes STILLFEED's monthly awareness and reduction model on top of the background usage-analysis foundation built in STEP 07. The architecture adheres to all product, mathematical, and ethical constraints:
1. **Calm, Non-Judgmental Semantics:** Strictly zero scores out of 100, zero grades, zero streak loss/penalties, and zero shame, guilt, failure, or "lazy" messaging.
2. **Deterministic Mathematical Model:** User-selected monthly reduction tier is strictly 5%, 10%, 15%, or 20%. Integer-safe arithmetic is used exclusively with zero floating-point imprecision.
3. **Robust Baseline Formulation:** Formed from a minimum of 7 completed calendar days, with in-flight days excluded, zero-usage days accounted for, and extreme outliers safely clamped.
4. **Strict Monthly Isolation:** Each calendar month is an immutable, isolated record. Current-month adjustments never alter past months or contaminate future baselines.
5. **Fail-Safe Protection Integration:** The awareness model provides data to the product layer but never weakens or bypasses STEP 06's real-time Reels and Shorts blocking. Blocking fails safely and remains active regardless of awareness state.

---

## 1. Monthly State Design

The monthly awareness domain model is defined in [`AwarenessMonthlyState.kt`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/domain/model/AwarenessMonthlyState.kt) and persisted in SQLite via Room entity [`AwarenessMonthlyEntity.kt`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/data/local/entity/AwarenessMonthlyEntity.kt):

```
+-------------------------------------------------------------------------------+
|                       AwarenessMonthlyState (Domain Model)                     |
+-------------------------------------------------------------------------------+
| - monthIdentifier: String                  (e.g., "2026-10")                  |
| - targetReductionPercentage: Int           (strictly 0, 5, 10, 15, or 20)      |
| - baselineMinutes: Int                     (integer minutes from >=7 days)     |
| - currentProgressMinutes: Int              (accumulated usage this month)      |
| - targetUsageMinutes: Int                  (max allowance = baseline - red.)   |
| - reductionAmountMinutes: Int              (reduction target in minutes)       |
| - status: MonthlyAwarenessStatus           (calm enum: BASELINE_FORMING, etc.)|
| - createdAtMillis: Long                    (creation epoch ms)                 |
| - updatedAtMillis: Long                    (last updated epoch ms)             |
+-------------------------------------------------------------------------------+
```

### Persistence & Schema Evolution:
* Database schema upgraded from `version = 1` to `version = 2` in [`StillfeedDatabase.kt`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/data/local/StillfeedDatabase.kt).
* Non-destructive migration [`MIGRATION_1_2`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/data/local/StillfeedDatabase.kt#L54-L61) adds `targetUsageMinutes`, `reductionAmountMinutes`, and `status` columns with safe default values.
* Room JSON schema generated at [`schemas/com.stillfeed.app.data.local.StillfeedDatabase/2.json`](file:///d:/SKILLFEED/app/schemas/com.stillfeed.app.data.local.StillfeedDatabase/2.json).

---

## 2. Percentage Rule

Defined in [`AwarenessPercentage.kt`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/domain/model/AwarenessPercentage.kt):

* Supported choices are strictly:
  * **5%** (`AwarenessPercentage.FIVE`)
  * **10%** (`AwarenessPercentage.TEN`)
  * **15%** (`AwarenessPercentage.FIFTEEN`)
  * **20%** (`AwarenessPercentage.TWENTY`)
* **Strict Validation:** Any invalid percentage (e.g., 0%, 7%, 25%, -5%, 100%) is immediately rejected with `IllegalArgumentException` by [`ReductionCalculator.validatePercentage(...)`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/awareness/reduction/ReductionCalculator.kt#L27-L34).
* **Monthly Locking:** The chosen percentage is fixed for the selected month record in Room. Mid-month changes must be explicitly initiated through the user product flow; past months are never retroactively recalculated.

---

## 3. Baseline Algorithm

Implemented in [`BaselineCalculator.kt`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/awareness/baseline/BaselineCalculator.kt):

1. **Required Days for Formation:**
   * Requires at least **7 completed calendar days** (`MIN_BASELINE_DAYS = 7`) of daily usage data.
   * If fewer than 7 days exist, `isSufficient = false`, `baselineMinutes = 0`, and status remains `MonthlyAwarenessStatus.BASELINE_FORMING`.
2. **Incomplete Days Handling:**
   * The active in-flight day (today) is strictly excluded via `todayDateString` to prevent partial-day bias.
3. **Zero-Usage Days Handling:**
   * Zero-usage days during active tracking are counted as 0 minutes in the average. If all 7 days have zero usage, baseline is 0 minutes.
4. **Outlier Clamping:**
   * Extreme days (e.g., accidental 18-hour autoplay) are clamped to `MAX_DAILY_USAGE_MINUTES` (720 minutes / 12 hours) and capped at $2.5 \times \text{median}$ of the candidate days.
5. **Monthly Recalculation & Locking:**
   * At month rollover, the baseline is computed from the preceding month's completed days (or prior 7–30 completed days). Once written to the month's record, it is fixed and does not drift daily.
6. **Zero Behavioral Labels:**
   * No labels like "heavy user", "binge", or "addicted". Pure mathematical facts.

---

## 4. Reduction Calculation

Implemented in [`ReductionCalculator.kt`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/awareness/reduction/ReductionCalculator.kt):

* **Formulas:**
  $$\text{reductionAmountMinutes} = \left\lfloor \frac{\text{baselineMinutes} \times \text{percentage} + 50}{100} \right\rfloor$$
  $$\text{targetUsageMinutes} = \max(0, \text{baselineMinutes} - \text{reductionAmountMinutes})$$
* **Integer Arithmetic:** Uses 64-bit `Long` intermediates (`baselineMinutes.toLong() * percentage`) to prevent 32-bit integer overflow.
* **Exact Rounding:** Integer half-up rounding guarantees $\text{targetUsageMinutes} + \text{reductionAmountMinutes} = \text{baselineMinutes}$.
* **Zero Floating-Point Imprecision:** Completely avoids IEEE 754 float drift (e.g., `0.30000000000000004`).

---

## 5. Month Isolation

Orchestrated in [`AwarenessManager.kt`](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/awareness/AwarenessManager.kt) and verified via Room DAO:

* **Primary Key Isolation:** Each calendar month is stored with a unique `monthIdentifier` (e.g. `"2026-09"`, `"2026-10"`).
* **Immutability of Historical Months:** Updating the current month's target or progress never updates or mutates records for previous months.
* **Future Data Protection:** Queries bound lookback windows to `nowMillis` and month boundaries; current month never reads future data.

---

## 6. Protection Engine Integration

Verified in [`ProtectionAwarenessIntegrationTest.kt`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/awareness/ProtectionAwarenessIntegrationTest.kt):

* **Product Invariant:** Instantaneous blocking of Instagram Reels and YouTube Shorts (STEP 06) is unconditional when protection is enabled.
* **Fail-Safe Behavior:** Even if:
  * awareness data is unavailable;
  * UsageStats permission is missing;
  * baseline is forming (`BASELINE_FORMING`);
  * monthly state cannot be loaded from Room;
  the protection engine continues to immediately issue `ProtectionDecision.Block` for Reels and Shorts. Awareness never weakens protection.

---

## 7. Edge-Case Handling

| Edge Case | Handling Strategy |
|---|---|
| **First-ever user with no history** | Returns `BASELINE_FORMING`, `baselineMinutes = 0`, `targetUsageMinutes = 0`. |
| **Fewer than 7 baseline days** | Baseline formation marked insufficient (`isSufficient = false`); safe no-op. |
| **All zero-usage days** | Returns 0 baseline minutes safely without division by zero. |
| **Missing UsageStats permission** | Returns status `DATA_UNAVAILABLE`. Protection remains 100% active. |
| **Month rollover** | Generates new isolated record with prior month's baseline; preserves prior month. |
| **Timezone / UTC boundary** | All month keys and timestamps standardized to UTC (`ZoneOffset.UTC`). |
| **Process recreation** | Restores state from SQLite Room table `awareness_monthly_state`. |
| **Large usage values** | 64-bit integer calculations prevent integer overflow. |

---

## 8. Privacy Review

Zero personal data is persisted or processed by the awareness model:
* **Persisted Data:** Month key (`"2026-10"`), integer percentages (5, 10, 15, 20), integer minutes (baseline, progress, target, reduction), status enum string, and epoch timestamps.
* **Forbidden Content:** Zero collection, storage, or transmission of chat messages, captions, search queries, screen pixels, camera, microphone, or credentials.

---

## 9. Exact Test Totals

All unit test suites executed with 100% success rate:

```
Test Results:
  Total Tests:    218
  Passed:         218
  Failed:           0
  Ignored:          0
  Pass Rate:     100%
  Duration:      28.57s
```

### Breakdown by Package:

| Package | Tests | Status | Description |
|---|---|---|---|
| `com.stillfeed.app.auth` | 32 | PASS | Account creation, login, validation, PBKDF2 hashing |
| `com.stillfeed.app.awareness` | **22** | **PASS** | **STEP 08 Percentages, baseline, reduction, manager, integration** |
| `com.stillfeed.app.data.auth` | 19 | PASS | Production auth repository, secure session storage |
| `com.stillfeed.app.data.local` | 6 | PASS | Room DAOs, entity migration, schema integrity |
| `com.stillfeed.app.data.local.device` | 6 | PASS | Android ID provider and device binding |
| `com.stillfeed.app.data.preferences` | 6 | PASS | DataStore app preferences |
| `com.stillfeed.app.data.remote` | 19 | PASS | Remote Supabase clients, restore authorization |
| `com.stillfeed.app.data.repository` | 41 | PASS | Local repositories, data deletion, restore flows |
| `com.stillfeed.app.protection` | 39 | PASS | STEP 06 Accessibility Service, classifiers, engine |
| `com.stillfeed.app.usage` | 28 | PASS | STEP 07 UsageStats, sessions, reconciler, worker |
| **Grand Total** | **218** | **PASS** | **Zero failures across the entire test suite** |

### Breakdown of STEP 08 Awareness Test Suites:

| Test Class | Tests | Status | Description |
|---|---|---|---|
| [`AwarenessPercentageTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/awareness/AwarenessPercentageTest.kt) | 2 | PASS | 5%, 10%, 15%, 20% validation; invalid percentage rejection |
| [`BaselineCalculatorTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/awareness/BaselineCalculatorTest.kt) | 6 | PASS | 7-day requirement, incomplete days, zero usage, outlier clamping |
| [`ReductionCalculatorTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/awareness/ReductionCalculatorTest.kt) | 6 | PASS | Target formula, reduction amounts, integer rounding, status states |
| [`AwarenessManagerTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/awareness/AwarenessManagerTest.kt) | 5 | PASS | Monthly creation, percentage locking, month isolation, permission handling |
| [`ProtectionAwarenessIntegrationTest`](file:///d:/SKILLFEED/app/src/test/java/com/stillfeed/app/awareness/ProtectionAwarenessIntegrationTest.kt) | 3 | PASS | Reels/Shorts blocking invariant when awareness forming or unavailable |
| **STEP 08 Total** | **22** | **PASS** | **All 22 new STEP 08 tests passing** |

---

## 10. Build Results

All Gradle verification tasks completed successfully:

* `compileDebugKotlin`: **SUCCESS** (Exit code 0)
* `compileDebugUnitTestKotlin`: **SUCCESS** (Exit code 0)
* `testDebugUnitTest`: **SUCCESS** (218 tests completed, 0 failures, exit code 0)
* `assembleDebug`: **SUCCESS** (Exit code 0, 46s build time)

---

## 11. Runtime Status

* **Host Environment:** Windows execution environment.
* **Physical Device / Emulator Runtime:** **N/A** (No physical Android device or active Android emulator attached; verified exhaustively via Robolectric SDK 34 runtime environment).

---

## 12. Exact Files & Classes Changed

### New Files Created:
1. `app/src/main/java/com/stillfeed/app/domain/model/AwarenessPercentage.kt` — Enum and validator for 5%, 10%, 15%, 20%.
2. `app/src/main/java/com/stillfeed/app/domain/model/MonthlyAwarenessStatus.kt` — Calm status enum (`BASELINE_FORMING`, `ON_TRACK`, `REDUCING`, `TARGET_REACHED`, `DATA_UNAVAILABLE`).
3. `app/src/main/java/com/stillfeed/app/awareness/baseline/BaselineCalculator.kt` — Deterministic baseline engine with 7-day rule, outlier capping, and incomplete-day filtering.
4. `app/src/main/java/com/stillfeed/app/awareness/reduction/ReductionCalculator.kt` — Pure domain reduction amount and target allowance calculator.
5. `app/src/main/java/com/stillfeed/app/awareness/AwarenessManager.kt` — Monthly awareness orchestrator with month isolation.
6. `app/src/test/java/com/stillfeed/app/awareness/AwarenessPercentageTest.kt` — 2 unit tests.
7. `app/src/test/java/com/stillfeed/app/awareness/BaselineCalculatorTest.kt` — 6 unit tests.
8. `app/src/test/java/com/stillfeed/app/awareness/ReductionCalculatorTest.kt` — 6 unit tests.
9. `app/src/test/java/com/stillfeed/app/awareness/AwarenessManagerTest.kt` — 5 unit tests.
10. `app/src/test/java/com/stillfeed/app/awareness/ProtectionAwarenessIntegrationTest.kt` — 3 unit tests.
11. `app/schemas/com.stillfeed.app.data.local.StillfeedDatabase/2.json` — Room version 2 schema JSON.
12. `Step Reports/step_08_verification_report.md` — STEP 08 verification report.

### Existing Files Modified:
1. `app/src/main/java/com/stillfeed/app/domain/model/AwarenessMonthlyState.kt` — Added `targetUsageMinutes`, `reductionAmountMinutes`, `status`, and helper formulas.
2. `app/src/main/java/com/stillfeed/app/data/local/entity/AwarenessMonthlyEntity.kt` — Added Room columns for target usage, reduction amount, and status.
3. `app/src/main/java/com/stillfeed/app/data/local/mapper/AwarenessMonthlyMapper.kt` — Bidirectional entity-domain mapping with safe fallbacks.
4. `app/src/main/java/com/stillfeed/app/data/local/StillfeedDatabase.kt` — Upgraded to schema version 2 with `MIGRATION_1_2`.
5. `app/src/main/java/com/stillfeed/app/StillfeedApplication.kt` — Wired `awarenessManager` lazy singleton.
6. `app/src/main/java/com/stillfeed/app/usage/worker/UsageAnalysisWorker.kt` — Automatically refreshes monthly awareness progress upon sync completion.
7. `app/src/test/java/com/stillfeed/app/data/local/RoomDaoTest.kt` — Verified schema version 2 columns on SQLite.

---

## 13. Known Limitations

1. **Mid-Month Initial Setup:**
   * If a user installs the application halfway through a calendar month, their first month's baseline will be marked `BASELINE_FORMING` until 7 full calendar days of usage history are recorded.
2. **Fixed Monthly Awareness Percentage:**
   * In accordance with product rules, the awareness percentage cannot be silently altered; once selected for a month, it remains locked for that calendar month record unless changed via the dedicated user settings flow.

---

## Conclusion & Verification Verdict

STEP 08 satisfies all architectural, mathematical, and ethical requirements. The awareness model deterministically handles 5%, 10%, 15%, and 20% tiers; isolates each calendar month; calculates integer-safe baselines and targets; presents calm, non-judgmental semantics; maintains strict Reel/Short protection invariants; and passes all 218 unit tests with 100% success.

**STEP 08 STATUS: PASS**
