package com.lifuyue.kora.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

data class WindowAdaptiveState(
    val widthDp: Int,
) {
    val isExpanded: Boolean
        get() = widthDp >= 840

    val useDualPane: Boolean
        get() = isExpanded
}

@Composable
fun rememberWindowAdaptiveState(): WindowAdaptiveState {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        WindowAdaptiveState(widthDp = configuration.screenWidthDp)
    }
}
