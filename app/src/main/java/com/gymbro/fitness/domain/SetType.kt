package com.gymbro.fitness.domain

/** How a set was performed. Warm-ups are logged but don't count toward volume, PRs or weekly sets. */
enum class SetType(val shortLabel: String, val description: String) {
    WORKING("", "Working set"),
    WARMUP("W", "Warm-up"),
    DROP("D", "Drop set"),
    FAILURE("F", "To failure");

    val countsAsWork: Boolean get() = this != WARMUP

    /** Tap order in the logger: working → warm-up → drop → failure → working. */
    fun next(): SetType = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromStorage(value: String?): SetType = entries.firstOrNull { it.name == value } ?: WORKING
    }
}
