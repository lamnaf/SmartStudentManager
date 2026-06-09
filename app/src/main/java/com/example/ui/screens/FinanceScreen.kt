package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.ui.StudentViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(viewModel: StudentViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val allowance by viewModel.monthlyBudget.collectAsState()
    val errorMsg by viewModel.errorState.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    // --- Expense Form Inputs ---
    var nameInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("🍔 Makan") }
    var amountInput by remember { mutableStateOf("") }
    var dateInputMs by remember { mutableStateOf(System.currentTimeMillis()) }

    // --- Budget Dialog Inputs ---
    var budgetInput by remember { mutableStateOf(allowance.toInt().toString()) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // Categories
    val categories = listOf("🍔 Makan", "🚌 Transportasi", "📚 Kuliah", "🎮 Hiburan", "📦 Lainnya")
    
    // Category Colors
    val categoryColors = remember {
        mapOf(
            "🍔 Makan" to Color(0xFFEF4444),       // Red
            "🚌 Transportasi" to Color(0xFFF59E0B),  // Amber
            "📚 Kuliah" to Color(0xFF3B82F6),        // Blue
            "🎮 Hiburan" to Color(0xFF10B981),       // Green
            "📦 Lainnya" to Color(0xFF8B5CF6)        // Purple
        )
    }

    LaunchedEffect(expenseToEdit) {
        if (expenseToEdit != null) {
            nameInput = expenseToEdit!!.name
            categoryInput = expenseToEdit!!.category
            amountInput = expenseToEdit!!.amount.toLong().toString()
            dateInputMs = expenseToEdit!!.date
        } else {
            nameInput = ""
            categoryInput = "🍔 Makan"
            amountInput = ""
            dateInputMs = System.currentTimeMillis()
        }
    }

    // --- Calculations ---
    val totalExpense = expenses.sumOf { it.amount }
    val remainingBudget = allowance - totalExpense
    val topExpenses = expenses.sortedByDescending { it.amount }.take(3)

    // Category Spend Sums
    val categorySums = expenses.groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Keuangan Mahasiswa", 
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(
                        onClick = { 
                            budgetInput = allowance.toInt().toString()
                            showBudgetDialog = true 
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .shadow(2.dp, RoundedCornerShape(10.dp))
                            .background(Color.White, RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Settings, "Set Budget Limit", tint = BluePrimary, modifier = Modifier.size(18.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    expenseToEdit = null
                    showFormDialog = true
                },
                containerColor = GreenSecondary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = Color.LightGray)
                    .testTag("add_expense_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // --- Financial Dashboard Overview Card ---
            Card(
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
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
                            Text("Pemberian Uang Saku / Budget", fontSize = 11.sp, color = Color.Gray)
                            Text(formatRupiah(allowance), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenSecondary.copy(alpha = 0.15f))
                                .clickable { 
                                    budgetInput = allowance.toInt().toString()
                                    showBudgetDialog = true 
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Edit limit", fontSize = 11.sp, color = GreenSecondary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = Color(0xFFF1F5F9))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Pengeluaran", fontSize = 11.sp, color = Color.Gray)
                            Text(formatRupiah(totalExpense), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WarningRed)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sisa Limit Saku", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = formatRupiah(remainingBudget), 
                                fontSize = 16.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = if (remainingBudget >= 0) GreenSecondary else WarningRed
                            )
                        }
                    }

                    // Status Keuangan Label
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (remainingBudget >= 0.3 * allowance) GreenSecondary.copy(alpha = 0.1f)
                                else WarningRed.copy(alpha = 0.1f)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (remainingBudget < 0) "⚠️ Keuangan Kritis! Saku minus!"
                                  else if (remainingBudget < 0.2 * allowance) "⚠️ Sisa uang saku menipis, mohon berhemat."
                                  else "✅ Status Keuangan: Aman & Sehat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingBudget >= 0.2 * allowance) GreenSecondary else WarningRed
                        )
                    }
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

            // --- Custom Financial Chart & Lists ---
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Canvas Pie Chart Section (Only if there are expenses)
                if (expenses.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Komposisi Pengeluaran", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom Circular Chart Canvas Representation
                                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            var startAngle = -90f
                                            categorySums.forEach { (cat, amount) ->
                                                val sweepAngle = (amount / totalExpense).toFloat() * 360f
                                                drawArc(
                                                    color = categoryColors[cat] ?: Color.Gray,
                                                    startAngle = startAngle,
                                                    sweepAngle = sweepAngle,
                                                    useCenter = false,
                                                    style = Stroke(width = 16.dp.toPx())
                                                )
                                                startAngle += sweepAngle
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Saku", fontSize = 10.sp, color = Color.Gray)
                                            Text(
                                                text = String.format("%.0f%%", (totalExpense / allowance).coerceIn(0.0, 1.0) * 100),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(20.dp))

                                    // Chart Legend Listing
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        categorySums.forEach { (category, amount) ->
                                            val categoryColor = categoryColors[category] ?: Color.Gray
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(10.dp).background(categoryColor, RoundedCornerShape(2.dp)))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "$category (${String.format("%.0f%%", (amount / totalExpense) * 100)})",
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Top Expenses Listing ---
                if (topExpenses.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("🔥 Top Pengeluaran Terbesar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                topExpenses.forEach { exp ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(exp.category + " " + exp.name, fontSize = 12.sp, color = Color.DarkGray)
                                        Text(formatRupiah(exp.amount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningRed)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Historical Expense Title ---
                item {
                    Text(
                        text = "Riwayat Pengeluaran",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (expenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada catatan pengeluaran. Ketuk '+' untuk menambah.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(expenses, key = { it.id }) { item ->
                        ExpenseRow(
                            expense = item,
                            color = categoryColors[item.category] ?: Color.Gray,
                            onEditClick = {
                                expenseToEdit = item
                                showFormDialog = true
                            },
                            onDeleteClick = { viewModel.deleteExpenseById(item.id) },
                            dateFormatter = dateFormatter
                        )
                    }
                }
            }
        }
    }

    // --- Expense Input Form Dialog ---
    if (showFormDialog) {
        AlertDialog(
            onDismissRequest = {
                showFormDialog = false
                viewModel.clearError()
            },
            title = {
                Text(
                    text = if (expenseToEdit == null) "Tambah Pengeluaran" else "Edit Pengeluaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Pengeluaran") },
                        modifier = Modifier.fillMaxWidth().testTag("form_expense_name"),
                        singleLine = true
                    )

                    // Category dropdown picker mock / simple clickable row selects instead of heavy menus
                    Text("Kategori Pengeluaran", fontSize = 12.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            val isSelected = categoryInput == cat
                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GreenSecondary else Color(0xFFF1F5F9))
                                    .clickable { categoryInput = cat }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, fontSize = 10.sp, color = if (isSelected) Color.White else Color.Black)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            val isSelected = categoryInput == cat
                            Box(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) GreenSecondary else Color(0xFFF1F5F9))
                                    .clickable { categoryInput = cat }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, fontSize = 10.sp, color = if (isSelected) Color.White else Color.Black)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Nominal Pengeluaran") },
                        modifier = Modifier.fillMaxWidth().testTag("form_expense_amount"),
                        singleLine = true
                    )

                    // Date Picker Trigger
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE2E8F0))
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.YEAR, year)
                                        cal.set(Calendar.MONTH, month)
                                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        dateInputMs = cal.timeInMillis
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Date picker", tint = GreenSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Tanggal Pengeluaran", fontSize = 10.sp, color = Color.Gray)
                                Text(dateFormatter.format(Date(dateInputMs)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (expenseToEdit == null) {
                            viewModel.addExpense(
                                name = nameInput,
                                category = categoryInput,
                                amountText = amountInput,
                                dateMs = dateInputMs
                            )
                        } else {
                            viewModel.editExpense(
                                id = expenseToEdit!!.id,
                                name = nameInput,
                                category = categoryInput,
                                amountText = amountInput,
                                dateMs = dateInputMs
                            )
                        }
                        if (errorMsg == null) {
                            showFormDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSecondary)
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
            }
        )
    }

    // --- Starting Allowance Settings Dialog ---
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Atur Limit Uang Saku", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Masukkan nominal budget uang saku / pemasukan bulanan untuk pelacakan keuangan Anda:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Limit Saku Bulanan") },
                        modifier = Modifier.fillMaxWidth().testTag("form_budget_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = budgetInput.toDoubleOrNull()
                        if (num != null && num > 0) {
                            viewModel.updateMonthlyBudget(num)
                            showBudgetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ExpenseRow(
    expense: ExpenseEntity,
    color: Color,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle category marker
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                // Show first emoji letter
                Text(expense.category.take(2), fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(expense.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(expense.category + " • " + dateFormatter.format(Date(expense.date)), fontSize = 10.sp, color = Color.Gray)
            }

            Text(
                text = "-" + formatRupiah(expense.amount),
                fontSize = 14.sp,
                color = WarningRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, "Edit", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Delete", tint = WarningRed, modifier = Modifier.size(16.dp))
            }
        }
    }
}
