package com.example.todolist

import android.app.Application
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
}