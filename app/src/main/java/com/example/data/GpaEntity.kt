package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gpa_items")
data class GpaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseName: String,
    val sks: Int,
    val gradeLetter: String, // A, B, C, D, E
    val weight: Double // A = 4.0, B = 3.0, C = 2.0, D = 1.0, E = 0.0
)
