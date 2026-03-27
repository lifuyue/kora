package com.lifuyue.kora.feature.chat

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MarkdownRendererTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun markdownRendererExposesImageLatexAndMermaidNodes() {
        composeRule.setContent {
            MarkdownMessage(
                markdown =
                    """
                    ![图示](https://example.com/diagram.png)
                    行内公式 ${'$'}E=mc^2${'$'}
                    ${'$'}${'$'}x^2 + y^2 = z^2${'$'}${'$'}
                    ```mermaid
                    graph TD
                    A-->B
                    ```
                    ```kotlin
                    println("hi")
                    ```
                    """.trimIndent(),
                onCopyCode = {},
            )
        }

        composeRule.onNodeWithTag("markdown-image:https://example.com/diagram.png").fetchSemanticsNode()
        composeRule.onNodeWithTag("markdown-latex-inline:E=mc^2").fetchSemanticsNode()
        composeRule.onNodeWithTag("markdown-latex-block:x^2 + y^2 = z^2").fetchSemanticsNode()
        composeRule.onNodeWithTag("markdown-code-block:mermaid").fetchSemanticsNode()
        composeRule.onNodeWithTag("markdown-code-block:kotlin").fetchSemanticsNode()
    }
}
