package com.gymbro.fitness.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FitnessCalculatorTest {

    @Test
    fun `bmr uses Mifflin-St Jeor constants`() {
        assertEquals(1780.0, FitnessCalculator.bmr(80.0, 180.0, 30, "Male"), 0.001)
        assertEquals(1614.0, FitnessCalculator.bmr(80.0, 180.0, 30, "Female"), 0.001)
        assertEquals(1697.0, FitnessCalculator.bmr(80.0, 180.0, 30, "Other"), 0.001)
    }

    @Test
    fun `tdee falls back to sedentary for unknown level`() {
        assertEquals(1200.0, FitnessCalculator.tdee(1000.0, "Unknown"), 0.001)
        assertEquals(1550.0, FitnessCalculator.tdee(1000.0, "Moderately Active"), 0.001)
    }

    @Test
    fun `cut targets protein per kg and keeps calories consistent`() {
        val macros = FitnessCalculator.macroTargets(tdee = 2700.0, weightKg = 80.0, goal = "Cut")
        assertEquals(2200, macros.calories)
        assertEquals(176, macros.proteinGrams) // 2.2 g/kg
        assertEquals(61, macros.fatsGrams) // 25% of 2200 kcal
        val macroCalories = macros.proteinGrams * 4 + macros.carbsGrams * 4 + macros.fatsGrams * 9
        assertTrue(kotlin.math.abs(macroCalories - macros.calories) <= 4)
    }

    @Test
    fun `cut never drops below 1200 kcal and fats respect the per kg floor`() {
        val macros = FitnessCalculator.macroTargets(tdee = 1300.0, weightKg = 100.0, goal = "Cut")
        assertEquals(1200, macros.calories)
        assertEquals(60, macros.fatsGrams) // 0.6 g/kg beats 25% of calories
        assertTrue(macros.carbsGrams >= 0)
    }

    @Test
    fun `workout calories scale with time and body weight`() {
        assertEquals(400, FitnessCalculator.workoutCalories(activeSeconds = 3600, bodyWeightKg = 80.0))
        assertEquals(0, FitnessCalculator.workoutCalories(activeSeconds = 0, bodyWeightKg = 80.0))
    }

    @Test
    fun `estimated one rep max uses Epley`() {
        assertEquals(100.0, FitnessCalculator.estimatedOneRepMax(100.0, 1), 0.001)
        assertEquals(133.333, FitnessCalculator.estimatedOneRepMax(100.0, 10), 0.001)
        assertEquals(0.0, FitnessCalculator.estimatedOneRepMax(0.0, 10), 0.001)
        assertEquals(0.0, FitnessCalculator.estimatedOneRepMax(100.0, 0), 0.001)
    }

    @Test
    fun `streak counts consecutive days ending today`() {
        val now = noon()
        val sessions = listOf(daysAgo(now, 0), daysAgo(now, 0), daysAgo(now, 1), daysAgo(now, 2), daysAgo(now, 4))
        assertEquals(3, FitnessCalculator.workoutStreak(sessions, now))
    }

    @Test
    fun `streak survives until the end of the day after the last workout`() {
        val now = noon()
        assertEquals(2, FitnessCalculator.workoutStreak(listOf(daysAgo(now, 1), daysAgo(now, 2)), now))
    }

    @Test
    fun `streak is zero after a missed day or with no sessions`() {
        val now = noon()
        assertEquals(0, FitnessCalculator.workoutStreak(listOf(daysAgo(now, 2)), now))
        assertEquals(0, FitnessCalculator.workoutStreak(emptyList(), now))
    }

    @Test
    fun `overload adds an increment when every set hit the target`() {
        val last = listOf(LoggedSet(60.0, 8), LoggedSet(60.0, 8), LoggedSet(60.0, 9))
        assertEquals(62.5, FitnessCalculator.suggestNextWeight(last, targetReps = 8, routineWeightKg = 50.0), 0.001)
    }

    @Test
    fun `overload repeats the weight after a missed rep target`() {
        val last = listOf(LoggedSet(60.0, 8), LoggedSet(60.0, 6))
        assertEquals(60.0, FitnessCalculator.suggestNextWeight(last, targetReps = 8, routineWeightKg = 50.0), 0.001)
    }

    @Test
    fun `overload uses small steps for light weights and routine weight without history`() {
        assertEquals(11.0, FitnessCalculator.suggestNextWeight(listOf(LoggedSet(10.0, 12)), 12, 8.0), 0.001)
        assertEquals(50.0, FitnessCalculator.suggestNextWeight(emptyList(), 8, 50.0), 0.001)
        assertEquals(0.0, FitnessCalculator.suggestNextWeight(listOf(LoggedSet(0.0, 10)), 8, 0.0), 0.001)
    }

    @Test
    fun `plate calculator loads heaviest plates first per side`() {
        val loadout = FitnessCalculator.platesPerSide(102.5)
        assertEquals(listOf(25.0, 15.0, 1.25), loadout.platesPerSideKg)
        assertEquals(0.0, loadout.remainderKg, 0.001)
    }

    @Test
    fun `plate calculator reports what cannot be matched`() {
        val loadout = FitnessCalculator.platesPerSide(61.0)
        assertEquals(listOf(20.0), loadout.platesPerSideKg)
        assertEquals(1.0, loadout.remainderKg, 0.001)
        assertTrue(FitnessCalculator.platesPerSide(20.0).platesPerSideKg.isEmpty())
        assertTrue(FitnessCalculator.platesPerSide(15.0).platesPerSideKg.isEmpty())
    }

    @Test
    fun `pound users progress in 5 lb steps`() {
        val lb = WeightUnit.LB
        val last = listOf(LoggedSet(lb.toKg(135.0), 5), LoggedSet(lb.toKg(135.0), 5))
        val next = FitnessCalculator.suggestNextWeight(last, 5, 0.0, lb.largeIncrementKg, lb.smallIncrementKg)
        assertEquals("140", lb.format(next))
    }

    @Test
    fun `weight unit formats and converts`() {
        assertEquals("62.5", WeightUnit.KG.format(62.5))
        assertEquals("100", WeightUnit.KG.format(100.0))
        assertEquals("225", WeightUnit.LB.format(WeightUnit.LB.toKg(225.0)))
        assertEquals("220.5 lb", WeightUnit.LB.formatWithUnit(100.0))
        assertEquals(WeightUnit.KG, WeightUnit.fromStorage(null))
        assertEquals(WeightUnit.LB, WeightUnit.fromStorage("LB"))
    }

    private fun noon(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
    }.timeInMillis

    private fun daysAgo(now: Long, days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -days)
    }.timeInMillis
}
