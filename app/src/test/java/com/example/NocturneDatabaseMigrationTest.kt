package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NocturneDatabase
import com.example.data.model.GoalPeriod
import com.example.data.model.PersonalGoal
import com.example.data.model.RecurrenceEditScope
import com.example.data.model.RecurrenceType
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.BackupManager
import com.example.data.repository.GoalRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.ConflictDetector
import com.example.domain.InsightsCalculator
import com.example.domain.RecurrenceEngine
import com.example.util.PersianCalendarUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.After
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
class NocturneDatabaseMigrationTest {

    private lateinit var context: Context
    private lateinit var db: NocturneDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("migration_test_nocturne.db")
        db = Room.inMemoryDatabaseBuilder(context, NocturneDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
        context.deleteDatabase("migration_test_nocturne.db")
    }

    @Test
    fun migration1To2_preservesExistingTasksWithoutDataLoss() = runTest {
        val dbName = "migration_test_nocturne.db"
        // Build an exact v1 SQLite database and insert a row
        val v1Helper = object : SQLiteOpenHelper(context, dbName, null, 1) {
            override fun onCreate(sqlite: SQLiteDatabase) {
                sqlite.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `aiNotes` TEXT NOT NULL,
                        `subtasksSerialized` TEXT NOT NULL,
                        `jalaliDateKey` TEXT NOT NULL,
                        `timeHour` INTEGER NOT NULL,
                        `timeMinute` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `priorityLevel` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `isAiGenerated` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                sqlite.execSQL(
                    """
                    INSERT INTO `tasks` (
                        `id`, `title`, `aiNotes`, `subtasksSerialized`, `jalaliDateKey`,
                        `timeHour`, `timeMinute`, `durationMinutes`, `priorityLevel`,
                        `category`, `isCompleted`, `isAiGenerated`, `createdAtMillis`
                    ) VALUES (
                        42, 'جلسه مهم نسخه ۱', 'یادداشت قدیمی', 's1::0::آماده‌سازی اسلاید', '1405-08-25',
                        14, 30, 60, 2, 'جلسه', 0, 1, 1700000000000
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(sqlite: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        v1Helper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, NocturneDatabase::class.java, dbName)
            .addMigrations(NocturneDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val all = migratedDb.taskDao().getAllTasksOnce()
        assertEquals(1, all.size)
        val preserved = all.first()
        assertEquals(42L, preserved.id)
        assertEquals("جلسه مهم نسخه ۱", preserved.title)
        assertEquals(14, preserved.timeHour)
        assertEquals(30, preserved.timeMinute)
        assertEquals(60, preserved.durationMinutes)
        assertEquals("NONE", preserved.recurrenceType)

        // Verify v2 now accepts nullable timeHour/timeMinute (unscheduled tasks)
        val repo = TaskRepository(migratedDb.taskDao())
        val unscheduledId = repo.addTask(
            TaskItem(
                title = "کار بدون ساعت مشخص",
                jalaliDateKey = "1405-08-25",
                timeHour = null,
                timeMinute = null
            )
        )
        val loadedUnscheduled = migratedDb.taskDao().getTaskById(unscheduledId)
        assertNotNull(loadedUnscheduled)
        assertNull(loadedUnscheduled!!.timeHour)
        assertNull(loadedUnscheduled.timeMinute)
        migratedDb.close()
    }

    @Test
    fun repository_startsEmpty_andExplicitDemoImportWorks() = runTest {
        val repo = TaskRepository(db.taskDao())
        assertEquals(0, repo.getTaskCount())
        val imported = repo.importDemoTasks(replaceExisting = false)
        assertTrue(imported > 0)
        assertEquals(imported, repo.getTaskCount())
        repo.clearAllUserTasks()
        assertEquals(0, repo.getTaskCount())
    }

    @Test
    fun atomicSubtaskAndCompletionToggles_preserveStateUnderConcurrency() = runTest {
        val repo = TaskRepository(db.taskDao())
        val id = repo.addTask(
            TaskItem(
                title = "پروژه موازی",
                jalaliDateKey = "1405-08-25",
                timeHour = 9,
                timeMinute = 0,
                subtasks = listOf(
                    SubtaskItem("sub1", "مرحله ۱", false),
                    SubtaskItem("sub2", "مرحله ۲", false)
                )
            )
        )
        val initialLoaded = repo.getTaskById(id)!!
        assertEquals(2, initialLoaded.subtasks.size)
        val sub0 = initialLoaded.subtasks[0].id
        val sub1 = initialLoaded.subtasks[1].id

        // Toggle sub0 & sub1 concurrently on IO threads
        awaitAll(
            async(Dispatchers.IO) { repo.toggleSubtask(id, sub0) },
            async(Dispatchers.IO) { repo.toggleSubtask(id, sub1) }
        )
        val updated = repo.getTaskById(id)!!
        assertEquals(2, updated.subtasks.size)
        assertTrue(updated.subtasks.all { it.isDone })

        repo.toggleTaskCompleted(id)
        assertTrue(db.taskDao().getTaskById(id)!!.isCompleted)
    }

    @Test
    fun recurrence_conflicts_goals_andBackupRestoreRoundTrip() = runTest {
        val taskRepo = TaskRepository(db.taskDao())
        val goalRepo = GoalRepository(db.goalDao())
        val prefsRepo = UserPreferencesRepository(context)
        val backupManager = BackupManager(db, prefsRepo)

        val day0 = PersianCalendarUtil.parseDateKey("1405-08-25")
        val day1 = PersianCalendarUtil.addDays(day0, 1)
        val day2 = PersianCalendarUtil.addDays(day0, 2)

        val recurringId = taskRepo.addTask(
            TaskItem(
                title = "مرور روزانه",
                jalaliDateKey = day0.dateKey,
                timeHour = 10,
                timeMinute = 0,
                durationMinutes = 45,
                priority = TaskPriority.MEDIUM,
                recurrenceType = RecurrenceType.DAILY
            )
        )
        val overlappingId = taskRepo.addTask(
            TaskItem(
                title = "تماس هم‌زمان",
                jalaliDateKey = day1.dateKey,
                timeHour = 10,
                timeMinute = 15,
                durationMinutes = 30
            )
        )
        assertTrue(overlappingId > 0L)

        // Expand recurring task on day1 -> should detect conflict with 10:15 task
        val day1Tasks = RecurrenceEngine.tasksForDate(taskRepo.getAllTasksOnce(), day1.dateKey)
        assertEquals(2, day1Tasks.size)
        val conflicts = ConflictDetector.findConflicts(day1Tasks)
        assertEquals(1, conflicts.size)
        assertNotNull(conflicts.first().suggestedFreeSlotHour)

        // Exclude day1 occurrence using ONLY_THIS
        val orig = taskRepo.getAllTasksOnce().first { it.id == recurringId }
        taskRepo.updateRecurringTask(
            original = orig,
            updated = orig.copy(title = "مرور ویژه فردا", timeHour = 15, timeMinute = 0),
            occurrenceDateKey = day1.dateKey,
            scope = RecurrenceEditScope.ONLY_THIS
        )
        val day1After = RecurrenceEngine.tasksForDate(taskRepo.getAllTasksOnce(), day1.dateKey)
        assertTrue(day1After.any { it.title == "مرور ویژه فردا" })
        assertFalse(day1After.any { it.title == "مرور روزانه" })
        // Day2 still has original recurring instance
        assertTrue(RecurrenceEngine.tasksForDate(taskRepo.getAllTasksOnce(), day2.dateKey).any { it.title == "مرور روزانه" })

        // Goal & habit streak
        val goalId = goalRepo.saveGoal(
            PersonalGoal(
                title = "۳۰ دقیقه مطالعه",
                period = GoalPeriod.DAILY,
                targetPerPeriod = 1,
                reminderHour = 20,
                reminderMinute = 0
            )
        )
        goalRepo.toggleHabitForDate(goalId, day0.dateKey)
        val summaries = InsightsCalculator.calculateGoalProgress(
            goalRepo.getAllGoalsOnce(),
            db.goalDao().getAllHabitLogsOnce().map { it.id to it }.map {
                com.example.data.model.HabitLog(
                    id = it.second.id,
                    goalId = it.second.goalId,
                    jalaliDateKey = it.second.jalaliDateKey,
                    count = it.second.count
                )
            },
            today = day0
        )
        assertEquals(1, summaries.first().currentStreakDays)

        // Export -> inspect -> restore replace
        val json = backupManager.exportToJson()
        val preview = backupManager.inspectBackup(json)
        assertTrue(preview.isValid)
        assertTrue(preview.validTasksCount >= 2)
        assertEquals(1, preview.goalsCount)
        assertEquals(1, preview.habitLogsCount)

        taskRepo.clearAllUserTasks()
        assertEquals(0, taskRepo.getTaskCount())

        val summary = backupManager.restoreBackup(preview, replaceExisting = true, restorePreferences = false)
        assertEquals(preview.validTasksCount, summary.restoredTasks)
        assertEquals(1, summary.restoredGoals)
    }
}
