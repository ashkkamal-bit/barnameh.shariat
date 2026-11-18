package com.example.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import com.example.MainActivity
import com.example.data.local.NocturneDatabase
import com.example.data.local.toDomain
import com.example.data.repository.UserPreferencesRepository
import com.example.notifications.AlarmScheduler
import com.example.notifications.ReminderReconciliationManager

object NocturneWidgetActions {
    val TASK_ID_PARAM = ActionParameters.Key<Long>("task_id")

    const val EXTRA_WIDGET_ACTION = "extra_widget_action"
    const val EXTRA_WIDGET_TASK_ID = "extra_widget_task_id"
    const val ACTION_OPEN_APP = "open_app"
    const val ACTION_ADD_TASK = "add_task"
    const val ACTION_OPEN_VOICE = "open_voice"
    const val ACTION_OPEN_TASK = "open_task"

    fun launchMainIntent(
        context: Context,
        mode: String = ACTION_OPEN_APP,
        taskId: Long? = null
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WIDGET_ACTION, mode)
            if (taskId != null && taskId > 0L) {
                putExtra(EXTRA_WIDGET_TASK_ID, taskId)
            }
        }
    }

    fun taskToggleParams(taskId: Long): ActionParameters =
        actionParametersOf(TASK_ID_PARAM to taskId)
}

class ToggleTaskCompleteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[NocturneWidgetActions.TASK_ID_PARAM] ?: return
        val db = NocturneDatabase.getInstance(context)
        db.taskDao().toggleTaskCompletionAtomic(taskId)
        val updated = db.taskDao().getTaskById(taskId)?.toDomain()
        if (updated != null) {
            if (updated.isCompleted) {
                AlarmScheduler.cancelTaskAlarms(context, taskId)
            } else {
                val prefs = UserPreferencesRepository(context).getPreferencesOnce()
                AlarmScheduler.scheduleTaskAlarms(context, updated, prefs)
            }
        }
        NocturneWidgetUpdater.updateAll(context)
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        ReminderReconciliationManager.reconcileAll(context, notifyOverdue = false)
    }
}
