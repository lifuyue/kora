package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveChatScaffold(
    isExpanded: Boolean,
    conversationPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
) {
    if (isExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
            ) {
                conversationPane()
            }
            Box(
                modifier =
                    Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .widthIn(max = 960.dp),
            ) {
                detailPane()
            }
        }
    } else {
        detailPane()
    }
}
