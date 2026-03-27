package com.lifuyue.kora

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = KoraApplication::class)
class KoraApplicationTest {
    @Test
    fun runtimeEnvironmentUsesKoraApplication() {
        val application = RuntimeEnvironment.getApplication()

        assertTrue(application is KoraApplication)
    }
}
