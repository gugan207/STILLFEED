package com.stillfeed.app.domain.repository

import com.stillfeed.app.domain.model.RestoreDecision
import com.stillfeed.app.domain.model.RestoreExecutionResult

/**
 * Domain repository contract dedicated to evaluating and executing device-bound history restore.
 *
 * Core Product Policy (STEP 05):
 * - SAME DEVICE: Authenticated account + matching device-binding proof -> history restore permitted.
 * - DIFFERENT DEVICE: Authenticated account + non-matching device-binding proof -> history restore denied.
 * - UNKNOWN / INDETERMINATE DEVICE -> history restore denied.
 * - Never trusts account login alone to permit history restoration!
 */
interface DeviceBoundRestoreRepository {
    /**
     * Evaluates whether the currently authenticated user on this device is eligible to restore history.
     */
    suspend fun evaluateRestoreEligibility(userId: String): RestoreDecision

    /**
     * Executes history restore from remote storage into local Room database only if eligibility is permitted.
     */
    suspend fun executeDeviceBoundRestore(userId: String): RestoreExecutionResult
}
