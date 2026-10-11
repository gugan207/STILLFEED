# STEP 04 — LOCAL PERSISTENCE & DEVICE-BINDING REMEDIATION REPORT

## 1. Technical Audit of Previous Step 04 Architecture & Flaws Identified

During the Step 04 architectural audit, a critical platform flaw was identified in the initial device-binding implementation:

| Inspected Component | Previous Flawed Implementation | Actual Android OS Behavior | Security & Product Impact |
|---|---|---|---|
| `AppScopedDeviceIdentityProvider` | Generated a UUID and stored it in private `SharedPreferences("stillfeed_device_identity")`. | Android OS unconditionally erases all private app storage (`/data/data/com.stillfeed.app/`) upon app uninstallation. | **Broken same-device restore:** On reinstall on the *same device*, the stored UUID is gone. The app generated a new random UUID and reported `MISMATCH`, failing to verify the same device. |
| Android Auto Backup (`android:allowBackup="true"`) | Default configuration with empty backup rules. | Google Cloud Backup and Device-to-Device transfer copy private storage to Google Drive and transfer it to a **new phone**. | **Violated product rule:** Relational databases and private files would automatically leak to a *different phone*, violating the non-negotiable rule that history must *never* restore across devices. |
| Invalidation of Device Guarantee | Claimed app-private storage alone could guarantee same-device restore. | No app-private file survives uninstall/reinstall without platform/backup assistance. | **False architectural guarantee:** Claiming an app-private UUID survives uninstall was technically untrue. |

---

## 2. Investigation of Android Platform Mechanisms & Trade-Offs

To establish a technically sound architecture that does not make false guarantees, the following Android-supported mechanisms were audited against the product requirement:

### Mechanism A: Android Scoped `Settings.Secure.ANDROID_ID` (Selected Default)
* **Platform Specification (Android 8.0+ / API 26+)**: Android 8.0 introduced app-scoped `ANDROID_ID`, unique per combination of (app signing key, device user, and physical device).
* **Same-Device Reinstall Behavior**: On the **same device**, under the same user profile and app signing key, `ANDROID_ID` remains stable and **survives app uninstallation and reinstallation** without requiring cloud backup.
* **New-Device Behavior**: On a **different device**, `ANDROID_ID` is a completely distinct 64-bit value. Verification fails with `MismatchDifferentDevice`.
* **Platform Limitations**:
  * Changes if the user executes a factory reset of the device.
  * Differs across work/secondary user profiles on the same hardware.
  * Can be `null` or a dummy placeholder (`9774d56d682e549c`) on certain broken emulator runtimes.
  * To prevent PII leakage, the raw OS identifier must never be stored directly; STILLFEED derives a salt-keyed `SHA-256` token (`dev_bind_<hex>`).

### Mechanism B: Google Play Services Block Store (Documented Architecture Target)
* **Platform Specification**: Google Play Services Block Store (`com.google.android.gms.auth.blockstore`) allows apps to store encrypted byte tokens with Play Services on the device.
* **Configuration**: Calling `StoreBytesData.Builder.setShouldBackupToCloud(false)` explicitly confines the token to the **local device**, preventing cloud sync.
* **Same-Device Reinstall Behavior**: Survives uninstallation and reinstallation on the same phone via Play Services cache.
* **New-Device Behavior**: Because cloud backup is disabled, the token is not transferred to a new phone.
* **Platform Limitations**: Requires Google Play Services; non-functional on de-Googled AOSP distributions or devices without GMS.

### Mechanism C: Android Auto Backup Exclusion (`data_extraction_rules.xml` & `backup_rules.xml`)
* **Platform Remediation**: To guarantee that history does not leak to a new device via cloud backup or cable migration, `stillfeed.db` and auth session storage are explicitly excluded:
  ```xml
  <cloud-backup>
      <exclude domain="database" path="stillfeed.db" />
      <exclude domain="sharedpref" path="stillfeed_secure_auth_session.xml" />
  </cloud-backup>
  <device-transfer>
      <exclude domain="database" path="stillfeed.db" />
      <exclude domain="sharedpref" path="stillfeed_secure_auth_session.xml" />
  </device-transfer>
  ```

### Mechanism D: App-Private UUID Fallback
* **Platform Behavior**: Pure in-app UUID in private storage.
* **Durability**: Explicitly marked `isPlatformDurableAcrossReinstall = false`.
* **Contract**: Explicitly reports `IndeterminatePlatformLimitation`, **refusing to claim a false same-device guarantee** when durable platform signals are absent.

---

## 3. Remediated Device-Binding Architecture

The hard-coded identity implementation was replaced with a decoupled, provider-based architecture:

```
[UI / Auth ViewModel]
        ↓
[DeviceBindingRepository] (Domain Interface)
        ↓
[LocalDeviceBindingRepository] (Data Repository)
   ├── [DeviceBindingDao] (Room: records binding status & metadata)
   └── [DeviceBindingProvider] (Platform Provider Abstraction)
            ├── AndroidIdDeviceBindingProvider (Default: Scoped ANDROID_ID hash)
            ├── BlockStoreDeviceBindingProvider (Prepared future GMS provider)
            └── InstallationUuidDeviceBindingProvider (Non-durable fallback)
```

### Verification States (`DeviceBindingVerificationResult`)
* `MatchSameDevice`: Platform token reliably matches registered account binding token on this same physical device.
* `MismatchDifferentDevice`: Platform token belongs to a different device; cross-device restore is strictly blocked.
* `IndeterminatePlatformLimitation`: Platform signal is non-durable or unavailable; system explicitly halts restore and informs the caller rather than faking verification.

---

## 4. Verification & Test Suite

The test suite was expanded and executed via Gradle. All 72 unit tests passed with 100% success rate:

| Test Class | Test Case | Status | Verification Focus |
|---|---|---|---|
| `DeviceBindingRepositoryTest` | `firstInstallationBinding_initializesBindingAndMarksBound` | **PASS** | First install establishes binding and marks state `BOUND`. |
| `DeviceBindingRepositoryTest` | `normalSameInstallationMatch_returnsMatchSameDevice` | **PASS** | Active installation matches registered token; transitions to `VERIFIED_SAME_DEVICE`. |
| `DeviceBindingRepositoryTest` | `reinstallRestoreBehavior_supportedByChosenMechanism_restoresSameDeviceMatch` | **PASS** | Simulates uninstall where Room DB is wiped; durable platform token restores same-device match. |
| `DeviceBindingRepositoryTest` | `differentDeviceRejection_whenPlatformCanReliablyEnforceIt_rejectsWithMismatch` | **PASS** | Token from foreign phone is strictly rejected (`MismatchDifferentDevice`); prevents cross-device restore. |
| `DeviceBindingRepositoryTest` | `inabilityToClaimSameDeviceRestoration_whenRequiredPlatformSignalIsUnavailable_yieldsIndeterminate` | **PASS** | Non-durable mechanism explicitly yields `IndeterminatePlatformLimitation` instead of faking a guarantee. |
| `DeviceBindingRepositoryTest` | `androidIdDeviceBindingProvider_derivesAppScopedHash_andEnforcesMatching` | **PASS** | Verifies SHA-256 derived token on valid `ANDROID_ID`. |
| `DeviceBindingRepositoryTest` | `androidIdDeviceBindingProvider_whenAndroidIdUnavailable_returnsIndeterminateResult` | **PASS** | Detects dummy emulator ID (`9774d56d682e549c`) and yields indeterminate result. |
| `RoomDaoTest` | 6 test cases | **PASS** | All Room DAOs verified with in-memory SQLite under Robolectric. |
| `UserRepositoryTest` | 4 test cases | **PASS** | Insert, read, update, and persistence across repository recreation. |
| `ProtectionStateRepositoryTest` | 3 test cases | **PASS** | Default disabled, toggle enable/disable, persistence across recreation. |
| `UsageRepositoryTest` | 3 test cases | **PASS** | Privacy-minimal usage event insertion, date range queries, batch persistence. |
| `AwarenessRepositoryTest` | 3 test cases | **PASS** | Monthly state creation, update, and isolation across distinct months. |
| `UnlockHistoryRepositoryTest` | 2 test cases | **PASS** | Record unlock allowances and query by timestamp range. |
| `AppPreferencesRepositoryTest` | 6 test cases | **PASS** | Jetpack DataStore preferences, first-launch, onboarding state, reset. |
| `DataDeletionReadinessTest` | 6 test cases | **PASS** | Deterministic purging of user data, usage logs, unlock history, awareness, binding, preferences. |
| `Auth*Test` & `PasswordHasherTest` | 32 test cases | **PASS** | Retained STEP 03/03A/03B authentication test suite remains 100% passing. |

### Build Outputs
* `compileDebugKotlin`: **PASS** (0 errors, 0 warnings)
* `compileDebugUnitTestKotlin`: **PASS** (0 errors, 0 warnings)
* `testDebugUnitTest`: **PASS** (72 actionable tests executed, 0 failed, 0 skipped)
* `assembleDebug`: **PASS** (Debug APK generated at `app/build/outputs/apk/debug/app-debug.apk`, 11.6 MB)
* `Runtime / Device`: **N/A** (No physical device or running emulator attached via ADB)

---

## 5. Summary of Architecture Gates & Product Constraints

1. **Same-Device Reinstall**: Supported via [AndroidIdDeviceBindingProvider](file:///d:/SKILLFEED/app/src/main/java/com/stillfeed/app/data/local/device/DeviceBindingProvider.kt) (and future Block Store).
2. **Different-Device Restore Blocked**: Enforced both at application layer via `MismatchDifferentDevice` rejection and at Android OS layer via `data_extraction_rules.xml` and `backup_rules.xml` database exclusions.
3. **No False Guarantees**: Any scenario where platform durability is unavailable yields `IndeterminatePlatformLimitation`, explicitly blocking unauthorized history restores.
4. **Figma File Unmodified**: 0 changes made to Figma file `SW3qMYI0E3pnYALuExsdd8`.
5. **Authentication UI Unmodified**: 0 changes made to Auth UI components or screens.

---

## 6. Remaining Issues & Limitations

1. **Unresolved Cloud Architecture Requirement**:
   * Exact requirement: *"Remote/sync restoration is deferred until the production authentication/cloud architecture is selected."*
   * When production cloud auth (Firebase, Supabase, AWS Cognito, custom backend) is chosen, the remote sync service will query `DeviceBindingRepository` to verify the installation identifier before allowing history restoration.
2. **Protection Engine Boundaries**:
   * Accessibility Service, background usage analysis, active app detection (Instagram, YouTube), Reels/Shorts blocking, and monthly awareness algorithms (5%/10%/15%/20%) remain unbuilt as required until subsequent verified steps.
3. **Hardware Runtime**:
   * No physical device or running emulator was attached via ADB; runtime verification is recorded as N/A.

---

**STOP.** STEP 04 Remediation is complete and verified. STEP 05 has not been started.
