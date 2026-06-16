package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val sender: String, // "User" or "GymBro"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ActiveWorkoutSet(
    val setIndex: Int,
    val targetWeightKg: Double,
    val targetReps: Int,
    initialActualWeightKg: String = "",
    initialActualReps: String = "",
    initialIsCompleted: Boolean = false
) {
    var actualWeightKg by mutableStateOf(initialActualWeightKg)
    var actualReps by mutableStateOf(initialActualReps)
    var isCompleted by mutableStateOf(initialIsCompleted)
}

data class ActiveWorkoutExercise(
    val exerciseId: String,
    val name: String,
    val muscleGroup: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    val targetWeightKg: Double,
    val sets: List<ActiveWorkoutSet>,
    val lastSessionMessage: String = ""
)

data class ActiveWorkoutSession(
    val routineId: Int,
    val routineName: String,
    val exercises: List<ActiveWorkoutExercise>,
    val startTimeMillis: Long = System.currentTimeMillis()
)

class GymBroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GymBroRepository
    val userProfile: StateFlow<UserProfile?>
    val allRoutines: StateFlow<List<Routine>>
    val allSessions: StateFlow<List<WorkoutSession>>
    val allWeightLogs: StateFlow<List<WeightLog>>
    val allCustomFoods: StateFlow<List<CustomFood>>
    val allStepsLogs: StateFlow<List<StepsLog>>

    // Selected Date for Nutrition Tracker
    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Food logs for the selected date
    val foodLogsForSelectedDay: StateFlow<List<FoodLog>> = _selectedDate
        .flatMapLatest { date -> repository.getFoodLogsForDay(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Hydration logs for the selected date
    val waterIntakeForSelectedDay: StateFlow<Int> = _selectedDate
        .flatMapLatest { date -> repository.getWaterIntakeForDay(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allExercises: StateFlow<List<Exercise>>

    // Active Workout State
    private val _activeWorkout = MutableStateFlow<ActiveWorkoutSession?>(null)
    val activeWorkout: StateFlow<ActiveWorkoutSession?> = _activeWorkout.asStateFlow()

    // Rest Timer State
    private val _restTimerSecondsLeft = MutableStateFlow(0)
    val restTimerSecondsLeft: StateFlow<Int> = _restTimerSecondsLeft.asStateFlow()

    private val _restTimerTotalSeconds = MutableStateFlow(0)
    val restTimerTotalSeconds: StateFlow<Int> = _restTimerTotalSeconds.asStateFlow()

    private val _restTimerActive = MutableStateFlow(false)
    val restTimerActive: StateFlow<Boolean> = _restTimerActive.asStateFlow()

    private var timerJob: Job? = null

    // Chat with AI GymBro State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "GymBro",
                text = "What's up, champ! I'm GymBro, your virtual AI workout partner! Ask me to generate a personalized routine, review your macro-splits, design a meal plan, or explain how to master progressive overload! Let's get these gains! 💪"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Streak and PR states
    private val _workoutStreak = MutableStateFlow(0)
    val workoutStreak: StateFlow<Int> = _workoutStreak.asStateFlow()

    private val _isPrCelebration = MutableStateFlow<String?>(null) // Name of exercise for PR celebration banner
    val isPrCelebration: StateFlow<String?> = _isPrCelebration.asStateFlow()

    init {
        val database = GymBroDatabase.getDatabase(application, viewModelScope)
        repository = GymBroRepository(
            userProfileDao = database.userProfileDao(),
            exerciseDao = database.exerciseDao(),
            routineDao = database.routineDao(),
            workoutSessionDao = database.workoutSessionDao(),
            workoutSetLogDao = database.workoutSetLogDao(),
            weightLogDao = database.weightLogDao(),
            foodLogDao = database.foodLogDao(),
            customFoodDao = database.customFoodDao(),
            hydrationLogDao = database.hydrationLogDao(),
            stepsLogDao = database.stepsLogDao()
        )

        userProfile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allRoutines = repository.allRoutines.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allSessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allWeightLogs = repository.allWeightLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allStepsLogs = repository.allStepsLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCustomFoods = repository.allCustomFoods.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allExercises = repository.allExercises.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Calculate current workout streak
        viewModelScope.launch {
            allSessions.collect { sessions ->
                _workoutStreak.value = calculateStreak(sessions)
            }
        }
    }

    // Onboarding and recalculations
    fun completeOnboarding(
        age: Int,
        gender: String,
        weightKg: Double,
        heightCm: Double,
        activityLevel: String,
        primaryGoal: String,
        targetWeightKg: Double,
        timeframeWeeks: Int
    ) {
        viewModelScope.launch {
            val bmr = calculateBMR(weightKg, heightCm, age, gender)
            val tdee = calculateTDEE(bmr, activityLevel)
            val (calories, protein, carbs, fats) = calculateDailyMacros(tdee, weightKg, primaryGoal)

            val profile = UserProfile(
                age = age,
                gender = gender,
                weightKg = weightKg,
                heightCm = heightCm,
                activityLevel = activityLevel,
                primaryGoal = primaryGoal,
                targetWeightKg = targetWeightKg,
                timeframeWeeks = timeframeWeeks,
                bmr = bmr,
                tdee = tdee,
                targetCalories = calories,
                targetProteinGrams = protein,
                targetCarbsGrams = carbs,
                targetFatsGrams = fats,
                onboardingCompleted = true
            )
            repository.saveUserProfile(profile)
        }
    }

    fun updateWeight(newWeightKg: Double) {
        viewModelScope.launch {
            val profile = repository.getUserProfileOneShot()
            if (profile != null) {
                val bmr = calculateBMR(newWeightKg, profile.heightCm, profile.age, profile.gender)
                val tdee = calculateTDEE(bmr, profile.activityLevel)
                val (calories, protein, carbs, fats) = calculateDailyMacros(tdee, newWeightKg, profile.primaryGoal)

                val updatedProfile = profile.copy(
                    weightKg = newWeightKg,
                    bmr = bmr,
                    tdee = tdee,
                    targetCalories = calories,
                    targetProteinGrams = protein,
                    targetCarbsGrams = carbs,
                    targetFatsGrams = fats
                )
                repository.saveUserProfile(updatedProfile)
                
                // Add to weight history log
                repository.logWeight(WeightLog(dateMillis = System.currentTimeMillis(), weightKg = newWeightKg))
            }
        }
    }

    fun updateDailyMacroTargets(protein: Int, carbs: Int, fats: Int) {
        viewModelScope.launch {
            val profile = repository.getUserProfileOneShot()
            if (profile != null) {
                // Keep calories synced with manually overridden macros
                val newCalories = (protein * 4) + (carbs * 4) + (fats * 9)
                val updatedProfile = profile.copy(
                    targetProteinGrams = protein,
                    targetCarbsGrams = carbs,
                    targetFatsGrams = fats,
                    targetCalories = newCalories
                )
                repository.saveUserProfile(updatedProfile)
            }
        }
    }

    private fun calculateBMR(weight: Double, height: Double, age: Int, gender: String): Double {
        return if (gender.lowercase() == "male") {
            10.0 * weight + 6.25 * height - 5.0 * age + 5.0
        } else {
            10.0 * weight + 6.25 * height - 5.0 * age - 161.0
        }
    }

    private fun calculateTDEE(bmr: Double, activityLevel: String): Double {
        val multiplier = when (activityLevel) {
            "Sedentary" -> 1.2
            "Lightly Active" -> 1.375
            "Moderately Active" -> 1.55
            "Very Active" -> 1.725
            "Highly Active" -> 1.9
            else -> 1.2
        }
        return bmr * multiplier
    }

    private fun calculateDailyMacros(tdee: Double, weight: Double, goal: String): Quadruple<Int, Int, Int, Int> {
        val calories = when (goal) {
            "Cut" -> (tdee - 500).coerceAtLeast(1200.0).toInt()
            "Maintain" -> tdee.toInt()
            "Bulk" -> (tdee + 350).toInt()
            "Recomp" -> (tdee - 100).toInt()
            else -> tdee.toInt()
        }

        // Calorie splits based on goal ratios
        val (pRatio, cRatio, fRatio) = when (goal) {
            "Cut" -> Triple(0.35, 0.40, 0.25)
            "Maintain" -> Triple(0.25, 0.45, 0.30)
            "Bulk" -> Triple(0.25, 0.50, 0.25)
            "Recomp" -> Triple(0.30, 0.40, 0.30)
            else -> Triple(0.25, 0.45, 0.30)
        }

        val protein = ((calories * pRatio) / 4.0).toInt()
        val carbs = ((calories * cRatio) / 4.0).toInt()
        val fats = ((calories * fRatio) / 9.0).toInt()

        return Quadruple(calories, protein, carbs, fats)
    }

    // Workout Tracker
    fun startWorkout(routine: Routine) {
        viewModelScope.launch {
            val parsedExercises = parseExercisesJson(routine.exercisesJson)
            val activeExercises = parsedExercises.map { re ->
                // Check past weights from logged sets
                val logsFlow = repository.getSetLogsForExercise(re.exerciseId)
                val pastLogs = logsFlow.firstOrNull() ?: emptyList()
                val lastWeightInfo = if (pastLogs.isNotEmpty()) {
                    val lastLog = pastLogs.first()
                    "Last time: ${lastLog.weightKg}kg x ${lastLog.reps} reps"
                } else {
                    "First session for this exercise! Let's go!"
                }

                ActiveWorkoutExercise(
                    exerciseId = re.exerciseId,
                    name = re.name,
                    muscleGroup = re.muscleGroup,
                    targetSets = re.targetSets,
                    targetReps = re.targetReps,
                    restSeconds = re.restSeconds,
                    targetWeightKg = re.targetWeightKg,
                    lastSessionMessage = lastWeightInfo,
                    sets = List(re.targetSets) { i ->
                        ActiveWorkoutSet(
                            setIndex = i,
                            targetWeightKg = re.targetWeightKg,
                            targetReps = re.targetReps
                        )
                    }
                )
            }

            _activeWorkout.value = ActiveWorkoutSession(
                routineId = routine.id,
                routineName = routine.name,
                exercises = activeExercises
            )
        }
    }

    fun toggleSetCompleted(exerciseIndex: Int, setIndex: Int) {
        val current = _activeWorkout.value ?: return
        val exercise = current.exercises.getOrNull(exerciseIndex) ?: return
        val set = exercise.sets.getOrNull(setIndex) ?: return

        set.isCompleted = !set.isCompleted

        // Start rest timer if completed
        if (set.isCompleted) {
            startRestTimer(exercise.restSeconds)

            // Let's check for PR in completed sets!
            val actualWeight = set.actualWeightKg.toDoubleOrNull() ?: set.targetWeightKg
            val actualReps = set.actualReps.toIntOrNull() ?: set.targetReps
            viewModelScope.launch {
                val bestPastMax = repository.getOneRepMaxForExercise(exercise.exerciseId) ?: 0.0
                if (actualWeight > bestPastMax && bestPastMax > 0.0) {
                    _isPrCelebration.value = exercise.name
                    delay(3000)
                    _isPrCelebration.value = null
                }
            }
        } else {
            stopRestTimer()
        }

        // Trigger Compose recomposure
        _activeWorkout.value = current.copy()
    }

    fun finishWorkout() {
        val current = _activeWorkout.value ?: return
        viewModelScope.launch {
            var totalVolume = 0.0
            var completedCount = 0

            val now = System.currentTimeMillis()

            current.exercises.forEach { ex ->
                ex.sets.forEach { set ->
                    if (set.isCompleted) {
                        val weight = set.actualWeightKg.toDoubleOrNull() ?: set.targetWeightKg
                        val reps = set.actualReps.toIntOrNull() ?: set.targetReps
                        totalVolume += weight * reps
                        completedCount++

                        // Save set to log list in Room
                        repository.insertSetLog(
                            WorkoutSetLog(
                                exerciseId = ex.exerciseId,
                                exerciseName = ex.name,
                                dateMillis = now,
                                weightKg = weight,
                                reps = reps,
                                setIndex = set.setIndex
                            )
                        )
                    }
                }
            }

            val elapsedTimeSeconds = ((System.currentTimeMillis() - current.startTimeMillis) / 1000).toInt()
            val elapsedMinutes = (elapsedTimeSeconds / 60).coerceAtLeast(1)
            val calories = (totalVolume * 0.03 + elapsedMinutes * 5).toInt()

            val sessionLog = WorkoutSession(
                routineName = current.routineName,
                dateMillis = now,
                activeTimeSeconds = elapsedTimeSeconds,
                totalVolumeLoadKg = totalVolume,
                completedSetsCount = completedCount,
                caloriesBurnt = calories
            )

            repository.logSession(sessionLog)
            _activeWorkout.value = null
            stopRestTimer()
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: String) {
        val current = _activeWorkout.value ?: return
        val set = current.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex) ?: return
        set.actualWeightKg = weight
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: String) {
        val current = _activeWorkout.value ?: return
        val set = current.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex) ?: return
        set.actualReps = reps
    }

    fun addCustomExercise(name: String, muscleGroup: String, equipment: String) {
        viewModelScope.launch {
            val id = name.uppercase().replace(" ", "_")
            val exercise = Exercise(
                id = id,
                name = name,
                muscleGroup = muscleGroup,
                equipment = equipment,
                isCustom = true
            )
            repository.insertExercise(exercise)
        }
    }

    fun cancelWorkout() {
        _activeWorkout.value = null
        stopRestTimer()
    }

    // Routines management
    fun createRoutine(name: String, description: String, exercises: List<RoutineExercise>) {
        viewModelScope.launch {
            val json = serializeRoutineExercises(exercises)
            val routine = Routine(
                name = name,
                description = description,
                exercisesJson = json
            )
            repository.insertRoutine(routine)
        }
    }

    fun updateRoutineExercises(routineId: Int, name: String, description: String, exercises: List<RoutineExercise>) {
        viewModelScope.launch {
            val db = GymBroDatabase.getDatabase(getApplication(), viewModelScope)
            val json = serializeRoutineExercises(exercises)
            val routine = Routine(
                id = routineId,
                name = name,
                description = description,
                exercisesJson = json
            )
            repository.updateRoutine(routine)
        }
    }

    fun deleteRoutine(id: Int) {
        viewModelScope.launch {
            repository.deleteRoutineById(id)
        }
    }

    // Helper functions to parse from/to json manually for rock-solid stability
    fun parseExercisesJson(json: String): List<RoutineExercise> {
        if (json.isEmpty() || json == "[]") return emptyList()
        return try {
            val list = mutableListOf<RoutineExercise>()
            val cleaned = json.trim().removePrefix("[").removeSuffix("]")
            if (cleaned.isEmpty()) return emptyList()
            val items = cleaned.split("},")
            for (item in items) {
                var doc = item.trim()
                if (!doc.endsWith("}")) {
                    doc = "$doc}"
                }
                val exerciseId = getJsonStringValue(doc, "exerciseId")
                val name = getJsonStringValue(doc, "name")
                val muscleGroup = getJsonStringValue(doc, "muscleGroup")
                val targetSets = getJsonIntValue(doc, "targetSets", 3)
                val targetReps = getJsonIntValue(doc, "targetReps", 10)
                val restSeconds = getJsonIntValue(doc, "restSeconds", 90)
                val targetWeight = getJsonDoubleValue(doc, "targetWeightKg", 0.0)

                list.add(
                    RoutineExercise(
                        exerciseId = exerciseId,
                        name = name,
                        muscleGroup = muscleGroup,
                        targetSets = targetSets,
                        targetReps = targetReps,
                        restSeconds = restSeconds,
                        targetWeightKg = targetWeight
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Steps Tracking
    fun logDailySteps(steps: Int, dateString: String = getCurrentDateString()) {
        viewModelScope.launch {
            val calories = (steps * 0.04).toInt()
            repository.insertStepsLog(
                StepsLog(
                    dateString = dateString,
                    stepsCount = steps,
                    caloriesBurnt = calories
                )
            )
        }
    }

    private fun serializeRoutineExercises(exercises: List<RoutineExercise>): String {
        val sb = StringBuilder()
        sb.append("[")
        exercises.forEachIndexed { i, re ->
            sb.append("{")
            sb.append("\"exerciseId\":\"${re.exerciseId}\",")
            sb.append("\"name\":\"${re.name}\",")
            sb.append("\"muscleGroup\":\"${re.muscleGroup}\",")
            sb.append("\"targetSets\":${re.targetSets},")
            sb.append("\"targetReps\":${re.targetReps},")
            sb.append("\"restSeconds\":${re.restSeconds},")
            sb.append("\"targetWeightKg\":${re.targetWeightKg}")
            sb.append("}")
            if (i < exercises.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun getJsonStringValue(json: String, key: String): String {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun getJsonIntValue(json: String, key: String, defaultValue: Int): Int {
        val pattern = "\"$key\"\\s*:\\s*([0-9.]+)".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.toInt() ?: defaultValue
    }

    private fun getJsonDoubleValue(json: String, key: String, defaultValue: Double): Double {
        val pattern = "\"$key\"\\s*:\\s*([0-9.]+)".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: defaultValue
    }

    // Rest Timer Core
    private fun startRestTimer(seconds: Int) {
        timerJob?.cancel()
        _restTimerTotalSeconds.value = seconds
        _restTimerSecondsLeft.value = seconds
        _restTimerActive.value = true

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_restTimerSecondsLeft.value > 0) {
                delay(1000)
                _restTimerSecondsLeft.value -= 1
            }
            _restTimerActive.value = false
        }
    }

    private fun stopRestTimer() {
        timerJob?.cancel()
        _restTimerActive.value = false
        _restTimerSecondsLeft.value = 0
    }

    // Nutrition Tracking
    fun changeSelectedDate(dateString: String) {
        _selectedDate.value = dateString
    }

    fun changeActiveDateOffset(days: Int) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val date = format.parse(_selectedDate.value) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = date
            calendar.add(Calendar.DAY_OF_YEAR, days)
            _selectedDate.value = format.format(calendar.time)
        } catch (e: Exception) {
            _selectedDate.value = getCurrentDateString()
        }
    }

    fun logFoodItem(name: String, brand: String, calories: Int, protein: Int, carbs: Int, fats: Int, portion: String, mealType: String) {
        viewModelScope.launch {
            val foodLog = FoodLog(
                name = name,
                brand = brand,
                calories = calories,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatsGrams = fats,
                portionSize = portion,
                mealType = mealType,
                dateString = _selectedDate.value
            )
            repository.insertFoodLog(foodLog)
        }
    }

    fun deleteLoggedFood(id: Int) {
        viewModelScope.launch {
            repository.deleteFoodLog(id)
        }
    }

    fun addCustomFoodToDatabase(name: String, brand: String, calories: Int, protein: Int, carbs: Int, fats: Int, portion: String) {
        viewModelScope.launch {
            val customFood = CustomFood(
                name = name,
                brand = brand,
                calories = calories,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatsGrams = fats,
                portionSize = portion
            )
            repository.insertCustomFood(customFood)
        }
    }

    fun deleteCustomFoodFromDatabase(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomFood(id)
        }
    }

    fun logWaterIntake(amountMl: Int) {
        viewModelScope.launch {
            val log = HydrationLog(
                dateString = _selectedDate.value,
                waterMl = amountMl
            )
            repository.logWaterIntake(log)
        }
    }



    // Weight/Measurements History Tracking
    fun logMorningWeightAndMetrics(weight: Double, waist: Double?, arms: Double?, chest: Double?, note: String) {
        viewModelScope.launch {
            val log = WeightLog(
                dateMillis = System.currentTimeMillis(),
                weightKg = weight,
                waistCm = waist,
                armsCm = arms,
                chestCm = chest,
                note = note
            )
            repository.logWeight(log)

            // Auto update user's profile weight too!
            val profile = repository.getUserProfileOneShot()
            if (profile != null) {
                updateWeight(weight)
            }
        }
    }

    fun deleteWeightLogEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteWeightLog(id)
        }
    }

    // Chatbot AI Assistant Gain Partner
    fun sendMessageToGymBro(text: String) {
        val userMsg = ChatMessage(sender = "User", text = text)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val profile = repository.getUserProfileOneShot()
            val profileContext = if (profile != null) {
                """
                    User Info:
                    - Age: ${profile.age} years old
                    - Gender: ${profile.gender}
                    - Weight: ${profile.weightKg}kg
                    - Height: ${profile.heightCm}cm
                    - Activity Level: ${profile.activityLevel}
                    - Goal: ${profile.primaryGoal} (Target weight: ${profile.targetWeightKg}kg in ${profile.timeframeWeeks} weeks)
                    - Calculated TDEE: ${profile.tdee.toInt()} kcal
                    - Caloric Target: ${profile.targetCalories} kcal (Ratio: Carbs ${profile.targetCarbsGrams}g, Protein ${profile.targetProteinGrams}g, Fats ${profile.targetFatsGrams}g)
                """.trimIndent()
            } else {
                "User has not set up their profile or completed onboarding yet."
            }

            val reply = GeminiClient.generateGymBroResponse(text, profileContext)
            _chatMessages.value = _chatMessages.value + ChatMessage(sender = "GymBro", text = reply)
            _isChatLoading.value = false
        }
    }

    // Helper functions
    private fun calculateStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        val sorted = sessions.map { it.dateMillis }.sortedDescending()
        val format = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        var streak = 0
        val todayStr = format.format(Date())
        val calendar = Calendar.getInstance()

        // Get unique days completed list
        val uniqueDays = sorted.map { format.format(Date(it)) }.distinct()
        if (uniqueDays.isEmpty()) return 0

        // Check if there is a session today or yesterday to continue streak
        var checkDate = Date()
        var checkStr = format.format(checkDate)

        if (uniqueDays[0] != todayStr) {
            // Check yesterday
            calendar.time = checkDate
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = format.format(calendar.time)
            if (uniqueDays[0] == yesterdayStr) {
                checkDate = calendar.time
                checkStr = yesterdayStr
            } else {
                return 0 // Streak broken
            }
        }

        for (day in uniqueDays) {
            if (day == checkStr) {
                streak++
                // Move backwards one day to check
                calendar.time = checkDate
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                checkDate = calendar.time
                checkStr = format.format(checkDate)
            } else {
                break
            }
        }

        return streak
    }

    private fun getCurrentDateString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(Date())
    }
}

class GymBroViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GymBroViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GymBroViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
