package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudentViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.WarningRed

fun formatRupiah(amount: Double): String {
    return "Rp " + String.format("%,.0f", amount).replace(',', '.')
}

@Composable
fun DashboardScreen(
    viewModel: StudentViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToAcademic: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gpaItems by viewModel.gpaItems.collectAsState()
    val allowance by viewModel.monthlyBudget.collectAsState()

    // --- Statistics Calculations ---
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = totalTasks - completedTasks

    val totalExpenses = expenses.sumOf { it.amount }
    val remainingMoney = allowance - totalExpenses

    val avgGrade = if (grades.isNotEmpty()) {
        grades.map { it.finalGrade }.average()
    } else {
        0.0
    }

    // --- GPA Prediction Calculation ---
    val totalSks = gpaItems.sumOf { it.sks }
    val totalWeightSks = gpaItems.sumOf { it.weight * it.sks }
    val predictedGpa = if (totalSks > 0) totalWeightSks / totalSks else 0.0

    val gpaCategory = when {
        predictedGpa >= 3.5 -> "🏆 Sangat Memuaskan"
        predictedGpa >= 3.0 -> "🎯 Memuaskan"
        predictedGpa >= 2.0 -> "📚 Baik"
        gpaItems.isNotEmpty() -> "⚠ Perlu Peningkatan"
        else -> "Belum ada matkul"
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BluePrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎓",
                                fontSize = 20.sp
                            )
                        }
                        Column {
                            Text(
                                text = "SMART STUDENT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Halo Mahasiswa 👋",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp))
                            .background(Color.White, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Student Icon",
                            tint = BluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // --- Financial Standing Warning Badge or Alert ---
            if (remainingMoney < (0.15 * allowance)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarningRed.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = WarningRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (remainingMoney < 0) "🚨 Sisa uang Anda minus! Kurangi pengeluaran." 
                              else "🚨 Sisa uang menipis! (Kurang dari 15%)",
                        fontSize = 12.sp,
                        color = WarningRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // --- Grid of Summary Cards ---
            Text(
                text = "Ringkasan Aktivitas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Total Tugas",
                    value = "$totalTasks",
                    subtitle = "$completedTasks Selesai",
                    icon = Icons.Default.List,
                    color = BluePrimary,
                    modifier = Modifier.weight(1f).clickable { onNavigateToTasks() }
                )
                SummaryCard(
                    title = "Pending Tugas",
                    value = "$pendingTasks",
                    subtitle = "Harus dikerjakan",
                    icon = Icons.Default.Timer,
                    color = WarningRed,
                    modifier = Modifier.weight(1f).clickable { onNavigateToTasks() }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Pengeluaran",
                    value = formatRupiah(totalExpenses),
                    subtitle = "Bulan ini",
                    icon = Icons.Default.ShoppingCart,
                    color = WarningRed,
                    modifier = Modifier.weight(1.1f).clickable { onNavigateToFinance() }
                )
                SummaryCard(
                    title = "Sisa Uang",
                    value = formatRupiah(remainingMoney),
                    subtitle = "Dari budget",
                    icon = Icons.Default.Wallet,
                    color = if (remainingMoney >= 0) GreenSecondary else WarningRed,
                    modifier = Modifier.weight(1.1f).clickable { onNavigateToFinance() }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    title = "Rata-rata Nilai",
                    value = String.format("%.1f", avgGrade),
                    subtitle = "Dari ${grades.size} matkul",
                    icon = Icons.Default.Assignment,
                    color = BluePrimary,
                    modifier = Modifier.weight(1f).clickable { onNavigateToAcademic() }
                )
                SummaryCard(
                    title = "Prediksi IPK",
                    value = String.format("%.2f", predictedGpa),
                    subtitle = gpaCategory,
                    icon = Icons.Default.Grade,
                    color = GreenSecondary,
                    modifier = Modifier.weight(1.1f).clickable { onNavigateToAcademic() }
                )
            }

            // --- Native Visual Ring Progress Section ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val expensePercentage = if (allowance > 0) (totalExpenses / allowance).toFloat().coerceIn(0f, 1f) else 0f
                    // Custom Circular Canvas Chart
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Background track
                            drawCircle(
                                color = Color(0xFFF1F5F9),
                                radius = size.minDimension / 2 - 8.dp.toPx(),
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Expense arc
                            drawArc(
                                brush = Brush.sweepGradient(listOf(WarningRed, BluePrimary)),
                                startAngle = -90f,
                                sweepAngle = expensePercentage * 360f,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.0f%%", expensePercentage * 100),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Budget Terpakai",
                                fontSize = 8.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Analisis Keuangan Instan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Anda telah menggunakan ${formatRupiah(totalExpenses)} dari allowance ${formatRupiah(allowance)}.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (expensePercentage > 0.8) "⚠ Pengeluaran kritis! Segera batasi hiburan." 
                                  else if (expensePercentage > 0.5) "💡 Pengeluaran sedang, kelola dengan hati-hati."
                                  else "👍 Keuangan Anda sangat sehat!",
                            fontSize = 11.sp,
                            color = if (expensePercentage > 0.8) WarningRed else GreenSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.LightGray)
            .testTag("summary_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.05f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
