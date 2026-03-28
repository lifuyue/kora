package com.lifuyue.kora.feature.knowledge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal fun rememberKnowledgeDualPaneEnabled(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        configuration.screenWidthDp >= 840
    }
}
