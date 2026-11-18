package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.nocturneDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nocturne_preferences"
)

data class NocturnePreferences(
    val hapticsEnabled: Boolean = true,
    val speechLanguageTag: String = "fa-IR",
    val ttsEnabled: Boolean = false,
    val ttsSpeechRate: Float = 1.0f,
    val autoListenOnVoiceTap: Boolean = false,
    val defaultBlockMinutes: Int = 45,
    val defaultReminderOffsetMinutes: Int = 10,
    val taskRemindersEnabled: Boolean = true,
    val overdueAlertsEnabled: Boolean = true,
    val morningSummaryEnabled: Boolean = false,
    val morningSummaryHour: Int = 8,
    val eveningSummaryEnabled: Boolean = false,
    val eveningSummaryHour: Int = 21,
    val habitRemindersEnabled: Boolean = true,
    val isDarkTheme: Boolean = true,
    val usePersianDigits: Boolean = true,
    val useSystemTimezone: Boolean = true,
    val customTimezoneId: String = "",
    // Widget settings
    val widgetVisibleCount: Int = 3,
    val widgetShowCompleted: Boolean = false,
    val widgetShowDescriptions: Boolean = false,
    val widgetShowPriorities: Boolean = true,
    val widgetDarkTheme: Boolean = true,
    val widgetHidePrivateTitles: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val SPEECH_LANG = stringPreferencesKey("speech_lang")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val AUTO_LISTEN = booleanPreferencesKey("auto_listen")
        val DEFAULT_DURATION = intPreferencesKey("default_duration")
        val DEFAULT_REMINDER_OFFSET = intPreferencesKey("default_reminder_offset")
        val TASK_REMINDERS = booleanPreferencesKey("task_reminders")
        val OVERDUE_ALERTS = booleanPreferencesKey("overdue_alerts")
        val MORNING_SUMMARY = booleanPreferencesKey("morning_summary")
        val MORNING_HOUR = intPreferencesKey("morning_hour")
        val EVENING_SUMMARY = booleanPreferencesKey("evening_summary")
        val EVENING_HOUR = intPreferencesKey("evening_hour")
        val HABIT_REMINDERS = booleanPreferencesKey("habit_reminders")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val PERSIAN_DIGITS = booleanPreferencesKey("persian_digits")
        val SYSTEM_TIMEZONE = booleanPreferencesKey("system_timezone")
        val CUSTOM_TIMEZONE = stringPreferencesKey("custom_timezone")
        val WIDGET_VISIBLE_COUNT = intPreferencesKey("widget_visible_count")
        val WIDGET_SHOW_COMPLETED = booleanPreferencesKey("widget_show_completed")
        val WIDGET_SHOW_DESCRIPTIONS = booleanPreferencesKey("widget_show_descriptions")
        val WIDGET_SHOW_PRIORITIES = booleanPreferencesKey("widget_show_priorities")
        val WIDGET_DARK_THEME = booleanPreferencesKey("widget_dark_theme")
        val WIDGET_HIDE_PRIVATE = booleanPreferencesKey("widget_hide_private")
    }

    val preferences: Flow<NocturnePreferences> = context.nocturneDataStore.data.map { prefs ->
        NocturnePreferences(
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            speechLanguageTag = prefs[Keys.SPEECH_LANG] ?: "fa-IR",
            ttsEnabled = prefs[Keys.TTS_ENABLED] ?: false,
            ttsSpeechRate = (prefs[Keys.TTS_RATE] ?: 1.0f).coerceIn(0.6f, 1.6f),
            autoListenOnVoiceTap = prefs[Keys.AUTO_LISTEN] ?: false,
            defaultBlockMinutes = (prefs[Keys.DEFAULT_DURATION] ?: 45).coerceIn(10, 240),
            defaultReminderOffsetMinutes = (prefs[Keys.DEFAULT_REMINDER_OFFSET] ?: 10).coerceIn(0, 120),
            taskRemindersEnabled = prefs[Keys.TASK_REMINDERS] ?: true,
            overdueAlertsEnabled = prefs[Keys.OVERDUE_ALERTS] ?: true,
            morningSummaryEnabled = prefs[Keys.MORNING_SUMMARY] ?: false,
            morningSummaryHour = (prefs[Keys.MORNING_HOUR] ?: 8).coerceIn(5, 11),
            eveningSummaryEnabled = prefs[Keys.EVENING_SUMMARY] ?: false,
            eveningSummaryHour = (prefs[Keys.EVENING_HOUR] ?: 21).coerceIn(17, 23),
            habitRemindersEnabled = prefs[Keys.HABIT_REMINDERS] ?: true,
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            usePersianDigits = prefs[Keys.PERSIAN_DIGITS] ?: true,
            useSystemTimezone = prefs[Keys.SYSTEM_TIMEZONE] ?: true,
            customTimezoneId = prefs[Keys.CUSTOM_TIMEZONE] ?: "",
            widgetVisibleCount = (prefs[Keys.WIDGET_VISIBLE_COUNT] ?: 3).coerceIn(1, 8),
            widgetShowCompleted = prefs[Keys.WIDGET_SHOW_COMPLETED] ?: false,
            widgetShowDescriptions = prefs[Keys.WIDGET_SHOW_DESCRIPTIONS] ?: false,
            widgetShowPriorities = prefs[Keys.WIDGET_SHOW_PRIORITIES] ?: true,
            widgetDarkTheme = prefs[Keys.WIDGET_DARK_THEME] ?: true,
            widgetHidePrivateTitles = prefs[Keys.WIDGET_HIDE_PRIVATE] ?: false
        )
    }

    suspend fun getPreferencesOnce(): NocturnePreferences = preferences.first()

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.nocturneDataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setSpeechLanguageTag(tag: String) {
        context.nocturneDataStore.edit { it[Keys.SPEECH_LANG] = tag }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.nocturneDataStore.edit { it[Keys.TTS_ENABLED] = enabled }
    }

    suspend fun setTtsSpeechRate(rate: Float) {
        context.nocturneDataStore.edit { it[Keys.TTS_RATE] = rate.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setAutoListenOnVoiceTap(enabled: Boolean) {
        context.nocturneDataStore.edit { it[Keys.AUTO_LISTEN] = enabled }
    }

    suspend fun setDefaultBlockMinutes(minutes: Int) {
        context.nocturneDataStore.edit { it[Keys.DEFAULT_DURATION] = minutes.coerceIn(10, 240) }
    }

    suspend fun setDefaultReminderOffsetMinutes(minutes: Int) {
        context.nocturneDataStore.edit { it[Keys.DEFAULT_REMINDER_OFFSET] = minutes.coerceIn(0, 120) }
    }

    suspend fun setTaskRemindersEnabled(enabled: Boolean) {
        context.nocturneDataStore.edit { it[Keys.TASK_REMINDERS] = enabled }
    }

    suspend fun setOverdueAlertsEnabled(enabled: Boolean) {
        context.nocturneDataStore.edit { it[Keys.OVERDUE_ALERTS] = enabled }
    }

    suspend fun setMorningSummary(enabled: Boolean, hour: Int) {
        context.nocturneDataStore.edit {
            it[Keys.MORNING_SUMMARY] = enabled
            it[Keys.MORNING_HOUR] = hour.coerceIn(5, 11)
        }
    }

    suspend fun setEveningSummary(enabled: Boolean, hour: Int) {
        context.nocturneDataStore.edit {
            it[Keys.EVENING_SUMMARY] = enabled
            it[Keys.EVENING_HOUR] = hour.coerceIn(17, 23)
        }
    }

    suspend fun setHabitRemindersEnabled(enabled: Boolean) {
        context.nocturneDataStore.edit { it[Keys.HABIT_REMINDERS] = enabled }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.nocturneDataStore.edit { it[Keys.DARK_THEME] = isDark }
    }

    suspend fun setUsePersianDigits(usePersian: Boolean) {
        context.nocturneDataStore.edit { it[Keys.PERSIAN_DIGITS] = usePersian }
    }

    suspend fun setTimezoneConfig(useSystem: Boolean, customId: String) {
        context.nocturneDataStore.edit {
            it[Keys.SYSTEM_TIMEZONE] = useSystem
            it[Keys.CUSTOM_TIMEZONE] = customId
        }
    }

    suspend fun updateWidgetSettings(
        visibleCount: Int? = null,
        showCompleted: Boolean? = null,
        showDescriptions: Boolean? = null,
        showPriorities: Boolean? = null,
        darkTheme: Boolean? = null,
        hidePrivate: Boolean? = null
    ) {
        context.nocturneDataStore.edit { prefs ->
            visibleCount?.let { prefs[Keys.WIDGET_VISIBLE_COUNT] = it.coerceIn(1, 8) }
            showCompleted?.let { prefs[Keys.WIDGET_SHOW_COMPLETED] = it }
            showDescriptions?.let { prefs[Keys.WIDGET_SHOW_DESCRIPTIONS] = it }
            showPriorities?.let { prefs[Keys.WIDGET_SHOW_PRIORITIES] = it }
            darkTheme?.let { prefs[Keys.WIDGET_DARK_THEME] = it }
            hidePrivate?.let { prefs[Keys.WIDGET_HIDE_PRIVATE] = it }
        }
    }

    suspend fun applyFromBackup(backupPrefs: NocturnePreferences) {
        context.nocturneDataStore.edit { prefs ->
            prefs[Keys.HAPTICS] = backupPrefs.hapticsEnabled
            prefs[Keys.SPEECH_LANG] = backupPrefs.speechLanguageTag
            prefs[Keys.TTS_ENABLED] = backupPrefs.ttsEnabled
            prefs[Keys.TTS_RATE] = backupPrefs.ttsSpeechRate
            prefs[Keys.AUTO_LISTEN] = backupPrefs.autoListenOnVoiceTap
            prefs[Keys.DEFAULT_DURATION] = backupPrefs.defaultBlockMinutes
            prefs[Keys.DEFAULT_REMINDER_OFFSET] = backupPrefs.defaultReminderOffsetMinutes
            prefs[Keys.TASK_REMINDERS] = backupPrefs.taskRemindersEnabled
            prefs[Keys.OVERDUE_ALERTS] = backupPrefs.overdueAlertsEnabled
            prefs[Keys.MORNING_SUMMARY] = backupPrefs.morningSummaryEnabled
            prefs[Keys.MORNING_HOUR] = backupPrefs.morningSummaryHour
            prefs[Keys.EVENING_SUMMARY] = backupPrefs.eveningSummaryEnabled
            prefs[Keys.EVENING_HOUR] = backupPrefs.eveningSummaryHour
            prefs[Keys.HABIT_REMINDERS] = backupPrefs.habitRemindersEnabled
            prefs[Keys.DARK_THEME] = backupPrefs.isDarkTheme
            prefs[Keys.PERSIAN_DIGITS] = backupPrefs.usePersianDigits
            prefs[Keys.SYSTEM_TIMEZONE] = backupPrefs.useSystemTimezone
            prefs[Keys.CUSTOM_TIMEZONE] = backupPrefs.customTimezoneId
            prefs[Keys.WIDGET_VISIBLE_COUNT] = backupPrefs.widgetVisibleCount
            prefs[Keys.WIDGET_SHOW_COMPLETED] = backupPrefs.widgetShowCompleted
            prefs[Keys.WIDGET_SHOW_DESCRIPTIONS] = backupPrefs.widgetShowDescriptions
            prefs[Keys.WIDGET_SHOW_PRIORITIES] = backupPrefs.widgetShowPriorities
            prefs[Keys.WIDGET_DARK_THEME] = backupPrefs.widgetDarkTheme
            prefs[Keys.WIDGET_HIDE_PRIVATE] = backupPrefs.widgetHidePrivateTitles
        }
    }
}
