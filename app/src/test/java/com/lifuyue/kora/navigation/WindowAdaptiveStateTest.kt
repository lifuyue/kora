package com.lifuyue.kora.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowAdaptiveStateTest {
    @Test
    fun compactWidthDisablesDualPane() {
        assertFalse(WindowAdaptiveState(widthDp = 600).useDualPane)
    }

    @Test
    fun expandedWidthEnablesDualPane() {
        assertTrue(WindowAdaptiveState(widthDp = 960).useDualPane)
    }
}
