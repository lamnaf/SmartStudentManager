package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.MainHostScreen
import com.example.ui.StudentViewModel
import com.example.ui.StudentViewModelFactory
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val database by lazy { AppDatabase.getDatabase(this) }
  private val repository by lazy { AppRepository(database.appDao()) }

  private val viewModel: StudentViewModel by viewModels {
    StudentViewModelFactory(repository, this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var showSplash by remember { mutableStateOf(true) }

        if (showSplash) {
          SplashScreen(onTimeout = { showSplash = false })
        } else {
          MainHostScreen(viewModel = viewModel)
        }
      }
    }
  }
}

