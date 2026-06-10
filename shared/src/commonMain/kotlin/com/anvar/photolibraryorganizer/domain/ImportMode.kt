package com.anvar.photolibraryorganizer.domain

enum class ImportMode(
    val title: String,
    val description: String,
) {
    ScanOnly(
        title = "Scan only",
        description = "Ничего не меняет на диске, только показывает план разбора.",
    ),
    Copy(
        title = "Copy",
        description = "Копирует файлы в новую библиотеку и оставляет исходники на месте.",
    ),
    Move(
        title = "Move",
        description = "Переносит файлы после проверки, чтобы не занимать место копиями.",
    ),
}
