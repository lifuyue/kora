package com.lifuyue.kora.feature.chat

import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MarkdownTextViewTest {
    @Test
    fun configureMarkdownTextViewEnablesClickableLinks() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val textView = TextView(context)

        configureMarkdownTextView(
            textView = textView,
            markdown = "访问 [OpenAI](https://openai.com)",
        )

        assertTrue(textView.movementMethod is LinkMovementMethod)
    }

    @Test
    fun configureMarkdownTextViewKeepsHeadingsListsAndLinksObservable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val textView = TextView(context)

        configureMarkdownTextView(
            textView = textView,
            markdown =
                """
                # 标题
                - 第一项
                - 第二项

                访问 [OpenAI](https://openai.com)
                """.trimIndent(),
        )

        val rendered = textView.text
        assertNotNull(rendered)
        assertTrue(rendered is Spanned)
        assertTrue(rendered.toString().contains("标题"))
        assertTrue(rendered.toString().contains("第一项"))
        assertTrue(rendered.toString().contains("第二项"))

        val links = (rendered as Spanned).getSpans(0, rendered.length, URLSpan::class.java)
        assertEquals(1, links.size)
        assertEquals("https://openai.com", links.single().url)
    }
}
