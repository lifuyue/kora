package com.lifuyue.kora

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lifuyue.kora.testing.KoraTestOverrides
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = KoraApplication::class, sdk = [35])
class DebugDemoOverridesTest {
    @After
    fun tearDown() {
        KoraTestOverrides.reset()
    }

    @Test
    fun openDemoChatExtraInstallsSnapshotAndShellOverride() {
        installDebugDemoOverrides(Intent().putExtra("com.lifuyue.kora.extra.OPEN_DEMO_CHAT", true))

        val snapshot = checkNotNull(KoraTestOverrides.snapshotOverride).value
        assertEquals("demo-app", snapshot.selectedAppId)
        assertEquals(true, snapshot.onboardingCompleted)
        assertNotNull(KoraTestOverrides.shellRouteOverride)
    }

    @Test
    fun openDebugShellExtraInstallsSnapshotWithoutShellOverride() {
        installDebugDemoOverrides(Intent().putExtra("com.lifuyue.kora.extra.OPEN_DEBUG_SHELL", true))

        val snapshot = checkNotNull(KoraTestOverrides.snapshotOverride).value
        assertEquals("demo-app", snapshot.selectedAppId)
        assertEquals(true, snapshot.onboardingCompleted)
        assertNull(KoraTestOverrides.shellRouteOverride)
    }

    @Test
    fun parsesOpenAiDebugConnectionOverrideFromIntentExtras() {
        val override =
            readDebugConnectionOverride(
                Intent()
                    .putExtra("com.lifuyue.kora.extra.DEBUG_CONNECTION_TYPE", "OPENAI_COMPATIBLE")
                    .putExtra("com.lifuyue.kora.extra.DEBUG_CONNECTION_BASE_URL", "https://api.siliconflow.cn/v1")
                    .putExtra("com.lifuyue.kora.extra.DEBUG_CONNECTION_API_KEY", "sk-test")
                    .putExtra("com.lifuyue.kora.extra.DEBUG_CONNECTION_MODEL", "Qwen/Qwen3.5-4B"),
            )

        assertNotNull(override)
        assertEquals("https://api.siliconflow.cn/v1", override?.serverBaseUrl)
        assertEquals("sk-test", override?.apiKey)
        assertEquals("Qwen/Qwen3.5-4B", override?.model)
    }

    @Test
    fun returnsNullWhenDebugConnectionExtrasAreIncomplete() {
        val override =
            readDebugConnectionOverride(
                Intent()
                    .putExtra("com.lifuyue.kora.extra.DEBUG_CONNECTION_TYPE", "OPENAI_COMPATIBLE")
                    .putExtra("com.lifuyue.kora.extra.DEBUG_CONNECTION_BASE_URL", "https://api.siliconflow.cn/v1"),
            )

        assertNull(override)
    }
}
