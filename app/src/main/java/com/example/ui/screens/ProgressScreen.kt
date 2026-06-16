package com.example.ui.screens

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
import com.example.data.WeightLog
import com.example.ui.GymBroViewModel
import com.example.ui.theme.*
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
                weightInput = it.weightKg.toString()
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
                                    text = String.format(Locale.US, "%.1f", profile?.weightKg ?: 82.4),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "kg",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Dynamic calculated change
                            val loggedDiff = if (weightLogs.size >= 2) {
                                val latest = weightLogs.first().weightKg
                                val prev = weightLogs[1].weightKg
                                latest - prev
                            } else {
                                -0.4
                            }
                            val changeText = if (loggedDiff < 0) {
                                String.format(Locale.US, "▼ %.1fkg since prev", -loggedDiff)
                            } else {
                                String.format(Locale.US, "▲ %.1fkg since prev", loggedDiff)
                            }
                            Text(
                                text = changeText,
                                fontSize = 11.sp,
                                color = if (loggedDiff < 0) FitGreen else ProteinOrange,
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
                            val highestVolume = sessions.maxOfOrNull { it.totalVolumeLoadKg } ?: 12400.0
                            val prKiloText = if (highestVolume >= 1000) {
                                String.format(Locale.US, "%.1fk", highestVolume / 1000.0)
                            } else {
                                String.format(Locale.US, "%.0f", highestVolume)
                            }
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = prKiloText,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "kg",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "NEW RECORD! 🏆",
                                fontSize = 10.sp,
                                color = ProteinOrange,
                                fontWeight = FontWeight.Black,
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
                        
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val todaysSteps = stepsLogs.filter { it.dateString == today }.sumOf { it.caloriesBurnt }
                        val todaysWorkout = sessions.filter { 
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.dateMillis)) == today 
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
                                Text("${todaysSteps} kcal", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
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
                                text = "CORRELATION INSIGHTS",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Adhering to your dynamic caloric splits has a direct correlation with your physical transformation parameters. High workout volumes trigger hypertrophic stimulus.",
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
                                    Text(text = "${totalVol.toInt()} kg", fontSize = 18.sp, color = FitGreen, fontWeight = FontWeight.Black)
                                    Text(text = "Accumulated", fontSize = 10.sp, color = TextSecondary)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(DarkCard, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "TARGET ACCURACY", fontSize = 11.sp, color = TextSecondary)
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
                    WeightLogItem(log = log, onDelete = { viewModel.deleteWeightLogEntry(log.id) })
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
                            label = { Text("BODY WEIGHT (kg)", color = TextPrimary) },
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
                            val w = weightInput.toDoubleOrNull()
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
                        Text(text = "${log.weightKg} kg", fontSize = 16.sp, color = GymVolt, fontWeight = FontWeight.Black)
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
