package com.precisionfarming.mobile.i18n

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Static so a locale change recomposes the tree under [androidx.compose.runtime.CompositionLocalProvider].
 * [S.t] stays a plain function so it can run inside list builders and coroutines.
 */
val LocalAppLocale = staticCompositionLocalOf { AppLocale.PT_BR }

/** Persisted locale. UI collects [locale] (or reads [LocalAppLocale]) to recompose. */
object LocaleStore {
    private const val PREFS = "pf_locale"
    private const val KEY = "locale"

    private var prefs: android.content.SharedPreferences? = null

    private val _locale = MutableStateFlow(AppLocale.PT_BR)
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _locale.value = AppLocale.fromTag(p.getString(KEY, AppLocale.PT_BR.tag))
    }

    @JvmName("updateLocale")
    fun setLocale(next: AppLocale) {
        if (_locale.value == next) return
        _locale.value = next
        prefs?.edit()?.putString(KEY, next.tag)?.apply()
    }
}

/** String helper — call as `S.t("nav.home")`. Locale changes recompose via [LocalAppLocale]. */
object S {
    fun t(key: String): String = translate(LocaleStore.locale.value, key)

    fun t(key: String, vararg vars: Pair<String, String>): String {
        var out = t(key)
        vars.forEach { (name, value) -> out = out.replace("{$name}", value) }
        return out
    }

    private fun translate(locale: AppLocale, key: String): String {
        val map = when (locale) {
            AppLocale.PT_BR -> Pt.map
            AppLocale.EN_US -> En.map
        }
        return map[key] ?: key
    }
}

/** Maps a queued `lastError` code onto i18n, or returns the raw text when it is not a known code. */
fun commandErrorLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return S.t("common.error")
    val key = "error.$raw"
    val mapped = S.t(key)
    return if (mapped == key) raw else mapped
}
