package com.gymbro.fitness.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.fitness.BuildConfig
import com.gymbro.fitness.ui.GymBroViewModel
import com.gymbro.fitness.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDialog(
    viewModel: GymBroViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "Data exported" else "Export failed. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = DarkSurface,
            title = { Text("Delete all your data?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently erases your profile, workouts, routines, nutrition, weight and steps logs on this device. " +
                        "Export first if you want a copy. This can't be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllData()
                    confirmDelete = false
                    onDismiss()
                }) {
                    Text("DELETE EVERYTHING", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Settings", color = GymVolt, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("YOUR DATA", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("gymbro-export-$date.json")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export my data (JSON)", color = TextPrimary)
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete all my data", color = ErrorRed)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "GymBro gives general fitness and nutrition information, not medical advice. " +
                        "Talk to a doctor before starting a new program, especially with an injury or health condition.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 11.sp, color = TextSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = GymVolt)
            }
        }
    )
}
