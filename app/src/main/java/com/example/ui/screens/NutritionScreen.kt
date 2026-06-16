package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FoodLog

import com.example.ui.GymBroViewModel
import com.example.ui.theme.*

@Composable
fun NutritionScreen(
    viewModel: GymBroViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val foodLogs by viewModel.foodLogsForSelectedDay.collectAsState()
    val waterIntake by viewModel.waterIntakeForSelectedDay.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var showEditGoalsDialog by remember { mutableStateOf(false) }
    var editProteinGoal by remember { mutableStateOf("") }
    var editCarbsGoal by remember { mutableStateOf("") }
    var editFatsGoal by remember { mutableStateOf("") }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf("Breakfast") }

    // Manual Food Add Form
    var foodName by remember { mutableStateOf("") }
    var foodProtein by remember { mutableStateOf("") }
    var foodCarbs by remember { mutableStateOf("") }
    var foodFats by remember { mutableStateOf("") }

    // Daily calculations
    val targetCal = profile?.targetCalories ?: 2200
    val targetProtein = profile?.targetProteinGrams ?: 140
    val targetCarbs = profile?.targetCarbsGrams ?: 250
    val targetFats = profile?.targetFatsGrams ?: 70

    val loggedCal = foodLogs.sumOf { it.calories }
    val loggedProtein = foodLogs.sumOf { it.proteinGrams }
    val loggedCarbs = foodLogs.sumOf { it.carbsGrams }
    val loggedFats = foodLogs.sumOf { it.fatsGrams }

    val caloriesRemaining = (targetCal - loggedCal).coerceAtLeast(0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Date offset selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.changeActiveDateOffset(-1) }) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Prev Day", tint = GymVolt)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "DATE RADAR", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Text(text = selectedDate, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }

                    IconButton(onClick = { viewModel.changeActiveDateOffset(1) }) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next Day", tint = GymVolt)
                    }
                }
            }
        }

        // Dashboard Calories Progress Tracker HUD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = CardStroke, shape = RoundedCornerShape(24.dp))
                    .testTag("nutrition_dashboard")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAILY NUTRITION",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { 
                                editProteinGoal = targetProtein.toString()
                                editCarbsGoal = targetCarbs.toString()
                                editFatsGoal = targetFats.toString()
                                showEditGoalsDialog = true 
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Goals", tint = GymVolt)
                            }
                            Box(
                                modifier = Modifier
                                    .background(GymVolt.copy(alpha = 0.15f), shape = RoundedCornerShape(100))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                val ratio = if (targetCal > 0) ((loggedCal.toFloat() / targetCal.toFloat())).coerceIn(0f, 1f) else 0f
                                Text(
                                    text = "${(ratio * 100).toInt()}% Goal",
                                    color = GymVolt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val formatter = java.text.DecimalFormat("#,###")
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formatter.format(loggedCal),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = " / ${formatter.format(targetCal)} kcal",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = if (caloriesRemaining > 0) "$caloriesRemaining kcal left to hit target" else "Target calories achieved! 🔥",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Circular design summary ratio percentage
                        Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                            val ratio = (loggedCal.toFloat() / targetCal.toFloat()).coerceIn(0f, 1f)
                            CircularProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxSize(),
                                color = GymVolt,
                                strokeWidth = 5.dp,
                                trackColor = CardStroke
                            )
                            Text(
                                text = "${(ratio * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = GymVolt,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Three columns macro bento layout (Protein, Carbs, Fats)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BentoMacroColumn(
                            label = "Protein",
                            current = loggedProtein,
                            target = targetProtein,
                            color = GymVolt,
                            modifier = Modifier.weight(1f)
                        )
                        BentoMacroColumn(
                            label = "Carbs",
                            current = loggedCarbs,
                            target = targetCarbs,
                            color = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )
                        BentoMacroColumn(
                            label = "Fats",
                            current = loggedFats,
                            target = targetFats,
                            color = ProteinOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Hydration Tracker
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HYDRATION TRACKER",
                            color = GymVolt,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$waterIntake ml",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val waterTarget = 3000
                    val waterFraction = (waterIntake.toFloat() / waterTarget.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { waterFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = GymVolt,
                        trackColor = CardStroke
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.logWaterIntake(250) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                        ) {
                            Text("+250ml", color = TextPrimary)
                        }
                        Button(
                            onClick = { viewModel.logWaterIntake(500) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                        ) {
                            Text("+500ml", color = TextPrimary)
                        }
                        Button(
                            onClick = { viewModel.logWaterIntake(1000) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                        ) {
                            Text("+1L", color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Manual Food Add Button
        item {
            Button(
                onClick = {
                    foodName = ""
                    foodProtein = ""
                    foodCarbs = ""
                    foodFats = ""
                    selectedMealType = "Breakfast"
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("manually_add_food_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.AddBox, contentDescription = "Manual log", tint = GymVolt)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "MANUALLY LOG CUSTOM FOOD", color = GymVolt, fontWeight = FontWeight.Bold)
            }
        }

        // Logs split into categories: Breakfast, Lunch, Dinner, Snacks
        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { meal ->
            val logs = foodLogs.filter { it.mealType == meal }
            if (logs.isNotEmpty()) {
                item {
                    Text(
                        text = meal.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = GymVolt,
                        fontWeight = FontWeight.Black
                    )
                }

                items(logs) { log ->
                    FoodLogItem(log = log, onDelete = { viewModel.deleteLoggedFood(log.id) })
                }
            }
        }
    }

    // Manual / Selected Food Log popup Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log Food Item details", color = GymVolt, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Meal selector
                    Text(text = "MEAL CAT", fontSize = 11.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { type ->
                            val isSelected = selectedMealType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) GymVolt else DarkCard)
                                    .clickable { selectedMealType = type },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else TextPrimary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Remark (e.g. Whey Protein)", color = TextPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = foodProtein,
                            onValueChange = { foodProtein = it },
                            label = { Text("Protein (g)", color = TextPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        OutlinedTextField(
                            value = foodCarbs,
                            onValueChange = { foodCarbs = it },
                            label = { Text("Carbs (g)", color = TextPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )

                        OutlinedTextField(
                            value = foodFats,
                            onValueChange = { foodFats = it },
                            label = { Text("Fats (g)", color = TextPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prot = foodProtein.toIntOrNull() ?: 0
                        val carb = foodCarbs.toIntOrNull() ?: 0
                        val fat = foodFats.toIntOrNull() ?: 0
                        val calories = (prot * 4) + (carb * 4) + (fat * 9)

                        if (foodName.isNotBlank()) {
                            viewModel.logFoodItem(
                                name = foodName,
                                brand = "",
                                calories = calories,
                                protein = prot,
                                carbs = carb,
                                fats = fat,
                                portion = "custom",
                                mealType = selectedMealType
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GymVolt)
                ) {
                    Text(text = "LOG ITEM", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showEditGoalsDialog) {
        AlertDialog(
            onDismissRequest = { showEditGoalsDialog = false },
            title = { Text("Edit Daily Macro Goals", color = GymVolt, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editProteinGoal,
                        onValueChange = { editProteinGoal = it },
                        label = { Text("Daily Protein (g)", color = TextPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = editCarbsGoal,
                        onValueChange = { editCarbsGoal = it },
                        label = { Text("Daily Carbs (g)", color = TextPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = editFatsGoal,
                        onValueChange = { editFatsGoal = it },
                        label = { Text("Daily Fats (g)", color = TextPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymVolt, focusedLabelColor = GymVolt, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = editProteinGoal.toIntOrNull() ?: targetProtein
                        val c = editCarbsGoal.toIntOrNull() ?: targetCarbs
                        val f = editFatsGoal.toIntOrNull() ?: targetFats
                        viewModel.updateDailyMacroTargets(p, c, f)
                        showEditGoalsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GymVolt)
                ) {
                    Text(text = "SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalsDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }


}

@Composable
fun FoodLogItem(
    log: FoodLog,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = log.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (log.brand.isNotEmpty()) {
                        Text(text = " - ${log.brand}", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "Prot: ${log.proteinGrams}g", fontSize = 11.sp, color = FitGreen, fontWeight = FontWeight.Bold)
                    Text(text = "Carbs: ${log.carbsGrams}g", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                    Text(text = "Fats: ${log.fatsGrams}g", fontSize = 11.sp, color = ProteinOrange, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "${log.calories} kcal", color = GymVolt, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Delete entry", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun BentoMacroColumn(
    label: String,
    current: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fraction = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = CardStroke
        )
        Column {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${current}g / ${target}g",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    current: Int,
    target: Int,
    color: Color
) {
    val fraction = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = "$current / $target g", fontSize = 10.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = CardStroke
        )
    }
}
