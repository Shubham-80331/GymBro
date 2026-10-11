package com.gymbro.fitness.data

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
    exportSchema = true
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
                    seedDefaults(database)
                }
            }
        }
    }
}

/** Built-in exercises and starter routines; used on first launch and after "Delete all my data". */
suspend fun seedDefaults(database: GymBroDatabase) {
    ensureBuiltInExercises(database)
    populateInitialRoutines(database.routineDao())
}

/** Built-in exercise library. Inserted with IGNORE, so it's safe to run on every launch. */
private val BUILT_IN_EXERCISES = listOf(
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
    Exercise("CABLE_CRUNCH", "Cable Crunch", "Core", "Cable"),

    // Chest (more)
    Exercise("INCLINE_BENCH", "Incline Bench Press", "Chest", "Barbell"),
    Exercise("DECLINE_BENCH", "Decline Bench Press", "Chest", "Barbell"),
    Exercise("DB_BENCH", "Dumbbell Bench Press", "Chest", "Dumbbell"),
    Exercise("DB_FLY", "Dumbbell Fly", "Chest", "Dumbbell"),
    Exercise("PEC_DECK", "Pec Deck", "Chest", "Machine"),
    Exercise("CHEST_PRESS_MACHINE", "Machine Chest Press", "Chest", "Machine"),
    Exercise("DIPS", "Dips", "Chest", "Bodyweight"),
    Exercise("SMITH_INCLINE", "Smith Machine Incline Press", "Chest", "Machine"),

    // Back (more)
    Exercise("CHIN_UPS", "Chin-Ups", "Back", "Bodyweight"),
    Exercise("DB_ROW", "One-Arm Dumbbell Row", "Back", "Dumbbell"),
    Exercise("T_BAR_ROW", "T-Bar Row", "Back", "Barbell"),
    Exercise("PENDLAY_ROW", "Pendlay Row", "Back", "Barbell"),
    Exercise("CHEST_SUPPORTED_ROW", "Chest-Supported Row", "Back", "Machine"),
    Exercise("STRAIGHT_ARM_PULLDOWN", "Straight-Arm Pulldown", "Back", "Cable"),
    Exercise("RACK_PULL", "Rack Pull", "Back", "Barbell"),
    Exercise("BACK_EXTENSION", "Back Extension", "Back", "Bodyweight"),
    Exercise("SHRUGS", "Barbell Shrug", "Back", "Barbell"),
    Exercise("DB_SHRUGS", "Dumbbell Shrug", "Back", "Dumbbell"),

    // Legs (more)
    Exercise("FRONT_SQUAT", "Front Squat", "Legs", "Barbell"),
    Exercise("ROMANIAN_DEADLIFT", "Romanian Deadlift", "Legs", "Barbell"),
    Exercise("DB_RDL", "Dumbbell Romanian Deadlift", "Legs", "Dumbbell"),
    Exercise("HACK_SQUAT", "Hack Squat", "Legs", "Machine"),
    Exercise("GOBLET_SQUAT", "Goblet Squat", "Legs", "Dumbbell"),
    Exercise("WALKING_LUNGE", "Walking Lunge", "Legs", "Dumbbell"),
    Exercise("LEG_EXTENSION", "Leg Extension", "Legs", "Machine"),
    Exercise("SEATED_LEG_CURL", "Seated Leg Curl", "Legs", "Machine"),
    Exercise("HIP_THRUST", "Hip Thrust", "Legs", "Barbell"),
    Exercise("GLUTE_BRIDGE", "Glute Bridge", "Legs", "Bodyweight"),
    Exercise("SUMO_DEADLIFT", "Sumo Deadlift", "Legs", "Barbell"),
    Exercise("STEP_UP", "Dumbbell Step-Up", "Legs", "Dumbbell"),
    Exercise("SEATED_CALF_RAISE", "Seated Calf Raise", "Legs", "Machine"),
    Exercise("HIP_ABDUCTION", "Hip Abduction", "Legs", "Machine"),
    Exercise("BW_SQUAT", "Bodyweight Squat", "Legs", "Bodyweight"),

    // Shoulders (more)
    Exercise("DB_SHOULDER_PRESS", "Seated Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
    Exercise("ARNOLD_PRESS", "Arnold Press", "Shoulders", "Dumbbell"),
    Exercise("SHOULDER_PRESS_MACHINE", "Machine Shoulder Press", "Shoulders", "Machine"),
    Exercise("CABLE_LATERAL_RAISE", "Cable Lateral Raise", "Shoulders", "Cable"),
    Exercise("REAR_DELT_FLY", "Rear Delt Fly", "Shoulders", "Dumbbell"),
    Exercise("REVERSE_PEC_DECK", "Reverse Pec Deck", "Shoulders", "Machine"),
    Exercise("FRONT_RAISE", "Front Raise", "Shoulders", "Dumbbell"),
    Exercise("UPRIGHT_ROW", "Upright Row", "Shoulders", "Barbell"),
    Exercise("PUSH_PRESS", "Push Press", "Shoulders", "Barbell"),

    // Arms (more)
    Exercise("BARBELL_CURL", "Barbell Curl", "Arms", "Barbell"),
    Exercise("EZ_BAR_CURL", "EZ-Bar Curl", "Arms", "Barbell"),
    Exercise("PREACHER_CURL", "Preacher Curl", "Arms", "Machine"),
    Exercise("INCLINE_DB_CURL", "Incline Dumbbell Curl", "Arms", "Dumbbell"),
    Exercise("CABLE_CURL", "Cable Curl", "Arms", "Cable"),
    Exercise("CONCENTRATION_CURL", "Concentration Curl", "Arms", "Dumbbell"),
    Exercise("OVERHEAD_TRICEP_EXT", "Overhead Tricep Extension", "Arms", "Cable"),
    Exercise("CLOSE_GRIP_BENCH", "Close-Grip Bench Press", "Arms", "Barbell"),
    Exercise("DB_KICKBACK", "Tricep Kickback", "Arms", "Dumbbell"),
    Exercise("ROPE_PUSHDOWN", "Rope Pushdown", "Arms", "Cable"),
    Exercise("BENCH_DIPS", "Bench Dips", "Arms", "Bodyweight"),
    Exercise("WRIST_CURL", "Wrist Curl", "Arms", "Dumbbell"),

    // Core (more)
    Exercise("CRUNCH", "Crunch", "Core", "Bodyweight"),
    Exercise("RUSSIAN_TWIST", "Russian Twist", "Core", "Bodyweight"),
    Exercise("AB_WHEEL", "Ab Wheel Rollout", "Core", "Bodyweight"),
    Exercise("SIDE_PLANK", "Side Plank", "Core", "Bodyweight"),
    Exercise("DEAD_BUG", "Dead Bug", "Core", "Bodyweight"),
    Exercise("PALLOF_PRESS", "Pallof Press", "Core", "Cable"),
    Exercise("CABLE_WOODCHOP", "Cable Woodchop", "Core", "Cable"),
    Exercise("LEG_RAISE", "Lying Leg Raise", "Core", "Bodyweight"),

    // Full Body
    Exercise("POWER_CLEAN", "Power Clean", "Full Body", "Barbell"),
    Exercise("KB_SWING", "Kettlebell Swing", "Full Body", "Dumbbell"),
    Exercise("THRUSTER", "Thruster", "Full Body", "Barbell"),
    Exercise("FARMERS_WALK", "Farmer's Walk", "Full Body", "Dumbbell"),
    Exercise("BURPEE", "Burpee", "Full Body", "Bodyweight"),
    Exercise("SLED_PUSH", "Sled Push", "Full Body", "Machine")
)

/** Adds any built-in exercises missing from the library (e.g. ones added in an app update). */
suspend fun ensureBuiltInExercises(database: GymBroDatabase) {
    database.exerciseDao().insertExercises(BUILT_IN_EXERCISES)
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
