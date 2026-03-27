package com.lifuyue.kora.feature.chat

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

private val codeFenceRegex = Regex("```([A-Za-z0-9_+-]*)\\n([\\s\\S]*?)```")

internal fun parseAssistantBlocks(markdown: String): List<AssistantBlock> {
    if (markdown.isBlank()) {
        return listOf(AssistantBlock.Markdown("..."))
    }

    val blocks = mutableListOf<AssistantBlock>()
    var currentIndex = 0
    codeFenceRegex.findAll(markdown).forEach { match ->
        if (match.range.first > currentIndex) {
            val plainMarkdown = markdown.substring(currentIndex, match.range.first)
            if (plainMarkdown.isNotBlank()) {
                blocks += AssistantBlock.Markdown(plainMarkdown)
            }
        }
        val language = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd()
        blocks += AssistantBlock.CodeFence(language = language, code = code)
        currentIndex = match.range.last + 1
    }
    if (currentIndex < markdown.length) {
        val plainMarkdown = markdown.substring(currentIndex)
        if (plainMarkdown.isNotBlank()) {
            blocks += AssistantBlock.Markdown(plainMarkdown)
        }
    }
    return blocks.ifEmpty { listOf(AssistantBlock.Markdown(markdown)) }
}

@Composable
fun MarkdownMessage(
    markdown: String,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseAssistantBlocks(markdown) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        blocks.forEach { block ->
            when (block) {
                is AssistantBlock.Markdown -> MarkdownText(block.markdown)
                is AssistantBlock.CodeFence -> {
                    CodeFenceCard(
                        language = block.language,
                        code = block.code,
                        onCopyCode = onCopyCode,
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                textSize = 15f
            }
        },
        update = { textView ->
            Markwon.create(textView.context).setMarkdown(textView, markdown)
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CodeFenceCard(
    language: String,
    code: String,
    onCopyCode: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (language.isBlank()) "code" else language,
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(onClick = { onCopyCode(code) }) {
                    Text("复制代码")
                }
            }
            SelectionContainer {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
