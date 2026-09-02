package com.precisionfarming.mobile.i18n

enum class AppLocale(val tag: String) {
    PT_BR("pt-BR"),
    EN_US("en-US");

    companion object {
        fun fromTag(tag: String?): AppLocale =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: PT_BR
    }
}
