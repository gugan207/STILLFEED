package com.stillfeed.app.protection.detection

import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.SurfaceClassification

/**
 * Contract for identifying and classifying the visible application surface from an
 * [AccessibilityWindowContext] snapshot.
 */
interface SurfaceClassifier {
    /**
     * Returns true if this classifier handles the given [packageName].
     */
    fun canHandle(packageName: String): Boolean

    /**
     * Classifies the surface for the provided [context].
     */
    fun classify(context: AccessibilityWindowContext): SurfaceClassification
}
