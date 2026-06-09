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
import com.example.data.GpaEntity
import com.example.ui.StudentViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.WarningRed
import com.example.ui.theme.WarningRedLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaScreen(viewModel: StudentViewModel) {
    val gpaItems by viewModel.gpaItems.collectAsState()
    val errorMsg by viewModel.errorState.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }

    // --- Form Fields ---
    var courseInput by remember { mutableStateOf("") }
    var sksInput by remember { mutableStateOf("") }
    var gradeInput by remember { mutableStateOf("A") }

    val gradeOptions = listOf("A", "B", "C", "D", "E")

    // --- Calculations ---
    val totalSks = gpaItems.sumOf { it.sks }
    val totalCourses = gpaItems.size
    val totalWeightSks = gpaItems.sumOf { it.weight * it.sks }
    val calculatedGpa = if (totalSks > 0) totalWeightSks / totalSks else 0.0

    val ipkCategory = when {
        calculatedGpa >= 3.5 -> "🏆 Sangat Memuaskan"
        calculatedGpa >= 3.0 -> "🎯 Memuaskan"
        calculatedGpa >= 2.0 -> "📚 Baik"
        gpaItems.isNotEmpty() -> "⚠️ Perlu Peningkatan"
        else -> "Tambahkan matkul"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // --- GPA Statistics Dashboard Overview Card ---
        Card(
            modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Prediksi IPK Kumulatif", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = String.format("%.2f", calculatedGpa), 
                            fontSize = 32.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = GreenSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(ipkCategory, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenSecondary)
                    }
                }

                Divider(color = Color(0xFFF1F5F9))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Mata Kuliah", fontSize = 11.sp, color = Color.Gray)
                        Text("$totalCourses Matkul", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total SKS Terdaftar", fontSize = 11.sp, color = Color.Gray)
                        Text("$totalSks SKS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        // --- Error Alert ---
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

        // --- Action Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { viewModel.clearGpaItems() },
                colors = ButtonDefaults.textButtonColors(contentColor = WarningRed)
            ) {
                Text("Bersihkan Semua")
            }

            Button(
                onClick = {
                    courseInput = ""
                    sksInput = ""
                    gradeInput = "A"
                    showFormDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSecondary),
                modifier = Modifier.testTag("add_course_gpa_btn")
            ) {
                Icon(Icons.Default.Add, "Add Course")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Matkul")
            }
        }

        // --- List of Courses ---
        if (gpaItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada mata kuliah terdaftar untuk prediksi IPK.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(gpaItems, key = { it.id }) { item ->
                    GpaItemRow(item = item, onDeleteClick = { viewModel.deleteGpaItemById(item.id) })
                }
            }
        }
    }

    // --- Course Add Dialog ---
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = {
                showFormDialog = false
                viewModel.clearError()
            },
            title = { Text("Tambah Matkul Prediksi IPK", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = courseInput,
                        onValueChange = { courseInput = it },
                        label = { Text("Nama Mata Kuliah") },
                        modifier = Modifier.fillMaxWidth().testTag("form_gpa_course"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sksInput,
                        onValueChange = { sksInput = it },
                        label = { Text("Jumlah SKS") },
                        modifier = Modifier.fillMaxWidth().testTag("form_gpa_sks"),
                        singleLine = true
                    )

                    // Grade letters select row
                    Text("Pilih Grade Surat", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gradeOptions.forEach { grade ->
                            val isSelected = gradeInput == grade
                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GreenSecondary else Color(0xFFF1F5F9))
                                    .clickable { gradeInput = grade }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = grade,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addGpaItem(
                            courseName = courseInput,
                            sksText = sksInput,
                            gradeLetter = gradeInput
                        )
                        if (errorMsg == null) {
                            showFormDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSecondary)
                ) {
                    Text("Tambah")
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
fun GpaItemRow(item: GpaEntity, onDeleteClick: () -> Unit) {
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
                Text(item.courseName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Beban: ${item.sks} SKS • Bobot Grade: ${item.weight}", fontSize = 11.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Grade letter circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GreenSecondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.gradeLetter,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenSecondary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Delete Course", tint = WarningRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}
