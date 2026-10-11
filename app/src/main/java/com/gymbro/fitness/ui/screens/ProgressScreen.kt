package com.gymbro.fitness.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.fitness.data.WeightLog
import com.gymbro.fitness.ui.GymBroViewModel
import com.gymbro.fitness.ui.theme.*
import com.gymbro.fitness.domain.WeightUnit
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressScreen(
    viewModel: GymBroViewModel,
    modifier: Modifier = Modifier
) {
    val weightLogs by viewModel.allWeightLogs.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()
    val stepsLogs by viewModel.allStepsLogs.collectAsState()
    val exerciseBests by viewModel.exerciseBests.collectAsState()
    val unit by viewModel.weightUnit.collectAsState()
    val weeklySets by viewModel.weeklySetsByMuscle.collectAsState()
    var selectedLift by remember { mutableStateOf<com.gymbro.fitness.ui.ExerciseBest?>(null) }

    selectedLift?.let { lift ->
        val history by remember(lift.exerciseId) { viewModel.oneRepMaxHistory(lift.exerciseId) }
            .collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { selectedLift = null },
            containerColor = DarkSurface,
            title = { Text(lift.exerciseName, color = GymVolt, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estimated 1RM per session", color = TextSecondary, fontSize = 12.sp)
                    if (history.size < 2) {
                        Text("Log this lift in at least two workouts to see a trend.", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        OneRepMaxChart(points = history.map { unit.fromKg(it.second) })
                        val first = history.first().second
                        val last = history.last().second
                        Text(
                            text = String.format(
                                Locale.US,
                                "%s → %s (%+.1f %s) over %d sessions",
                                unit.format(first),
                                unit.formatWithUnit(last),
                                unit.fromKg(last - first),
                                unit.label,
                                history.size
                            ),
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLift = null }) { Text("CLOSE", color = GymVolt) }
            }
        )
    }

    var showLogDialog by remember { mutableStateOf(false) }
    var showStepsDialog by remember { mutableStateOf(false) }
    var stepsInput by remember { mutableStateOf("") }

    // Input fields state
    var weightInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var armsInput by remember { mutableStateOf("") }
    var chestInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    // Initial prefill
    LaunchedEffect(profile) {
        profile?.let {
            if (weightInput.isEmpty()) {
                weightInput = unit.format(it.weightKg)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogDialog = true },
                containerColor = GymVolt,
                contentColor = Color.Black,
                modifier = Modifier.testTag("log_metrics_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Log progress metrics")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Header stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "METRICS HUD",
                            color = GymVolt,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Physical Matrix",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Trends & Insights Bento Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // First Bento Card: Dynamic Weight Trend
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(width = 1.dp, color = CardStroke, shape = RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "WEIGHT",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = profile?.weightKg?.let { unit.format(it) } ?: "--",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = unit.label,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Dynamic calculated change
                            val loggedDiff = if (weightLogs.size >= 2) {
                                unit.fromKg(weightLogs[0].weightKg - weightLogs[1].weightKg)
                            } else {
                                null
                            }
                            val changeText = when {
                                loggedDiff == null -> "Log twice to see change"
                                loggedDiff < 0 -> String.format(Locale.US, "▼ %.1f%s since prev", -loggedDiff, unit.label)
                                else -> String.format(Locale.US, "▲ %.1f%s since prev", loggedDiff, unit.label)
                            }
                            Text(
                                text = changeText,
                                fontSize = 11.sp,
                                color = when {
                                    loggedDiff == null -> TextSecondary
                                    loggedDiff < 0 -> FitGreen
                                    else -> ProteinOrange
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Second Bento Card: Volume PR
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(width = 1.dp, color = CardStroke, shape = RoundedCornerShape(24.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "VOLUME PR",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val highestVolume = sessions.maxOfOrNull { it.totalVolumeLoadKg }
                            // Sessions are ordered newest first, so the record is "new" when the latest session holds it.
                            val latestIsRecord = sessions.size >= 2 && highestVolume != null &&
                                sessions.first().totalVolumeLoadKg >= highestVolume
                            val highestVolumeInUnit = highestVolume?.let { unit.fromKg(it) }
                            val prKiloText = if (highestVolumeInUnit == null) {
                                "--"
                            } else if (highestVolumeInUnit >= 1000) {
                                String.format(Locale.US, "%.1fk", highestVolumeInUnit / 1000.0)
                            } else {
                                String.format(Locale.US, "%.0f", highestVolumeInUnit)
                            }
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = prKiloText,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = unit.label,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    highestVolume == null -> "Finish a workout to set one"
                                    latestIsRecord -> "NEW RECORD! 🏆"
                                    else -> "Best single session"
                                },
                                fontSize = 10.sp,
                                color = if (latestIsRecord) ProteinOrange else TextSecondary,
                                fontWeight = if (latestIsRecord) FontWeight.Black else FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Caloric Expenditure Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(width = 1.dp, color = CardStroke, shape = RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CALORIC EXPENDITURE", fontSize = 12.sp, color = ProteinOrange, fontWeight = FontWeight.Bold)
                                Text("Steps + Gym Workout", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showStepsDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("LOG STEPS", color = GymVolt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        // Each entry is the day's total, so the most recent entry for today wins.
                        val todaysStepsLog = stepsLogs.filter { it.dateString == today }.maxByOrNull { it.id }
                        val todaysStepCount = todaysStepsLog?.stepsCount ?: 0
                        val todaysSteps = todaysStepsLog?.caloriesBurnt ?: 0
                        val todaysWorkout = sessions.filter { 
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.dateMillis)) == today 
                        }.sumOf { it.caloriesBurnt }
                        val totalCalories = todaysSteps + todaysWorkout

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Total
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL BURNT", fontSize = 10.sp, color = TextSecondary)
                                Text("${totalCalories} kcal", fontSize = 20.sp, color = ProteinOrange, fontWeight = FontWeight.Black)
                            }
                            // Breakdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text("STEPS", fontSize = 10.sp, color = TextSecondary)
                                Text("$todaysStepCount", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("${todaysSteps} kcal", fontSize = 10.sp, color = TextSecondary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WORKOUT", fontSize = 10.sp, color = TextSecondary)
                                Text("${todaysWorkout} kcal", fontSize = 16.sp, color = FitGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Custom Weight & Moving Average Chart Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = CardStroke, shape = RoundedCornerShape(24.dp))
                        .testTag("weight_chart_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "WEIGHT & TRENDLINE (Moving Avg)",
                                    fontSize = 12.sp,
                                    color = GymVolt,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Overload Consistency",
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(ElectricBlue, RoundedCornerShape(4.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Daily", fontSize = 10.sp, color = TextSecondary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).background(ProteinOrange, RoundedCornerShape(4.dp)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Trend", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (weightLogs.size < 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(DarkCard, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log weight on at least 2 separate days to reveal trend lines!",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Draw dynamic customized line chart
                            val sortedLogs = weightLogs.sortedBy { it.dateMillis }
                            WeightLineChart(
                                logs = sortedLogs,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }
            }

            // Weekly sets per muscle group
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "WEEKLY SETS PER MUSCLE", fontSize = 12.sp, color = GymVolt, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Last 7 days. Around 10–20 hard sets per muscle a week works well for growth.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core").forEach { muscle ->
                            val count = weeklySets[muscle] ?: 0
                            val barColor = when {
                                count < 10 -> ProteinOrange
                                count <= 20 -> FitGreen
                                else -> ElectricBlue
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = muscle, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                                LinearProgressIndicator(
                                    progress = { (count / 20f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp),
                                    color = barColor,
                                    trackColor = DarkCard
                                )
                                Text(
                                    text = "$count",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            // Lift PRs: best estimated 1RM per exercise
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LIFT PRs (EST. 1RM)",
                            fontSize = 12.sp,
                            color = GymVolt,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (exerciseBests.isEmpty()) {
                            Text(
                                text = "Finish a workout to start tracking your strength.",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
                            exerciseBests.take(8).forEach { best ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedLift = best }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = best.exerciseName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "%s %s × %d · %s",
                                                unit.format(best.bestWeightKg),
                                                unit.label,
                                                best.bestReps,
                                                dateFormat.format(Date(best.achievedAtMillis))
                                            ),
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = unit.formatWithUnit(best.estimatedOneRepMaxKg),
                                        color = FitGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Correlation Panel (Adherence / Volume Matrix)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = "Analytics", tint = GymVolt)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LAST 7 DAYS",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                        val weekSessions = sessions.filter { it.dateMillis >= weekAgo }
                        val weekVolume = weekSessions.sumOf { it.totalVolumeLoadKg }
                        val weekWeights = weightLogs.filter { it.dateMillis >= weekAgo }
                        val weekWeightChange = if (weekWeights.size >= 2) {
                            weekWeights.first().weightKg - weekWeights.last().weightKg
                        } else {
                            null
                        }
                        val summaryText = buildString {
                            if (weekSessions.isEmpty()) {
                                append("No workouts logged this week yet.")
                            } else {
                                append("${weekSessions.size} workout${if (weekSessions.size == 1) "" else "s"}, ")
                                append(String.format(Locale.US, "%,.0f %s lifted.", unit.fromKg(weekVolume), unit.label))
                            }
                            if (weekWeightChange != null) {
                                append(String.format(Locale.US, " Weight %+.1f %s.", unit.fromKg(weekWeightChange), unit.label))
                            }
                        }
                        Text(
                            text = summaryText,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DarkCard, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "LIFTING VOL", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val totalVol = sessions.sumOf { it.totalVolumeLoadKg }
                                    Text(text = String.format(Locale.US, "%,.0f %s", unit.fromKg(totalVol), unit.label), fontSize = 18.sp, color = FitGreen, fontWeight = FontWeight.Black)
                                    Text(text = "All time", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DarkCard, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "GOAL", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val goal = profile?.primaryGoal ?: "Maintain"
                                    Text(text = goal.uppercase(), fontSize = 18.sp, color = ProteinOrange, fontWeight = FontWeight.Black)
                                    Text(text = "${profile?.targetCalories ?: 0} kcal goal", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Historical Logs list
            item {
                Text(
                    text = "METRICS CHRONICLES",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (weightLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No metrics logged yet. Start today!", color = TextSecondary)
                    }
                }
            } else {
                items(weightLogs.sortedByDescending { it.dateMillis }) { log ->
                    WeightLogItem(log = log, unit = unit, onDelete = { viewModel.deleteWeightLogEntry(log.id) })
                }
            }
        }

        // Custom Log dialog
        if (showLogDialog) {
            AlertDialog(
                onDismissRequest = { showLogDialog = false },
                title = { Text("Log Metric Entry", color = GymVolt, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("BODY WEIGHT (${unit.label})", color = TextPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        OutlinedTextField(
                            value = waistInput,
                            onValueChange = { waistInput = it },
                            label = { Text("WAIST MEASURE (cm) - Optional", color = TextPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = armsInput,
                                onValueChange = { armsInput = it },
                                label = { Text("ARMS (cm)", color = TextPrimary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )

                            OutlinedTextField(
                                value = chestInput,
                                onValueChange = { chestInput = it },
                                label = { Text("CHEST (cm)", color = TextPrimary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }

                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("TRAINING DAY NOTES", color = TextPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val w = weightInput.trim().toDoubleOrNull()?.let(unit::toKg)
                            if (w != null) {
                                viewModel.logMorningWeightAndMetrics(
                                    weight = w,
                                    waist = waistInput.toDoubleOrNull(),
                                    arms = armsInput.toDoubleOrNull(),
                                    chest = chestInput.toDoubleOrNull(),
                                    note = noteInput
                                )
                                showLogDialog = false
                                noteInput = ""
                                waistInput = ""
                                armsInput = ""
                                chestInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymVolt)
                    ) {
                        Text(text = "LOG ENTRY", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogDialog = false }) {
                        Text("CANCEL", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }

        // Steps Log dialog
        if (showStepsDialog) {
            AlertDialog(
                onDismissRequest = { showStepsDialog = false },
                title = { Text("Log Daily Steps", color = ProteinOrange, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = stepsInput,
                        onValueChange = { stepsInput = it },
                        label = { Text("Total Steps", color = TextPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ProteinOrange, focusedLabelColor = ProteinOrange, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val steps = stepsInput.toIntOrNull()
                            if (steps != null) {
                                viewModel.logDailySteps(steps)
                                showStepsDialog = false
                                stepsInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ProteinOrange)
                    ) {
                        Text(text = "LOG STEPS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStepsDialog = false }) {
                        Text("CANCEL", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

@Composable
fun WeightLineChart(
    logs: List<WeightLog>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val weights = logs.map { it.weightKg }
        val minVal = weights.minOrNull() ?: 50.0
        val maxVal = weights.maxOrNull() ?: 100.0
        val diff = (maxVal - minVal).coerceAtLeast(1.0)

        // Give vertical padding
        val yMin = minVal - diff * 0.1
        val yMax = maxVal + diff * 0.1
        val yDiff = yMax - yMin

        val xSpacing = width / (logs.size - 1).coerceAtLeast(1)

        val points = logs.mapIndexed { idx, log ->
            val x = idx * xSpacing
            val y = height - ((log.weightKg - yMin) / yDiff * height).toFloat()
            Offset(x, y)
        }

        // Calculate moving average points (period of 2 or 3)
        val trendPoints = logs.mapIndexed { idx, _ ->
            // Past 3 points
            val average = logs.subList((idx - 2).coerceAtLeast(0), idx + 1).map { it.weightKg }.average()
            val x = idx * xSpacing
            val y = height - ((average - yMin) / yDiff * height).toFloat()
            Offset(x, y)
        }

        // Draw grids
        drawRect(color = Color.Transparent)
        for (i in 1..4) {
            val hY = height * (i / 5f)
            drawLine(
                color = CardStroke.copy(alpha = 0.5f),
                start = Offset(0f, hY),
                end = Offset(width, hY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw background path gradient under weight line
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(0f, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(width, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ElectricBlue.copy(alpha = 0.3f), Color.Transparent)
                )
            )

            // Draw Daily connection path
            val linePath = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = ElectricBlue,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw Trendline path (orange)
            val trendPath = Path().apply {
                moveTo(trendPoints[0].x, trendPoints[0].y)
                for (i in 1 until trendPoints.size) {
                    lineTo(trendPoints[i].x, trendPoints[i].y)
                }
            }
            drawPath(
                path = trendPath,
                color = ProteinOrange,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )

            // Draw point dots
            points.forEach { pt ->
                drawCircle(color = TextPrimary, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = ElectricBlue, radius = 3.dp.toPx(), center = pt)
            }
        }
    }
}

@Composable
fun WeightLogItem(
    log: WeightLog,
    unit: WeightUnit,
    onDelete: () -> Unit
) {
    val formatter = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
    val formattedDate = formatter.format(Date(log.dateMillis))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = "Calendar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = formattedDate, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(text = "WEIGHT", fontSize = 10.sp, color = TextSecondary)
                        Text(text = unit.formatWithUnit(log.weightKg), fontSize = 16.sp, color = GymVolt, fontWeight = FontWeight.Black)
                    }

                    if (log.waistCm != null) {
                        Column {
                            Text(text = "WAIST", fontSize = 10.sp, color = TextSecondary)
                            Text(text = "${log.waistCm} cm", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (log.armsCm != null) {
                        Column {
                            Text(text = "ARMS", fontSize = 10.sp, color = TextSecondary)
                            Text(text = "${log.armsCm} cm", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (log.chestCm != null) {
                        Column {
                            Text(text = "CHEST", fontSize = 10.sp, color = TextSecondary)
                            Text(text = "${log.chestCm} cm", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (log.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Notes: ${log.note}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/** Simple line chart of values in display units, oldest to newest. */
@Composable
private fun OneRepMaxChart(points: List<Double>) {
    val minValue = points.min()
    val maxValue = points.max()
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(DarkCard, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        val stepX = if (points.size > 1) size.width / (points.size - 1) else 0f
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minValue) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = GymVolt, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
        }
        drawPath(
            path = path,
            color = GymVolt,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}
