package com.anvar.photolibraryorganizer.presentation

enum class AppSection(
    val title: String,
) {
    AllPhotos("Все фото"),
    Years("Годы"),
    Months("Месяцы"),
    WithoutDate("Без даты"),
    Import("Импорт"),
    Duplicates("Дубликаты"),
    Errors("Ошибки"),
}
