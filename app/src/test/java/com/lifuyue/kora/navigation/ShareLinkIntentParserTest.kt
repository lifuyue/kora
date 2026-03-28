package com.lifuyue.kora.navigation

import android.net.Uri
import com.lifuyue.kora.core.database.store.ShareLinkPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShareLinkIntentParserTest {
    @Test
    fun parserAcceptsCanonicalAndLegacyShareUris() {
        assertEquals(
            ShareLinkPayload(shareId = "s1", outLinkUid = "u1", chatId = "c1"),
            parseShareLinkUri(Uri.parse("kora://share/chat?shareId=s1&outLinkUid=u1&chatId=c1")),
        )
        assertEquals(
            ShareLinkPayload(shareId = "s1", outLinkUid = "u1", chatId = "c1"),
            parseShareLinkUri(Uri.parse("kora://share/s1?outLinkUid=u1&chatId=c1")),
        )
    }

    @Test
    fun parserRejectsMissingRequiredParams() {
        assertNull(parseShareLinkUri(Uri.parse("kora://share/chat?shareId=s1")))
    }
}
