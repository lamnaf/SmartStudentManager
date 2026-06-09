package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GradeEntity
import com.example.ui.StudentViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.WarningRed
import com.example.ui.theme.WarningRedLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeScreen(viewModel: StudentViewModel) {
    val grades by viewModel.grades.collectAsState()
    val errorMsg by viewModel.errorState.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }

    // --- Inputs ---
    var courseInput by remember { mutableStateOf("") }
    var taskInput by remember { mutableStateOf("") }
    var utsInput by remember { mutableStateOf("") }
    var uasInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // --- Header Instruction ---
        Card(
            modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Kalkulator Nilai Akhir Kuliah",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bobot penilaian standard universitas: 30% Tugas + 30% UTS + 40% UAS",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // --- Error Alerts ---
        if (errorMsg != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = WarningRedLight)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, "Error", tint = WarningRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(errorMsg!!, color = WarningRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Default.Close, "Dismiss", tint = WarningRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- Action Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Riwayat Nilai", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = { 
                    courseInput = ""
                    taskInput = ""
                    utsInput = ""
                    uasInput = ""
                    showFormDialog = true 
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.testTag("add_grade_log_btn")
            ) {
                Icon(Icons.Default.Add, "Calculate Grade")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kalkulasi Nilai")
            }
        }

        // --- Scrollable History ---
        if (grades.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat kalkulasi nilai.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(grades, key = { it.id }) { item ->
                    GradeItemRow(grade = item, onDeleteClick = { viewModel.deleteGradeById(item.id) })
                }
            }
        }
    }

    // --- Input and Calc Dialog ---
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = {
                showFormDialog = false
                viewModel.clearError()
            },
            title = { Text("Kalkulasi & Simpan Nilai", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = courseInput,
                        onValueChange = { courseInput = it },
                        label = { Text("Nama Mata Kuliah") },
                        modifier = Modifier.fillMaxWidth().testTag("form_grade_course"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = taskInput,
                        onValueChange = { taskInput = it },
                        label = { Text("Nilai Tugas (0-100)") },
                        modifier = Modifier.fillMaxWidth().testTag("form_grade_task"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = utsInput,
                        onValueChange = { utsInput = it },
                        label = { Text("Nilai UTS (0-100)") },
                        modifier = Modifier.fillMaxWidth().testTag("form_grade_uts"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uasInput,
                        onValueChange = { uasInput = it },
                        label = { Text("Nilai UAS (0-100)") },
                        modifier = Modifier.fillMaxWidth().testTag("form_grade_uas"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.calculateAndSaveGrade(
                            courseName = courseInput,
                            taskText = taskInput,
                            utsText = utsInput,
                            uasText = uasInput
                        )
                        if (errorMsg == null) {
                            showFormDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Hitung & Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFormDialog = false
                    viewModel.clearError()
                }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun GradeItemRow(grade: GradeEntity, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(grade.courseName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(
                    text = "Tugas: ${grade.taskGrade.toInt()} • UTS: ${grade.utsGrade.toInt()} • UAS: ${grade.uasGrade.toInt()}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (grade.status == "Lulus") GreenSecondary.copy(alpha = 0.15f)
                                else WarningRed.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = grade.status,
                            fontSize = 9.sp,
                            color = if (grade.status == "Lulus") GreenSecondary else WarningRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Highlight Grade
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 6.dp)) {
                Text(
                    text = String.format("%.1f", grade.finalGrade),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BluePrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(grade.gradeLetter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Delete Grade", tint = WarningRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}
