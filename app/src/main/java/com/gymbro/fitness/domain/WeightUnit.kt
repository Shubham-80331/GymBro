package com.gymbro.fitness.domain

import java.util.Locale

/** Weights are always stored in kg; this converts at the edges (input and display). */
enum class WeightUnit(val label: String, private val kgPerUnit: Double) {
    KG("kg", 1.0),
    LB("lb", 0.45359237);

    fun fromKg(kg: Double): Double = kg / kgPerUnit
    fun toKg(value: Double): Double = value * kgPerUnit

    /** 60.0 -> "60", 62.5 -> "62.5"; rounded to one decimal in this unit. */
    fun format(kg: Double): String {
        val value = Math.round(fromKg(kg) * 10) / 10.0
        return if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.1f", value)
    }

    fun formatWithUnit(kg: Double): String = "${format(kg)} $label"

    /** Smallest sensible jump for a heavy lift and for a light one (dumbbells, isolation). */
    val largeIncrementKg: Double get() = if (this == KG) 2.5 else toKg(5.0)
    val smallIncrementKg: Double get() = if (this == KG) 1.0 else toKg(2.5)

    val defaultBar: Double get() = if (this == KG) 20.0 else 45.0
    val plates: List<Double>
        get() = if (this == KG) FitnessCalculator.STANDARD_PLATES_KG else listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    companion object {
        fun fromStorage(value: String?): WeightUnit = entries.firstOrNull { it.name == value } ?: KG
    }
}
