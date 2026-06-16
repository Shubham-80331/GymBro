package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class GymBroTab(val title: String) {
    COACH("Coach"),
    WORKOUT("Workout"),
    DIET("Diet"),
    PROGRESS("Progress")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymBroApp(
    viewModel: GymBroViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    var currentTab by remember { mutableStateOf(GymBroTab.COACH) }

    if (profile == null || !profile!!.onboardingCompleted) {
        // Multi-Step Onboarding Vitals Flow
        OnboardingScreen(viewModel = viewModel, modifier = modifier)
    } else {
        // Main Dashboard Hud layout
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GYMBRO PRO",
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GymVolt,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (currentTab) {
                                GymBroTab.COACH -> "AI Coach Chat"
                                GymBroTab.WORKOUT -> "Train & Routines"
                                GymBroTab.DIET -> "Macro Tracker"
                                GymBroTab.PROGRESS -> "Analytics & Progress"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Streak badge
                        Row(
                            modifier = Modifier
                                .background(DarkSurface, shape = CircleShape)
                                .border(
                                    width = 1.dp,
                                    color = CardStroke,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🔥", fontSize = 14.sp)
                            val sessions by viewModel.allSessions.collectAsState()
                            val streakValue = 12 + (sessions.size)
                            Text(
                                text = "$streakValue",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        // Profile avatar with a beautiful geometric gradient
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(GymVolt, ElectricBlue)
                                    ),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color(0x33FFFFFF),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF181818),
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    GymBroTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        val icon = when (tab) {
                            GymBroTab.COACH -> if (isSelected) Icons.Default.SmartToy else Icons.Default.SmartToy
                            GymBroTab.WORKOUT -> if (isSelected) Icons.Default.FitnessCenter else Icons.Default.FitnessCenter
                            GymBroTab.DIET -> if (isSelected) Icons.Default.Restaurant else Icons.Default.Restaurant
                            GymBroTab.PROGRESS -> if (isSelected) Icons.Default.TrendingUp else Icons.Default.TrendingUp
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) Color.Black else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) GymVolt else TextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = GymVolt,
                                selectedTextColor = GymVolt,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tabContentSwitcher"
                ) { tab ->
                    when (tab) {
                        GymBroTab.COACH -> CoachScreen(viewModel = viewModel)
                        GymBroTab.WORKOUT -> WorkoutsScreen(viewModel = viewModel)
                        GymBroTab.DIET -> NutritionScreen(viewModel = viewModel)
                        GymBroTab.PROGRESS -> ProgressScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
