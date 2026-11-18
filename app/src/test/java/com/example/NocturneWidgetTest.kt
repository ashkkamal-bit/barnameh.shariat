package com.example

import android.content.Context
import androidx.glance.GlanceId
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NocturneDatabase
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.TaskRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.util.PersianCalendarUtil
import com.example.widgets.NocturneWidgetActions
import com.example.widgets.ToggleTaskCompleteAction
import com.example.widgets.WidgetDataProvider
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NocturneWidgetTest {

    private lateinit var context: Context
    private lateinit var db: NocturneDatabase
    private lateinit var taskRepo: TaskRepository
    private lateinit var prefsRepo: UserPreferencesRepository

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        db = NocturneDatabase.getInstance(context)
        taskRepo = TaskRepository(db.taskDao())
        prefsRepo = UserPreferencesRepository(context)
        taskRepo.clearAllUserTasks()
    }

    @Test
    fun widgetSnapshot_emptyDb_reportsZeroCountsSafely() = runTest {
        val snap = WidgetDataProvider.loadSnapshot(context)
        assertEquals(0, snap.totalTodayCount)
        assertEquals(0, snap.pendingTodayCount)
        assertEquals(0, snap.completedTodayCount)
        assertEquals(0, snap.progressPercent)
        assertNull(snap.nextScheduledTask)
    }

    @Test
    fun widgetSnapshot_masksPrivateTitles_computesProgress_andActionTogglesCompletion() = runTest {
        val today = PersianCalendarUtil.today()
        prefsRepo.updateWidgetSettings(
            visibleCount = 4,
            showCompleted = false,
            showDescriptions = true,
            showPriorities = true,
            darkTheme = true,
            hidePrivate = true
        )

        val privateId = taskRepo.addTask(
            TaskItem(
                title = "جلسه محرمانه حقوقی",
                jalaliDateKey = today.dateKey,
                timeHour = 23,
                timeMinute = 30,
                priority = TaskPriority.HIGH,
                isPrivate = true
            )
        )
        taskRepo.addTask(
            TaskItem(
                title = "مرور ایمیل‌ها",
                jalaliDateKey = today.dateKey,
                timeHour = 8,
                timeMinute = 0,
                isCompleted = true
            )
        )

        val snap = WidgetDataProvider.loadSnapshot(context)
        assertEquals(2, snap.totalTodayCount)
        assertEquals(1, snap.pendingTodayCount)
        assertEquals(1, snap.completedTodayCount)
        assertEquals(50, snap.progressPercent)
        assertNotNull(snap.nextScheduledTask)
        assertEquals("فعالیت خصوصی", snap.displayTitle(snap.nextScheduledTask!!))

        // Execute Glance ActionCallback completion toggle
        val fakeGlanceId = object : GlanceId {}
        ToggleTaskCompleteAction().onAction(
            context = context,
            glanceId = fakeGlanceId,
            parameters = NocturneWidgetActions.taskToggleParams(privateId)
        )
        assertTrue(db.taskDao().getTaskById(privateId)!!.isCompleted)

        // Reset privacy pref & clean tasks
        prefsRepo.updateWidgetSettings(hidePrivate = false)
        taskRepo.clearAllUserTasks()
        assertFalse(WidgetDataProvider.loadSnapshot(context).totalTodayCount > 0)
    }
}
