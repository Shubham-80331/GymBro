package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        Exercise::class,
        Routine::class,
        WorkoutSession::class,
        WorkoutSetLog::class,
        WeightLog::class,
        FoodLog::class,
        CustomFood::class,
        HydrationLog::class,
        StepsLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GymBroDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutSetLogDao(): WorkoutSetLogDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun customFoodDao(): CustomFoodDao
    abstract fun hydrationLogDao(): HydrationLogDao
    abstract fun stepsLogDao(): StepsLogDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add caloriesBurnt to workout_sessions
                database.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `caloriesBurnt` INTEGER NOT NULL DEFAULT 0")
                // Create steps_logs table
                database.execSQL("CREATE TABLE IF NOT EXISTS `steps_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `stepsCount` INTEGER NOT NULL, `caloriesBurnt` INTEGER NOT NULL)")
            }
        }

        @Volatile
        private var INSTANCE: GymBroDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): GymBroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GymBroDatabase::class.java,
                    "gymbro_database"
                )
                .addMigrations(MIGRATION_2_3)
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialExercises(database.exerciseDao())
                    populateInitialRoutines(database.routineDao())
                }
            }
        }

        private suspend fun populateInitialExercises(exerciseDao: ExerciseDao) {
            val initialExercises = listOf(
                // Chest
                Exercise("BENCH_PRESS", "Bench Press", "Chest", "Barbell"),
                Exercise("INC_DB_PRESS", "Incline Dumbbell Press", "Chest", "Dumbbell"),
                Exercise("CHEST_FLY_CABLE", "Chest Fly", "Chest", "Cable"),
                Exercise("PUSH_UPS", "Push-Ups", "Chest", "Bodyweight"),
                
                // Back
                Exercise("DEADLIFT", "Deadlift", "Back", "Barbell"),
                Exercise("PULL_UPS", "Pull-Ups", "Back", "Bodyweight"),
                Exercise("LAT_PULLDOWN", "Lat Pulldown", "Back", "Cable"),
                Exercise("BENT_OVER_ROW", "Bent Over Row", "Back", "Barbell"),
                Exercise("SEATED_ROW_CABLE", "Seated Cable Row", "Back", "Cable"),
                
                // Legs
                Exercise("SQUAT", "Squat", "Legs", "Barbell"),
                Exercise("LEG_PRESS", "Leg Press", "Legs", "Machine"),
                Exercise("SPLIT_SQUAT", "Bulgarian Split Squat", "Legs", "Dumbbell"),
                Exercise("LEG_CURL", "Lying Leg Curl", "Legs", "Machine"),
                Exercise("CALF_RAISE", "Calf Raises", "Legs", "Machine"),
                
                // Shoulders
                Exercise("OVERHEAD_PRESS", "Overhead Press", "Shoulders", "Barbell"),
                Exercise("LATERAL_RAISE", "Lateral Raise", "Shoulders", "Dumbbell"),
                Exercise("FACE_PULLS", "Face Pulls", "Shoulders", "Cable"),
                
                // Arms
                Exercise("BICEP_CURL", "Dumbbell Bicep Curl", "Arms", "Dumbbell"),
                Exercise("TRICEP_PUSHDOWN", "Tricep Pushdown", "Arms", "Cable"),
                Exercise("HAMMER_CURL", "Hammer Curl", "Arms", "Dumbbell"),
                Exercise("SKULL_CRUSHER", "Skull Crusher", "Arms", "Barbell"),
                
                // Core
                Exercise("PLANK", "Plank", "Core", "Bodyweight"),
                Exercise("HANGING_LEG_RAISE", "Hanging Leg Raise", "Core", "Bodyweight"),
                Exercise("CABLE_CRUNCH", "Cable Crunch", "Core", "Cable")
            )
            exerciseDao.insertExercises(initialExercises)
        }

        private suspend fun populateInitialRoutines(routineDao: RoutineDao) {
            val pushPullLegs = listOf(
                Routine(
                    name = "Push Day",
                    description = "Focus on Chest, Shoulders, and Triceps",
                    exercisesJson = """
                        [
                            {"exerciseId":"BENCH_PRESS","name":"Bench Press","muscleGroup":"Chest","targetSets":4,"targetReps":8,"restSeconds":120,"targetWeightKg":60.0},
                            {"exerciseId":"INC_DB_PRESS","name":"Incline Dumbbell Press","muscleGroup":"Chest","targetSets":3,"targetReps":10,"restSeconds":90,"targetWeightKg":20.0},
                            {"exerciseId":"OVERHEAD_PRESS","name":"Overhead Press","muscleGroup":"Shoulders","targetSets":3,"targetReps":8,"restSeconds":90,"targetWeightKg":35.0},
                            {"exerciseId":"LATERAL_RAISE","name":"Lateral Raise","muscleGroup":"Shoulders","targetSets":3,"targetReps":12,"restSeconds":60,"targetWeightKg":8.0},
                            {"exerciseId":"TRICEP_PUSHDOWN","name":"Tricep Pushdown","muscleGroup":"Arms","targetSets":3,"targetReps":12,"restSeconds":60,"targetWeightKg":15.0}
                        ]
                    """.trimIndent()
                ),
                Routine(
                    name = "Pull Day",
                    description = "Focus on Back and Biceps",
                    exercisesJson = """
                        [
                            {"exerciseId":"DEADLIFT","name":"Deadlift","muscleGroup":"Back","targetSets":3,"targetReps":5,"restSeconds":180,"targetWeightKg":100.0},
                            {"exerciseId":"PULL_UPS","name":"Pull-Ups","muscleGroup":"Back","targetSets":3,"targetReps":8,"restSeconds":90,"targetWeightKg":0.0},
                            {"exerciseId":"LAT_PULLDOWN","name":"Lat Pulldown","muscleGroup":"Back","targetSets":3,"targetReps":10,"restSeconds":90,"targetWeightKg":45.0},
                            {"exerciseId":"BICEP_CURL","name":"Dumbbell Bicep Curl","muscleGroup":"Arms","targetSets":3,"targetReps":10,"restSeconds":60,"targetWeightKg":12.0}
                        ]
                    """.trimIndent()
                ),
                Routine(
                    name = "Legs Day",
                    description = "Focus on Quads, Hamstrings, and Calves",
                    exercisesJson = """
                        [
                            {"exerciseId":"SQUAT","name":"Squat","muscleGroup":"Legs","targetSets":4,"targetReps":8,"restSeconds":120,"targetWeightKg":70.0},
                            {"exerciseId":"LEG_PRESS","name":"Leg Press","muscleGroup":"Legs","targetSets":3,"targetReps":10,"restSeconds":90,"targetWeightKg":120.0},
                            {"exerciseId":"LEG_CURL","name":"Lying Leg Curl","muscleGroup":"Legs","targetSets":3,"targetReps":12,"restSeconds":60,"targetWeightKg":30.0},
                            {"exerciseId":"CALF_RAISE","name":"Calf Raises","muscleGroup":"Legs","targetSets":3,"targetReps":15,"restSeconds":60,"targetWeightKg":40.0}
                        ]
                    """.trimIndent()
                )
            )
            pushPullLegs.forEach { routineDao.insertRoutine(it) }
        }
    }
}
