package com.gymbro.fitness.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymbro.fitness.data.*
import com.gymbro.fitness.domain.CommonFoods
import com.gymbro.fitness.domain.FitnessCalculator
import com.gymbro.fitness.domain.LoggedSet
import com.gymbro.fitness.domain.ProgramTemplate
import com.gymbro.fitness.domain.WeightUnit
import com.gymbro.fitness.notifications.RestTimerNotifier
import com.gymbro.fitness.widget.GymBroWidget
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val sender: String, // "User" or "GymBro"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false // error notices are shown but never sent back to the model
)

class ActiveWorkoutSet(
    val setIndex: Int,
    val targetWeightKg: Double,
    val targetReps: Int,
    initialActualWeightKg: String = "",
    initialActualReps: String = "",
    initialIsCompleted: Boolean = false,
    // What was lifted in this set last session (null when there is no history).
    val previousWeightKg: Double? = null,
    val previousReps: Int? = null
) {
    var actualWeightKg by mutableStateOf(initialActualWeightKg)
    var actualReps by mutableStateOf(initialActualReps)
    var isCompleted by mutableStateOf(initialIsCompleted)

    /** What gets logged: the typed value (in the user's unit), or the target when left blank. */
    fun loggedWeightKg(unit: WeightUnit): Double = actualWeightKg.toDoubleOrNull()?.let(unit::toKg) ?: targetWeightKg
    fun loggedReps(): Int = actualReps.toIntOrNull() ?: targetReps
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

data class ExerciseBest(
    val exerciseName: String,
    val estimatedOneRepMaxKg: Double,
    val bestWeightKg: Double,
    val bestReps: Int,
    val achievedAtMillis: Long
)

data class WorkoutSummary(
    val routineName: String,
    val durationSeconds: Int,
    val completedSets: Int,
    val totalVolumeKg: Double,
    val caloriesBurnt: Int,
    val prExerciseNames: List<String>
)

data class ActiveWorkoutSession(
    val routineId: Int,
    val routineName: String,
    val exercises: List<ActiveWorkoutExercise>,
    val startTimeMillis: Long = System.currentTimeMillis()
)

class GymBroViewModel(application: Application) : AndroidViewModel(application) {

    private val moshi = Moshi.Builder().build()
    private val routineExercisesAdapter = moshi.adapter<List<RoutineExercise>>(
        Types.newParameterizedType(List::class.java, RoutineExercise::class.java)
    )

    // Declared before init so they exist when init restores an in-progress workout.
    private val activeWorkoutFile = File(application.filesDir, "active_workout.json")
    private val activeWorkoutAdapter = moshi.adapter(ActiveWorkoutSnapshot::class.java)
    private var saveActiveWorkoutJob: Job? = null

    private val settingsPrefs = application.getSharedPreferences("gymbro_settings", android.content.Context.MODE_PRIVATE)
    private val _weightUnit = MutableStateFlow(WeightUnit.fromStorage(settingsPrefs.getString("weight_unit", null)))
    val weightUnit: StateFlow<WeightUnit> = _weightUnit.asStateFlow()

    fun setWeightUnit(unit: WeightUnit) {
        _weightUnit.value = unit
        settingsPrefs.edit().putString("weight_unit", unit.name).apply()
    }

    private val database: GymBroDatabase
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

    /** Best set per exercise, ranked by estimated 1RM. */
    val exerciseBests: StateFlow<List<ExerciseBest>>

    /** Most recently logged distinct foods, newest first, for one-tap re-logging. */
    val recentFoods: StateFlow<List<FoodLog>>

    /** Completed sets per muscle group over the last 7 days. */
    val weeklySetsByMuscle: StateFlow<Map<String, Int>>

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

    private val _workoutSummary = MutableStateFlow<WorkoutSummary?>(null)
    val workoutSummary: StateFlow<WorkoutSummary?> = _workoutSummary.asStateFlow()

    fun dismissWorkoutSummary() {
        _workoutSummary.value = null
    }

    private val _isPrCelebration = MutableStateFlow<String?>(null) // Name of exercise for PR celebration banner
    val isPrCelebration: StateFlow<String?> = _isPrCelebration.asStateFlow()

    init {
        database = GymBroDatabase.getDatabase(application, viewModelScope)
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

        exerciseBests = repository.allSetLogs
            .map { logs ->
                logs.groupBy { it.exerciseId }.values.mapNotNull { sets ->
                    val best = sets.maxByOrNull { FitnessCalculator.estimatedOneRepMax(it.weightKg, it.reps) }
                        ?: return@mapNotNull null
                    val oneRepMax = FitnessCalculator.estimatedOneRepMax(best.weightKg, best.reps)
                    if (oneRepMax <= 0.0) return@mapNotNull null
                    ExerciseBest(
                        exerciseName = best.exerciseName,
                        estimatedOneRepMaxKg = oneRepMax,
                        bestWeightKg = best.weightKg,
                        bestReps = best.reps,
                        achievedAtMillis = best.dateMillis
                    )
                }.sortedByDescending { it.estimatedOneRepMaxKg }
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        recentFoods = repository.allFoodLogs
            .map { logs -> logs.distinctBy { it.name.trim().lowercase() }.take(12) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        weeklySetsByMuscle = combine(repository.allSetLogs, repository.allExercises) { logs, exercises ->
            val muscleById = exercises.associate { it.id to it.muscleGroup }
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            logs.filter { it.dateMillis >= weekAgo }
                .groupingBy { muscleById[it.exerciseId] ?: "Other" }
                .eachCount()
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        allExercises = repository.allExercises.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        restoreActiveWorkout()
        restoreChatHistory(database.chatMessageDao())
        // New built-in exercises from app updates reach existing installs too.
        viewModelScope.launch(Dispatchers.IO) { ensureBuiltInExercises(database) }

        // Calculate current workout streak
        viewModelScope.launch {
            allSessions.collect { sessions ->
                _workoutStreak.value = FitnessCalculator.workoutStreak(sessions.map { it.dateMillis })
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
            val bmr = FitnessCalculator.bmr(weightKg, heightCm, age, gender)
            val tdee = FitnessCalculator.tdee(bmr, activityLevel)
            val macros = FitnessCalculator.macroTargets(tdee, weightKg, primaryGoal)

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
                targetCalories = macros.calories,
                targetProteinGrams = macros.proteinGrams,
                targetCarbsGrams = macros.carbsGrams,
                targetFatsGrams = macros.fatsGrams,
                onboardingCompleted = true
            )
            repository.saveUserProfile(profile)
        }
    }

    fun updateWeight(newWeightKg: Double) {
        viewModelScope.launch {
            repository.logWeight(WeightLog(dateMillis = System.currentTimeMillis(), weightKg = newWeightKg))
            updateProfileWeight(newWeightKg)
        }
    }

    /** Recalculates targets for a new body weight without adding a weight log entry. */
    private suspend fun updateProfileWeight(newWeightKg: Double) {
        val profile = repository.getUserProfileOneShot() ?: return
        val bmr = FitnessCalculator.bmr(newWeightKg, profile.heightCm, profile.age, profile.gender)
        val tdee = FitnessCalculator.tdee(bmr, profile.activityLevel)
        val macros = FitnessCalculator.macroTargets(tdee, newWeightKg, profile.primaryGoal)
        repository.saveUserProfile(
            profile.copy(
                weightKg = newWeightKg,
                bmr = bmr,
                tdee = tdee,
                targetCalories = macros.calories,
                targetProteinGrams = macros.proteinGrams,
                targetCarbsGrams = macros.carbsGrams,
                targetFatsGrams = macros.fatsGrams
            )
        )
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

    // Workout Tracker
    fun startWorkout(routine: Routine) {
        viewModelScope.launch {
            val parsedExercises = parseExercisesJson(routine.exercisesJson)
            val activeExercises = parsedExercises.map { re ->
                // Last session's sets drive the "previous" column and the overload suggestion.
                val pastLogs = repository.getSetLogsForExercise(re.exerciseId).firstOrNull().orEmpty()
                val lastSessionSets = pastLogs
                    .filter { it.dateMillis == pastLogs.firstOrNull()?.dateMillis }
                    .sortedBy { it.setIndex }
                val unit = _weightUnit.value
                val suggestedWeight = FitnessCalculator.suggestNextWeight(
                    lastSessionSets = lastSessionSets.map { LoggedSet(it.weightKg, it.reps) },
                    targetReps = re.targetReps,
                    routineWeightKg = re.targetWeightKg,
                    largeIncrementKg = unit.largeIncrementKg,
                    smallIncrementKg = unit.smallIncrementKg
                )
                val lastWorkingWeight = lastSessionSets.maxOfOrNull { it.weightKg } ?: 0.0
                // Best estimated 1RM per past session, newest first, to spot a stalled lift.
                val sessionBests = pastLogs.groupBy { it.dateMillis }
                    .toSortedMap(compareByDescending { it })
                    .values
                    .map { sets -> sets.maxOf { FitnessCalculator.estimatedOneRepMax(it.weightKg, it.reps) } }
                val stalled = FitnessCalculator.isPlateau(sessionBests)
                val plannedWeight = if (stalled) {
                    FitnessCalculator.deloadWeight(lastWorkingWeight, unit.largeIncrementKg)
                } else {
                    suggestedWeight
                }
                val coachMessage = when {
                    lastSessionSets.isEmpty() -> "First session for this exercise! Let's go!"
                    stalled -> "Stalled 3 sessions → deload to ${unit.formatWithUnit(plannedWeight)} and build back up"
                    suggestedWeight > lastWorkingWeight ->
                        "Hit every rep last time → go up to ${unit.formatWithUnit(suggestedWeight)} today"
                    else -> "Repeat ${unit.formatWithUnit(suggestedWeight)} and beat last time's reps"
                }

                ActiveWorkoutExercise(
                    exerciseId = re.exerciseId,
                    name = re.name,
                    muscleGroup = re.muscleGroup,
                    targetSets = re.targetSets,
                    targetReps = re.targetReps,
                    restSeconds = re.restSeconds,
                    targetWeightKg = plannedWeight,
                    lastSessionMessage = coachMessage,
                    sets = List(re.targetSets) { i ->
                        val previous = lastSessionSets.getOrNull(i)
                        ActiveWorkoutSet(
                            setIndex = i,
                            targetWeightKg = plannedWeight,
                            targetReps = re.targetReps,
                            previousWeightKg = previous?.weightKg,
                            previousReps = previous?.reps
                        )
                    }
                )
            }

            _activeWorkout.value = ActiveWorkoutSession(
                routineId = routine.id,
                routineName = routine.name,
                exercises = activeExercises
            )
            saveActiveWorkout()
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

            val unit = _weightUnit.value
            val setOneRepMax = FitnessCalculator.estimatedOneRepMax(set.loggedWeightKg(unit), set.loggedReps())
            // Earlier sets in this workout count too, so only a set that beats them is a new PR.
            val bestThisSession = exercise.sets
                .filter { it !== set && it.isCompleted }
                .maxOfOrNull { FitnessCalculator.estimatedOneRepMax(it.loggedWeightKg(unit), it.loggedReps()) } ?: 0.0
            viewModelScope.launch {
                val bestHistoric = repository.getBestEstimatedOneRepMax(exercise.exerciseId) ?: 0.0
                // With no history the first lift is a baseline, not a record.
                if (bestHistoric > 0.0 && setOneRepMax > maxOf(bestHistoric, bestThisSession)) {
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
        saveActiveWorkout()
    }

    fun finishWorkout() {
        val current = _activeWorkout.value ?: return
        viewModelScope.launch {
            var totalVolume = 0.0
            var completedCount = 0
            val prExercises = linkedSetOf<String>()

            val now = System.currentTimeMillis()

            current.exercises.forEach { ex ->
                var bestSoFar = repository.getBestEstimatedOneRepMax(ex.exerciseId) ?: 0.0
                val hasHistory = bestSoFar > 0.0
                ex.sets.filter { it.isCompleted }.forEach { set ->
                    val weight = set.loggedWeightKg(_weightUnit.value)
                    val reps = set.loggedReps()
                    totalVolume += weight * reps
                    completedCount++

                    val oneRepMax = FitnessCalculator.estimatedOneRepMax(weight, reps)
                    val isPr = hasHistory && oneRepMax > bestSoFar
                    bestSoFar = maxOf(bestSoFar, oneRepMax)
                    if (isPr) prExercises += ex.name

                    repository.insertSetLog(
                        WorkoutSetLog(
                            exerciseId = ex.exerciseId,
                            exerciseName = ex.name,
                            dateMillis = now,
                            weightKg = weight,
                            reps = reps,
                            setIndex = set.setIndex,
                            isPr = isPr
                        )
                    )
                }
            }

            val elapsedTimeSeconds = ((now - current.startTimeMillis) / 1000).toInt()
            val bodyWeightKg = repository.getUserProfileOneShot()?.weightKg ?: DEFAULT_BODY_WEIGHT_KG

            val sessionLog = WorkoutSession(
                routineName = current.routineName,
                dateMillis = now,
                activeTimeSeconds = elapsedTimeSeconds,
                totalVolumeLoadKg = totalVolume,
                completedSetsCount = completedCount,
                caloriesBurnt = FitnessCalculator.workoutCalories(elapsedTimeSeconds, bodyWeightKg)
            )

            repository.logSession(sessionLog)
            GymBroWidget.refresh(getApplication())
            _workoutSummary.value = WorkoutSummary(
                routineName = current.routineName,
                durationSeconds = elapsedTimeSeconds,
                completedSets = completedCount,
                totalVolumeKg = totalVolume,
                caloriesBurnt = sessionLog.caloriesBurnt,
                prExerciseNames = prExercises.toList()
            )
            _activeWorkout.value = null
            clearSavedActiveWorkout()
            stopRestTimer()
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: String) {
        val current = _activeWorkout.value ?: return
        val set = current.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex) ?: return
        set.actualWeightKg = weight
        saveActiveWorkout()
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: String) {
        val current = _activeWorkout.value ?: return
        val set = current.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex) ?: return
        set.actualReps = reps
        saveActiveWorkout()
    }

    fun addCustomExercise(name: String, muscleGroup: String, equipment: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        viewModelScope.launch {
            val existing = repository.searchExercises(trimmedName)
            if (existing.any { it.name.equals(trimmedName, ignoreCase = true) }) return@launch
            val exercise = Exercise(
                // Random id so a custom exercise can never replace a built-in one.
                id = "CUSTOM_${UUID.randomUUID()}",
                name = trimmedName,
                muscleGroup = muscleGroup,
                equipment = equipment,
                isCustom = true
            )
            repository.insertExercise(exercise)
        }
    }

    fun cancelWorkout() {
        _activeWorkout.value = null
        clearSavedActiveWorkout()
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

    /** Adds every day of [template] as a routine, resolving names from the exercise library. */
    fun addProgramTemplate(template: ProgramTemplate) {
        viewModelScope.launch {
            val library = repository.allExercises.first().associateBy { it.id }
            template.days.forEach { day ->
                val exercises = day.exercises.mapNotNull { slot ->
                    val exercise = library[slot.exerciseId] ?: return@mapNotNull null
                    RoutineExercise(
                        exerciseId = exercise.id,
                        name = exercise.name,
                        muscleGroup = exercise.muscleGroup,
                        targetSets = slot.sets,
                        targetReps = slot.reps,
                        restSeconds = slot.restSeconds,
                        targetWeightKg = 0.0
                    )
                }
                repository.insertRoutine(
                    Routine(
                        name = day.name,
                        description = "${template.name} · ${day.description}",
                        exercisesJson = serializeRoutineExercises(exercises)
                    )
                )
            }
        }
    }

    fun deleteWorkoutSession(session: WorkoutSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun deleteRoutine(id: Int) {
        viewModelScope.launch {
            repository.deleteRoutineById(id)
        }
    }

    fun parseExercisesJson(json: String): List<RoutineExercise> {
        if (json.isBlank()) return emptyList()
        return try {
            routineExercisesAdapter.fromJson(json).orEmpty()
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

    private fun serializeRoutineExercises(exercises: List<RoutineExercise>): String =
        routineExercisesAdapter.toJson(exercises)

    // Active workout persistence: survives the process being killed mid-workout.
    private fun saveActiveWorkout() {
        val snapshot = _activeWorkout.value?.toSnapshot() ?: return
        saveActiveWorkoutJob?.cancel()
        saveActiveWorkoutJob = viewModelScope.launch(Dispatchers.IO) {
            delay(300) // debounce keystrokes in the weight/reps fields
            runCatching { activeWorkoutFile.writeText(activeWorkoutAdapter.toJson(snapshot)) }
        }
    }

    private fun clearSavedActiveWorkout() {
        saveActiveWorkoutJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) { activeWorkoutFile.delete() }
    }

    private fun restoreActiveWorkout() {
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                runCatching {
                    if (activeWorkoutFile.exists()) activeWorkoutAdapter.fromJson(activeWorkoutFile.readText()) else null
                }.getOrNull()
            }
            if (snapshot != null && _activeWorkout.value == null) {
                _activeWorkout.value = snapshot.toActiveWorkout()
            }
        }
    }

    // Rest Timer Core
    private fun startRestTimer(seconds: Int) {
        timerJob?.cancel()
        val endAtMillis = System.currentTimeMillis() + seconds * 1000L
        _restTimerTotalSeconds.value = seconds
        _restTimerSecondsLeft.value = seconds
        _restTimerActive.value = true
        RestTimerNotifier.start(getApplication(), seconds)

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            // Derive from the end time so the countdown doesn't drift while the app is in the background.
            while (true) {
                val millisLeft = endAtMillis - System.currentTimeMillis()
                if (millisLeft <= 0) break
                _restTimerSecondsLeft.value = ((millisLeft + 999) / 1000).toInt()
                delay(minOf(millisLeft, 250L))
            }
            _restTimerSecondsLeft.value = 0
            _restTimerActive.value = false
        }
    }

    private fun stopRestTimer() {
        timerJob?.cancel()
        _restTimerActive.value = false
        _restTimerSecondsLeft.value = 0
        RestTimerNotifier.cancel(getApplication())
    }

    // Nutrition Tracking
    fun changeSelectedDate(dateString: String) {
        _selectedDate.value = dateString
    }

    fun changeActiveDateOffset(days: Int) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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
            GymBroWidget.refresh(getApplication())
        }
    }

    // Food database search (Open Food Facts)
    private val _foodSearchResults = MutableStateFlow<List<FoodSearchResult>>(emptyList())
    val foodSearchResults: StateFlow<List<FoodSearchResult>> = _foodSearchResults.asStateFlow()
    private val _foodSearchState = MutableStateFlow<String?>(null) // null = idle, else a status message
    val foodSearchState: StateFlow<String?> = _foodSearchState.asStateFlow()
    private var foodSearchJob: Job? = null

    fun searchFoods(query: String) {
        if (query.isBlank()) return
        foodSearchJob?.cancel()
        _foodSearchState.value = "Searching…"
        // Built-in common foods show instantly (and offline); database results follow.
        val local = CommonFoods.search(query)
        _foodSearchResults.value = local
        foodSearchJob = viewModelScope.launch {
            try {
                val remote = withContext(Dispatchers.IO) { FoodSearchClient.search(query) }
                val results = local + remote
                _foodSearchResults.value = results
                _foodSearchState.value = if (results.isEmpty()) "No foods found. Try another name." else null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _foodSearchState.value = if (local.isEmpty()) {
                    "Couldn't reach the food database. Check your connection."
                } else {
                    "Offline: showing common foods only."
                }
            }
        }
    }

    fun clearFoodSearch() {
        foodSearchJob?.cancel()
        _foodSearchResults.value = emptyList()
        _foodSearchState.value = null
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
            updateProfileWeight(weight)
        }
    }

    fun deleteWeightLogEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteWeightLog(id)
        }
    }

    // Chatbot AI Assistant Gain Partner
    private lateinit var chatDao: ChatMessageDao

    private fun restoreChatHistory(dao: ChatMessageDao) {
        chatDao = dao
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { dao.getAll() }
            if (saved.isNotEmpty()) {
                val greeting = _chatMessages.value.take(1)
                _chatMessages.value = greeting + saved.map {
                    ChatMessage(sender = it.sender, text = it.text, timestamp = it.timestamp, isError = it.isError)
                }
            }
        }
    }

    /** Shows a message and saves it so the conversation survives app restarts. */
    private fun appendChatMessage(message: ChatMessage) {
        _chatMessages.value = _chatMessages.value + message
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.insert(
                ChatMessageEntity(
                    sender = message.sender,
                    text = message.text,
                    timestamp = message.timestamp,
                    isError = message.isError
                )
            )
        }
    }

    fun clearChat() {
        _chatMessages.value = _chatMessages.value.take(1)
        viewModelScope.launch(Dispatchers.IO) { chatDao.clear() }
    }

    fun sendMessageToGymBro(text: String) {
        val userMsg = ChatMessage(sender = "User", text = text)
        appendChatMessage(userMsg)
        _isChatLoading.value = true

        viewModelScope.launch {
            val context = coachContext()

            val conversation = _chatMessages.value
                .filter { !it.isError }
                .map { CoachTurn(fromUser = it.sender == "User", text = it.text) }
            val reply = GeminiClient.generateGymBroResponse(conversation, context)
            appendChatMessage(ChatMessage(sender = "GymBro", text = reply.text, isError = reply.isError))
            _isChatLoading.value = false
        }
    }

    // Settings: data export and deletion

    /** Writes everything the user entered as JSON to [uri]; reports success on the main thread. */
    fun exportData(uri: android.net.Uri, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val export = repository.buildExport(System.currentTimeMillis())
                    val json = moshi.adapter(GymBroExport::class.java).indent("  ").toJson(export)
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open $uri")
                }.isSuccess
            }
            onDone(ok)
        }
    }

    /** Wipes every table and restores the built-in exercises and starter routines. */
    fun deleteAllData() {
        cancelWorkout()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.clearAllTables()
                seedDefaults(database)
            }
            _chatMessages.value = _chatMessages.value.take(1) // keep only the greeting
        }
    }

    private suspend fun coachContext(): String {
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
        return profileContext + "\n" + recentTrainingContext()
    }

    // AI routine builder: the coach drafts a routine, the user reviews and saves it.
    data class AiRoutineDraft(val name: String, val description: String, val exercises: List<RoutineExercise>)

    private val _aiRoutineDraft = MutableStateFlow<AiRoutineDraft?>(null)
    val aiRoutineDraft: StateFlow<AiRoutineDraft?> = _aiRoutineDraft.asStateFlow()
    private val _aiRoutineStatus = MutableStateFlow<String?>(null)
    val aiRoutineStatus: StateFlow<String?> = _aiRoutineStatus.asStateFlow()

    fun requestAiRoutine(request: String) {
        if (request.isBlank()) return
        _aiRoutineStatus.value = "Building your routine…"
        _aiRoutineDraft.value = null
        viewModelScope.launch {
            val library = repository.allExercises.first().associateBy { it.id }
            val catalog = library.values.joinToString("\n") { "${it.id}: ${it.name} (${it.muscleGroup}, ${it.equipment})" }
            val reply = GeminiClient.generateRoutineJson(request, coachContext(), catalog)
            if (reply.isError) {
                _aiRoutineStatus.value = reply.text
                return@launch
            }
            val plan = runCatching { moshi.adapter(AiRoutinePlan::class.java).fromJson(reply.text) }.getOrNull()
            // Never trust ids or numbers from the model: keep known exercises and clamp to sane ranges.
            val exercises = plan?.exercises.orEmpty().mapNotNull { slot ->
                val exercise = library[slot.exerciseId] ?: return@mapNotNull null
                RoutineExercise(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    muscleGroup = exercise.muscleGroup,
                    targetSets = slot.sets.coerceIn(1, 6),
                    targetReps = slot.reps.coerceIn(1, 30),
                    restSeconds = slot.restSeconds.coerceIn(30, 300),
                    targetWeightKg = 0.0
                )
            }.distinctBy { it.exerciseId }
            if (plan == null || exercises.isEmpty()) {
                _aiRoutineStatus.value = "I couldn't build a usable routine from that. Try describing it differently."
                return@launch
            }
            _aiRoutineStatus.value = null
            _aiRoutineDraft.value = AiRoutineDraft(
                name = plan.name.ifBlank { "Coach Routine" }.take(40),
                description = plan.description.take(120),
                exercises = exercises
            )
        }
    }

    fun saveAiRoutineDraft() {
        val draft = _aiRoutineDraft.value ?: return
        _aiRoutineDraft.value = null
        createRoutine(draft.name, draft.description, draft.exercises)
        appendChatMessage(
            ChatMessage(
                sender = "GymBro",
                text = "Saved \"${draft.name}\" to your routines. Find it in the Workout tab and let's get after it! 💪"
            )
        )
    }

    fun dismissAiRoutineDraft() {
        _aiRoutineDraft.value = null
        _aiRoutineStatus.value = null
    }

    /** Recent sessions and weight trend so the coach can answer from the user's real data. */
    private suspend fun recentTrainingContext(): String {
        val dateFormat = SimpleDateFormat("EEE d MMM", Locale.US)
        // Read from the database directly: the UI-facing state flows are empty until a screen subscribes.
        val sessions = repository.allSessions.first().take(5)
        val weights = repository.allWeightLogs.first()
        return buildString {
            appendLine("Recent workouts (newest first):")
            if (sessions.isEmpty()) {
                appendLine("- none logged yet")
            } else {
                sessions.forEach { session ->
                    appendLine(
                        "- ${dateFormat.format(Date(session.dateMillis))}: ${session.routineName}, " +
                            "${session.completedSetsCount} sets, ${session.totalVolumeLoadKg.toInt()} kg volume, " +
                            "${session.activeTimeSeconds / 60} min"
                    )
                }
            }
            appendLine("Current workout streak: ${_workoutStreak.value} days")
            if (weights.size >= 2) {
                val change = weights.first().weightKg - weights.last().weightKg
                append(
                    String.format(
                        Locale.US,
                        "Weight trend: %.1f kg -> %.1f kg (%+.1f kg) over %d weigh-ins",
                        weights.last().weightKg, weights.first().weightKg, change, weights.size
                    )
                )
            }
        }
    }

    private companion object {
        const val DEFAULT_BODY_WEIGHT_KG = 75.0
    }

    private fun getCurrentDateString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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

@JsonClass(generateAdapter = true)
data class ActiveWorkoutSnapshot(
    val routineId: Int,
    val routineName: String,
    val startTimeMillis: Long,
    val exercises: List<ActiveExerciseSnapshot>
)

@JsonClass(generateAdapter = true)
data class ActiveExerciseSnapshot(
    val exerciseId: String,
    val name: String,
    val muscleGroup: String,
    val targetSets: Int,
    val targetReps: Int,
    val restSeconds: Int,
    val targetWeightKg: Double,
    val lastSessionMessage: String,
    val sets: List<ActiveSetSnapshot>
)

@JsonClass(generateAdapter = true)
data class ActiveSetSnapshot(
    val setIndex: Int,
    val targetWeightKg: Double,
    val targetReps: Int,
    val actualWeightKg: String,
    val actualReps: String,
    val isCompleted: Boolean,
    val previousWeightKg: Double? = null,
    val previousReps: Int? = null
)

private fun ActiveWorkoutSession.toSnapshot() = ActiveWorkoutSnapshot(
    routineId = routineId,
    routineName = routineName,
    startTimeMillis = startTimeMillis,
    exercises = exercises.map { ex ->
        ActiveExerciseSnapshot(
            exerciseId = ex.exerciseId,
            name = ex.name,
            muscleGroup = ex.muscleGroup,
            targetSets = ex.targetSets,
            targetReps = ex.targetReps,
            restSeconds = ex.restSeconds,
            targetWeightKg = ex.targetWeightKg,
            lastSessionMessage = ex.lastSessionMessage,
            sets = ex.sets.map { set ->
                ActiveSetSnapshot(
                    setIndex = set.setIndex,
                    targetWeightKg = set.targetWeightKg,
                    targetReps = set.targetReps,
                    actualWeightKg = set.actualWeightKg,
                    actualReps = set.actualReps,
                    isCompleted = set.isCompleted,
                    previousWeightKg = set.previousWeightKg,
                    previousReps = set.previousReps
                )
            }
        )
    }
)

private fun ActiveWorkoutSnapshot.toActiveWorkout() = ActiveWorkoutSession(
    routineId = routineId,
    routineName = routineName,
    startTimeMillis = startTimeMillis,
    exercises = exercises.map { ex ->
        ActiveWorkoutExercise(
            exerciseId = ex.exerciseId,
            name = ex.name,
            muscleGroup = ex.muscleGroup,
            targetSets = ex.targetSets,
            targetReps = ex.targetReps,
            restSeconds = ex.restSeconds,
            targetWeightKg = ex.targetWeightKg,
            lastSessionMessage = ex.lastSessionMessage,
            sets = ex.sets.map { set ->
                ActiveWorkoutSet(
                    setIndex = set.setIndex,
                    targetWeightKg = set.targetWeightKg,
                    targetReps = set.targetReps,
                    initialActualWeightKg = set.actualWeightKg,
                    initialActualReps = set.actualReps,
                    initialIsCompleted = set.isCompleted,
                    previousWeightKg = set.previousWeightKg,
                    previousReps = set.previousReps
                )
            }
        )
    }
)
