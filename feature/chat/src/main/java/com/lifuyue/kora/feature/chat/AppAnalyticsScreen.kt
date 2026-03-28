package com.lifuyue.kora.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppAnalyticsScreen(
    uiState: AppAnalyticsUiState,
    onRangeChanged: (AnalyticsRange) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AnalyticsRange.entries.forEach { range ->
                OutlinedButton(onClick = { onRangeChanged(range) }) {
                    Text(range.raw)
                }
            }
        }
        when (uiState.status) {
            AnalyticsStatus.Loading -> Text("Loading analytics…")
            AnalyticsStatus.Error -> Text(uiState.errorMessage ?: "Analytics unavailable", color = MaterialTheme.colorScheme.error)
            AnalyticsStatus.Empty -> Text("No analytics data")
            AnalyticsStatus.Success -> {
                AnalyticsMetricCard("Requests", uiState.requestCount.toString())
                AnalyticsMetricCard("Conversations", uiState.conversationCount.toString())
                AnalyticsMetricCard("Input tokens", uiState.inputTokens.toString())
                AnalyticsMetricCard("Output tokens", uiState.outputTokens.toString())
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    label: String,
    value: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
