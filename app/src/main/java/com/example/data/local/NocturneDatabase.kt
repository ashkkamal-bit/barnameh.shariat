package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, GoalEntity::class, HabitLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NocturneDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun goalDao(): GoalDao

    companion object {
        const val DATABASE_NAME = "nocturne_planner.db"

        @Volatile
        private var INSTANCE: NocturneDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate tasks table non-destructively so timeHour and timeMinute become nullable,
                // and add v2 columns for reminders, recurrence, privacy, and timestamps.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tasks_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `aiNotes` TEXT NOT NULL,
                        `subtasksSerialized` TEXT NOT NULL,
                        `jalaliDateKey` TEXT NOT NULL,
                        `timeHour` INTEGER,
                        `timeMinute` INTEGER,
                        `durationMinutes` INTEGER NOT NULL,
                        `isDurationUserSpecified` INTEGER NOT NULL DEFAULT 1,
                        `priorityLevel` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `isAiGenerated` INTEGER NOT NULL,
                        `remindersEnabled` INTEGER NOT NULL DEFAULT 1,
                        `reminderOffsetsCsv` TEXT NOT NULL DEFAULT '10,0',
                        `recurrenceType` TEXT NOT NULL DEFAULT 'NONE',
                        `recurrenceInterval` INTEGER NOT NULL DEFAULT 1,
                        `recurrenceEndDateKey` TEXT,
                        `recurrenceExceptionsCsv` TEXT NOT NULL DEFAULT '',
                        `seriesParentId` INTEGER,
                        `timezoneId` TEXT NOT NULL DEFAULT '',
                        `isPrivate` INTEGER NOT NULL DEFAULT 0,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `tasks_new` (
                        `id`, `title`, `aiNotes`, `subtasksSerialized`, `jalaliDateKey`,
                        `timeHour`, `timeMinute`, `durationMinutes`, `isDurationUserSpecified`,
                        `priorityLevel`, `category`, `isCompleted`, `isAiGenerated`,
                        `remindersEnabled`, `reminderOffsetsCsv`, `recurrenceType`,
                        `recurrenceInterval`, `recurrenceEndDateKey`, `recurrenceExceptionsCsv`,
                        `seriesParentId`, `timezoneId`, `isPrivate`, `createdAtMillis`, `updatedAtMillis`
                    )
                    SELECT
                        `id`, `title`, `aiNotes`, `subtasksSerialized`, `jalaliDateKey`,
                        `timeHour`, `timeMinute`, `durationMinutes`, 1,
                        `priorityLevel`, `category`, `isCompleted`, `isAiGenerated`,
                        1, '10,0', 'NONE',
                        1, NULL, '',
                        NULL, '', 0, `createdAtMillis`, `createdAtMillis`
                    FROM `tasks`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `tasks`")
                db.execSQL("ALTER TABLE `tasks_new` RENAME TO `tasks`")

                // Create goals and habit_logs tables
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `periodCode` TEXT NOT NULL,
                        `targetPerPeriod` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `reminderHour` INTEGER NOT NULL,
                        `reminderMinute` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `jalaliDateKey` TEXT NOT NULL,
                        `count` INTEGER NOT NULL,
                        `loggedAtMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_logs_goalId` ON `habit_logs` (`goalId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_goalId_jalaliDateKey` ON `habit_logs` (`goalId`, `jalaliDateKey`)"
                )
            }
        }

        fun getInstance(context: Context): NocturneDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NocturneDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
