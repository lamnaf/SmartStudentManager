package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class StudentViewModel(
    private val repository: AppRepository,
    context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("smart_student_prefs", Context.MODE_PRIVATE)

    // --- Dynamic Pocket Allowance / Budget ---
    private val _monthlyBudget = MutableStateFlow(sharedPrefs.getFloat("monthly_budget", 3000000.0f).toDouble()) // Load from Prefs
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    fun updateMonthlyBudget(newBudget: Double) {
        _monthlyBudget.value = newBudget
        sharedPrefs.edit().putFloat("monthly_budget", newBudget.toFloat()).apply() // Save to Prefs
    }

    // --- State Streams from Database ---
    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grades: StateFlow<List<GradeEntity>> = repository.allGrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gpaItems: StateFlow<List<GpaEntity>> = repository.allGpaItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Input Form States ---
    val taskFilter = MutableStateFlow("Semua") // "Semua", "Selesai", "Belum Selesai"
    val taskSearchQuery = MutableStateFlow("")

    val errorState = MutableStateFlow<String?>(null)

    fun clearError() {
        errorState.value = null
    }

    // --- Validation Helper ---
    private fun validateText(text: String, fieldName: String): Boolean {
        if (text.trim().isEmpty()) {
            errorState.value = "$fieldName tidak boleh kosong!"
            return false
        }
        return true
    }

    // --- Task Database Actions ---
    fun addTask(title: String, course: String, dueDateMs: Long, description: String) {
        if (!validateText(title, "Nama Tugas") || !validateText(course, "Mata Kuliah")) return
        viewModelScope.launch {
            try {
                repository.insertTask(
                    TaskEntity(
                        title = title.trim(),
                        course = course.trim(),
                        dueDate = dueDateMs,
                        description = description.trim(),
                        isCompleted = false
                    )
                )
                clearError()
            } catch (e: Exception) {
                errorState.value = "Gagal menyimpan tugas: ${e.message}"
            }
        }
    }

    fun updateTaskStatus(task: TaskEntity, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = isCompleted))
        }
    }

    fun editTask(id: Int, title: String, course: String, dueDateMs: Long, description: String, isCompleted: Boolean) {
        if (!validateText(title, "Nama Tugas") || !validateText(course, "Mata Kuliah")) return
        viewModelScope.launch {
            try {
                repository.updateTask(
                    TaskEntity(
                        id = id,
                        title = title.trim(),
                        course = course.trim(),
                        dueDate = dueDateMs,
                        description = description.trim(),
                        isCompleted = isCompleted
                    )
                )
                clearError()
            } catch (e: Exception) {
                errorState.value = "Gagal mengupdate tugas: ${e.message}"
            }
        }
    }

    fun deleteTaskById(id: Int) {
        viewModelScope.launch {
            repository.deleteTaskById(id)
        }
    }

    // --- Expense Database Actions ---
    fun addExpense(name: String, category: String, amountText: String, dateMs: Long) {
        if (!validateText(name, "Nama Pengeluaran")) return
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            errorState.value = "Nominal pengeluaran harus berupa angka positif!"
            return
        }
        viewModelScope.launch {
            try {
                repository.insertExpense(
                    ExpenseEntity(
                        name = name.trim(),
                        category = category,
                        amount = amount,
                        date = dateMs
                    )
                )
                clearError()
            } catch (e: Exception) {
                errorState.value = "Gagal menyimpan pengeluaran: ${e.message}"
            }
        }
    }

    fun editExpense(id: Int, name: String, category: String, amountText: String, dateMs: Long) {
        if (!validateText(name, "Nama Pengeluaran")) return
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            errorState.value = "Nominal pengeluaran harus berupa angka positif!"
            return
        }
        viewModelScope.launch {
            try {
                repository.updateExpense(
                    ExpenseEntity(
                        id = id,
                        name = name.trim(),
                        category = category,
                        amount = amount,
                        date = dateMs
                    )
                )
                clearError()
            } catch (e: Exception) {
                errorState.value = "Gagal mengubah pengeluaran: ${e.message}"
            }
        }
    }

    fun deleteExpenseById(id: Int) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    // --- Calculator Grade Actions ---
    fun calculateAndSaveGrade(courseName: String, taskText: String, utsText: String, uasText: String) {
        if (!validateText(courseName, "Nama Mata Kuliah")) return
        
        val task = taskText.toDoubleOrNull()
        val uts = utsText.toDoubleOrNull()
        val uas = uasText.toDoubleOrNull()

        if (task == null || task < 0 || task > 100 ||
            uts == null || uts < 0 || uts > 100 ||
            uas == null || uas < 0 || uas > 100
        ) {
            errorState.value = "Semua nilai harus berupa angka antara 0 - 100!"
            return
        }

        val finalGrade = (0.3 * task) + (0.3 * uts) + (0.4 * uas)
        val gradeLetter = when {
            finalGrade >= 80 -> "A"
            finalGrade >= 70 -> "B"
            finalGrade >= 60 -> "C"
            finalGrade >= 50 -> "D"
            else -> "E"
        }
        val status = if (finalGrade >= 60) "Lulus" else "Tidak Lulus"

        viewModelScope.launch {
            try {
                repository.insertGrade(
                    GradeEntity(
                        courseName = courseName.trim(),
                        taskGrade = task,
                        utsGrade = uts,
                        uasGrade = uas,
                        finalGrade = finalGrade,
                        gradeLetter = gradeLetter,
                        status = status
                    )
                )
                clearError()
            } catch (e: Exception) {
                errorState.value = "Gagal menyimpan nilai: ${e.message}"
            }
        }
    }

    fun deleteGradeById(id: Int) {
        viewModelScope.launch {
            repository.deleteGradeById(id)
        }
    }

    // --- GPA Prediction Actions ---
    fun addGpaItem(courseName: String, sksText: String, gradeLetter: String) {
        if (!validateText(courseName, "Nama Mata Kuliah")) return
        val sks = sksText.toIntOrNull()
        if (sks == null || sks <= 0) {
            errorState.value = "SKS harus berupa angka positif lebih dari 0!"
            return
        }

        val weight = when (gradeLetter) {
            "A" -> 4.0
            "B" -> 3.0
            "C" -> 2.0
            "D" -> 1.0
            "E" -> 0.0
            else -> {
                errorState.value = "Grade tidak valid!"
                return
            }
        }

        viewModelScope.launch {
            try {
                repository.insertGpa(
                    GpaEntity(
                        courseName = courseName.trim(),
                        sks = sks,
                        gradeLetter = gradeLetter,
                        weight = weight
                    )
                )
                clearError()
            } catch (e: Exception) {
                errorState.value = "Gagal menyimpan prediksi matkul: ${e.message}"
            }
        }
    }

    fun deleteGpaItemById(id: Int) {
        viewModelScope.launch {
            repository.deleteGpaById(id)
        }
    }

    fun clearGpaItems() {
        viewModelScope.launch {
            repository.clearGpa()
        }
    }
}

class StudentViewModelFactory(
    private val repository: AppRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
