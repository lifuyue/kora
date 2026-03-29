package com.lifuyue.kora.i18n

import androidx.core.os.LocaleListCompat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppLocaleControllerTest {
    private lateinit var fakeBridge: FakeLocaleBridge

    @Before
    fun setUp() {
        fakeBridge = FakeLocaleBridge()
        AppLocaleController.localeBridge = fakeBridge
        AppLocaleController.resetForTest()
    }

    @Test
    fun apply_doesNotSetLocalesAgainForSameLanguageTag() {
        AppLocaleController.apply("zh-CN")
        AppLocaleController.apply("zh-CN")

        assertEquals(1, fakeBridge.setCalls)
        assertEquals("zh-CN", fakeBridge.currentLocales.toLanguageTags())
    }

    @Test
    fun apply_switchesLocalesWhenLanguageTagChanges() {
        AppLocaleController.apply("zh-CN")
        AppLocaleController.apply("en")

        assertEquals(2, fakeBridge.setCalls)
        assertEquals("en", fakeBridge.currentLocales.toLanguageTags())
    }

    @Test
    fun apply_acceptsFollowSystemAfterExplicitLocale() {
        AppLocaleController.apply("zh-CN")
        AppLocaleController.apply(null)

        assertEquals(2, fakeBridge.setCalls)
        assertEquals("", fakeBridge.currentLocales.toLanguageTags())
    }
}

private class FakeLocaleBridge : AppLocaleBridge {
    var currentLocales: LocaleListCompat = LocaleListCompat.getEmptyLocaleList()
    var setCalls: Int = 0

    override fun getApplicationLocales(): LocaleListCompat = currentLocales

    override fun setApplicationLocales(locales: LocaleListCompat) {
        currentLocales = locales
        setCalls += 1
    }
}
