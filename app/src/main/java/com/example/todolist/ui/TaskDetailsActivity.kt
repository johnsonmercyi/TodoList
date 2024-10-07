package com.example.todolist.ui

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todolist.R
import com.example.todolist.TaskCompletionReceiver
import com.example.todolist.TodoListApplication
import com.example.todolist.entity.Task
import com.example.todolist.ui.view_models.TaskViewModel
import com.example.todolist.ui.view_models.TaskViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

class TaskDetailsActivity : AppCompatActivity() {
    private lateinit var titleView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var completedButton: ImageButton
    private lateinit var editButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var deadlineView: TextView
    private lateinit var shareButton: ImageButton

    companion object {
        private const val TAG = "TaskDetailsActivity"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_task_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.taskDetails)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Start other implementations here
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val taskViewModel: TaskViewModel by viewModels {
            TaskViewModelFactory((application as TodoListApplication))
        }

        titleView = findViewById(R.id.title)
        descriptionView = findViewById(R.id.description)
        completedButton = findViewById(R.id.completedButton)
        editButton = findViewById(R.id.editButton)
        deleteButton = findViewById(R.id.deleteButton)
        deadlineView = findViewById(R.id.deadline)
        shareButton = findViewById(R.id.shareButton)

        val id = intent.getIntExtra("task_id", -1)

        completedButton.setOnClickListener {
            // Handle click event for completedButton
            val task = taskViewModel.allTasks.value?.find { it.id == id }
            val updatedTask = task?.copy(isCompleted = true)
            if (updatedTask != null) {
                taskViewModel.updateTask(updatedTask)

                // Dynamically register the TaskCompletionReceiver
//                val receiver = TaskCompletionReceiver()
//                val intentFilter = IntentFilter(TaskCompletionReceiver.ACTION_TASK_COMPLETED)
//
////              Register receiver conditionally based on Android version
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    registerReceiver(receiver, intentFilter, RECEIVER_NOT_EXPORTED)
//                } else {
//                    registerReceiver(receiver, intentFilter)
//                }


                val intent = Intent(TaskCompletionReceiver.ACTION_TASK_COMPLETED).apply {
                    Log.i(TAG, "Sending complete task notification...")
                    putExtra("task_title", task.title)
                }
                intent.setPackage(packageName)
                sendBroadcast(intent)

                Toast.makeText(this, "Task completed!", Toast.LENGTH_SHORT).show()
            }
        }

        editButton.setOnClickListener {
            // Handle click event for editButton
            val intent = Intent(this, EditTaskActivity::class.java)
            intent.putExtra("task_id", id)
            startActivity(intent)
        }

        /**
         * Delete button click listener.
         */
        deleteButton.setOnClickListener {
            // Handle click event for deleteButton

            val task = taskViewModel.allTasks.value?.find { it.id == id }

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Warning")
            builder.setMessage("This is a permanent operation. Are you sure you want to delete this task?")
            builder.setPositiveButton("Yes") { dialog, which ->
                // Delete the task from the database
                if (task != null) {
                    taskViewModel.deleteTask(task)

                    // Optionally, show a Toast to confirm deletion
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, which ->
                // Do nothing or show a Toast to indicate cancellation
                Toast.makeText(this, "Your task is safe!", Toast.LENGTH_SHORT).show()
            }
            val dialog = builder.create()
            dialog.show()
        }

        /**
         * Share button click listener.
         */
        shareButton.setOnClickListener {
            val task = taskViewModel.allTasks.value?.find { it.id == id }
            if (task != null) {
                shareTask(task)
            } else {
                Toast.makeText(this, "Sorry! Task cannot be shared.", Toast.LENGTH_LONG).show()
            }
        }


        /**
         * Observe the LiveData from the ViewModel
         */
        taskViewModel.allTasks.observe(this) { tasks ->
            if (id != -1) {
                val task = tasks.find { it.id == id } // Use stream function to find the task

                if (task != null) {
                    // Update UI elements with task details
                    titleView.text = task.title
                    descriptionView.text = task.description

                    val inputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("EEE, d'th' MMM yyyy 'at' hh:mm a", Locale.getDefault())
                    val date = inputFormat.parse(task.deadline)
                    val outputDate = date?.let { outputFormat.format(it) }

                    deadlineView.text = outputDate

                    if (task.isCompleted) {
                        completedButton.isEnabled = false
                        editButton.isEnabled = false
                        completedButton.setBackgroundResource(R.drawable.ui_radius_task_card_completed)
                        editButton.setBackgroundResource(R.drawable.ui_radius_task_card_completed)
                    } else {
                        completedButton.isEnabled = true
                        editButton.isEnabled = true
                        completedButton.setBackgroundResource(R.drawable.rounded_image_button_primary)
                        editButton.setBackgroundResource(R.drawable.rounded_image_button_primary)
                    }
                }
            }



        }
    }

    private fun shareTask(task: Task) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Hey! I'm excited to share my task with you!${System.lineSeparator()}Check it out: Title: ${task.title}${System.lineSeparator()}Note: ${task.description}")
            type = "text/html"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share Task")
        startActivity(shareIntent)
    }

    override fun onStart() {
        super.onStart()
        Log.i("MY_TAG: ", "TaskDetailsActivity: onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.i("MY_TAG: ", "TaskDetailsActivity: onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i("MY_TAG: ", "TaskDetailsActivity: onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i("MY_TAG: ", "TaskDetailsActivity: onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("MY_TAG: ", "TaskDetailsActivity: onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("MY_TAG: ", "TaskDetailsActivity: onRestart")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish() // Handle back button press
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}