package com.stillfeed.app.feature.navigation

import com.stillfeed.app.core.navigation.Screen
import com.stillfeed.app.core.ui.components.StillfeedNavDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests verifying route definitions and navigation integrity for STEP 09.
 */
class HomeNavigationTest {

    @Test
    fun routes_areDistinctAndValid() {
        val routes = listOf(
            Screen.Auth.route,
            Screen.Bootstrap.route,
            Screen.Onboarding.route,
            Screen.Home.route,
            Screen.AwarenessSelection.route,
            Screen.Insights.route,
            Screen.Settings.route
        )

        // All routes must be non-empty and unique
        routes.forEach { route ->
            assertNotNull(route)
            assert(route.isNotBlank())
        }

        assertEquals("All screen routes must be unique", routes.size, routes.toSet().size)
    }

    @Test
    fun awarenessSelectionRoute_matchesContract() {
        assertEquals("awareness_selection", Screen.AwarenessSelection.route)
        assertEquals("home", Screen.Home.route)
        assertEquals("insights", Screen.Insights.route)
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun bottomNavigationDestinations_containHomeInsightsSettings() {
        val destinations = StillfeedNavDestination.values().toList()
        assertEquals(3, destinations.size)
        assertEquals(StillfeedNavDestination.Home, destinations[0])
        assertEquals(StillfeedNavDestination.Insights, destinations[1])
        assertEquals(StillfeedNavDestination.Settings, destinations[2])

        assertEquals("Home", StillfeedNavDestination.Home.label)
        assertEquals("Insights", StillfeedNavDestination.Insights.label)
        assertEquals("Settings", StillfeedNavDestination.Settings.label)
    }

    @Test
    fun settingsRoute_isDistinctFromHomeAndAuth() {
        assertNotEquals(Screen.Home.route, Screen.Settings.route)
        assertNotEquals(Screen.Auth.route, Screen.Settings.route)
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun bottomNavigation_primaryTabs_mapDirectlyToScreens() {
        val mapping = mapOf(
            StillfeedNavDestination.Home to Screen.Home.route,
            StillfeedNavDestination.Insights to Screen.Insights.route,
            StillfeedNavDestination.Settings to Screen.Settings.route
        )
        assertEquals(3, mapping.size)
        assertEquals("home", mapping[StillfeedNavDestination.Home])
        assertEquals("insights", mapping[StillfeedNavDestination.Insights])
        assertEquals("settings", mapping[StillfeedNavDestination.Settings])
    }

    @Test
    fun tabNavigation_destinationResolution_anchorsAtHome() {
        val rootTab = Screen.Home.route
        val secondaryTabs = listOf(Screen.Insights.route, Screen.Settings.route)
        secondaryTabs.forEach { tabRoute ->
            assertNotEquals("Secondary tab must not be root tab", rootTab, tabRoute)
        }
    }
}
