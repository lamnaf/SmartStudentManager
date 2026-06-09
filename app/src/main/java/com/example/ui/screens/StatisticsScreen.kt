package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudentViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.WarningRed

@Composable
fun StatisticsScreen(viewModel: StudentViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gpaItems by viewModel.gpaItems.collectAsState()

    // --- Core Analytic Counts ---
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = totalTasks - completedTasks

    // Expense sums by Category
    val expenseSums = expenses.groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
    val totalExpense = expenses.sumOf { it.amount }

    // Academic averages
    val avgGrade = if (grades.isNotEmpty()) grades.map { it.finalGrade }.average() else 0.0

    // GPA calculations
    val totalSks = gpaItems.sumOf { it.sks }
    val weightSks = gpaItems.sumOf { it.weight * it.sks }
    val predictedGpa = if (totalSks > 0) weightSks / totalSks else 0.0

    // High and Low Course Final Grades
    val maxGrade = if (grades.isNotEmpty()) grades.maxOf { it.finalGrade } else 0.0
    val minGrade = if (grades.isNotEmpty()) grades.minOf { it.finalGrade } else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Text("Statistik & KPI Akademik", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        // --- KPI Dashboard ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMiniCard(
                title = "IPK Prediksi",
                value = String.format("%.2f", predictedGpa),
                color = GreenSecondary,
                modifier = Modifier.weight(1f)
            )
            KpiMiniCard(
                title = "Nilai Tertinggi",
                value = String.format("%.1f", maxGrade),
                color = BluePrimary,
                modifier = Modifier.weight(1f)
            )
            KpiMiniCard(
                title = "Nilai Terendah",
                value = String.format("%.1f", minGrade),
                color = WarningRed,
                modifier = Modifier.weight(1f)
            )
        }

        // --- Task Status Pie Chart Card ---
        Card(
            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Status Penyelesaian Tugas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (totalTasks == 0) {
                    Text("Belum ada data tugas kuliah.", color = Color.Gray, fontSize = 12.sp)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val completeRatio = completedTasks.toFloat() / totalTasks.toFloat()
                                drawArc(
                                    color = Color(0xFFF1F5F9),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx())
                                )
                                drawArc(
                                    color = GreenSecondary,
                                    startAngle = -90f,
                                    sweepAngle = completeRatio * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12.dp.toPx())
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.0f%%", (completedTasks.toFloat() / totalTasks) * 100),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Selesai", fontSize = 8.sp, color = Color.Gray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(GreenSecondary, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Selesai ($completedTasks)", fontSize = 11.sp, color = Color.DarkGray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pending ($pendingTasks)", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }

        // --- Category Expenses Bar Chart Card ---
        Card(
            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Distribusi Pengeluaran Keuangan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (expenses.isEmpty()) {
                    Text(
                        text = "Belum ada data keuangan untuk grafik.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    val maxExpense = expenseSums.values.maxOrNull() ?: 1.0

                    // Draw Beautiful Native Bar Columns
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        expenseSums.forEach { (cat, amount) ->
                            val ratio = (amount / maxExpense).toFloat()
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Rp " + String.format("%,.0f", amount).replace(',', '.'), fontSize = 11.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF1F5F9))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(ratio)
                                            .background(WarningRed)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Grades Line Plot Card ---
        Card(
            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tren Nilai Kuliah Akhir",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (grades.isEmpty()) {
                    Text(
                        text = "Belum ada riwayat nilai untuk grafik tren.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Custom Canvas Line Chart Plotter
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        val points = grades.map { it.finalGrade }
                        val widthBetweenPoints = size.width / (points.size.coerceAtLeast(2) - 1)
                        val maxPoints = 100f
                        val heightRatio = size.height / maxPoints

                        // Draw Grid lines
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(0f, size.height * 0.2f),
                            end = Offset(size.width, size.height * 0.2f)
                        )
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(0f, size.height * 0.5f),
                            end = Offset(size.width, size.height * 0.5f)
                        )

                        // Plot connecting lines and circles
                        for (i in 0 until points.size - 1) {
                            val startX = i * widthBetweenPoints
                            val startY = size.height - (points[i].toFloat() * heightRatio)
                            val endX = (i + 1) * widthBetweenPoints
                            val endY = size.height - (points[i+1].toFloat() * heightRatio)

                            drawLine(
                                color = BluePrimary,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 3.dp.toPx()
                            )
                            drawCircle(
                                color = BluePrimary,
                                radius = 4.dp.toPx(),
                                center = Offset(startX, startY)
                            )
                            if (i == points.size - 2) {
                                drawCircle(
                                    color = BluePrimary,
                                    radius = 4.dp.toPx(),
                                    center = Offset(endX, endY)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Average score: ${String.format("%.1f", avgGrade)} points",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun KpiMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
