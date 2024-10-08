package com.example.todolist

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import androidx.room.Room
import com.example.todolist.database.AppDatabase

class TodoListApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todo_list_database"
        ).build()
    }

    /**
     * Register system broadcast receiver
     */
    private lateinit var systemBroadcastReceiver: SystemBroadcastReceiver
    private lateinit var taskCompletionReceiver: TaskCompletionReceiver

    override fun onCreate() {
        super.onCreate()

        // Register system broadcast receiver
        registerSystemBroadcastReceiver()

        // Register custom broadcast receiver
        registerCustomBroadcastReceiver()
    }

    private fun registerSystemBroadcastReceiver() {
        // Initialize and register the BroadcastReceiver
        systemBroadcastReceiver = SystemBroadcastReceiver()

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }

        // Register the receiver to listen for system broadcasts for the entire app lifecycle
        registerReceiver(systemBroadcastReceiver, intentFilter)
    }

    private fun registerCustomBroadcastReceiver() {
        // Dynamically register the TaskCompletionReceiver
        taskCompletionReceiver = TaskCompletionReceiver()
        val intentFilter = IntentFilter(TaskCompletionReceiver.ACTION_TASK_COMPLETED)

        // Register receiver conditionally based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(taskCompletionReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(taskCompletionReceiver, intentFilter)
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        // Unregister the system broadcast receiver when the app is closing
        unregisterReceiver(systemBroadcastReceiver)

        // Unregister the custom broadcast receiver when the app is closing
        unregisterReceiver(taskCompletionReceiver)

    }
}