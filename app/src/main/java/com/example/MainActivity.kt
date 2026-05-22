package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.TacticsDatabase
import com.example.data.TacticsRepository
import com.example.ui.TacticsGameScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TacticsViewModel
import com.example.viewmodel.TacticsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB & Repository
        val database = TacticsDatabase.getDatabase(applicationContext)
        val repository = TacticsRepository(database.tacticsDao())

        // 2. Create ViewModel with custom Factory
        val factory = TacticsViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[TacticsViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Set up screen drawing & let composable handle edge-to-edge sub-padding
                    TacticsGameScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
