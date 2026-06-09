package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.ui.StudentViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.GreenSecondary
import com.example.ui.theme.WarningRed
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(viewModel: StudentViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val gpaItems by viewModel.gpaItems.collectAsState()
    val allowance by viewModel.monthlyBudget.collectAsState()

    val context = LocalContext.current

    // --- Dynamic Statistics Calculations ---
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = totalTasks - completedTasks
    val completionPercentage = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

    val totalExpense = expenses.sumOf { it.amount }
    val remainingBudget = allowance - totalExpense
    val financeStatus = if (remainingBudget < 0) "Overspending" else if (totalExpense > 0.85 * allowance) "Suku Krisis" else "Keuangan Terjaga"

    val avgGrade = if (grades.isNotEmpty()) grades.map { it.finalGrade }.average() else 0.0
    val totalSks = gpaItems.sumOf { it.sks }
    val totalWeightSks = gpaItems.sumOf { it.weight * it.sks }
    val calculatedGpa = if (totalSks > 0) totalWeightSks / totalSks else 0.0
    val gpaItemsCount = gpaItems.size
    val achievementCategory = if (calculatedGpa >= 3.5) "Sangat Memuaskan" else if (calculatedGpa >= 3.0) "Memuaskan" else if (calculatedGpa >= 2.0) "Cukup" else "Perlu Peningkatan"

    // --- Dynamic Recommendations Generator ---
    val automaticInsights = remember(tasks, expenses, gpaItems, allowance) {
        val list = mutableListOf<String>()

        if (pendingTasks > 2) {
            list.add("⚠ Banyak tugas belum selesai: Terdapat $pendingTasks tugas aktif yang menunggu penyelesaian Anda.")
        } else if (totalTasks > 0 && pendingTasks == 0) {
            list.add("🏆 Kerja luar biasa! Semua tugas akademik Anda saat ini telah selesai diselesaikan.")
        }

        if (totalExpense > 0.85 * allowance) {
            list.add("⚠ Pengeluaran cukup tinggi: Pemakaian dana saku Anda telah melebihi 85% limit bulanan Anda.")
        } else if (remainingBudget < 0) {
            list.add("🚨 Anggaran saku negatif: Anda mengalami overspending sebesar ${formatRupiah(remainingBudget)}.")
        } else if (expenses.isNotEmpty()) {
            list.add("💵 Keuangan terjaga: Sisa anggaran Anda aman sebesar ${formatRupiah(remainingBudget)}.")
        }

        if (calculatedGpa >= 3.5) {
            list.add("🏆 Prestasi akademik sangat baik: Proyeksi IPK Anda mencapai ${String.format("%.2f", calculatedGpa)} ($achievementCategory).")
        } else if (calculatedGpa > 0.0 && calculatedGpa < 2.5) {
            list.add("📚 Fokus belajar diperkaya: Proyeksi IPK Anda di bawah target. Tingkatkan jam belajar dan prioritaskan tugas.")
        }

        if (list.isEmpty()) {
            list.add("📝 Belum ada insight. Catat tugas, keuangan, dan nilai untuk menerima anjuran otomatis mahasiswa.")
        }
        list
    }

    // --- State Variables ---
    var isGenerating by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var generatedFileName by remember { mutableStateOf("") }
    var generatedFileLoc by remember { mutableStateOf("") }
    var currentActiveFile by remember { mutableStateOf<File?>(null) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<File?>(null) }
    var errorSnackbarMessage by remember { mutableStateOf<String?>(null) }
    var successSnackbarMessage by remember { mutableStateOf<String?>(null) }
    var reportsList by remember { mutableStateOf(listOf<File>()) }
    var showExportBottomSheet by remember { mutableStateOf(false) }

    // Helper to refresh the file-based report history list
    val refreshReports = {
        val dir = File(context.filesDir, "reports")
        if (dir.exists()) {
            reportsList = dir.listFiles { file -> file.extension == "pdf" }
                ?.sortedByDescending { file -> file.lastModified() }
                ?: emptyList()
        } else {
            reportsList = emptyList()
        }
    }

    // Storage Access Framework (SAF) Launcher to select path and save PDF securely
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            isGenerating = true
            
            // Generate Timestamped Filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "SmartStudentManager_Report_$timestamp.pdf"
            
            try {
                // Generate standard document
                val pdfDoc = generatePdfDocument(
                    context = context,
                    totalTasks = totalTasks,
                    completedTasks = completedTasks,
                    pendingTasks = pendingTasks,
                    totalExpense = totalExpense,
                    remainingBudget = remainingBudget,
                    allowance = allowance,
                    avgGrade = avgGrade,
                    calculatedGpa = calculatedGpa,
                    totalSks = totalSks,
                    gpaItemsCount = gpaItemsCount,
                    insights = automaticInsights
                )

                // Save locally too for in-app history lists
                val localReportsDir = File(context.filesDir, "reports")
                if (!localReportsDir.exists()) {
                    localReportsDir.mkdirs()
                }
                val localFile = File(localReportsDir, fileName)
                val localOut = FileOutputStream(localFile)
                pdfDoc.writeTo(localOut)
                localOut.close()

                // Save directly to SAF Uri chosen by user
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDoc.writeTo(outputStream)
                }
                pdfDoc.close()

                // Update parameters
                generatedFileName = fileName
                generatedFileLoc = "Tersimpan di Folder Pilihan Anda (File Manager)"
                currentActiveFile = localFile

                // Refresh lists
                refreshReports()

                // Show Snackbar and Toast
                Toast.makeText(context, "Laporan berhasil disimpan ke lokasi pilihan", Toast.LENGTH_SHORT).show()
                successSnackbarMessage = "Laporan berhasil disimpan ke folder pilihan Anda"

                // Close loader and trigger Success dialog
                isGenerating = false
                showSuccessDialog = true
            } catch (e: Exception) {
                isGenerating = false
                errorSnackbarMessage = "Gagal menyimpan file ke lokasi pilihan: ${e.message}"
            }
        }
    }

    // Direct helper to generate and share the report in one tap
    val sharePdfDirect = {
        isGenerating = true
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SmartStudentManager_Report_$timestamp.pdf"
        
        try {
            val pdfDoc = generatePdfDocument(
                context = context,
                totalTasks = totalTasks,
                completedTasks = completedTasks,
                pendingTasks = pendingTasks,
                totalExpense = totalExpense,
                remainingBudget = remainingBudget,
                allowance = allowance,
                avgGrade = avgGrade,
                calculatedGpa = calculatedGpa,
                totalSks = totalSks,
                gpaItemsCount = gpaItemsCount,
                insights = automaticInsights
            )
            val localReportsDir = File(context.filesDir, "reports")
            if (!localReportsDir.exists()) {
                localReportsDir.mkdirs()
            }
            val localFile = File(localReportsDir, fileName)
            val localOut = FileOutputStream(localFile)
            pdfDoc.writeTo(localOut)
            localOut.close()
            pdfDoc.close()

            refreshReports()
            isGenerating = false
            
            // Share immediately
            sharePdfGeneral(context, localFile)
        } catch (e: Exception) {
            isGenerating = false
            errorSnackbarMessage = "Gagal membagikan laporan: ${e.message}"
        }
    }

    // Load initial reports history once
    LaunchedEffect(Unit) {
        refreshReports()
    }

    // core export helper function
    val triggerExportFlow = {
        isGenerating = true
        
        // Generate Timestamped Filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SmartStudentManager_Report_$timestamp.pdf"

        try {
            // Generate standard document
            val pdfDoc = generatePdfDocument(
                context = context,
                totalTasks = totalTasks,
                completedTasks = completedTasks,
                pendingTasks = pendingTasks,
                totalExpense = totalExpense,
                remainingBudget = remainingBudget,
                allowance = allowance,
                avgGrade = avgGrade,
                calculatedGpa = calculatedGpa,
                totalSks = totalSks,
                gpaItemsCount = gpaItemsCount,
                insights = automaticInsights
            )

            // Save locally for in-app history lists (No external permissions required)
            val localReportsDir = File(context.filesDir, "reports")
            if (!localReportsDir.exists()) {
                localReportsDir.mkdirs()
            }
            val localFile = File(localReportsDir, fileName)
            val localOut = FileOutputStream(localFile)
            pdfDoc.writeTo(localOut)
            localOut.close()
            pdfDoc.close()

            // Update parameters
            generatedFileName = fileName
            generatedFileLoc = "Tersimpan di Dokumen Internal Aplikasi"
            currentActiveFile = localFile

            // Refresh indices
            refreshReports()

            // Show Toast and set successSnackbarMessage
            Toast.makeText(context, "Laporan berhasil dibuat", Toast.LENGTH_SHORT).show()
            successSnackbarMessage = "Laporan berhasil dibuat"

            // Close progress loader and trigger Success dialog
            isGenerating = false
            showSuccessDialog = true
        } catch (e: Exception) {
            isGenerating = false
            errorSnackbarMessage = "Gagal membuat laporan: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Clean premium grayish-blue background
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Banner Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BluePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Laporan Aktivitas Pintar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Unduh, tinjau, dan bagikan ringkasan proaktif Anda.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // --- Core Export & Preview Action Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "⚙️ Kelola Ekspor Dokumen",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showExportBottomSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(48.dp)
                            .testTag("export_pdf_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Unduh")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { showPreviewDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF475569)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.0f)
                            .height(48.dp)
                            .testTag("preview_pdf_btn")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Quick Target Shares Card
                Divider(color = Color(0xFFF1F5F9))

                Text(
                    text = "🚀 Kirim Dokumen Cepat",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // WhatsApp Share Shortcut Button
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val fileTemp = File(context.filesDir, "SmartStudentManager_Report_$timestamp.pdf")
                            try {
                                val pdfDoc = generatePdfDocument(
                                    context = context,
                                    totalTasks = totalTasks,
                                    completedTasks = completedTasks,
                                    pendingTasks = pendingTasks,
                                    totalExpense = totalExpense,
                                    remainingBudget = remainingBudget,
                                    allowance = allowance,
                                    avgGrade = avgGrade,
                                    calculatedGpa = calculatedGpa,
                                    totalSks = totalSks,
                                    gpaItemsCount = gpaItemsCount,
                                    insights = automaticInsights
                                )
                                val outStream = FileOutputStream(fileTemp)
                                pdfDoc.writeTo(outStream)
                                outStream.close()
                                pdfDoc.close()
                                sharePdfToWhatsApp(context, fileTemp)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)), // WA green Accent
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.0f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Email Share Shortcut Button
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val fileTemp = File(context.filesDir, "SmartStudentManager_Report_$timestamp.pdf")
                            try {
                                val pdfDoc = generatePdfDocument(
                                    context = context,
                                    totalTasks = totalTasks,
                                    completedTasks = completedTasks,
                                    pendingTasks = pendingTasks,
                                    totalExpense = totalExpense,
                                    remainingBudget = remainingBudget,
                                    allowance = allowance,
                                    avgGrade = avgGrade,
                                    calculatedGpa = calculatedGpa,
                                    totalSks = totalSks,
                                    gpaItemsCount = gpaItemsCount,
                                    insights = automaticInsights
                                )
                                val outStream = FileOutputStream(fileTemp)
                                pdfDoc.writeTo(outStream)
                                outStream.close()
                                pdfDoc.close()
                                sharePdfToEmail(context, fileTemp)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)), // Orange Accent representing Gmail/Email
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.0f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirim Email", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Success Alert Container ---
        if (successSnackbarMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16A34A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { successSnackbarMessage = null }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sukses",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successSnackbarMessage!!,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { successSnackbarMessage = null }) {
                        Icon(Icons.Default.Close, "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- Error Alert Container ---
        if (errorSnackbarMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningRed.copy(alpha = 0.9f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { errorSnackbarMessage = null }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, "Error", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorSnackbarMessage!!,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { errorSnackbarMessage = null }) {
                        Icon(Icons.Default.Close, "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- Simulated / Automatic AI Insights Panel ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "💡 Insight Otomatis Mahasiswa",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Divider(color = Color(0xFFF1F5F9))
                automaticInsights.forEach { insight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = insight,
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // --- Report History Section (RIWAYAT LAPORAN) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "📁 Riwayat Laporan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${reportsList.size} Laporan",
                            fontSize = 10.sp,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Divider(color = Color(0xFFF1F5F9))

                if (reportsList.isEmpty()) {
                    // Empty state visual layout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Belum Ada Laporan Terbuka",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Tekan 'Export PDF' untuk men-generate cetakan pertama Anda.",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    // Loop reports list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reportsList.forEach { file ->
                            ReportHistoryItem(
                                file = file,
                                onOpen = { openPdfFile(context, file) },
                                onShare = { sharePdfGeneral(context, file) },
                                onDelete = { showDeleteConfirmDialog = file }
                            )
                        }
                    }
                }
            }
        }

        // --- Data Summaries List ---
        ReportSummaryCard(
            title = "Ringkasan Tugas Kuliah 📚",
            metrics = listOf(
                "Total Tugas Terdaftar" to "$totalTasks",
                "Tugas Selesai ✅" to "$completedTasks",
                "Tugas Belum Selesai ⏳" to "$pendingTasks",
                "Persentase Penyelesaian" to "$completionPercentage %"
            )
        )

        ReportSummaryCard(
            title = "Ringkasan Keuangan Pocket 💰",
            metrics = listOf(
                "Pocket Budget Limit" to formatRupiah(allowance),
                "Total Pengeluaran Saku" to formatRupiah(totalExpense),
                "Sisa Limit Keuangan" to formatRupiah(remainingBudget),
                "Status Keuangan" to financeStatus
            )
        )

        ReportSummaryCard(
            title = "Ringkasan Nilai & IPK 🎓",
            metrics = listOf(
                "Rata-Rata Nilai Kuis 🎯" to String.format("%.1f points", avgGrade),
                "Proyeksi IPK Kumulatif 🏆" to String.format("%.2f", calculatedGpa),
                "Jumlah SKS Selesai" to "$totalSks SKS",
                "Prestasi Akademis" to achievementCategory
            )
        )

        Spacer(modifier = Modifier.height(10.dp))
    }

    // --- DIALOG 1: Progress Loader (Membuat Laporan) ---
    if (isGenerating) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = BluePrimary, strokeWidth = 3.dp)
                    Text(
                        text = "⏳ Membuat laporan...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Mengintegrasikan tugas, keuangan, nilai, dan insight cerdas...",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // --- CUSTOM EXPORT BOTTOM SHEET DIALOG ---
    if (showExportBottomSheet) {
        Dialog(onDismissRequest = { showExportBottomSheet = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Minimalist Drag handle bar decoration
                    Box(
                        modifier = Modifier
                            .size(36.dp, 4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    )
                    
                    Text(
                        text = "Ekspor Laporan PDF 📄",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Pilih metode penyimpanan atau bagikan langsung laporan akademik Anda.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Option 1: Simpan Cepat
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportBottomSheet = false
                                triggerExportFlow()
                            }
                            .testTag("export_quick_action"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BluePrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📄", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Simpan Cepat",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Simpan ke direktori dokumen internal aplikasi",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // Option 2: Simpan ke Lokasi Pilihan
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportBottomSheet = false
                                // SAF launcher for user to choose location to save PDF
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val fileName = "SmartStudentManager_Report_$timestamp.pdf"
                                createDocumentLauncher.launch(fileName)
                            }
                            .testTag("export_saf_action"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF3C7)), // Warm Amber container
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📂", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pilih Lokasi Penyimpanan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Gunakan File Manager untuk memilih folder sendiri",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    // Option 3: Bagikan PDF
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportBottomSheet = false
                                sharePdfDirect()
                            }
                            .testTag("export_share_action"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFDCFCE7)), // Success green container
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔗", fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Bagikan PDF",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Kirim langsung lewat WhatsApp, Email, dll.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    TextButton(
                        onClick = { showExportBottomSheet = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Batal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // --- DIALOG 2: Export Success Notification ---
    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)), // Green success Container
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Laporan Selesai Dibuat!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NAMA FILE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = generatedFileName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LOKASI",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = generatedFileLoc,
                            fontSize = 10.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showSuccessDialog = false
                                currentActiveFile?.let { openPdfFile(context, it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📂 Buka PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showSuccessDialog = false
                                currentActiveFile?.let { sharePdfGeneral(context, it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF334155)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔗 Bagikan PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = { showSuccessDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tutup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- DIALOG 3: In-App PDF Mockup Paper Layout Preview ---
    if (showPreviewDialog) {
        Dialog(onDismissRequest = { showPreviewDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Dark premium slate card context
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White)
                            Text(
                                text = "Pratinjau Laporan",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                        IconButton(onClick = { showPreviewDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }

                    // Interactive simulated Paper Sheet inside scrolling container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BluePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("SSM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("SMART STUDENT MANAGER", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                    Text("Laporan Aktivitas Mahasiswa", fontSize = 9.sp, color = Color(0xFF475569))
                                    val mockPrintDate = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())
                                    Text("Tanggal Cetak: $mockPrintDate", fontSize = 7.5.sp, color = Color(0xFF64748B))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                            // Section I
                            MockPaperSection(
                                title = "📚 PROGRESS TUGAS KULIAH",
                                items = listOf(
                                    "Total Tugas" to "$totalTasks",
                                    "Tugas Selesai" to "$completedTasks",
                                    "Belum Selesai" to "$pendingTasks",
                                    "Persentase" to "$completionPercentage%"
                                )
                            )

                            // Section II
                            MockPaperSection(
                                title = "💰 STATUS KEUANGAN KANTONG SAKU",
                                items = listOf(
                                    "Limit Bulanan" to formatRupiah(allowance),
                                    "Pengeluaran" to formatRupiah(totalExpense),
                                    "Sisa Dana" to formatRupiah(remainingBudget),
                                    "Status" to financeStatus
                                )
                            )

                            // Section III
                            MockPaperSection(
                                title = "🎓 PRESTASI AKADEMIK & NILAI",
                                items = listOf(
                                    "Mata Kuliah" to "$gpaItemsCount",
                                    "Rata-rata Nilai" to String.format("%.1f pts", avgGrade),
                                    "Prediksi IPK" to String.format("%.2f", calculatedGpa),
                                    "Predikat" to achievementCategory
                                )
                            )

                            // Section IV: Insights Recommendation Card
                            if (automaticInsights.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "💡 REKOMENDASI & INSIGHT AKADEMIK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF854D0E)
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFFDF5))
                                            .border(0.5.dp, Color(0xFFFEF08A), RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            // Left border vertical decor bar
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEAB308))
                                            )
                                            
                                            Column(
                                                modifier = Modifier.padding(start = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                automaticInsights.take(4).forEach { insight ->
                                                    Row(verticalAlignment = Alignment.Top) {
                                                        Text("• ", fontSize = 9.sp, color = Color(0xFFEAB308))
                                                        Text(
                                                            text = insight,
                                                            fontSize = 8.5.sp,
                                                            color = Color(0xFF334155),
                                                            lineHeight = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Footer mockup
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Generated by Smart Student Manager", fontSize = 7.sp, color = Color(0xFF64748B))
                                Text("Fullstack Developer: Labib Achmad", fontSize = 7.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                Text("Halaman 1", fontSize = 7.sp, color = Color(0xFF64748B))
                            }
                        }
                    }

                    // Actions bottom row (Print options / Download link)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showPreviewDialog = false
                                // Print using systems native PrintManager
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val fileTemp = File(context.filesDir, "SmartStudentManager_Report_$timestamp.pdf")
                                try {
                                    val pdfDoc = generatePdfDocument(
                                        context = context,
                                        totalTasks = totalTasks,
                                        completedTasks = completedTasks,
                                        pendingTasks = pendingTasks,
                                        totalExpense = totalExpense,
                                        remainingBudget = remainingBudget,
                                        allowance = allowance,
                                        avgGrade = avgGrade,
                                        calculatedGpa = calculatedGpa,
                                        totalSks = totalSks,
                                        gpaItemsCount = gpaItemsCount,
                                        insights = automaticInsights
                                    )
                                    val outStream = FileOutputStream(fileTemp)
                                    pdfDoc.writeTo(outStream)
                                    outStream.close()
                                    pdfDoc.close()
                                    
                                    // Trigger PrintManager activity setup
                                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                    val adapter = PdfPrintAdapter(fileTemp)
                                    printManager.print("Smart Student Manager Report", adapter, null)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal meluncurkan print manager: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak / System Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showPreviewDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White),
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Tutup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG 4: Deletion Confirmation Modal ---
    showDeleteConfirmDialog?.let { fileToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Hapus Laporan?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Apakah Anda yakin ingin menghapus laporan '${fileToDelete.name}' dari riwayat penyimpanan lokal?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            fileToDelete.delete()
                            refreshReports()
                            showDeleteConfirmDialog = null
                            Toast.makeText(context, "Laporan berhasil dihapus", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Batal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// --- Preview Document Mock Component helpers ---
@Composable
fun MockPaperSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Section Subheading
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .padding(vertical = 5.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Informasi", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1.5f))
                Text("Nilai", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            
            HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 0.5.dp)
            
            // Item Rows
            items.forEachIndexed { idx, (label, valStr) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                        .padding(vertical = 5.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, fontSize = 8.sp, color = Color(0xFF475569), modifier = Modifier.weight(1.5f))
                    Text(valStr, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
                if (idx < items.size - 1) {
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                }
            }
        }
    }
}

// --- Report History Item UI Card ---
@Composable
fun ReportHistoryItem(
    file: File,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(file) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(file.lastModified()))
    }
    val sizeString = remember(file) {
        val bytes = file.length()
        if (bytes < 1024) "$bytes B"
        else String.format("%.1f KB", bytes / 1024f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BluePrimary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📅 $dateString", fontSize = 9.sp, color = Color(0xFF64748B))
                    Text(text = "💾 $sizeString", fontSize = 9.sp, color = Color(0xFF64748B))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 4.dp)
                    ) {
                        Text("Tersimpan", color = Color(0xFF2E7D32), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Buka",
                        tint = BluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = WarningRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// --- Common Report Summary Card (for standard list views) ---
@Composable
fun ReportSummaryCard(
    title: String,
    metrics: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Divider(color = Color(0xFFF1F5F9))
            metrics.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, fontSize = 12.sp, color = Color.Gray)
                    Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

// --- Native Document Creator Visual Layout Engine ---
private class PdfPageHelper(val pdfDocument: PdfDocument) {
    var currentPageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
    var currentPage = pdfDocument.startPage(pageInfo)
    var canvas = currentPage.canvas
    var currentY = 40f
    val marginX = 45f
    val pageWidth = 595f
    val pageHeight = 842f
    val bottomMargin = 65f

    fun checkAndPageBreak(requiredHeight: Float) {
        if (currentY + requiredHeight > pageHeight - bottomMargin) {
            drawFooter()
            pdfDocument.finishPage(currentPage)
            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage.canvas
            currentY = 40f // margin top for next page
            drawSubsequentPageHeader()
        }
    }

    fun finishLastPage() {
        drawFooter()
        pdfDocument.finishPage(currentPage)
    }

    private fun drawFooter() {
        val paint = Paint().apply { isAntiAlias = true }
        
        // Horizontal divider line
        paint.color = android.graphics.Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(marginX, pageHeight - 50f, pageWidth - marginX, pageHeight - 50f, paint)

        // Footer text left
        paint.reset()
        paint.isAntiAlias = true
        paint.color = android.graphics.Color.parseColor("#64748B")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("Generated by Smart Student Manager", marginX, pageHeight - 35f, paint)

        // Footer text center
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Fullstack Developer: Labib Achmad", pageWidth / 2f, pageHeight - 35f, paint)

        // Footer text right (page number)
        paint.reset()
        paint.isAntiAlias = true
        paint.color = android.graphics.Color.parseColor("#64748B")
        paint.textSize = 8f
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Halaman $currentPageNumber", pageWidth - marginX, pageHeight - 35f, paint)
    }

    private fun drawSubsequentPageHeader() {
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = android.graphics.Color.parseColor("#94A3B8")
        paint.textSize = 8f
        canvas.drawText("SMART STUDENT MANAGER - Laporan Aktivitas Mahasiswa (Lanjutan)", marginX, 30f, paint)
        
        paint.color = android.graphics.Color.parseColor("#E2E8F0")
        paint.strokeWidth = 0.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(marginX, 35f, pageWidth - marginX, 35f, paint)
        
        currentY = 55f
    }
}

fun generatePdfDocument(
    context: Context,
    totalTasks: Int,
    completedTasks: Int,
    pendingTasks: Int,
    totalExpense: Double,
    remainingBudget: Double,
    allowance: Double,
    avgGrade: Double,
    calculatedGpa: Double,
    totalSks: Int,
    gpaItemsCount: Int,
    insights: List<String>
): PdfDocument {
    val pdfDocument = PdfDocument()
    val helper = PdfPageHelper(pdfDocument)
    val canvas = helper.canvas

    val fillPaint = Paint().apply { style = Paint.Style.FILL }
    val textPaint = Paint().apply { isAntiAlias = true }

    // --- Header Rendering ---
    // 1. Draw elegant academic service stamp badge
    fillPaint.color = android.graphics.Color.parseColor("#2563EB") // BluePrimary
    canvas.drawRoundRect(45f, helper.currentY + 5f, 85f, helper.currentY + 45f, 6f, 6f, fillPaint)

    // Inner letters
    textPaint.color = android.graphics.Color.WHITE
    textPaint.textSize = 12f
    textPaint.isFakeBoldText = true
    textPaint.textAlign = Paint.Align.CENTER
    canvas.drawText("SSM", 65f, helper.currentY + 29f, textPaint)

    // Header Content
    textPaint.textAlign = Paint.Align.LEFT
    textPaint.color = android.graphics.Color.parseColor("#0F172A") // slate-900
    textPaint.textSize = 18f
    textPaint.isFakeBoldText = true
    canvas.drawText("SMART STUDENT MANAGER", 100f, helper.currentY + 20f, textPaint)

    textPaint.color = android.graphics.Color.parseColor("#475569") // slate-600
    textPaint.textSize = 11f
    textPaint.isFakeBoldText = false
    canvas.drawText("Laporan Aktivitas Mahasiswa", 100f, helper.currentY + 34f, textPaint)

    val printDate = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date())
    textPaint.color = android.graphics.Color.parseColor("#64748B") // slate-500
    textPaint.textSize = 8f
    canvas.drawText("Tanggal Cetak: $printDate", 100f, helper.currentY + 45f, textPaint)

    helper.currentY += 65f

    // Decorative slim horizontal separator rule
    paintLine(canvas, 45f, helper.currentY, 550f, helper.currentY, "#E2E8F0")
    helper.currentY += 15f

    // --- Calculations ---
    val completionPercentage = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0
    val financeStatus = if (remainingBudget < 0) "Overspending" else if (totalExpense > 0.85 * allowance) "Status Kritis" else "Keuangan Terjaga"
    val achievementCategory = if (calculatedGpa >= 3.5) "Sangat Memuaskan" else if (calculatedGpa >= 3.0) "Memuaskan" else if (calculatedGpa >= 2.0) "Cukup" else "Perlu Peningkatan"

    // Helper Table Painter
    fun drawSectionTable(
        title: String,
        rows: List<Pair<String, String>>
    ) {
        val totalTableHeight = 16f + 8f + 22f + (rows.size * 18f) + 15f
        helper.checkAndPageBreak(totalTableHeight)

        val activeCanvas = helper.canvas
        val startX = helper.marginX
        val endX = 550f
        val colDividerX = 320f // split ratio optimal for long descriptions

        // Stroke paint for borders
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = android.graphics.Color.parseColor("#CBD5E1")
        }

        // 1. Draw Section Subheading
        textPaint.color = android.graphics.Color.parseColor("#0F172A")
        textPaint.textSize = 12f
        textPaint.isFakeBoldText = true
        activeCanvas.drawText(title, startX, helper.currentY + 12f, textPaint)
        helper.currentY += 20f

        // 2. Draw Header Blue Row Background
        fillPaint.color = android.graphics.Color.parseColor("#F1F5F9")
        activeCanvas.drawRect(startX, helper.currentY, endX, helper.currentY + 22f, fillPaint)

        // Header Borders
        activeCanvas.drawRect(startX, helper.currentY, endX, helper.currentY + 22f, borderPaint)
        activeCanvas.drawLine(colDividerX, helper.currentY, colDividerX, helper.currentY + 22f, borderPaint)

        // Header Text Columns
        textPaint.textSize = 9.5f
        textPaint.isFakeBoldText = true
        textPaint.color = android.graphics.Color.parseColor("#334155")
        activeCanvas.drawText("Informasi", startX + 12f, helper.currentY + 15f, textPaint)
        activeCanvas.drawText("Nilai", colDividerX + 12f, helper.currentY + 15f, textPaint)

        helper.currentY += 22f

        // 3. Row Painter
        rows.forEachIndexed { index, (label, value) ->
            val rowY = helper.currentY

            // Alternating Stripe Row
            if (index % 2 == 1) {
                fillPaint.color = android.graphics.Color.parseColor("#F8FAFC")
                activeCanvas.drawRect(startX, rowY, endX, rowY + 18f, fillPaint)
            }

            // Outer Borders
            activeCanvas.drawRect(startX, rowY, endX, rowY + 18f, borderPaint)
            activeCanvas.drawLine(colDividerX, rowY, colDividerX, rowY + 18f, borderPaint)

            // Text Label rendering
            textPaint.color = android.graphics.Color.parseColor("#334155")
            textPaint.isFakeBoldText = false
            textPaint.textSize = 9f
            activeCanvas.drawText(label, startX + 12f, rowY + 12f, textPaint)

            // Text Value rendering
            textPaint.color = android.graphics.Color.parseColor("#0F172A")
            textPaint.isFakeBoldText = true
            activeCanvas.drawText(value, colDividerX + 12f, rowY + 12f, textPaint)

            helper.currentY += 18f
        }

        helper.currentY += 12f // spacer below table
    }

    // --- Task Table ---
    drawSectionTable(
        "📚 PROGRESS TUGAS KULIAH",
        listOf(
            "Total Tugas" to "$totalTasks",
            "Tugas Selesai" to "$completedTasks",
            "Belum Selesai" to "$pendingTasks",
            "Persentase" to "$completionPercentage%"
        )
    )

    // --- Finances Table ---
    drawSectionTable(
        "💰 STATUS KEUANGAN KANTONG SAKU",
        listOf(
            "Limit Bulanan" to formatRupiah(allowance),
            "Pengeluaran" to formatRupiah(totalExpense),
            "Sisa Dana" to formatRupiah(remainingBudget),
            "Status" to financeStatus
        )
    )

    // --- Academics Table ---
    drawSectionTable(
        "🎓 PRESTASI AKADEMIK & NILAI",
        listOf(
            "Mata Kuliah" to "$gpaItemsCount",
            "Rata-rata Nilai" to String.format("%.1f pts", avgGrade),
            "Prediksi IPK" to String.format("%.2f", calculatedGpa),
            "Predikat" to achievementCategory
        )
    )

    // --- Recommendation Section ---
    if (insights.isNotEmpty()) {
        val startX = helper.marginX
        val endX = 550f

        // Word wrap algorithms to ensure beautiful fit inside container bounds
        fun wrapText(text: String, width: Float, paint: Paint): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val testWidth = paint.measureText(testLine)
                if (testWidth <= width) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine)
                    }
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }
            return lines
        }

        textPaint.textSize = 9.5f
        val wrappedInsights = insights.take(4).map { insight ->
            wrapText(insight, 460f, textPaint)
        }

        // Total content box sizing
        val recBoxHeight = wrappedInsights.sumOf { lines -> lines.size * 14 }.toFloat() + (wrappedInsights.size * 6f) + 30f
        val blockHeightRequired = 16f + 10f + recBoxHeight + 15f
        helper.checkAndPageBreak(blockHeightRequired)

        val activeCanvas = helper.canvas
        val rectTop = helper.currentY

        // Title Header
        textPaint.textSize = 12f
        textPaint.isFakeBoldText = true
        textPaint.color = android.graphics.Color.parseColor("#854D0E") // Amber subtitle
        activeCanvas.drawText("💡 REKOMENDASI & INSIGHT AKADEMIK", startX, rectTop + 12f, textPaint)
        helper.currentY += 22f

        val boxTop = helper.currentY
        val boxBottom = boxTop + recBoxHeight

        // Fill background container safely following the text length
        fillPaint.color = android.graphics.Color.parseColor("#FFFDF5") // warm light tone
        activeCanvas.drawRoundRect(startX, boxTop, endX, boxBottom, 8f, 8f, fillPaint)

        // Draw Amber decorative accent bar on left edge
        fillPaint.color = android.graphics.Color.parseColor("#EAB308")
        activeCanvas.drawRect(startX, boxTop + 4f, startX + 4f, boxBottom - 4f, fillPaint)

        // Draw soft amber border line
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
            color = android.graphics.Color.parseColor("#FEF08A")
        }
        activeCanvas.drawRoundRect(startX, boxTop, endX, boxBottom, 8f, 8f, borderPaint)

        // Draw items
        var bulletY = boxTop + 18f
        wrappedInsights.forEach { lines ->
            // Bullet Point Symbol
            fillPaint.color = android.graphics.Color.parseColor("#EAB308")
            activeCanvas.drawCircle(startX + 18f, bulletY - 3f, 3f, fillPaint)

            textPaint.textSize = 9f
            textPaint.color = android.graphics.Color.parseColor("#1E293B")
            lines.forEach { line ->
                textPaint.isFakeBoldText = (line.contains("Penting:") || line.contains("Rekomendasi:") || line.startsWith("•"))
                activeCanvas.drawText(line, startX + 28f, bulletY, textPaint)
                bulletY += 14f
            }
            bulletY += 6f
        }

        helper.currentY = boxBottom + 15f
    }

    helper.finishLastPage()
    return pdfDocument
}

private fun paintLine(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float, colorHex: String) {
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor(colorHex)
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    canvas.drawLine(startX, startY, endX, endY, paint)
}

// --- Save Document to Downloads Directory using MediaStore on Q+ or raw File on older APIs ---
fun savePdfToDownloads(context: Context, pdfDocument: PdfDocument, filename: String): Uri? {
    // Deprecated following the new safe Scoped Storage Flow. All documents are now safely persisted in internal documents directory.
    return null
}

// --- Open local report file inside external PDF application ---
fun openPdfFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak ada aplikasi PDF viewer yang terpasang: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

// --- Share local report globally via share intent chooser ---
fun sharePdfGeneral(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan laporan: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --- WhatsApp Direct Attachment Share Intent ---
fun sharePdfToWhatsApp(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val waIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage("com.whatsapp")
    }
    try {
        context.startActivity(waIntent)
    } catch (e: Exception) {
        // Try business whatsapp package
        val wbIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp.w4b")
        }
        try {
            context.startActivity(wbIntent)
        } catch (ex: Exception) {
            // General backup chooser
            val backupIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(backupIntent, "Kirim WhatsApp via..."))
        }
    }
}

// --- Email Pre-Populated Attachment Share Intent ---
fun sharePdfToEmail(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Smart Student Manager")
            putExtra(
                Intent.EXTRA_TEXT,
                "Halo,\n\nBerikut saya lampirkan laporan yang dibuat dari aplikasi Smart Student Manager.\n\nTerima kasih."
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Kirim Laporan via Email..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal meluncurkan email: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// --- Custom PrintDocumentAdapter for native Android Print Layout systems ---
class PdfPrintAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null
        try {
            input = FileInputStream(file)
            output = FileOutputStream(destination?.fileDescriptor)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } >= 0) {
                output.write(buffer, 0, bytesRead)
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.toString())
        } finally {
            input?.close()
            output?.close()
        }
    }
}
