package com.example.todolist

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todolist.entity.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val GROUP_KEY = "com.todolist.TASK_NOTIFICATIONS"
        private const val SUMMARY_ID = 0
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.todolist.MARK_TASK_COMPLETE" -> handleMarkComplete(context, intent)
            "com.todolist.NOTIFICATION_DISMISSED" -> handleNotificationDismissed(context, intent)
        }
    }

    private fun handleMarkComplete(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        if (taskId != -1) {
            // Remove the notification
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(taskId.toInt())

            // Update task in database
            CoroutineScope(Dispatchers.IO).launch {
                val app = context.applicationContext as TodoListApplication
                val task: Task = app.database.taskDao().getTaskById(taskId).first()
                app.database.taskDao().updateTask(task.copy(isCompleted = true))

                // Check if there are any remaining notifications
                checkAndUpdateSummary(context, app)
            }
        }
    }

    private fun handleNotificationDismissed(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                val app = context.applicationContext as TodoListApplication
                // Check if there are any remaining notifications
                checkAndUpdateSummary(context, app)
            }
        }
    }

    private suspend fun checkAndUpdateSummary(context: Context, app: TodoListApplication) {

        if (ContextCompat.checkSelfPermission(
                app.applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("TaskBroadcastReceiver", "Notification permission not granted")
            return
        }

        val remainingTasks = app.database.taskDao().getIncompleteTasks().first()
        val notificationManager = NotificationManagerCompat.from(context)

        if (remainingTasks.isEmpty()) {
            // No more tasks, remove summary notification
            notificationManager.cancel(SUMMARY_ID)
        } else {
            // Update summary notification with new count
            val summaryNotification = NotificationCompat.Builder(context, "task_reminder_channel")
                .setContentTitle("Task Reminders")
                .setContentText("You have ${remainingTasks.size} pending tasks")
                .setSmallIcon(R.drawable.ic_notification_icon_primary)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(SUMMARY_ID, summaryNotification)
        }
    }
}