package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.ui.StudentViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.WarningRed
import com.example.ui.theme.WarningRedLight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: StudentViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val filter by viewModel.taskFilter.collectAsState()
    val searchQuery by viewModel.taskSearchQuery.collectAsState()
    val errorMsg by viewModel.errorState.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

    // --- Form Inputs ---
    var titleInput by remember { mutableStateOf("") }
    var courseInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var dueDateInputMs by remember { mutableStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Date formatter helper
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Initialize inputs when editing
    LaunchedEffect(taskToEdit) {
        if (taskToEdit != null) {
            titleInput = taskToEdit!!.title
            courseInput = taskToEdit!!.course
            descriptionInput = taskToEdit!!.description
            dueDateInputMs = taskToEdit!!.dueDate
        } else {
            titleInput = ""
            courseInput = ""
            descriptionInput = ""
            dueDateInputMs = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Manajemen Tugas Kuliah", 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showFormDialog = true
                },
                containerColor = BluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = Color.LightGray)
                    .testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.taskSearchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = Color.LightGray)
                    .testTag("task_search_input"),
                placeholder = { Text("Cari tugas atau mata kuliah...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // --- Filter Tabs ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Semua", "Belum Selesai", "Selesai").forEach { tab ->
                    val isActive = filter == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(if (isActive) 2.dp else 0.dp, RoundedCornerShape(12.dp), ambientColor = Color.LightGray)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) BluePrimary else Color(0xFFF1F5F9))
                            .clickable { viewModel.taskFilter.value = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isActive) Color.White else Color(0xFF475569),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // --- Error Box ---
            if (errorMsg != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarningRedLight)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, "Error", tint = WarningRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMsg!!, color = WarningRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Dismiss", tint = WarningRed)
                        }
                    }
                }
            }

            // --- Task List View ---
            val filteredTasks = tasks.filter { task ->
                val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) || 
                                     task.course.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (filter) {
                    "Selesai" -> task.isCompleted
                    "Belum Selesai" -> !task.isCompleted
                    else -> true
                }
                matchesSearch && matchesFilter
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📓", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak ada tugas yang ditemukan",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemRow(
                            task = task,
                            onCompleteChange = { viewModel.updateTaskStatus(task, it) },
                            onEditClick = {
                                taskToEdit = task
                                showFormDialog = true
                            },
                            onDeleteClick = { viewModel.deleteTaskById(task.id) },
                            dateFormatter = dateFormatter
                        )
                    }
                }
            }
        }
    }

    // --- Task Add/Edit Dialog ---
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = {
                showFormDialog = false
                viewModel.clearError()
            },
            title = {
                Text(
                    text = if (taskToEdit == null) "Tambah Tugas Baru" else "Edit Tugas Kuliah",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title input
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Nama Tugas") },
                        modifier = Modifier.fillMaxWidth().testTag("form_task_title"),
                        singleLine = true
                    )

                    // Course Input
                    OutlinedTextField(
                        value = courseInput,
                        onValueChange = { courseInput = it },
                        label = { Text("Mata Kuliah") },
                        modifier = Modifier.fillMaxWidth().testTag("form_task_course"),
                        singleLine = true
                    )

                    // Due Date Button / Trigger Dialog
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.YEAR, year)
                                        cal.set(Calendar.MONTH, month)
                                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        dueDateInputMs = cal.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = BluePrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Deadline", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    text = dateFormatter.format(Date(dueDateInputMs)),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    // Description Input
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Deskripsi / Catatan") },
                        modifier = Modifier.fillMaxWidth().testTag("form_task_desc"),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskToEdit == null) {
                            viewModel.addTask(
                                title = titleInput,
                                course = courseInput,
                                dueDateMs = dueDateInputMs,
                                description = descriptionInput
                            )
                        } else {
                            viewModel.editTask(
                                id = taskToEdit!!.id,
                                title = titleInput,
                                course = courseInput,
                                dueDateMs = dueDateInputMs,
                                description = descriptionInput,
                                isCompleted = taskToEdit!!.isCompleted
                            )
                        }
                        if (errorMsg == null) {
                            showFormDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFormDialog = false
                    viewModel.clearError()
                }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun TaskItemRow(
    task: TaskEntity,
    onCompleteChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    dateFormatter: SimpleDateFormat
) {
    // Determine Urgency Check
    val currentCal = Calendar.getInstance()
    val dueCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }

    val isToday = currentCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
                  currentCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)

    val diffMs = task.dueDate - System.currentTimeMillis()
    val diffDays = diffMs / (1000 * 60 * 60 * 24)
    val isNear = diffDays in 0..2 && !isToday

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Complete Checkbox
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onCompleteChange,
                    colors = CheckboxDefaults.colors(checkedColor = GreenSecondary)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) Color.Gray else Color.Black
                    )
                    Text(
                        text = task.course,
                        fontSize = 12.sp,
                        color = BluePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Action Menu
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, "Delete", tint = WarningRed, modifier = Modifier.size(20.dp))
                }
            }

            if (task.description.isNotEmpty()) {
                Text(
                    text = task.description,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(start = 48.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Due Date",
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateFormatter.format(Date(task.dueDate)),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // --- URGENCY TAGS ---
                if (!task.isCompleted) {
                    when {
                        isToday -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(WarningRed.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🚨 Deadline Hari Ini",
                                    fontSize = 10.sp,
                                    color = WarningRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        isNear -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF3C7)) // Soft Amber
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⚠ Deadline Mendekati",
                                    fontSize = 10.sp,
                                    color = Color(0xFFD97706), // Amber 600
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Selesai",
                            fontSize = 10.sp,
                            color = GreenSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
