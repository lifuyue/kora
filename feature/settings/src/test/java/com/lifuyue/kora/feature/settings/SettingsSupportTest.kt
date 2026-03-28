package com.lifuyue.kora.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SettingsSupportTest {
    @Test
    fun androidAppInfoProvider_usesProjectLicenseUrl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = AndroidAppInfoProvider(context)

        assertEquals("https://github.com/lifuyue/kora/blob/main/LICENSE", provider.licensesUrl())
    }
}
