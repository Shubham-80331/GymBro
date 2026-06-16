package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOneShot(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscle(muscleGroup: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR muscleGroup LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchExercises(query: String): List<Exercise>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY id DESC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Int): Routine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine)

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Int)
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession)
}

@Dao
interface WorkoutSetLogDao {
    @Query("SELECT * FROM workout_set_logs ORDER BY dateMillis DESC")
    fun getAllSetLogs(): Flow<List<WorkoutSetLog>>

    @Query("SELECT * FROM workout_set_logs WHERE exerciseId = :exerciseId ORDER BY dateMillis DESC")
    fun getSetLogsForExercise(exerciseId: String): Flow<List<WorkoutSetLog>>

    @Query("SELECT MAX(weightKg) FROM workout_set_logs WHERE exerciseId = :exerciseId")
    suspend fun getOneRepMaxForExercise(exerciseId: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(log: WorkoutSetLog)

    @Query("DELETE FROM workout_set_logs WHERE id = :id")
    suspend fun deleteSetLog(id: Int)
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY dateMillis DESC")
    fun getAllWeightLogs(): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(log: WeightLog)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightLog(id: Int)
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE dateString = :dateString ORDER BY id DESC")
    fun getFoodLogsForDay(dateString: String): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_logs ORDER BY dateString DESC, id DESC")
    fun getAllFoodLogs(): Flow<List<FoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(log: FoodLog)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFoodLog(id: Int)
}

@Dao
interface CustomFoodDao {
    @Query("SELECT * FROM custom_foods ORDER BY name ASC")
    fun getAllCustomFoods(): Flow<List<CustomFood>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(food: CustomFood)

    @Query("DELETE FROM custom_foods WHERE id = :id")
    suspend fun deleteCustomFood(id: Int)
}

@Dao
interface HydrationLogDao {
    @Query("SELECT COALESCE(SUM(waterMl), 0) FROM hydration_logs WHERE dateString = :dateString")
    fun getWaterIntakeForDay(dateString: String): Flow<Int>

    @Insert
    suspend fun insertHydrationLog(log: HydrationLog)
}

@Dao
interface StepsLogDao {
    @Query("SELECT * FROM steps_logs ORDER BY dateString DESC")
    fun getAllStepsLogs(): Flow<List<StepsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepsLog(log: StepsLog)
}
