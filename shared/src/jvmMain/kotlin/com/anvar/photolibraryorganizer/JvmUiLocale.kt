package com.anvar.photolibraryorganizer

import java.util.Locale

internal actual fun isRussianSystemLanguage(): Boolean {
    return Locale.getDefault().language.equals("ru", ignoreCase = true)
}
