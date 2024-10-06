package com.example.todolist.ui.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R
import com.example.todolist.entity.Task
import com.example.todolist.ui.TaskDetailsActivity

class TaskAdapter(
    private val onTaskChecked: (Task, Int) -> Unit,
    private val onTaskDeleted: (Task, Int) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = listOf()

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_card, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskTitle: TextView = itemView.findViewById(R.id.taskTitle)
        private val taskCheckbox: CheckBox = itemView.findViewById(R.id.taskCheckbox)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(task: Task) {
            taskTitle.text = task.title
            taskCheckbox.isChecked = task.isCompleted

            applyStyles(task.isCompleted)

            // Clear any existing listener before setting a new one
            taskCheckbox.setOnCheckedChangeListener(null)

            // Set listener for checkbox state change
            taskCheckbox.setOnCheckedChangeListener { _, isChecked ->
                Log.i("TaskAdapter", "Checkbox state changed to $isChecked for task: ${task.title}")
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTaskChecked(task.copy(isCompleted = isChecked), position)
                }
            }

            // Set listener for delete button click
            deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTaskDeleted(task, position)
                }
            }

            itemView.setOnClickListener {
                val intent = Intent(context, TaskDetailsActivity::class.java).apply {
                    putExtra("task_id", task.id)
                    putExtra("task_title", task.title)
                    putExtra("task_description", task.description)
                    putExtra("task_is_completed", task.isCompleted)
                    putExtra("task_deadline", task.deadline)
                }
                context.startActivity(intent)
                Log.i("TaskAdapter", "Item clicked for task: ${task.title}")
                Log.i("TaskAdapter", "Context: ${context::class.java.simpleName}")
            }
        }

        private fun applyStyles(isCompleted: Boolean) {
            (itemView as CardView).cardElevation = 6f
            if (isCompleted) {
                itemView.setBackgroundResource(R.drawable.ui_radius_task_card_completed)
                taskTitle.setTextColor(ContextCompat.getColor(context, R.color.gray_color))
                taskTitle.paintFlags = taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                taskCheckbox.buttonTintList = ContextCompat.getColorStateList(context, R.color.accent_color)
                deleteButton.imageTintList = ContextCompat.getColorStateList(context, R.color.accent_color)
            } else {
                itemView.setBackgroundResource(R.drawable.ui_radius_task_card)
                taskTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
                taskTitle.paintFlags = taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                taskCheckbox.buttonTintList = ContextCompat.getColorStateList(context, R.color.primary_color)
                deleteButton.imageTintList = ContextCompat.getColorStateList(context, R.color.primary_color)
            }
        }
    }
}