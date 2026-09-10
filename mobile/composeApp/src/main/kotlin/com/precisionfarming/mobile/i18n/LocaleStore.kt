package com.precisionfarming.mobile.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Persisted locale + string lookup. Read [locale] during composition to recompose on change. */
object LocaleStore {
    private const val PREFS = "pf_locale"
    private const val KEY = "locale"

    private var prefs: android.content.SharedPreferences? = null

    var locale by mutableStateOf(AppLocale.PT_BR)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        locale = AppLocale.fromTag(p.getString(KEY, AppLocale.PT_BR.tag))
    }

    @JvmName("updateLocale")
    fun setLocale(next: AppLocale) {
        if (locale == next) return
        locale = next
        prefs?.edit()?.putString(KEY, next.tag)?.apply()
    }
}

/** String helper — call as `S.t("nav.home")`. Touches [LocaleStore.locale] for recomposition. */
object S {
    fun t(key: String): String {
        val map = when (LocaleStore.locale) {
            AppLocale.PT_BR -> Pt.map
            AppLocale.EN_US -> En.map
        }
        return map[key] ?: key
    }

    fun t(key: String, vararg vars: Pair<String, String>): String {
        var out = t(key)
        vars.forEach { (name, value) -> out = out.replace("{$name}", value) }
        return out
    }
}
