package com.example.todolist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.example.todolist.ui.AddTaskActivity
import com.example.todolist.ui.QuoteActivity
import com.example.todolist.ui.TaskDetailsActivity
import com.example.todolist.ui.adapter.TaskAdapter
import com.example.todolist.ui.view_models.TaskViewModel
import com.example.todolist.ui.view_models.TaskViewModelFactory
import com.example.todolist.workers.TaskReminderWorker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var taskAdapter: TaskAdapter

    //
    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission and schedule worker
        requestNotificationPermission()

        handleNotificationClick(intent)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "To-Do List"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val taskViewModel: TaskViewModel by viewModels {
            TaskViewModelFactory((application as TodoListApplication))
        }

        // Set up Recycler
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        taskAdapter = TaskAdapter(
            onTaskChecked = { updatedTask, _ ->
                // Update the task in the database
                taskViewModel.updateTask(updatedTask)

                // Check if the task is marked as completed
                if (updatedTask.isCompleted) {

                    // Send broadcast to TaskCompletionReceiver to show notification
                    // Implicit Intent usage demonstrated
                    val intent = Intent(TaskCompletionReceiver.ACTION_TASK_COMPLETED).apply {
                        Log.i(TAG, "Sending complete task notification...")
                        putExtra("task_title", updatedTask.title)
                        putExtra("task_id", updatedTask.id)
                    }
                    intent.setPackage(packageName)
                    sendBroadcast(intent)

                    Toast.makeText(this, "Task completed!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Task completion cancelled!", Toast.LENGTH_SHORT).show()
                }
            },
            onTaskDeleted = { task, _ ->
                taskViewModel.deleteTask(task)
                Toast.makeText(this, "Task deleted!", Toast.LENGTH_SHORT).show()
            }, context = this
        )

        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)

        taskViewModel.allTasks.observe(this) { tasks ->
            taskAdapter.updateTasks(tasks)
        }

        // Handles floating action button click and opens AddTaskActivity
        findViewById<FloatingActionButton>(R.id.addTaskFab).setOnClickListener {
            // Explicit Intent usage demonstrated
            val intent = Intent(this, AddTaskActivity::class.java)
             startActivity(intent)
        }

        // Handles floating action button click and opens QuoteActivity
        findViewById<FloatingActionButton>(R.id.quoteFab).setOnClickListener {
            // Explicit Intent usage demonstrated
            val intent = Intent(this, QuoteActivity::class.java)
            startActivity(intent)
        }

    }

    // Request notification permission
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            // If permission was not granted, request it.
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission")

                // Request permission for notification
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            } else {
                // Permission was granted, schedule worker
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted")
                scheduleTaskReminderWorker()
            }
        } else {
            // For devices running lower than Tiramisu, schedule worker directly
            Log.d(TAG, "Running on pre-Tiramisu device, scheduling worker directly")
            scheduleTaskReminderWorker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted")
                scheduleTaskReminderWorker()
            } else {
                Log.d(TAG, "Notification permission denied")
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Schedule TaskReminderWorker Service to run periodically
    private fun scheduleTaskReminderWorker() {
        val workRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(
            12, TimeUnit.HOURS,  // Minimum interval allowed
            5, TimeUnit.MINUTES    // Flex interval
        ).setInitialDelay(1, TimeUnit.MINUTES)  // Initial delay
            .build()

        Log.d(TAG, "Scheduling TaskReminderWorker")

        // Enqueue the work request
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TaskReminderWorker",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun handleNotificationClick(intent: Intent?) {
        // Handle notification click
        intent?.getIntExtra("TASK_ID", -1)?.let { taskId ->
            if (taskId != -1) {
                // Navigate to task detail screen
                // And also demonstrates explicit intent usage
                val detailsIntent = Intent(this, TaskDetailsActivity::class.java).apply {
                    putExtra("task_id", taskId)
                }

                startActivity(detailsIntent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("MY_TAG: ", "MainActivity: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.i("MY_TAG: ", "MainActivity: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i("MY_TAG: ", "MainActivity: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i("MY_TAG: ", "MainActivity: onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("MY_TAG: ", "MainActivity: onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("MY_TAG: ", "MainActivity: onRestart")
    }

}