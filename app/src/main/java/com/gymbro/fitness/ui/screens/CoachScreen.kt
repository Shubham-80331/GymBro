package com.gymbro.fitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.fitness.ui.ChatMessage
import com.gymbro.fitness.ui.GymBroViewModel
import com.gymbro.fitness.ui.theme.*

@Composable
fun CoachScreen(
    viewModel: GymBroViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showRoutineBuilder by remember { mutableStateOf(false) }
    var routineRequest by remember { mutableStateOf("") }
    val routineDraft by viewModel.aiRoutineDraft.collectAsState()
    val routineStatus by viewModel.aiRoutineStatus.collectAsState()

    if (showRoutineBuilder) {
        AlertDialog(
            onDismissRequest = {
                showRoutineBuilder = false
                viewModel.dismissAiRoutineDraft()
            },
            containerColor = DarkSurface,
            title = { Text("Build a routine with GymBro", color = GymVolt, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val draft = routineDraft
                    if (draft == null) {
                        OutlinedTextField(
                            value = routineRequest,
                            onValueChange = { routineRequest = it },
                            placeholder = { Text("e.g. 45-min upper body, dumbbells only", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        routineStatus?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }
                    } else {
                        Text(draft.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (draft.description.isNotBlank()) {
                            Text(draft.description, color = TextSecondary, fontSize = 12.sp)
                        }
                        draft.exercises.forEach { ex ->
                            Text(
                                "• ${ex.name}: ${ex.targetSets} × ${ex.targetReps}, rest ${ex.restSeconds}s",
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Text("AI suggestions can be wrong. Adjust weights to what feels safe.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                if (routineDraft == null) {
                    TextButton(
                        onClick = { viewModel.requestAiRoutine(routineRequest) },
                        enabled = routineRequest.isNotBlank() && routineStatus != "Building your routine…"
                    ) {
                        Text("BUILD", color = GymVolt, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = {
                        viewModel.saveAiRoutineDraft()
                        showRoutineBuilder = false
                    }) {
                        Text("SAVE ROUTINE", color = GymVolt, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (routineDraft != null) {
                        viewModel.dismissAiRoutineDraft() // back to editing the request
                    } else {
                        showRoutineBuilder = false
                        viewModel.dismissAiRoutineDraft()
                    }
                }) {
                    Text(if (routineDraft != null) "TRY AGAIN" else "CANCEL", color = TextSecondary)
                }
            }
        )
    }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    // Scroll to bottom when messages load
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                // Suggestions row
                if (messages.size <= 2 && !isChatLoading) {
                    Text(
                        text = "QUICK DRILLS:",
                        color = GymVolt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestion2 = "Am I eating enough protein to gain muscle?"
                        val suggestion3 = "Teach me progression secrets"
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GymVolt)
                                .clickable {
                                    routineRequest = ""
                                    viewModel.dismissAiRoutineDraft()
                                    showRoutineBuilder = true
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Build Routine", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        listOf(
                            "Protein Advice" to suggestion2,
                            "Progression" to suggestion3
                        ).forEach { (label, promptText) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkCard)
                                    .clickable {
                                        viewModel.sendMessageToGymBro(promptText)
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = GymVolt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(onClick = {
                        routineRequest = ""
                        viewModel.dismissAiRoutineDraft()
                        showRoutineBuilder = true
                    }) {
                        Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = "Build a routine", tint = GymVolt)
                    }
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask GymBro dynamic workout advice...", color = TextSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_coach_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GymVolt,
                            unfocusedBorderColor = CardStroke,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessageToGymBro(textInput)
                                    textInput = ""
                                    focusManager.clearFocus()
                                }
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessageToGymBro(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .testTag("ai_coach_send_button")
                            .size(48.dp)
                            .background(GymVolt, shape = RoundedCornerShape(24.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Virtual Partner Avatar Status Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(GymVolt, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Coach Avatar",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GYMBRO AI COACH",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(FitGreen, RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "AI", color = FitGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = if (isChatLoading) "GymBro is thinking..." else "AI advice can be wrong · not medical advice",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(messages) { message ->
                    val isUser = message.sender == "User"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    )
                                )
                                .background(if (isUser) GymVolt else DarkSurface)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isUser) "YOU" else "GYMBRO AI",
                                    color = if (isUser) Color.Black else GymVolt,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    color = if (isUser) Color.Black else TextPrimary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                if (isChatLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = GymVolt,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "GymBro is drinking pre-workout...",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
