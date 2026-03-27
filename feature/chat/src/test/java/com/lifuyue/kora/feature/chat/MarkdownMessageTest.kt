package com.lifuyue.kora.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownMessageTest {
    @Test
    fun parserSplitsMarkdownAndCodeFences() {
        val blocks =
            parseAssistantBlocks(
                """
                标题

                ```kotlin
                println("hi")
                ```

                结尾
                """.trimIndent(),
            )

        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is AssistantBlock.Markdown)
        assertTrue(blocks[1] is AssistantBlock.CodeFence)
        assertEquals("kotlin", (blocks[1] as AssistantBlock.CodeFence).language)
    }
}
