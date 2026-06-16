package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "user_profile")
@JsonClass(generateAdapter = true)
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Only 1 profile entry exists
    val age: Int,
    val gender: String, // "Male" or "Female" or "Other"
    val weightKg: Double,
    val heightCm: Double,
    val activityLevel: String, // "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Highly Active"
    val primaryGoal: String, // "Cut", "Maintain", "Bulk", "Recomp"
    val targetWeightKg: Double,
    val timeframeWeeks: Int,
    val bmr: Double,
    val tdee: Double,
    val targetCalories: Int,
    val targetProteinGrams: Int,
    val targetCarbsGrams: Int,
    val targetFatsGrams: Int,
    val onboardingCompleted: Boolean = false
)

@Entity(tableName = "exercises")
@JsonClass(generateAdapter = true)
data class Exercise(
    @PrimaryKey val id: String, // Unique identifier (e.g. uppercase name "BENCH_PRESS")
    val name: String,
    val muscleGroup: String, // "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body"
    val equipment: String, // "Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight"
    val isCustom: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RoutineExercise(
    val exerciseId: String,
    val name: String,
    val muscleGroup: String,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val restSeconds: Int = 90,
    val targetWeightKg: Double = 0.0
)

@Entity(tableName = "routines")
@JsonClass(generateAdapter = true)
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val exercisesJson: String // Serialized List<RoutineExercise>
)

@Entity(tableName = "workout_sessions")
@JsonClass(generateAdapter = true)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routineName: String,
    val dateMillis: Long,
    val activeTimeSeconds: Int,
    val totalVolumeLoadKg: Double,
    val completedSetsCount: Int,
    val caloriesBurnt: Int = 0
)

@Entity(tableName = "workout_set_logs")
@JsonClass(generateAdapter = true)
data class WorkoutSetLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseId: String,
    val exerciseName: String,
    val dateMillis: Long,
    val weightKg: Double,
    val reps: Int,
    val setIndex: Int, // 0, 1, 2...
    val isPr: Boolean = false
)

@Entity(tableName = "weight_logs")
@JsonClass(generateAdapter = true)
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateMillis: Long,
    val weightKg: Double,
    val waistCm: Double? = null,
    val armsCm: Double? = null,
    val chestCm: Double? = null,
    val note: String = ""
)

@Entity(tableName = "food_logs")
@JsonClass(generateAdapter = true)
data class FoodLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String = "",
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int,
    val portionSize: String = "100g",
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snacks"
    val dateString: String // "YYYY-MM-DD" style
)

@Entity(tableName = "custom_foods")
@JsonClass(generateAdapter = true)
data class CustomFood(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String = "",
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int,
    val portionSize: String = "100g"
)

@Entity(tableName = "hydration_logs")
@JsonClass(generateAdapter = true)
data class HydrationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val waterMl: Int
)

@Entity(tableName = "steps_logs")
@JsonClass(generateAdapter = true)
data class StepsLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val stepsCount: Int,
    val caloriesBurnt: Int
)
