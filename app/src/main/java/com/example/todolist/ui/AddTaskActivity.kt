package com.example.todolist.ui

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.TimeUtils
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.todolist.R
import com.example.todolist.TodoListApplication
import com.example.todolist.entity.Task
import com.example.todolist.ui.view_models.TaskViewModel
import com.example.todolist.ui.view_models.TaskViewModelFactory
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.Timepoint
import java.util.Timer

class AddTaskActivity : AppCompatActivity() {
    private lateinit var titleText: EditText
    private lateinit var descriptionText: EditText
    private lateinit var deadlineText: EditText
    private lateinit var saveButton: Button
    private lateinit var deadlineBtn: ImageButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    companion object {
        private const val PREF_TEMP_TASK_DATA = "todo_pref"
        private const val TAG = "AddTaskActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addTaskActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Start other implementations here
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Add Task"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initComponents()

        val taskViewModel: TaskViewModel by viewModels {
            TaskViewModelFactory((application as TodoListApplication))
        }


    }

    override fun onStart() {
        super.onStart()
        Log.i("MY_TAG: ", "AddTaskActivity: onStart")
    }

    override fun onPause() {
        super.onPause()
        Log.i("MY_TAG: ", "AddTaskActivity: onPause")

        val title = titleText.text.toString()
        val description = descriptionText.text.toString()
        val deadline = deadlineText.text.toString()

        sharedPreferencesEditor.putString("task_title", title)
        sharedPreferencesEditor.putString("task_description", description)
        sharedPreferencesEditor.putString("task_deadline", deadline)
        sharedPreferencesEditor.apply()
    }

    override fun onResume() {
        super.onResume()
        Log.i("MY_TAG: ", "AddTaskActivity: onResume")

        val title = sharedPreferences.getString("task_title", "")
        val description = sharedPreferences.getString("task_description", "")
        val deadline = sharedPreferences.getString("task_deadline", "")

        titleText.setText(title)
        descriptionText.setText(description)
        deadlineText.setText(deadline)

    }

    override fun onStop() {
        super.onStop()
        Log.i("MY_TAG: ", "AddTaskActivity: onStop")
        if (isFinishing) {
            // Clear only specific temporary data when actually finishing the activity
            clearTemporaryData()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("MY_TAG: ", "AddTaskActivity: onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("MY_TAG: ", "AddTaskActivity: onRestart")
    }

    private fun clearTemporaryData() {
        Log.i(TAG, "Clearing temporary task data")
        sharedPreferences.edit().apply {
            // Clear only specific keys instead of all preferences
            remove("task_title")
            remove("task_description")
            remove("task_deadline")
            apply()
        }
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

    private fun initComponents () {
        titleText = findViewById(R.id.taskTitle)
        descriptionText = findViewById(R.id.taskNote)
        deadlineText = findViewById(R.id.deadlineText)
        saveButton = findViewById(R.id.saveTaskBtn)
        deadlineBtn = findViewById(R.id.deadlineBtn)

        sharedPreferences = getSharedPreferences(PREF_TEMP_TASK_DATA, MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()


        /**
         * Setup drawable for the deadline text field with only top-left and bottom-left
         * corner radius
         * */
        val deadlineTextShapeModel = ShapeAppearanceModel
            .builder()
            .setTopLeftCorner(CornerFamily.ROUNDED, 30f)
            .setBottomLeftCorner(CornerFamily.ROUNDED, 30f)
            .build()

        val deadlineTextDrawable = MaterialShapeDrawable(deadlineTextShapeModel)
        deadlineTextDrawable.fillColor = getColorStateList(R.color.white)
        deadlineText.background = deadlineTextDrawable

        /**
         * Setup drawable for the deadline button with only top-right and bottom-right
         * corner radius
         * */
        val deadlineBtnShapeModel = ShapeAppearanceModel
            .builder()
            .setTopRightCorner(CornerFamily.ROUNDED, 30f)
            .setBottomRightCorner(CornerFamily.ROUNDED, 30f)
            .build()

        val deadlineBtnDrawable = MaterialShapeDrawable(deadlineBtnShapeModel)
        deadlineBtnDrawable.fillColor = getColorStateList(R.color.primary_color)
        deadlineBtn.background = deadlineBtnDrawable

        val datePicker = DatePickerDialog.newInstance { view, year, monthOfYear, dayOfMonth ->
            val timePicker = TimePickerDialog.newInstance(
                { view, hourOfDay, minute, second ->
                    // Handle the selected date and time
                    val calendar = Calendar.getInstance()
                    calendar.set(year, monthOfYear, dayOfMonth, hourOfDay, minute)
                    val formattedDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(calendar.time)
                    deadlineText.setText(formattedDateTime)
                },
                true // Use 24-hour format
            )

            timePicker.setThemeDark(true)
            timePicker.accentColor = getResources().getColor(R.color.primary_color)
            timePicker.setOkColor(getResources().getColor(R.color.primary_color))
            timePicker.setCancelColor(getResources().getColor(R.color.primary_color))
            timePicker.title = "Task deadline time"
            timePicker.vibrate(true)

            timePicker.show(supportFragmentManager, "timePicker")
        }

        datePicker.setThemeDark(true)
        datePicker.accentColor = getResources().getColor(R.color.primary_color)
        datePicker.setOkColor(getResources().getColor(R.color.primary_color))
        datePicker.setCancelColor(getResources().getColor(R.color.primary_color))
        datePicker.setTitle("Task deadline date")
        datePicker.minDate = Calendar.getInstance()
        datePicker.vibrate(true)



        deadlineBtn.setOnClickListener {
            datePicker.show(supportFragmentManager, "dateTimePicker")
        }


        /**
         * Save button click listener.
         * Save the task to the database and finish (i.e close) the activity.
         * */
        saveButton.setOnClickListener {
            // Get the task details from the EditText fields
            val title = titleText.text.toString()
            val description = descriptionText.text.toString()
            val deadline = deadlineText.text.toString()

            // Validate the input
            if (title.isEmpty() || deadline.isEmpty()) {
                Toast.makeText(this@AddTaskActivity, "Please enter a title and deadline", Toast.LENGTH_SHORT).show()
            } else {
                // Save the task to the database
                val task = Task(title = title, description = description, deadline = deadline)
                lifecycleScope.launch {
                    (application as TodoListApplication).database.taskDao().insertTask(task)
                    setResult(Activity.RESULT_OK)
                    Toast.makeText(this@AddTaskActivity, "New task added!", Toast.LENGTH_SHORT).show()
                    clearTemporaryData()
                    finish()
                }
            }
        }
    }

}