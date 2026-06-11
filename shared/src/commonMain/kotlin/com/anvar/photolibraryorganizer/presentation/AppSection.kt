package com.anvar.photolibraryorganizer.presentation

enum class AppSection(
    val title: String,
    val navigationTitle: String = title,
) {
    AllPhotos("Все фото"),
    Years("Годы"),
    Months("Месяцы"),
    WithoutDate("Без даты"),
    Import("Импорт"),
    Duplicates("Дубликаты"),
    Unsupported("Неподдерживаемые", navigationTitle = "Пропущенные"),
    Errors("Ошибки"),
}
