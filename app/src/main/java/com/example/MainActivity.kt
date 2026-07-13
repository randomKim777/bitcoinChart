package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.CoinRepository
import com.example.data.database.AppDatabase
import com.example.ui.CoinDashboard
import com.example.ui.CoinViewModel
import com.example.ui.theme.MyApplicationTheme

// --- VIEWMODEL FACTORY ---

class ViewModelFactory(private val repository: CoinRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoinViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CoinViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- MAIN ACTIVITY ---

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: CoinRepository
    private lateinit var viewModel: CoinViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "bitpulse-db"
        )
        .fallbackToDestructiveMigration()
        .build()

        // Initialize Repository
        repository = CoinRepository(database.priceAlertDao())

        // Initialize ViewModel via Factory
        viewModel = ViewModelProvider(this, ViewModelFactory(repository))[CoinViewModel::class.java]

        setContent {
            MyApplicationTheme {
                CoinDashboard(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Proactively trigger a background polling session on app resume
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        // Stop background polling when the app goes into background to save resources
        viewModel.stopPolling()
    }
}
