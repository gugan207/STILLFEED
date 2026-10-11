package com.stillfeed.app.protection.permission

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.stillfeed.app.protection.accessibility.StillfeedAccessibilityService

/**
 * Domain-level contract for querying the system Accessibility Service permission state.
 */
interface AccessibilityPermissionChecker {
    fun isAccessibilityPermissionGranted(): Boolean
}

/**
 * Android implementation verifying whether [StillfeedAccessibilityService] is enabled in Secure Settings.
 */
class AndroidAccessibilityPermissionChecker(
    private val context: Context
) : AccessibilityPermissionChecker {

    override fun isAccessibilityPermissionGranted(): Boolean {
        val expectedService = "${context.packageName}/${StillfeedAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
