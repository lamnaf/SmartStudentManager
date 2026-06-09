package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.StudentViewModel
import com.example.ui.theme.BluePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicTabScreen(viewModel: StudentViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Kalkulator Nilai", "Prediksi IPK")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            text = "Alat Akademik Mahasiswa", 
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
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = BluePrimary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    text = title, 
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium, 
                                    color = if (selectedTab == index) BluePrimary else Color.Gray
                                ) 
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> GradeScreen(viewModel = viewModel)
                1 -> GpaScreen(viewModel = viewModel)
            }
        }
    }
}
