package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Routine
import com.example.data.RoutineExercise
import com.example.ui.ActiveWorkoutExercise
import com.example.ui.GymBroViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkoutsScreen(
    viewModel: GymBroViewModel,
    modifier: Modifier = Modifier
) {
    val routines by viewModel.allRoutines.collectAsState()
    val activeSession by viewModel.activeWorkout.collectAsState()
    val streak by viewModel.workoutStreak.collectAsState()
    val prCelebration by viewModel.isPrCelebration.collectAsState()

    var showCreateRoutineDialog by remember { mutableStateOf(false) }
    var routineNameInput by remember { mutableStateOf("") }
    var routineDescInput by remember { mutableStateOf("") }
    var selectedExercisesForRoutine by remember { mutableStateOf(listOf<RoutineExercise>()) }
    var showExercisePicker by remember { mutableStateOf(false) }
    val allExercises by viewModel.allExercises.collectAsState()

    // Edit routine state
    var editingRoutineId by remember { mutableIntStateOf(-1) }

    // Custom exercise creation state
    var showCustomExerciseForm by remember { mutableStateOf(false) }
    var customExName by remember { mutableStateOf("") }
    var customExMuscle by remember { mutableStateOf("Chest") }
    var customExEquipment by remember { mutableStateOf("Barbell") }

    AnimatedContent(
        targetState = activeSession != null,
        label = "workoutPanelToggle"
    ) { hasActiveWorkout ->
        if (hasActiveWorkout && activeSession != null) {
            // Live Active Workout screen
            ActiveWorkoutPanel(
                viewModel = viewModel,
                activeSession = activeSession!!,
                prCelebration = prCelebration
            )
        } else {
            // Routine dashboard and pre-made templates
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    // Streaks and Achievements HUD
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(width = 1.dp, color = CardStroke, shape = RoundedCornerShape(24.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Streak",
                                        tint = ProteinOrange,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "STREAK LEVEL", 
                                            fontSize = 11.sp, 
                                            color = TextSecondary, 
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "$streak DAYS ACTIVE",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = GymVolt
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF26262B), RoundedCornerShape(12.dp))
                                        .border(width = 1.dp, color = CardStroke, shape = RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "LVL ${1 + streak / 5}",
                                        color = GymVolt,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Pre-made Templates and Routines header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ROUTINES LAB",
                                    color = GymVolt,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "Custom Master Splits",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    editingRoutineId = -1
                                    routineNameInput = ""
                                    routineDescInput = ""
                                    selectedExercisesForRoutine = emptyList()
                                    showCreateRoutineDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GymVolt),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("create_routine_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "NEW SPLIT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (routines.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Routines initialization loading...", color = TextSecondary)
                            }
                        }
                    } else {
                        items(routines) { routine ->
                            RoutineItemCard(
                                routine = routine,
                                onStartClick = { viewModel.startWorkout(routine) },
                                onEditClick = {
                                    editingRoutineId = routine.id
                                    routineNameInput = routine.name
                                    routineDescInput = routine.description
                                    selectedExercisesForRoutine = viewModel.parseExercisesJson(routine.exercisesJson)
                                    showCreateRoutineDialog = true
                                },
                                onDeleteClick = { viewModel.deleteRoutine(routine.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create / Edit custom routine split
    if (showCreateRoutineDialog) {
        val isEditing = editingRoutineId > 0
        AlertDialog(
            onDismissRequest = { showCreateRoutineDialog = false },
            title = {
                Text(
                    text = if (isEditing) "Edit Workout Split" else "Design Custom Workout Split",
                    color = GymVolt,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = routineNameInput,
                        onValueChange = { routineNameInput = it },
                        label = { Text("ROUTINE NAME", color = TextPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    OutlinedTextField(
                        value = routineDescInput,
                        onValueChange = { routineDescInput = it },
                        label = { Text("SPLIT TARGET OBJECTIVES", color = TextPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Text("EXERCISES", color = GymVolt, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedExercisesForRoutine) { ex ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCard, RoundedCornerShape(8.dp))
                                    .border(1.dp, CardStroke, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = ex.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "${ex.targetSets} Sets x ${ex.targetReps} Reps | ${ex.muscleGroup}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedExercisesForRoutine = selectedExercisesForRoutine.filter { it.exerciseId != ex.exerciseId }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = ErrorRed)
                                    }
                                }
                            }
                        }
                        item {
                            Button(
                                onClick = { showExercisePicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Exercise", tint = GymVolt)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ADD EXERCISE", color = GymVolt, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routineNameInput.isNotBlank()) {
                            if (isEditing) {
                                viewModel.updateRoutineExercises(editingRoutineId, routineNameInput, routineDescInput, selectedExercisesForRoutine)
                            } else {
                                viewModel.createRoutine(routineNameInput, routineDescInput, selectedExercisesForRoutine)
                            }
                            showCreateRoutineDialog = false
                            routineNameInput = ""
                            routineDescInput = ""
                            selectedExercisesForRoutine = emptyList()
                            editingRoutineId = -1
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GymVolt)
                ) {
                    Text(text = if (isEditing) "UPDATE ROUTINE" else "SAVE ROUTINE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRoutineDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showExercisePicker) {
        AlertDialog(
            onDismissRequest = { showExercisePicker = false },
            title = { Text("Select Exercise", color = GymVolt, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    // Custom exercise creation toggle
                    if (showCustomExerciseForm) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkCard, RoundedCornerShape(8.dp))
                                .border(1.dp, GymVolt.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("CREATE CUSTOM EXERCISE", color = GymVolt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = customExName,
                                onValueChange = { customExName = it },
                                label = { Text("Exercise Name", color = TextPrimary) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val muscleGroups = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
                                LazyColumn(modifier = Modifier.weight(1f).heightIn(max = 100.dp)) {
                                    items(muscleGroups) { mg ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .background(if (customExMuscle == mg) GymVolt.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(4.dp))
                                                .clickable { customExMuscle = mg }
                                                .padding(4.dp)
                                        ) {
                                            Text(mg, color = if (customExMuscle == mg) GymVolt else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                val equipmentList = listOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight")
                                LazyColumn(modifier = Modifier.weight(1f).heightIn(max = 100.dp)) {
                                    items(equipmentList) { eq ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .background(if (customExEquipment == eq) GymVolt.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(4.dp))
                                                .clickable { customExEquipment = eq }
                                                .padding(4.dp)
                                        ) {
                                            Text(eq, color = if (customExEquipment == eq) GymVolt else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showCustomExerciseForm = false },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, TextSecondary)
                                ) {
                                    Text("CANCEL", color = TextSecondary, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        if (customExName.isNotBlank()) {
                                            viewModel.addCustomExercise(customExName, customExMuscle, customExEquipment)
                                            customExName = ""
                                            showCustomExerciseForm = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = GymVolt)
                                ) {
                                    Text("CREATE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Button(
                            onClick = {
                                customExName = ""
                                showCustomExerciseForm = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GymVolt.copy(alpha = 0.15f))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Custom", tint = GymVolt, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CREATE CUSTOM EXERCISE", color = GymVolt, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allExercises) { ex ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCard, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val newRoutineEx = RoutineExercise(
                                            exerciseId = ex.id,
                                            name = ex.name,
                                            muscleGroup = ex.muscleGroup,
                                            targetSets = 3,
                                            targetReps = 10,
                                            restSeconds = 90,
                                            targetWeightKg = 0.0
                                        )
                                        selectedExercisesForRoutine = selectedExercisesForRoutine + newRoutineEx
                                        showExercisePicker = false
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = ex.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = ex.muscleGroup, color = GymVolt, fontSize = 11.sp)
                                    }
                                    if (ex.isCustom) {
                                        Box(
                                            modifier = Modifier
                                                .background(GymVolt.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("CUSTOM", color = GymVolt, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showExercisePicker = false
                    showCustomExerciseForm = false
                }) {
                    Text("CLOSE", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun RoutineItemCard(
    routine: Routine,
    onStartClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Count exercises inside
    val exerciseCount = routine.exercisesJson.split("\"exerciseId\"").size - 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = GymVolt),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = routine.name.uppercase(), 
                            color = Color(0xFF0A0A0A), 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Black,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0A0A0A).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "$exerciseCount EXS", fontSize = 10.sp, color = Color(0xFF0A0A0A), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = routine.description, color = Color(0xFF0A0A0A).copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit routine", tint = Color(0xFF0A0A0A).copy(alpha = 0.6f))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete routine", tint = Color(0xFF0A0A0A).copy(alpha = 0.6f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "START TRAINING NOW", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
fun ActiveWorkoutPanel(
    viewModel: GymBroViewModel,
    activeSession: com.example.ui.ActiveWorkoutSession,
    prCelebration: String?
) {
    val secondsLeft by viewModel.restTimerSecondsLeft.collectAsState()
    val totalSeconds by viewModel.restTimerTotalSeconds.collectAsState()
    val timerActive by viewModel.restTimerActive.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Rest timer HUD visual bar
                if (timerActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(DarkCard, RoundedCornerShape(12.dp))
                            .border(1.dp, GymVolt.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer", tint = GymVolt)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "REST COUNTER", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$secondsLeft SEC SECONDS LEFT",
                                    color = GymVolt,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        val ratio = if (totalSeconds > 0) (secondsLeft.toFloat() / totalSeconds.toFloat()) else 0f
                        CircularProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.size(36.dp),
                            color = GymVolt,
                            trackColor = CardStroke,
                            strokeWidth = 4.dp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cancelWorkout() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed)
                    ) {
                        Text(text = "ABANDON LIFT", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.finishWorkout() },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("finish_workout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GymVolt)
                    ) {
                        Text(text = "FINISH & SAVE MATRIX", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
            ) {
                item {
                    Column {
                        Text(text = "ACTIVE WORKOUT MATRIX", color = FitGreen, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = activeSession.routineName.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val totalSets = activeSession.exercises.sumOf { it.targetSets }
                        val completedSets = activeSession.exercises.sumOf { it.sets.count { s -> s.isCompleted } }
                        val progress = if (totalSets > 0) completedSets.toFloat() / totalSets.toFloat() else 0f
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Workout Progress", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("$completedSets / $totalSets sets", fontSize = 12.sp, color = GymVolt, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = GymVolt,
                            trackColor = CardStroke
                        )
                    }
                }

                itemsIndexed(activeSession.exercises) { exIndex, exercise ->
                    ActiveExerciseCard(
                        exercise = exercise,
                        exerciseIndex = exIndex,
                        viewModel = viewModel,
                        timerActive = timerActive
                    )
                }
            }

            // PR Celebration Banner Overlay
            if (prCelebration != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Trophy", tint = GymVolt, modifier = Modifier.size(96.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "NEW PERSONAL RECORD!!", color = GymVolt, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(text = prCelebration.uppercase(), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Lifting matrix upgraded!", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveExerciseCard(
    exercise: ActiveWorkoutExercise,
    exerciseIndex: Int,
    viewModel: GymBroViewModel,
    timerActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Exercise header Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = exercise.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(text = "${exercise.muscleGroup.uppercase()} | ${exercise.targetSets} sets x ${exercise.targetReps} reps", color = GymVolt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = exercise.lastSessionMessage, color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sets list header titles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "SET", modifier = Modifier.weight(0.6f), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = "TARGETS", modifier = Modifier.weight(1.8f), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = "ACTUAL KG", modifier = Modifier.weight(1.2f), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = "REPS", modifier = Modifier.weight(1f), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = "STATE", modifier = Modifier.weight(0.8f), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Actual interactive rows
            exercise.sets.forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set index
                    Text(text = "S${set.setIndex + 1}", modifier = Modifier.weight(0.6f), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Target details
                    Text(text = "${set.targetWeightKg}kg x ${set.targetReps}", modifier = Modifier.weight(1.8f), color = TextSecondary, fontSize = 12.sp)

                    // Weight TextField Input - disabled after set is completed
                    OutlinedTextField(
                        value = set.actualWeightKg,
                        onValueChange = { viewModel.updateSetWeight(exerciseIndex, set.setIndex, it) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        enabled = !set.isCompleted,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            disabledTextColor = TextSecondary,
                            focusedBorderColor = GymVolt,
                            unfocusedBorderColor = CardStroke,
                            disabledBorderColor = CardStroke.copy(alpha = 0.5f)
                        ),
                        placeholder = { Text("-", color = TextSecondary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    )

                    // Repetitions input - disabled after set is completed
                    OutlinedTextField(
                        value = set.actualReps,
                        onValueChange = { viewModel.updateSetReps(exerciseIndex, set.setIndex, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !set.isCompleted,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            disabledTextColor = TextSecondary,
                            focusedBorderColor = GymVolt,
                            unfocusedBorderColor = CardStroke,
                            disabledBorderColor = CardStroke.copy(alpha = 0.5f)
                        ),
                        placeholder = { Text("-", color = TextSecondary, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    )

                    // Completed state checkbox - color indicates state
                    val checkboxColor = when {
                        set.isCompleted -> GymVolt       // Done (timer finished or completed)
                        else -> DarkCard                  // Not started
                    }
                    IconButton(
                        onClick = { viewModel.toggleSetCompleted(exerciseIndex, set.setIndex) },
                        enabled = !set.isCompleted || !timerActive,
                        modifier = Modifier
                            .weight(0.8f)
                            .size(36.dp)
                            .background(checkboxColor, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete Set",
                            tint = if (set.isCompleted) Color.Black else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
