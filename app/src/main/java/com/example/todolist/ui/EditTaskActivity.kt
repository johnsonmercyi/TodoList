package com.example.todolist.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.Timepoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditTaskActivity : AppCompatActivity() {
    private lateinit var titleText: EditText
    private lateinit var descriptionText: EditText
    private lateinit var deadlineText: EditText
    private lateinit var editButton: Button
    private lateinit var deadlineBtn: ImageButton
    private lateinit var task: Task

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editTaskActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Start other implementations here
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Edit Task"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initComponents()

        val taskViewModel: TaskViewModel by viewModels {
            TaskViewModelFactory((application as TodoListApplication))
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
        editButton = findViewById(R.id.editTaskBtn)
        deadlineBtn = findViewById(R.id.deadlineBtn)
        val app = (application as TodoListApplication)

        // Get Task values from Intent
        val id = intent.getIntExtra("task_id", -1)

        lifecycleScope.launch {
            // Retrieve task with the given ID from the database
            task = app.database.taskDao().getTaskById(id).first()

            // Update the UI with the task details
            titleText.setText(task.title)
            descriptionText.setText(task.description)
            deadlineText.setText(task.deadline)
        }

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
            timePicker.setMinTime(Timepoint(Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND))
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
        editButton.setOnClickListener {
            // Get the task details from the EditText fields
            val newTitle = titleText.text.toString()
            val newDescription = descriptionText.text.toString()
            val newDeadline = deadlineText.text.toString()

            Log.i("EDIT_TASK", "Title: $newTitle, Description: $newDescription, Deadline: $newDeadline")

            // Validate the input
            if (newTitle.isEmpty() || newDeadline.isEmpty()) {
                Toast.makeText(this@EditTaskActivity, "Title and deadline are required", Toast.LENGTH_SHORT).show()
            } else {
                // update this task in the database
                val task = Task(id = task.id, title = newTitle, description = newDescription, deadline = newDeadline, isCompleted = task.isCompleted)
                lifecycleScope.launch {
                    app.database.taskDao().updateTask(task)
                    setResult(Activity.RESULT_OK)
                    Toast.makeText(this@EditTaskActivity, "Task updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

}