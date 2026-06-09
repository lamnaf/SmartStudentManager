package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // e.g. "🍔 Makan", "🚌 Transportasi", "📚 Kuliah", "🎮 Hiburan", "📦 Lainnya"
    val amount: Double,
    val date: Long // Epoch milliseconds
)
