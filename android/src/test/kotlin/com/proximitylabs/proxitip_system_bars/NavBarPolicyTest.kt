package com.proximitylabs.proxitip_system_bars

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavBarPolicyTest {

    @Test
    fun defaultsWhenArgumentsEmpty() {
        val cfg = NavBarConfig.fromArguments(emptyMap<String, Any>())!!
        assertEquals(0xFF0D0D0F.toInt(), cfg.color)
        assertTrue(cfg.contrastEnforced)
        assertTrue(cfg.reapplyOnLifecycle)
        assertFalse(cfg.debugLogging)
    }

    @Test
    fun colorArrivingAsLongFromDartIsAccepted() {
        // Dart ints above 2^31 are delivered as java.lang.Long.
        val cfg = NavBarConfig.fromArguments(mapOf("color" to 0xFF0D0D0FL))!!
        assertEquals(0xFF0D0D0F.toInt(), cfg.color)
    }

    @Test
    fun explicitFlagsAreHonoured() {
        val cfg = NavBarConfig.fromArguments(
            mapOf(
                "color" to 0xFF000000L,
                "contrastEnforced" to false,
                "reapplyOnLifecycle" to false,
                "debugLogging" to true,
            ),
        )!!
        assertEquals(0xFF000000.toInt(), cfg.color)
        assertFalse(cfg.contrastEnforced)
        assertFalse(cfg.reapplyOnLifecycle)
        assertTrue(cfg.debugLogging)
    }

    @Test
    fun nonOpaqueOrMalformedArgumentsAreRejected() {
        assertNull(NavBarConfig.fromArguments(mapOf("color" to 0x800D0D0FL)))
        assertNull(NavBarConfig.fromArguments(mapOf("color" to 0x000D0D0FL)))
        assertNull(NavBarConfig.fromArguments(mapOf("color" to "#0D0D0F")))
        assertNull(NavBarConfig.fromArguments(null))
        assertNull(NavBarConfig.fromArguments(listOf(1, 2)))
    }

    @Test
    fun planOnAndroid16And15UsesEveryLever() {
        for (sdk in listOf(35, 36)) {
            val plan = NavBarPlan.forSdk(sdk)
            assertTrue(plan.setColor)
            assertTrue(plan.setDividerColor)
            assertTrue(plan.setContrast)
            assertTrue(plan.clearLegacyLightFlag)
            assertTrue(plan.clearAppearanceLightFlag)
        }
    }

    @Test
    fun planRespectsApiLevelsBelowR() {
        val p24 = NavBarPlan.forSdk(24)
        assertTrue(p24.setColor)
        assertFalse(p24.setDividerColor)
        assertFalse(p24.setContrast)
        assertFalse(p24.clearLegacyLightFlag)
        assertFalse(p24.clearAppearanceLightFlag)

        val p26 = NavBarPlan.forSdk(26)
        assertTrue(p26.clearLegacyLightFlag)
        assertFalse(p26.setDividerColor)

        val p29 = NavBarPlan.forSdk(29)
        assertTrue(p29.setContrast)
        assertFalse(p29.clearAppearanceLightFlag)

        assertTrue(NavBarPlan.forSdk(30).clearAppearanceLightFlag)
    }

    @Test
    fun clearingLightFlagKeepsOtherFlags() {
        // 0x510 = LAYOUT_STABLE | LAYOUT_FULLSCREEN | LIGHT_NAVIGATION_BAR
        assertEquals(0x500, clearLightNavigationFlag(0x510))
        assertEquals(0x500, clearLightNavigationFlag(0x500))
        assertEquals(0x2000, clearLightNavigationFlag(0x2010)) // keeps LIGHT_STATUS_BAR
    }
}
