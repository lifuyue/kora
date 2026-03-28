package com.lifuyue.kora.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal fun rememberSettingsDualPaneEnabled(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        configuration.screenWidthDp >= 840
    }
}
