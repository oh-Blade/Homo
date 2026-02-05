package com.homo.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homo.notes.data.SettingsRepository
import com.homo.notes.ui.NotesScreen
import com.homo.notes.ui.NotesViewModel
import com.homo.notes.ui.NotesViewModelFactory
import com.homo.notes.ui.theme.HomoNotesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = SettingsRepository(applicationContext)
        val viewModelFactory = NotesViewModelFactory(application, settingsRepository)
        setContent {
            val viewModel: NotesViewModel = viewModel(factory = viewModelFactory)
            HomoNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotesScreen(
                        viewModel = viewModel,
                        settingsRepository = settingsRepository,
                        onOpenSettings = { }
                    )
                }
            }
        }
    }
}
