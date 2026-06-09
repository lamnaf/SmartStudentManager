package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseName: String,
    val taskGrade: Double,
    val utsGrade: Double,
    val uasGrade: Double,
    val finalGrade: Double,
    val gradeLetter: String, // A, B, C, D, E
    val status: String // "Lulus" or "Tidak Lulus"
)
