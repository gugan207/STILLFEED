package com.stillfeed.app.protection.detection

import com.stillfeed.app.domain.model.SurfaceCategory
import com.stillfeed.app.protection.model.AccessibilityWindowContext
import com.stillfeed.app.protection.model.SurfaceClassification

/**
 * Composite surface classifier that delegates to app-specific classifiers
 * (e.g. [InstagramSurfaceClassifier], [YouTubeSurfaceClassifier]) based on package name.
 *
 * If the package is not monitored or no classifier matches, returns [SurfaceClassification.allowed]
 * ensuring safe zero-false-positive operation on all other applications.
 */
class CompositeSurfaceClassifier(
    private val classifiers: List<SurfaceClassifier> = listOf(
        InstagramSurfaceClassifier(),
        YouTubeSurfaceClassifier()
    )
) : SurfaceClassifier {

    override fun canHandle(packageName: String): Boolean =
        classifiers.any { it.canHandle(packageName) }

    override fun classify(context: AccessibilityWindowContext): SurfaceClassification {
        val matchingClassifier = classifiers.firstOrNull { it.canHandle(context.packageName) }
            ?: return SurfaceClassification.allowed(
                surfaceCategory = SurfaceCategory.UNKNOWN,
                signal = "Unmonitored package '${context.packageName}' allowed"
            )

        return matchingClassifier.classify(context)
    }
}
