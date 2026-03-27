package com.lifuyue.kora.feature.chat

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
