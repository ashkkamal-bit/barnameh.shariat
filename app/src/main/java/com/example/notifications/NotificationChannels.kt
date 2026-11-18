package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_TASK_REMINDERS = "nocturne_task_reminders"
    const val CHANNEL_IMPORTANT_ACTIVITIES = "nocturne_important_activities"
    const val CHANNEL_DAILY_SUMMARIES = "nocturne_daily_summaries"
    const val CHANNEL_HABIT_REMINDERS = "nocturne_habit_reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "یادآوری کارها",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان زمان شروع و یادآوری پیش از انجام فعالیت‌های روزانه"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_IMPORTANT_ACTIVITIES,
                "کارهای فوری و مهم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "هشدار ویژه برای فعالیت‌های با اولویت فوری و کارهای عقب‌افتاده"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_DAILY_SUMMARIES,
                "خلاصه روزانه",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "گزارش برنامه صبحگاهی و جمع‌بندی عملکرد شبانه"
            },
            NotificationChannel(
                CHANNEL_HABIT_REMINDERS,
                "یادآوری اهداف و عادت‌ها",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "یادآوری ثبت پیشرفت عادت‌ها و اهداف شخصی"
            }
        )

        manager.createNotificationChannels(channels)
    }
}
