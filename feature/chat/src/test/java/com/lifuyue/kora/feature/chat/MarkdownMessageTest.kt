package com.lifuyue.kora.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownMessageTest {
    @Test
    fun parserSplitsMarkdownAndCodeFences() {
        val blocks =
            parseMarkdownRenderNodes(
                """
                标题

                ```kotlin
                println("hi")
                ```

                结尾
                """.trimIndent(),
            )

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownRenderNode.MarkdownText)
        assertTrue(blocks[1] is MarkdownRenderNode.CodeBlock)
        assertEquals("kotlin", (blocks[1] as MarkdownRenderNode.CodeBlock).language)
    }

    @Test
    fun parserExtractsImagesLatexAndMermaidFallback() {
        val blocks =
            parseMarkdownRenderNodes(
                """
                说明 [OpenAI](https://openai.com)
                ![图示](https://example.com/diagram.png)
                行内公式 ${'$'}E=mc^2${'$'}
                ${'$'}${'$'}\int_a^b x^2 dx${'$'}${'$'}
                ```mermaid
                graph TD
                A-->B
                ```
                """.trimIndent(),
            )

        assertTrue(blocks.any { it is MarkdownRenderNode.Image && it.url == "https://example.com/diagram.png" })
        assertTrue(blocks.any { it is MarkdownRenderNode.Latex && !it.displayMode && it.source == "E=mc^2" })
        assertTrue(blocks.any { it is MarkdownRenderNode.Latex && it.displayMode && it.source.contains("x^2") })
        assertTrue(
            blocks.any {
                it is MarkdownRenderNode.CodeBlock &&
                    it.language == "mermaid" &&
                    it.isMermaidFallback
            },
        )
    }
}
