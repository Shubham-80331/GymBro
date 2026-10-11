package com.gymbro.fitness.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.gymbro.fitness.domain.WeightUnit
import com.gymbro.fitness.ui.GymBroViewModel
import com.gymbro.fitness.ui.theme.*

@Composable
fun OnboardingScreen(
    viewModel: GymBroViewModel,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) }

    // Onboarding form state, prefilled from the saved profile when editing goals later.
    val existing = remember { viewModel.userProfile.value }
    val unit by viewModel.weightUnit.collectAsState()
    var age by remember { mutableStateOf(existing?.age?.toString() ?: "25") }
    var gender by remember { mutableStateOf(existing?.gender ?: "Male") }
    var weight by remember { mutableStateOf(existing?.weightKg?.let { unit.format(it) } ?: unit.format(75.0)) }
    var height by remember { mutableStateOf(existing?.heightCm?.toString() ?: "175.0") }
    var activityLevel by remember { mutableStateOf(existing?.activityLevel ?: "Moderately Active") }
    var primaryGoal by remember { mutableStateOf(existing?.primaryGoal ?: "Bulk") }
    var targetWeight by remember { mutableStateOf(existing?.targetWeightKg?.let { unit.format(it) } ?: unit.format(80.0)) }
    var timeframeWeeks by remember { mutableStateOf(existing?.timeframeWeeks?.toString() ?: "12") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = "GymBro Logo",
                    tint = GymVolt,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "GYMBRO",
                    style = MaterialTheme.typography.displayMedium,
                    color = GymVolt,
                    fontWeight = FontWeight.Black
                )
            }

            // Cards according to step
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "stepAnimation"
            ) { currentStep ->
                when (currentStep) {
                    1 -> StepVitals(
                        age = age, onAgeChange = { age = it },
                        gender = gender, onGenderChange = { gender = it },
                        height = height, onHeightChange = { height = it },
                        weight = weight, onWeightChange = { weight = it },
                        unit = unit,
                        onUnitChange = { newUnit ->
                            // Convert what's typed so switching units doesn't change the actual weight.
                            weight.trim().toDoubleOrNull()?.let { weight = newUnit.format(unit.toKg(it)) }
                            targetWeight.trim().toDoubleOrNull()?.let { targetWeight = newUnit.format(unit.toKg(it)) }
                            viewModel.setWeightUnit(newUnit)
                        },
                        onNext = { step = 2 }
                    )
                    2 -> StepGoals(
                        activityLevel = activityLevel, onActivityChange = { activityLevel = it },
                        primaryGoal = primaryGoal, onGoalChange = { primaryGoal = it },
                        targetWeight = targetWeight, onTargetWeightChange = { targetWeight = it },
                        unit = unit,
                        timeframeWeeks = timeframeWeeks, onTimeframeChange = { timeframeWeeks = it },
                        onBack = { step = 1 },
                        onComplete = {
                            // Both steps only enable their buttons once these parse and are in range.
                            val finalAge = age.trim().toIntOrNull()
                            val finalWeight = weight.trim().toDoubleOrNull()?.let(unit::toKg)
                            val finalHeight = height.trim().toDoubleOrNull()
                            val finalTargetWeight = targetWeight.trim().toDoubleOrNull()?.let(unit::toKg)
                            val finalWeeks = timeframeWeeks.trim().toIntOrNull()
                            if (finalAge == null || finalWeight == null || finalHeight == null ||
                                finalTargetWeight == null || finalWeeks == null
                            ) {
                                step = 1
                                return@StepGoals
                            }

                            viewModel.completeOnboarding(
                                age = finalAge,
                                gender = gender,
                                weightKg = finalWeight,
                                heightCm = finalHeight,
                                activityLevel = activityLevel,
                                primaryGoal = primaryGoal,
                                targetWeightKg = finalTargetWeight,
                                timeframeWeeks = finalWeeks
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StepVitals(
    age: String, onAgeChange: (String) -> Unit,
    gender: String, onGenderChange: (String) -> Unit,
    height: String, onHeightChange: (String) -> Unit,
    weight: String, onWeightChange: (String) -> Unit,
    unit: WeightUnit, onUnitChange: (WeightUnit) -> Unit,
    onNext: () -> Unit
) {
    val ageValid = isValidAge(age)
    val heightValid = isValidHeight(height)
    val weightValid = isValidBodyWeight(weight, unit)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_vitals_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "STEP 1: YOUR VITALS",
                style = MaterialTheme.typography.labelMedium,
                color = GymVolt
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Establish your physical matrix.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Gender selector buttons
            Text(
                text = "GENDER",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Male", "Female", "Other").forEach { g ->
                    val isSelected = gender == g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GymVolt else DarkCard)
                            .clickable { onGenderChange(g) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = g.uppercase(),
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Weight unit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("UNITS", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                WeightUnit.entries.forEach { option ->
                    val selected = option == unit
                    OutlinedButton(
                        onClick = { if (!selected) onUnitChange(option) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) GymVolt else Color.Transparent,
                            contentColor = if (selected) Color.Black else TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text(option.label, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Age field
            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("AGE (Years)") },
                isError = !ageValid,
                supportingText = if (!ageValid) {{ Text("Enter an age from 13 to 100") }} else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GymVolt,
                    focusedLabelColor = GymVolt
                )
            )

            // Height and Weight Fields side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = height,
                    onValueChange = onHeightChange,
                    label = { Text("HEIGHT (cm)") },
                    isError = !heightValid,
                    supportingText = if (!heightValid) {{ Text("100–250 cm") }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymVolt,
                        focusedLabelColor = GymVolt
                    )
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = onWeightChange,
                    label = { Text("WEIGHT (${unit.label})") },
                    isError = !weightValid,
                    supportingText = if (!weightValid) {{ Text(bodyWeightRangeText(unit)) }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymVolt,
                        focusedLabelColor = GymVolt
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNext,
                enabled = ageValid && heightValid && weightValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("onboarding_next_button"),
                colors = ButtonDefaults.buttonColors(containerColor = GymVolt),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "NEXT MATRIX",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "NextStep",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
fun StepGoals(
    activityLevel: String, onActivityChange: (String) -> Unit,
    primaryGoal: String, onGoalChange: (String) -> Unit,
    targetWeight: String, onTargetWeightChange: (String) -> Unit,
    unit: WeightUnit,
    timeframeWeeks: String, onTimeframeChange: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val targetWeightValid = isValidBodyWeight(targetWeight, unit)
    val timeframeValid = isValidTimeframe(timeframeWeeks)
    val activityLevels = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Highly Active")
    val goals = listOf("Cut", "Maintain", "Bulk", "Recomp")

    var activityExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_goals_card"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "STEP 2: TARGET MATRIX",
                style = MaterialTheme.typography.labelMedium,
                color = GymVolt
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Specify your ultimate goal.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Primary Goal Selection
            Text(
                text = "PRIMARY GOAL",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                goals.forEach { g ->
                    val isSelected = primaryGoal == g
                    val label = when (g) {
                        "Cut" -> "CUT"
                        "Bulk" -> "BULK"
                        "Maintain" -> "STAY"
                        "Recomp" -> "RECOMP"
                        else -> g
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ProteinOrange else DarkCard)
                            .clickable { onGoalChange(g) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Target Weight and Timeframe Weeks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = onTargetWeightChange,
                    label = { Text("TARGET WEIGHT (${unit.label})") },
                    isError = !targetWeightValid,
                    supportingText = if (!targetWeightValid) {{ Text(bodyWeightRangeText(unit)) }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymVolt,
                        focusedLabelColor = GymVolt
                    )
                )

                OutlinedTextField(
                    value = timeframeWeeks,
                    onValueChange = onTimeframeChange,
                    label = { Text("TIMEFRAME (Wks)") },
                    isError = !timeframeValid,
                    supportingText = if (!timeframeValid) {{ Text("1–104 weeks") }} else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GymVolt,
                        focusedLabelColor = GymVolt
                    )
                )
            }

            // Activity level dropdown trigger
            Text(
                text = "ACTIVITY LEVEL",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCard)
                    .clickable { activityExpanded = true }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = activityLevel, color = TextPrimary)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = GymVolt)
                }

                DropdownMenu(
                    expanded = activityExpanded,
                    onDismissRequest = { activityExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    activityLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level, color = TextPrimary) },
                            onClick = {
                                onActivityChange(level)
                                activityExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymVolt)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "BACK")
                }

                Button(
                    onClick = onComplete,
                    enabled = targetWeightValid && timeframeValid,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(56.dp)
                        .testTag("onboarding_complete_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GymVolt),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "LOCK IN GAINS",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Lock",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

private fun isValidAge(value: String) = value.trim().toIntOrNull()?.let { it in 13..100 } == true
private fun isValidHeight(value: String) = value.trim().toDoubleOrNull()?.let { it in 100.0..250.0 } == true
private fun isValidBodyWeight(value: String, unit: WeightUnit) =
    value.trim().toDoubleOrNull()?.let { unit.toKg(it) in 30.0..300.0 } == true

private fun bodyWeightRangeText(unit: WeightUnit) = "${unit.format(30.0)}–${unit.format(300.0)} ${unit.label}"
private fun isValidTimeframe(value: String) = value.trim().toIntOrNull()?.let { it in 1..104 } == true
