package com.anvar.photolibraryorganizer.domain

enum class ImportMode(
    val title: String,
    val description: String,
) {
    ScanOnly(
        title = "Только сканировать",
        description = "Ничего не меняет на диске, только показывает план разбора.",
    ),
    Copy(
        title = "Копировать",
        description = "Копирует файлы в новую библиотеку и оставляет исходники на месте.",
    ),
    Move(
        title = "Переносить",
        description = "Переносит файлы после проверки, чтобы не занимать место копиями.",
    ),
}
