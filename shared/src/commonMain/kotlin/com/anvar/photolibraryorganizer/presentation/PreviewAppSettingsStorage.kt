package com.anvar.photolibraryorganizer.presentation

object PreviewAppSettingsStorage : AppSettingsStorage {
    override suspend fun loadSettings(): AppSettings = AppSettings()
    override suspend fun saveSettings(settings: AppSettings) = Unit
}
