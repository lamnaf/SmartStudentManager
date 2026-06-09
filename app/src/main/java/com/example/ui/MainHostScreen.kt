package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.*
import com.example.ui.theme.BluePrimary

@Composable
fun MainHostScreen(viewModel: StudentViewModel) {
    var currentTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = BluePrimary,
                windowInsets = WindowInsets.navigationBars
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BluePrimary,
                    selectedTextColor = BluePrimary,
                    indicatorColor = BluePrimary.copy(alpha = 0.1f),
                    unselectedIconColor = Color(0xFF94A3B8),
                    unselectedTextColor = Color(0xFF94A3B8)
                )

                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Tasks") },
                    label = { Text("Tugas", style = MaterialTheme.typography.labelSmall) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Wallet, contentDescription = "Finance") },
                    label = { Text("Keuangan", style = MaterialTheme.typography.labelSmall) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.School, contentDescription = "Academic") },
                    label = { Text("Akademik", style = MaterialTheme.typography.labelSmall) },
                    colors = itemColors
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                    label = { Text("Menu", style = MaterialTheme.typography.labelSmall) },
                    colors = itemColors
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTasks = { currentTab = 1 },
                    onNavigateToFinance = { currentTab = 2 },
                    onNavigateToAcademic = { currentTab = 3 }
                )
                1 -> TaskScreen(viewModel = viewModel)
                2 -> FinanceScreen(viewModel = viewModel)
                3 -> AcademicTabScreen(viewModel = viewModel)
                4 -> MoreMenuScreen(viewModel = viewModel)
            }
        }
    }
}
