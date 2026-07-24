package com.anvar.photolibraryorganizer

/**
 * Returns `true` when the host system language should use Russian UI copy.
 *
 * Compose resources handle stable labels through `values-ru`; this helper is
 * used for dynamic messages that are built in regular Kotlin code.
 */
internal expect fun isRussianSystemLanguage(): Boolean

internal fun uiText(
    ru: String,
    en: String,
): String {
    return if (isRussianSystemLanguage()) ru else en
}
