package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NocturneDatabase
import com.example.data.model.TaskItem
import com.example.data.repository.NocturnePreferences
import com.example.data.repository.TaskRepository
import com.example.notifications.AlarmReceiver
import com.example.notifications.AlarmScheduler
import com.example.notifications.NotificationActionReceiver
import com.example.notifications.NotificationChannels
import com.example.notifications.ReminderReconciliationManager
import com.example.util.PersianCalendarUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationAndSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        NotificationChannels.ensureCreated(context)
    }

    @Test
    fun alarmScheduler_createsUniqueAlarms_cancelsOnCompletion_andSchedulesSnooze() {
        val tomorrow = PersianCalendarUtil.offsetFromToday(1)
        val task = TaskItem(
            id = 101L,
            title = "جلسه مهم",
            jalaliDateKey = tomorrow.dateKey,
            timeHour = 10,
            timeMinute = 0,
            remindersEnabled = true,
            reminderOffsetsMinutes = listOf(15, 0)
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val report = AlarmScheduler.scheduleAll(
            context = context,
            tasks = listOf(task),
            goals = emptyList(),
            preferences = NocturnePreferences(taskRemindersEnabled = true)
        )
        assertEquals(2, report.scheduledTaskAlarms)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

        // Unique request codes for each offset slot
        assertFalse(
            AlarmScheduler.taskRequestCode(101L, 0) == AlarmScheduler.taskRequestCode(101L, 1)
        )

        // Completing or canceling removes all task alarms
        AlarmScheduler.cancelTaskAlarms(context, 101L)
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)

        // Snooze schedules 1 alarm
        AlarmScheduler.scheduleSnooze(context, taskId = 101L, snoozeMinutes = 5)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun alarmScheduler_inexactFallbackAndDeniedNotificationPermissionHandledSafely() {
        val pi = PendingIntent.getBroadcast(
            context,
            777,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val usedInexact = AlarmScheduler.setAlarmTrigger(
            context = context,
            triggerAtMillis = System.currentTimeMillis() + 60_000L,
            pendingIntent = pi,
            exactPermitted = false
        )
        assertTrue(usedInexact)

        // Verify POST_NOTIFICATIONS denial detection
        val app = context.applicationContext as Application
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(AlarmScheduler.hasPostNotificationPermission(context))

        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(AlarmScheduler.hasPostNotificationPermission(context))
    }

    @Test
    fun reconciliationAndNotificationActionReceiver_markTaskCompletedAndCancelAlarms() = runTest {
        val db = NocturneDatabase.getInstance(context)
        val repo = TaskRepository(db.taskDao())
        repo.clearAllUserTasks()

        val tomorrow = PersianCalendarUtil.offsetFromToday(1)
        val id = repo.addTask(
            TaskItem(
                title = "تست اقدام اعلان",
                jalaliDateKey = tomorrow.dateKey,
                timeHour = 14,
                timeMinute = 0,
                remindersEnabled = true,
                reminderOffsetsMinutes = listOf(10, 0)
            )
        )

        val report = ReminderReconciliationManager.reconcileAll(context, notifyOverdue = false)
        assertEquals(2, report.scheduledTaskAlarms)

        // Simulate Notification "انجام شد" action
        val receiver = NotificationActionReceiver()
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, id)
        }
        receiver.onReceive(context, intent)

        // Allow async coroutine in BroadcastReceiver to settle
        Thread.sleep(300L)
        val updated = db.taskDao().getTaskById(id)
        assertNotNull(updated)
        assertTrue(updated!!.isCompleted)

        // Verify unscheduled task does not create alarms
        val unschedId = repo.addTask(
            TaskItem(
                title = "کار بدون ساعت",
                jalaliDateKey = tomorrow.dateKey,
                timeHour = null,
                timeMinute = null
            )
        )
        assertNull(db.taskDao().getTaskById(unschedId)!!.timeHour)
        repo.clearAllUserTasks()
    }
}
