package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Tasks operations
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)


    // Expenses operations
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)


    // Grades operations
    @Query("SELECT * FROM grades ORDER BY id DESC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity)

    @Update
    suspend fun updateGrade(grade: GradeEntity)

    @Delete
    suspend fun deleteGrade(grade: GradeEntity)

    @Query("DELETE FROM grades WHERE id = :id")
    suspend fun deleteGradeById(id: Int)


    // GPA prediction operations
    @Query("SELECT * FROM gpa_items ORDER BY id DESC")
    fun getAllGpaItems(): Flow<List<GpaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGpa(gpa: GpaEntity)

    @Update
    suspend fun updateGpa(gpa: GpaEntity)

    @Delete
    suspend fun deleteGpa(gpa: GpaEntity)

    @Query("DELETE FROM gpa_items WHERE id = :id")
    suspend fun deleteGpaById(id: Int)

    @Query("DELETE FROM gpa_items")
    suspend fun clearGpaItems()
}
