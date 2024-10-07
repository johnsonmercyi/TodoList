package com.example.todolist

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class TaskCompletionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TASK_COMPLETED = "com.example.todolist.TASK_COMPLETED"
        private const val CHANNEL_ID = "task_completion_notifications"
        private const val CHANNEL_NAME = "Task Completion Notifications"
        private const val TAG = "TaskCompletionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Request to notify received!")
        val app = context.applicationContext as TodoListApplication
        createNotificationChannel(context)

        if (intent.action == ACTION_TASK_COMPLETED) {
            val taskTitle = intent.getStringExtra("task_title") ?: "Task"
            showNotification(context, taskTitle, app)
            Log.i(TAG, "Notification task completed!")
        }
    }

    /**
     * Create notification channel for system notifications
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for task completion"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, taskTitle: String, app: TodoListApplication) {

        if (ContextCompat.checkSelfPermission(
                app.applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("TaskCompletionReceiver", "Notification permission not granted")
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon_primary)
            .setContentTitle("Task Completed")
            .setContentText("'$taskTitle' has been completed 🎉🎇.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.i(TAG, "Notification sent!")
    }
}