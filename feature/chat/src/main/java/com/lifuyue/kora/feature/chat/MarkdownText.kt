package com.lifuyue.kora.feature.chat

import android.annotation.SuppressLint
import android.text.method.LinkMovementMethod
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.luminance
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import android.graphics.Color as AndroidColor

private val codeFenceRegex = Regex("```([A-Za-z0-9_+-]*)\\n([\\s\\S]*?)```")
private val richInlineRegex =
    Regex("""(?s)!\[([^\]]*)]\(([^)]+)\)|\$\$(.+?)\$\$|(?<!\$)\$([^\n$]+?)\$(?!\$)""")

internal sealed interface MarkdownRenderNode {
    data class MarkdownText(val markdown: String) : MarkdownRenderNode

    data class CodeBlock(
        val language: String,
        val code: String,
        val isMermaidFallback: Boolean = false,
    ) : MarkdownRenderNode

    data class Image(
        val url: String,
        val alt: String,
    ) : MarkdownRenderNode

    data class Latex(
        val source: String,
        val displayMode: Boolean,
    ) : MarkdownRenderNode
}

internal fun parseAssistantBlocks(markdown: String): List<AssistantBlock> {
    if (markdown.isBlank()) {
        return listOf(AssistantBlock.Markdown("..."))
    }

    val blocks = mutableListOf<AssistantBlock>()
    val markdownBuffer = StringBuilder()

    fun flushMarkdownBuffer() {
        if (markdownBuffer.isNotBlank()) {
            blocks += AssistantBlock.Markdown(markdownBuffer.toString())
            markdownBuffer.clear()
        }
    }

    parseMarkdownRenderNodes(markdown).forEach { node ->
        when (node) {
            is MarkdownRenderNode.MarkdownText -> markdownBuffer.append(node.markdown)
            is MarkdownRenderNode.Image -> markdownBuffer.append("![${node.alt}](${node.url})")
            is MarkdownRenderNode.Latex -> {
                if (node.displayMode) {
                    markdownBuffer
                        .append("${'$'}${'$'}")
                        .append(node.source)
                        .append("${'$'}${'$'}")
                } else {
                    markdownBuffer
                        .append("${'$'}")
                        .append(node.source)
                        .append("${'$'}")
                }
            }
            is MarkdownRenderNode.CodeBlock -> {
                flushMarkdownBuffer()
                blocks += AssistantBlock.CodeFence(language = node.language, code = node.code)
            }
        }
    }

    flushMarkdownBuffer()
    return blocks.ifEmpty { listOf(AssistantBlock.Markdown(markdown)) }
}

internal fun parseMarkdownRenderNodes(markdown: String): List<MarkdownRenderNode> {
    if (markdown.isBlank()) {
        return listOf(MarkdownRenderNode.MarkdownText("..."))
    }

    val nodes = mutableListOf<MarkdownRenderNode>()
    var currentIndex = 0
    codeFenceRegex.findAll(markdown).forEach { match ->
        if (match.range.first > currentIndex) {
            nodes += parseInlineRenderNodes(markdown.substring(currentIndex, match.range.first))
        }
        val language = match.groupValues[1].trim().lowercase()
        val code = match.groupValues[2].trimEnd()
        nodes +=
            MarkdownRenderNode.CodeBlock(
                language = language,
                code = code,
                isMermaidFallback = language == "mermaid",
            )
        currentIndex = match.range.last + 1
    }
    if (currentIndex < markdown.length) {
        nodes += parseInlineRenderNodes(markdown.substring(currentIndex))
    }
    return nodes.ifEmpty { listOf(MarkdownRenderNode.MarkdownText(markdown)) }
}

private fun parseInlineRenderNodes(markdown: String): List<MarkdownRenderNode> {
    if (markdown.isBlank()) {
        return emptyList()
    }

    val nodes = mutableListOf<MarkdownRenderNode>()
    var currentIndex = 0
    richInlineRegex.findAll(markdown).forEach { match ->
        if (match.range.first > currentIndex) {
            appendMarkdownNode(nodes, markdown.substring(currentIndex, match.range.first))
        }
        when {
            match.groupValues[2].isNotBlank() -> {
                nodes +=
                    MarkdownRenderNode.Image(
                        url = match.groupValues[2].trim(),
                        alt = match.groupValues[1].trim(),
                    )
            }
            match.groupValues[3].isNotBlank() -> {
                nodes +=
                    MarkdownRenderNode.Latex(
                        source = match.groupValues[3].trim(),
                        displayMode = true,
                    )
            }
            match.groupValues[4].isNotBlank() -> {
                nodes +=
                    MarkdownRenderNode.Latex(
                        source = match.groupValues[4].trim(),
                        displayMode = false,
                    )
            }
        }
        currentIndex = match.range.last + 1
    }
    if (currentIndex < markdown.length) {
        appendMarkdownNode(nodes, markdown.substring(currentIndex))
    }
    return nodes
}

private fun appendMarkdownNode(
    nodes: MutableList<MarkdownRenderNode>,
    markdown: String,
) {
    if (markdown.isNotBlank()) {
        nodes += MarkdownRenderNode.MarkdownText(markdown)
    }
}

@Composable
fun MarkdownMessage(
    markdown: String,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nodes = remember(markdown) { parseMarkdownRenderNodes(markdown) }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() <= 0.5f
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        nodes.forEach { node ->
            when (node) {
                is MarkdownRenderNode.MarkdownText -> MarkdownTextBlock(node.markdown, isDarkTheme = isDarkTheme)
                is MarkdownRenderNode.CodeBlock -> {
                    CodeFenceCard(
                        language = node.language,
                        code = node.code,
                        isMermaidFallback = node.isMermaidFallback,
                        onCopyCode = onCopyCode,
                        isDarkTheme = isDarkTheme,
                    )
                }
                is MarkdownRenderNode.Image -> MarkdownImage(node)
                is MarkdownRenderNode.Latex -> LatexBlock(node)
            }
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    MarkdownTextBlock(markdown = markdown, modifier = modifier)
}

@Composable
private fun MarkdownTextBlock(
    markdown: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() <= 0.5f,
) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary
    val codeTextColor = if (isDarkTheme) Color(0xFFD8DEE9) else Color(0xFF0F172A)
    val markwon = remember(context, isDarkTheme, linkColor, codeTextColor) {
        createMarkdownRenderer(
            context = context,
            linkColor = linkColor.toArgb(),
            codeTextColor = codeTextColor.toArgb(),
        )
    }
    AndroidView(
        factory = { textViewContext ->
            TextView(textViewContext).apply {
                textSize = 15f
            }
        },
        update = { textView ->
            configureMarkdownTextView(
                textView = textView,
                markdown = markdown,
                markwon = markwon,
                textColor = textColor.toArgb(),
                linkColor = linkColor.toArgb(),
            )
        },
        modifier = modifier.fillMaxWidth(),
    )
}

internal fun configureMarkdownTextView(
    textView: TextView,
    markdown: String,
    markwon: Markwon = createMarkdownRenderer(textView.context),
    textColor: Int = AndroidColor.BLACK,
    linkColor: Int = AndroidColor.BLUE,
) {
    textView.linksClickable = true
    textView.movementMethod = LinkMovementMethod.getInstance()
    textView.highlightColor = AndroidColor.TRANSPARENT
    textView.setTextColor(textColor)
    textView.setLinkTextColor(linkColor)
    markwon.setMarkdown(textView, markdown)
}

internal fun createMarkdownRenderer(
    context: android.content.Context,
    linkColor: Int = 0xFF0B57D0.toInt(),
    codeTextColor: Int = 0xFF0F172A.toInt(),
): Markwon =
    Markwon
        .builder(context)
        .usePlugin(
            object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .linkColor(linkColor)
                        .isLinkUnderlined(true)
                        .codeTextColor(codeTextColor)
                }
            },
        ).build()

@Composable
private fun MarkdownImage(node: MarkdownRenderNode.Image) {
    val context = LocalContext.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .testTag("markdown-image:${node.url}")
                .semantics {
                    contentDescription = node.alt.ifBlank { node.url }
                },
    ) {
        AsyncImage(
            model =
                ImageRequest
                    .Builder(context)
                    .data(node.url)
                    .crossfade(true)
                    .build(),
            contentDescription = node.alt.ifBlank { "markdown image" },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LatexBlock(node: MarkdownRenderNode.Latex) {
    val textColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (node.displayMode) 72.dp else 36.dp)
                .testTag(
                    if (node.displayMode) {
                        "markdown-latex-block:${node.source}"
                    } else {
                        "markdown-latex-inline:${node.source}"
                    },
                ).semantics {
                    contentDescription = node.source
                },
    ) {
        LatexWebView(
            source = node.source,
            displayMode = node.displayMode,
            textColor = textColor,
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LatexWebView(
    source: String,
    displayMode: Boolean,
    textColor: Color,
) {
    val html = remember(source, displayMode, textColor) { buildKatexHtml(source, displayMode, textColor) }
    AndroidView(
        factory = { context ->
            runCatching {
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                } as View
            }.getOrElse {
                TextView(context).apply {
                    textSize = 15f
                }
            }
        },
        update = { view ->
            when (view) {
                is WebView -> {
                    view.loadDataWithBaseURL(
                        "https://localhost/",
                        html,
                        "text/html",
                        "utf-8",
                        null,
                    )
                }
                is TextView -> {
                    view.text = source
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun buildKatexHtml(
    source: String,
    displayMode: Boolean,
    textColor: Color,
): String {
    val escapedSource = escapeHtml(source)
    val dollar = "${'$'}"
    val displayFormula = dollar + dollar + escapedSource + dollar + dollar
    val body =
        if (displayMode) {
            displayFormula
        } else {
            "\\(${escapedSource}\\)"
        }
    val katexDisplayDelimiter = dollar + dollar
    val padding = if (displayMode) "8px 0" else "2px 0"
    val textColorHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.css">
          <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/katex.min.js"></script>
          <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.11/dist/contrib/auto-render.min.js"
            onload="renderMathInElement(document.body, {delimiters: [{left: '$katexDisplayDelimiter', right: '$katexDisplayDelimiter', display: true}, {left: '\\(', right: '\\)', display: false}]});">
          </script>
          <style>
            body {
              margin: 0;
              padding: $padding;
              background: transparent;
              color: $textColorHex;
              font-size: 15px;
              overflow: hidden;
            }
          </style>
        </head>
        <body>$body</body>
        </html>
        """.trimIndent()
}

internal fun buildMermaidHtml(source: String): String {
    val escaped = escapeHtml(source)
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <script type="module">
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
            mermaid.initialize({ startOnLoad: true, securityLevel: 'loose', theme: 'default' });
          </script>
          <style>
            body {
              margin: 0;
              padding: 8px 0;
              background: transparent;
            }
            .mermaid {
              overflow-x: auto;
            }
          </style>
        </head>
        <body>
          <pre class="mermaid">$escaped</pre>
        </body>
        </html>
        """.trimIndent()
}

private fun escapeHtml(source: String): String =
    buildString(source.length) {
        source.forEach { char ->
            when (char) {
                '<' -> append("&lt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

internal fun buildHighlightedCode(
    language: String,
    code: String,
    isDarkTheme: Boolean = false,
): AnnotatedString {
    val normalizedLanguage = language.lowercase()
    val keywordRegex =
        when (normalizedLanguage) {
            "kotlin" ->
                Regex(
                    "\\b(fun|val|var|class|data|object|when|if|else|return|suspend|import|package|private|public)\\b",
                )
            "java" -> Regex("\\b(public|private|class|static|void|return|if|else|new|final)\\b")
            "python" -> Regex("\\b(def|class|return|if|else|elif|import|from|for|while|lambda)\\b")
            "javascript", "js", "typescript", "ts" ->
                Regex("\\b(function|const|let|var|return|if|else|class|new|import|from|export)\\b")
            else -> null
        }

    val stringRegex = Regex("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'")
    val commentRegex =
        when (normalizedLanguage) {
            "python" -> Regex("#.*$", setOf(RegexOption.MULTILINE))
            else -> Regex("//.*$", setOf(RegexOption.MULTILINE))
        }

    return buildAnnotatedString {
        append(code)
        keywordRegex?.findAll(code)?.forEach {
            addStyle(
                style = SpanStyle(color = if (isDarkTheme) Color(0xFFC4B5FD) else Color(0xFF7C3AED)),
                start = it.range.first,
                end = it.range.last + 1,
            )
        }
        stringRegex.findAll(code).forEach {
            addStyle(
                style = SpanStyle(color = if (isDarkTheme) Color(0xFF5EEAD4) else Color(0xFF0F766E)),
                start = it.range.first,
                end = it.range.last + 1,
            )
        }
        commentRegex.findAll(code).forEach {
            addStyle(
                style = SpanStyle(color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                start = it.range.first,
                end = it.range.last + 1,
            )
        }
    }
}

@Composable
private fun CodeFenceCard(
    language: String,
    code: String,
    isMermaidFallback: Boolean,
    onCopyCode: (String) -> Unit,
    isDarkTheme: Boolean,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("markdown-code-block:${if (language.isBlank()) "code" else language}"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (language.isBlank()) stringResource(R.string.markdown_code_language_fallback) else language,
                    style = MaterialTheme.typography.labelLarge,
                )
                TextButton(onClick = { onCopyCode(code) }) {
                    Text(stringResource(R.string.markdown_copy_code))
                }
            }
            if (isMermaidFallback) {
                MermaidDiagramBlock(code = code)
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    SelectionContainer {
                        Text(
                            text = buildHighlightedCode(language = language, code = code, isDarkTheme = isDarkTheme),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MermaidDiagramBlock(code: String) {
    val html = remember(code) { buildMermaidHtml(code) }
    val fallbackLabel = stringResource(R.string.markdown_mermaid_fallback_title)
    val testTag = "${ChatTestTags.MERMAID_BLOCK_PREFIX}${code.trim().hashCode()}"
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .testTag(testTag),
    ) {
        AndroidView(
            factory = { context ->
                runCatching {
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                    } as View
                }.getOrElse {
                    TextView(context).apply {
                        textSize = 15f
                        text = code
                    }
                }
            },
            update = { view ->
                when (view) {
                    is WebView -> {
                        view.loadDataWithBaseURL(
                            "https://localhost/",
                            html,
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                    is TextView -> {
                        view.text = code.ifBlank { fallbackLabel }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
