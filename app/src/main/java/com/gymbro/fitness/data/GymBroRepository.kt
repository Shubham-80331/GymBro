package com.gymbro.fitness.data

import com.squareup.moshi.JsonClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GymBroRepository(
    private val userProfileDao: UserProfileDao,
    private val exerciseDao: ExerciseDao,
    private val routineDao: RoutineDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutSetLogDao: WorkoutSetLogDao,
    private val weightLogDao: WeightLogDao,
    private val foodLogDao: FoodLogDao,
    private val customFoodDao: CustomFoodDao,
    private val hydrationLogDao: HydrationLogDao,
    private val stepsLogDao: StepsLogDao
) {
    // User Profile
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    suspend fun getUserProfileOneShot(): UserProfile? = userProfileDao.getUserProfileOneShot()
    suspend fun saveUserProfile(profile: UserProfile) = userProfileDao.insertOrUpdateProfile(profile)

    // Exercises
    val allExercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()
    fun getExercisesByMuscle(muscle: String): Flow<List<Exercise>> = exerciseDao.getExercisesByMuscle(muscle)
    suspend fun searchExercises(query: String): List<Exercise> = exerciseDao.searchExercises(query)
    suspend fun insertExercise(exercise: Exercise) = exerciseDao.insertExercise(exercise)

    // Routines
    val allRoutines: Flow<List<Routine>> = routineDao.getAllRoutines()
    suspend fun getRoutineById(id: Int): Routine? = routineDao.getRoutineById(id)
    suspend fun insertRoutine(routine: Routine) = routineDao.insertRoutine(routine)
    suspend fun updateRoutine(routine: Routine) = routineDao.updateRoutine(routine)
    suspend fun deleteRoutineById(id: Int) = routineDao.deleteRoutineById(id)

    // Workout Sessions
    val allSessions: Flow<List<WorkoutSession>> = workoutSessionDao.getAllSessions()
    suspend fun logSession(session: WorkoutSession) = workoutSessionDao.insertSession(session)

    // Set Logs
    val allSetLogs: Flow<List<WorkoutSetLog>> = workoutSetLogDao.getAllSetLogs()
    fun getSetLogsForExercise(exerciseId: String): Flow<List<WorkoutSetLog>> = workoutSetLogDao.getSetLogsForExercise(exerciseId)
    suspend fun getOneRepMaxForExercise(exerciseId: String): Double? = workoutSetLogDao.getOneRepMaxForExercise(exerciseId)
    suspend fun getBestEstimatedOneRepMax(exerciseId: String): Double? = workoutSetLogDao.getBestEstimatedOneRepMax(exerciseId)
    suspend fun insertSetLog(log: WorkoutSetLog) = workoutSetLogDao.insertSetLog(log)
    suspend fun deleteSetLog(id: Int) = workoutSetLogDao.deleteSetLog(id)

    // Weight Logs
    val allWeightLogs: Flow<List<WeightLog>> = weightLogDao.getAllWeightLogs()
    suspend fun logWeight(log: WeightLog) = weightLogDao.insertWeightLog(log)
    suspend fun deleteWeightLog(id: Int) = weightLogDao.deleteWeightLog(id)

    // Food Logs
    fun getFoodLogsForDay(dateString: String): Flow<List<FoodLog>> = foodLogDao.getFoodLogsForDay(dateString)
    val allFoodLogs: Flow<List<FoodLog>> = foodLogDao.getAllFoodLogs()
    suspend fun insertFoodLog(log: FoodLog) = foodLogDao.insertFoodLog(log)
    suspend fun deleteFoodLog(id: Int) = foodLogDao.deleteFoodLog(id)

    // Custom Foods
    val allCustomFoods: Flow<List<CustomFood>> = customFoodDao.getAllCustomFoods()
    suspend fun insertCustomFood(food: CustomFood) = customFoodDao.insertCustomFood(food)
    suspend fun deleteCustomFood(id: Int) = customFoodDao.deleteCustomFood(id)

    // Hydration Logs
    fun getWaterIntakeForDay(dateString: String): Flow<Int> = hydrationLogDao.getWaterIntakeForDay(dateString)
    suspend fun logWaterIntake(log: HydrationLog) = hydrationLogDao.insertHydrationLog(log)
    suspend fun getAllHydrationLogs(): List<HydrationLog> = hydrationLogDao.getAllHydrationLogs()

    // Steps Logs
    val allStepsLogs: Flow<List<StepsLog>> = stepsLogDao.getAllStepsLogs()
    suspend fun insertStepsLog(log: StepsLog) = stepsLogDao.insertStepsLog(log)

    /** Everything the user has entered, for "Export my data". Built-in exercises are left out. */
    suspend fun buildExport(nowMillis: Long): GymBroExport = GymBroExport(
        exportedAtMillis = nowMillis,
        profile = userProfileDao.getUserProfileOneShot(),
        customExercises = exerciseDao.getAllExercises().first().filter { it.isCustom },
        routines = routineDao.getAllRoutines().first(),
        workoutSessions = workoutSessionDao.getAllSessions().first(),
        workoutSets = workoutSetLogDao.getAllSetLogs().first(),
        weightLogs = weightLogDao.getAllWeightLogs().first(),
        foodLogs = foodLogDao.getAllFoodLogs().first(),
        customFoods = customFoodDao.getAllCustomFoods().first(),
        hydrationLogs = hydrationLogDao.getAllHydrationLogs(),
        stepsLogs = stepsLogDao.getAllStepsLogs().first()
    )
}

@JsonClass(generateAdapter = true)
data class GymBroExport(
    val formatVersion: Int = 1,
    val exportedAtMillis: Long,
    val profile: UserProfile?,
    val customExercises: List<Exercise>,
    val routines: List<Routine>,
    val workoutSessions: List<WorkoutSession>,
    val workoutSets: List<WorkoutSetLog>,
    val weightLogs: List<WeightLog>,
    val foodLogs: List<FoodLog>,
    val customFoods: List<CustomFood>,
    val hydrationLogs: List<HydrationLog>,
    val stepsLogs: List<StepsLog>
)
