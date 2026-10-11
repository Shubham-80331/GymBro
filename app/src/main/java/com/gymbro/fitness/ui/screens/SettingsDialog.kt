package com.gymbro.fitness.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
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
import com.gymbro.fitness.notifications.RestTimerNotifier
import com.gymbro.fitness.notifications.WorkoutReminder
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
    var reminder by remember { mutableStateOf(WorkoutReminder.settings(context)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* the reminder is scheduled either way; it only shows once notifications are allowed */ }

    fun saveReminder(updated: WorkoutReminder.Settings) {
        reminder = updated
        WorkoutReminder.update(context, updated)
    }

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
                Text("REMINDERS", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Daily workout reminder", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !RestTimerNotifier.canPostNotifications(context)
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            saveReminder(reminder.copy(enabled = enabled))
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = GymVolt)
                    )
                }
                if (reminder.enabled) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> saveReminder(reminder.copy(hour = hour, minute = minute)) },
                                reminder.hour,
                                reminder.minute,
                                android.text.format.DateFormat.is24HourFormat(context)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            String.format(Locale.US, "Remind me at %02d:%02d", reminder.hour, reminder.minute),
                            color = TextPrimary
                        )
                    }
                }

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
