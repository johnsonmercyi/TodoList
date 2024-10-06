package com.example.todolist.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.todolist.MainActivity
import com.example.todolist.R
import com.example.todolist.TaskBroadcastReceiver
import com.example.todolist.TodoListApplication
import com.example.todolist.entity.Task
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class TaskReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    companion object {
        private const val TAG = "TaskReminderWorker"
        private const val CHANNEL_ID = "task_reminder_channel"
        private const val CHANNEL_NAME = "Task Reminders"
        private const val GROUP_KEY = "com.todolist.TASK_NOTIFICATIONS"
        private const val SUMMARY_ID = 0
    }

    override fun doWork(): Result {
        Log.i(TAG, "Worker started execution") // Changed to Log.i for higher visibility

        createNotificationChannel()

        try {
            val pendingTasks = runBlocking { getPendingTasks() }
            Log.i(TAG, "Found ${pendingTasks.size} pending tasks")

            if (pendingTasks.isEmpty()) {
                Log.i(TAG, "No pending tasks to notify about")
                return Result.success()
            }

            pendingTasks.forEach { task ->
                showNotification(task)
            }

            // Show summary notification if there are multiple tasks
            if (pendingTasks.size > 1) {
                showSummaryNotification(pendingTasks.size)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in doWork()", e)
            return Result.failure()
        }
    }

    private fun createPendingIntent(task: Task? = null): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            task?.let {
                // Add task ID to open specific task detail screen if needed
                putExtra("TASK_ID", it.id)
            }
        }

        return PendingIntent.getActivity(
            applicationContext,
            task?.id?.toInt() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for task reminders"
            }

            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private suspend fun getPendingTasks(): List<Task> {
        val app = applicationContext as TodoListApplication
        return app.database.taskDao().getIncompleteTasks().first()
    }

    private fun showNotification(task: Task) {
        Log.i(TAG, "Preparing notification for task: ${task.title}")

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Notification permission not granted")
            return
        }

        try {
            val notificationManager = NotificationManagerCompat.from(applicationContext)

            // Create delete intent to handle notification dismissal
            val deleteIntent = Intent(applicationContext, TaskBroadcastReceiver::class.java).apply {
                action = "com.todolist.NOTIFICATION_DISMISSED"
                putExtra("TASK_ID", task.id)
            }
            val deletePendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                task.id.toInt(),
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create notification
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Task Reminder")
                .setContentText(task.title)
                .setSmallIcon(R.drawable.ic_notification_icon_primary)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setDeleteIntent(deletePendingIntent)
                .setContentIntent(createPendingIntent(task))
                // Add actions if needed
                .addAction(
                    R.drawable.ic_check,
                    "Mark Complete",
                    createMarkCompletePendingIntent(task)
                )
                .build()

            notificationManager.notify(task.id.toInt(), notification)
            Log.i(TAG, "Notification shown for task: ${task.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    private fun showSummaryNotification(taskCount: Int) {

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Notification permission not granted")
            return
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        val summaryNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Task Reminders")
            .setContentText("You have $taskCount pending tasks")
            .setSmallIcon(R.drawable.ic_notification_icon_primary)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent())
            .build()

        notificationManager.notify(SUMMARY_ID, summaryNotification)
    }

    private fun createMarkCompletePendingIntent(task: Task): PendingIntent {
        val intent = Intent(applicationContext, TaskBroadcastReceiver::class.java).apply {
            action = "com.todolist.MARK_TASK_COMPLETE"
            putExtra("TASK_ID", task.id)
        }

        return PendingIntent.getBroadcast(
            applicationContext,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}