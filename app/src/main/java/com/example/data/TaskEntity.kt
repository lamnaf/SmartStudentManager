package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val course: String,
    val dueDate: Long, // Epoch milliseconds
    val description: String,
    val isCompleted: Boolean = false
)
