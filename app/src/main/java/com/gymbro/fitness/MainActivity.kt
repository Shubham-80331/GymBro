package com.gymbro.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.gymbro.fitness.notifications.WorkoutReminder
import com.gymbro.fitness.ui.GymBroApp
import com.gymbro.fitness.ui.GymBroViewModel
import com.gymbro.fitness.ui.GymBroViewModelFactory
import com.gymbro.fitness.ui.theme.GymBroTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GymBroViewModel by viewModels {
        GymBroViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Alarms are dropped on force-stop; re-arm the daily reminder whenever the app opens.
        WorkoutReminder.reschedule(this)
        enableEdgeToEdge()
        setContent {
            GymBroTheme {
                GymBroApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
