package com.lifuyue.kora.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocaleController {
    internal var localeBridge: AppLocaleBridge = AppCompatLocaleBridge
    private var lastAppliedLanguageTag: String? = null
    private var hasAppliedLanguageTag: Boolean = false

    fun apply(languageTag: String?) {
        if (hasAppliedLanguageTag && languageTag == lastAppliedLanguageTag) {
            return
        }
        val locales =
            if (languageTag.isNullOrBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(languageTag)
            }
        if (localeBridge.getApplicationLocales() != locales) {
            localeBridge.setApplicationLocales(locales)
        }
        lastAppliedLanguageTag = languageTag
        hasAppliedLanguageTag = true
    }

    internal fun resetForTest() {
        lastAppliedLanguageTag = null
        hasAppliedLanguageTag = false
    }
}

internal interface AppLocaleBridge {
    fun getApplicationLocales(): LocaleListCompat

    fun setApplicationLocales(locales: LocaleListCompat)
}

private object AppCompatLocaleBridge : AppLocaleBridge {
    override fun getApplicationLocales(): LocaleListCompat = AppCompatDelegate.getApplicationLocales()

    override fun setApplicationLocales(locales: LocaleListCompat) {
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
