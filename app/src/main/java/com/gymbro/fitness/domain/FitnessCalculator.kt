package com.gymbro.fitness.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

data class PlateLoadout(val platesPerSideKg: List<Double>, val remainderKg: Double)

data class LoggedSet(val weightKg: Double, val reps: Int)

data class MacroTargets(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatsGrams: Int
)

/** Pure fitness math, kept free of Android types so it can be unit tested on the JVM. */
object FitnessCalculator {

    /** Mifflin-St Jeor. "Other" uses the midpoint of the male and female constants. */
    fun bmr(weightKg: Double, heightCm: Double, age: Int, gender: String): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        val genderConstant = when (gender.lowercase(Locale.US)) {
            "male" -> 5.0
            "female" -> -161.0
            else -> -78.0
        }
        return base + genderConstant
    }

    fun tdee(bmr: Double, activityLevel: String): Double {
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

    /**
     * Protein is set per kg of body weight (higher when cutting to protect muscle),
     * fats take 25% of calories but never less than 0.6 g/kg, and carbs fill the rest.
     */
    fun macroTargets(tdee: Double, weightKg: Double, goal: String): MacroTargets {
        val calories = when (goal) {
            "Cut" -> (tdee - 500).coerceAtLeast(1200.0)
            "Bulk" -> tdee + 350
            "Recomp" -> tdee - 100
            else -> tdee
        }.roundToInt()

        val proteinPerKg = when (goal) {
            "Cut" -> 2.2
            "Recomp" -> 2.0
            "Bulk" -> 1.8
            else -> 1.6
        }
        val protein = (weightKg * proteinPerKg).roundToInt()
        val fats = max(calories * 0.25 / 9.0, weightKg * 0.6).roundToInt()
        val carbs = ((calories - protein * 4 - fats * 9) / 4.0).roundToInt().coerceAtLeast(0)

        return MacroTargets(calories, protein, carbs, fats)
    }

    /** Resistance training at roughly 5 METs: kcal = MET × body weight (kg) × hours. */
    fun workoutCalories(activeSeconds: Int, bodyWeightKg: Double): Int {
        val hours = activeSeconds / 3600.0
        return (STRENGTH_TRAINING_MET * bodyWeightKg * hours).roundToInt()
    }

    /** Epley estimated one-rep max. A single rep is the lift itself. */
    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1 + reps / 30.0)
    }

    /**
     * Consecutive days with at least one workout, ending today or yesterday
     * (so the streak is not lost before the user has had a chance to train today).
     */
    fun workoutStreak(sessionTimesMillis: List<Long>, nowMillis: Long = System.currentTimeMillis()): Int {
        if (sessionTimesMillis.isEmpty()) return 0
        val format = SimpleDateFormat("yyyyMMdd", Locale.US)
        val trainedDays = sessionTimesMillis.map { format.format(Date(it)) }.toSet()

        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        if (format.format(calendar.time) !in trainedDays) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            if (format.format(calendar.time) !in trainedDays) return 0
        }

        var streak = 0
        while (format.format(calendar.time) in trainedDays) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    /**
     * Progressive overload: if every set last session hit the target reps, add a small increment;
     * otherwise repeat the same weight. Falls back to the routine's weight with no usable history.
     */
    fun suggestNextWeight(
        lastSessionSets: List<LoggedSet>,
        targetReps: Int,
        routineWeightKg: Double,
        largeIncrementKg: Double = 2.5,
        smallIncrementKg: Double = 1.0
    ): Double {
        val workingWeight = lastSessionSets.maxOfOrNull { it.weightKg } ?: return routineWeightKg
        if (workingWeight <= 0.0) return routineWeightKg
        val hitAllReps = lastSessionSets.all { it.reps >= targetReps }
        if (!hitAllReps) return workingWeight
        val increment = if (workingWeight >= 20.0) largeIncrementKg else smallIncrementKg
        return workingWeight + increment
    }

    /**
     * Plates to load on EACH side of the bar for [targetKg], heaviest first, using the greedy
     * method (exact for standard plate sets). [remainderKg] is what couldn't be matched.
     */
    fun platesPerSide(
        targetKg: Double,
        barKg: Double = 20.0,
        availablePlatesKg: List<Double> = STANDARD_PLATES_KG
    ): PlateLoadout {
        var perSide = (targetKg - barKg) / 2.0
        if (perSide <= 0.0) return PlateLoadout(emptyList(), 0.0)
        val plates = mutableListOf<Double>()
        for (plate in availablePlatesKg.sortedDescending()) {
            while (perSide + EPSILON >= plate) {
                plates += plate
                perSide -= plate
            }
        }
        return PlateLoadout(plates, (perSide * 2).coerceAtLeast(0.0))
    }

    val STANDARD_PLATES_KG = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    private const val EPSILON = 1e-6

    /**
     * True when the last [window] sessions (newest first, each the session's best estimated 1RM)
     * all came close to but failed to beat the best from before them, i.e. the lift has stalled.
     */
    fun isPlateau(sessionBestsNewestFirst: List<Double>, window: Int = 3): Boolean {
        if (sessionBestsNewestFirst.size <= window) return false
        val recent = sessionBestsNewestFirst.take(window)
        val bestBefore = sessionBestsNewestFirst.drop(window).maxOrNull() ?: return false
        // Sessions well below the old best are a deload or an off day, not a stall.
        return bestBefore > 0.0 && recent.all { it <= bestBefore && it >= bestBefore * 0.95 }
    }

    /** A deload drops the working weight about 10%, rounded down to the given step. */
    fun deloadWeight(workingWeightKg: Double, stepKg: Double = 2.5): Double {
        if (workingWeightKg <= 0.0) return 0.0
        val target = workingWeightKg * 0.9
        return (Math.floor(target / stepKg) * stepKg).coerceAtLeast(stepKg)
    }

    private const val STRENGTH_TRAINING_MET = 5.0
}
