package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    // Tasks flow & functions
    val allTasks: Flow<List<TaskEntity>> = appDao.getAllTasks()
    suspend fun insertTask(task: TaskEntity) = appDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = appDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = appDao.deleteTask(task)
    suspend fun deleteTaskById(id: Int) = appDao.deleteTaskById(id)

    // Expenses flow & functions
    val allExpenses: Flow<List<ExpenseEntity>> = appDao.getAllExpenses()
    suspend fun insertExpense(expense: ExpenseEntity) = appDao.insertExpense(expense)
    suspend fun updateExpense(expense: ExpenseEntity) = appDao.updateExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = appDao.deleteExpense(expense)
    suspend fun deleteExpenseById(id: Int) = appDao.deleteExpenseById(id)

    // Grades flow & functions
    val allGrades: Flow<List<GradeEntity>> = appDao.getAllGrades()
    suspend fun insertGrade(grade: GradeEntity) = appDao.insertGrade(grade)
    suspend fun updateGrade(grade: GradeEntity) = appDao.updateGrade(grade)
    suspend fun deleteGrade(grade: GradeEntity) = appDao.deleteGrade(grade)
    suspend fun deleteGradeById(id: Int) = appDao.deleteGradeById(id)

    // GPA flow & functions
    val allGpaItems: Flow<List<GpaEntity>> = appDao.getAllGpaItems()
    suspend fun insertGpa(gpa: GpaEntity) = appDao.insertGpa(gpa)
    suspend fun updateGpa(gpa: GpaEntity) = appDao.updateGpa(gpa)
    suspend fun deleteGpa(gpa: GpaEntity) = appDao.deleteGpa(gpa)
    suspend fun deleteGpaById(id: Int) = appDao.deleteGpaById(id)
    suspend fun clearGpa() = appDao.clearGpaItems()
}
