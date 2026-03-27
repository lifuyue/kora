package com.lifuyue.kora.feature.chat

import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHighlightTest {
    @Test
    fun kotlinHighlighterMarksKeywords() {
        val highlighted =
            buildHighlightedCode(
                language = "kotlin",
                code = "fun greet(name: String) = println(name)",
            )

        assertTrue(highlighted.spanStyles.isNotEmpty())
        assertTrue(
            highlighted.spanStyles.any {
                highlighted.text.substring(it.start, it.end) == "fun"
            },
        )
    }
}
