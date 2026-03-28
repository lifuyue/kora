package com.lifuyue.kora.feature.knowledge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdaptiveKnowledgeScaffold(
    isExpanded: Boolean,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
) {
    if (isExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
            ) {
                listPane()
            }
            Box(
                modifier =
                    Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .widthIn(max = 820.dp),
            ) {
                detailPane()
            }
        }
    } else {
        detailPane()
    }
}
