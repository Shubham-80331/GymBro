package com.gymbro.fitness.domain

/** One exercise slot in a template day; names and muscle groups come from the exercise library. */
data class TemplateExercise(
    val exerciseId: String,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int
)

data class TemplateDay(
    val name: String,
    val description: String,
    val exercises: List<TemplateExercise>
)

data class ProgramTemplate(
    val name: String,
    val summary: String,
    val level: String,
    val daysPerWeek: Int,
    val days: List<TemplateDay>
)

/** Well-known starter programs. Every exerciseId must exist in the built-in library. */
object ProgramTemplates {
    private fun ex(id: String, sets: Int, reps: Int, rest: Int = 90) = TemplateExercise(id, sets, reps, rest)

    val all: List<ProgramTemplate> = listOf(
        ProgramTemplate(
            name = "Full Body Beginner",
            summary = "Two alternating full-body days, three sessions a week. Learn the big lifts and add weight every session.",
            level = "Beginner",
            daysPerWeek = 3,
            days = listOf(
                TemplateDay(
                    "Full Body A", "Squat, bench, row",
                    listOf(
                        ex("SQUAT", 3, 8, 150),
                        ex("BENCH_PRESS", 3, 8, 120),
                        ex("BENT_OVER_ROW", 3, 8, 120),
                        ex("LATERAL_RAISE", 2, 12, 60),
                        ex("HANGING_LEG_RAISE", 2, 10, 60)
                    )
                ),
                TemplateDay(
                    "Full Body B", "Deadlift, press, pulldown",
                    listOf(
                        ex("DEADLIFT", 3, 5, 180),
                        ex("OVERHEAD_PRESS", 3, 8, 120),
                        ex("LAT_PULLDOWN", 3, 10, 90),
                        ex("LEG_CURL", 3, 12, 60),
                        ex("BICEP_CURL", 2, 12, 60)
                    )
                )
            )
        ),
        ProgramTemplate(
            name = "5×5 Strength",
            summary = "Classic linear strength program: heavy compound lifts, five sets of five, alternating A and B three times a week.",
            level = "Beginner–Intermediate",
            daysPerWeek = 3,
            days = listOf(
                TemplateDay(
                    "5×5 Workout A", "Squat, bench, row",
                    listOf(ex("SQUAT", 5, 5, 180), ex("BENCH_PRESS", 5, 5, 180), ex("BENT_OVER_ROW", 5, 5, 180))
                ),
                TemplateDay(
                    "5×5 Workout B", "Squat, press, deadlift",
                    listOf(ex("SQUAT", 5, 5, 180), ex("OVERHEAD_PRESS", 5, 5, 180), ex("DEADLIFT", 1, 5, 240))
                )
            )
        ),
        ProgramTemplate(
            name = "Upper / Lower",
            summary = "Four days a week, each muscle trained twice. A strength day and a volume day for each half.",
            level = "Intermediate",
            daysPerWeek = 4,
            days = listOf(
                TemplateDay(
                    "Upper A (Strength)", "Heavy press and pull",
                    listOf(
                        ex("BENCH_PRESS", 4, 6, 150),
                        ex("BENT_OVER_ROW", 4, 6, 150),
                        ex("OVERHEAD_PRESS", 3, 8, 120),
                        ex("PULL_UPS", 3, 8, 120),
                        ex("SKULL_CRUSHER", 3, 10, 60),
                        ex("BICEP_CURL", 3, 10, 60)
                    )
                ),
                TemplateDay(
                    "Lower A (Strength)", "Heavy squat and hinge",
                    listOf(
                        ex("SQUAT", 4, 6, 180),
                        ex("ROMANIAN_DEADLIFT", 3, 8, 150),
                        ex("LEG_PRESS", 3, 10, 120),
                        ex("LEG_CURL", 3, 12, 60),
                        ex("CALF_RAISE", 4, 12, 60)
                    )
                ),
                TemplateDay(
                    "Upper B (Volume)", "Moderate weight, more reps",
                    listOf(
                        ex("INC_DB_PRESS", 3, 10, 90),
                        ex("SEATED_ROW_CABLE", 3, 12, 90),
                        ex("DB_SHOULDER_PRESS", 3, 10, 90),
                        ex("LAT_PULLDOWN", 3, 12, 90),
                        ex("LATERAL_RAISE", 3, 15, 60),
                        ex("ROPE_PUSHDOWN", 3, 12, 60),
                        ex("HAMMER_CURL", 3, 12, 60)
                    )
                ),
                TemplateDay(
                    "Lower B (Volume)", "Unilateral work and accessories",
                    listOf(
                        ex("FRONT_SQUAT", 3, 8, 150),
                        ex("SPLIT_SQUAT", 3, 10, 90),
                        ex("HIP_THRUST", 3, 10, 90),
                        ex("LEG_EXTENSION", 3, 15, 60),
                        ex("SEATED_LEG_CURL", 3, 15, 60),
                        ex("SEATED_CALF_RAISE", 4, 15, 60)
                    )
                )
            )
        ),
        ProgramTemplate(
            name = "Bro Split",
            summary = "Five days, one muscle group a day with lots of volume. Popular for bodybuilding-style training.",
            level = "Intermediate",
            daysPerWeek = 5,
            days = listOf(
                TemplateDay(
                    "Chest Day", "Chest from every angle",
                    listOf(
                        ex("BENCH_PRESS", 4, 8, 120),
                        ex("INC_DB_PRESS", 3, 10, 90),
                        ex("DIPS", 3, 10, 90),
                        ex("CHEST_FLY_CABLE", 3, 12, 60),
                        ex("PEC_DECK", 3, 15, 60)
                    )
                ),
                TemplateDay(
                    "Back Day", "Width and thickness",
                    listOf(
                        ex("DEADLIFT", 3, 5, 180),
                        ex("PULL_UPS", 3, 8, 120),
                        ex("BENT_OVER_ROW", 3, 10, 90),
                        ex("LAT_PULLDOWN", 3, 12, 90),
                        ex("SEATED_ROW_CABLE", 3, 12, 90)
                    )
                ),
                TemplateDay(
                    "Shoulder Day", "All three delt heads",
                    listOf(
                        ex("OVERHEAD_PRESS", 4, 8, 120),
                        ex("ARNOLD_PRESS", 3, 10, 90),
                        ex("LATERAL_RAISE", 4, 15, 60),
                        ex("REAR_DELT_FLY", 3, 15, 60),
                        ex("SHRUGS", 3, 12, 60)
                    )
                ),
                TemplateDay(
                    "Leg Day", "Quads, hamstrings, calves",
                    listOf(
                        ex("SQUAT", 4, 8, 180),
                        ex("LEG_PRESS", 3, 12, 120),
                        ex("ROMANIAN_DEADLIFT", 3, 10, 120),
                        ex("LEG_EXTENSION", 3, 15, 60),
                        ex("LEG_CURL", 3, 15, 60),
                        ex("CALF_RAISE", 4, 15, 60)
                    )
                ),
                TemplateDay(
                    "Arm Day", "Biceps and triceps",
                    listOf(
                        ex("CLOSE_GRIP_BENCH", 3, 8, 120),
                        ex("BARBELL_CURL", 3, 10, 90),
                        ex("SKULL_CRUSHER", 3, 10, 90),
                        ex("INCLINE_DB_CURL", 3, 12, 60),
                        ex("ROPE_PUSHDOWN", 3, 12, 60),
                        ex("HAMMER_CURL", 3, 12, 60)
                    )
                )
            )
        )
    )
}
