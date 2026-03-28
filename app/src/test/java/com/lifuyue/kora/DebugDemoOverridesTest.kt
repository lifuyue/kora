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
}
