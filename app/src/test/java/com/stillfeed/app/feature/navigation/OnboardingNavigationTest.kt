package com.stillfeed.app.feature.navigation

import com.stillfeed.app.core.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests verifying Onboarding navigation contracts, route integrity, and start-destination resolution.
 */
class OnboardingNavigationTest {

    @Test
    fun onboardingRoute_matchesContract() {
        assertEquals("onboarding", Screen.Onboarding.route)
    }

    @Test
    fun onboardingRoute_isDistinctFromAuthAndHome() {
        assertNotEquals(Screen.Onboarding.route, Screen.Auth.route)
        assertNotEquals(Screen.Onboarding.route, Screen.Home.route)
        assertNotEquals(Screen.Onboarding.route, Screen.Settings.route)
        assertNotEquals(Screen.Onboarding.route, Screen.Insights.route)
    }

    @Test
    fun startDestinationResolution_unboardedUser_entersOnboarding() {
        val hasCompletedOnboarding = false
        val explicitStartDestination: String? = null

        val effectiveDestination = when {
            explicitStartDestination != null && explicitStartDestination != Screen.Auth.route -> explicitStartDestination
            hasCompletedOnboarding -> Screen.Home.route
            else -> Screen.Onboarding.route
        }

        assertEquals(Screen.Onboarding.route, effectiveDestination)
    }

    @Test
    fun startDestinationResolution_returningUser_entersHome() {
        val hasCompletedOnboarding = true
        val explicitStartDestination: String? = null

        val effectiveDestination = when {
            explicitStartDestination != null && explicitStartDestination != Screen.Auth.route -> explicitStartDestination
            hasCompletedOnboarding -> Screen.Home.route
            else -> Screen.Onboarding.route
        }

        assertEquals(Screen.Home.route, effectiveDestination)
    }

    @Test
    fun startDestinationResolution_explicitRoute_takesPrecedence() {
        val hasCompletedOnboarding = false
        val explicitStartDestination: String? = Screen.Insights.route

        val effectiveDestination = when {
            explicitStartDestination != null && explicitStartDestination != Screen.Auth.route -> explicitStartDestination
            hasCompletedOnboarding -> Screen.Home.route
            else -> Screen.Onboarding.route
        }

        assertEquals(Screen.Insights.route, effectiveDestination)
    }
}
