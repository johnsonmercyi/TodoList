package com.example.todolist

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat


class SystemBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "system_notifications"
        private const val CHANNEL_NAME = "System Notifications"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TodoListApplication
        createNotificationChannel(context)

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                // Notify user to check tasks when charging
                showNotification(context, "Device Charging", "Don't forget to check your tasks!", app)
            }
            WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

                val isWifiConnected = networkCapabilities != null &&
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

                if (isWifiConnected) {
                    // Notify user to share tasks when connected to Wi-Fi
                    showNotification(context, "Wi-Fi Connected", "You can share your tasks with your friends!", app)
                }
            }
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
                description = "Notifications for system events"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, title: String, content: String, app: TodoListApplication) {

        if (ContextCompat.checkSelfPermission(
                app.applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("TaskBroadcastReceiver", "Notification permission not granted")
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon_primary)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

}