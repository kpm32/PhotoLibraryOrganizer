package com.anvar.photolibraryorganizer

internal fun Long.toReadableSize(): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = toDouble()
    var unitIndex = 0

    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex += 1
    }

    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        "${(value * 10).toLong() / 10.0} ${units[unitIndex]}"
    }
}
