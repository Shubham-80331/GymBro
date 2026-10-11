package com.gymbro.fitness.domain

import com.gymbro.fitness.data.FoodSearchResult

/**
 * Everyday foods (with an Indian focus) that packaged-food databases cover poorly.
 * Values are approximate averages per 100 g as eaten (cooked where relevant); home recipes vary.
 */
object CommonFoods {
    private const val SOURCE = "Common food · approx."

    private fun food(name: String, kcal: Double, protein: Double, carbs: Double, fat: Double) =
        FoodSearchResult(name, SOURCE, kcal, protein, carbs, fat)

    val all: List<FoodSearchResult> = listOf(
        // Indian breads, rice and grains
        food("Roti / Chapati (whole wheat)", 297.0, 9.8, 46.0, 7.5),
        food("Plain Paratha", 326.0, 6.4, 45.0, 13.0),
        food("Naan", 290.0, 9.0, 50.0, 5.5),
        food("White Rice (cooked)", 130.0, 2.7, 28.0, 0.3),
        food("Basmati Rice (cooked)", 121.0, 3.5, 25.0, 0.4),
        food("Brown Rice (cooked)", 112.0, 2.3, 24.0, 0.8),
        food("Chicken Biryani", 200.0, 9.0, 25.0, 7.0),
        food("Khichdi", 120.0, 4.5, 20.0, 2.5),
        food("Poha", 130.0, 2.6, 23.0, 3.0),
        food("Upma", 150.0, 3.5, 22.0, 5.0),
        food("Idli", 130.0, 4.0, 27.0, 0.4),
        food("Plain Dosa", 168.0, 3.9, 29.0, 3.7),
        food("Oats (dry)", 389.0, 16.9, 66.0, 6.9),

        // Dals, curries and sides
        food("Dal Tadka (yellow lentils)", 105.0, 5.5, 14.0, 3.0),
        food("Moong Dal (cooked)", 105.0, 7.0, 18.0, 0.4),
        food("Rajma (kidney bean curry)", 140.0, 6.0, 18.0, 5.0),
        food("Chole (chickpea curry)", 165.0, 7.0, 21.0, 6.0),
        food("Sambar", 65.0, 3.0, 9.0, 2.0),
        food("Palak Paneer", 160.0, 8.0, 6.0, 12.0),
        food("Paneer", 296.0, 18.0, 3.6, 23.0),
        food("Chicken Curry", 150.0, 14.0, 5.0, 8.0),
        food("Tandoori Chicken", 165.0, 24.0, 4.0, 6.0),
        food("Samosa", 262.0, 4.0, 32.0, 13.0),
        food("Moong Sprouts", 30.0, 3.0, 6.0, 0.2),
        food("Soya Chunks (dry)", 345.0, 52.0, 33.0, 0.5),

        // Dairy
        food("Curd / Dahi (whole milk)", 61.0, 3.5, 4.7, 3.3),
        food("Greek Yogurt (plain, non-fat)", 59.0, 10.0, 3.6, 0.4),
        food("Milk (toned)", 58.0, 3.1, 4.7, 3.0),
        food("Milk (whole)", 61.0, 3.2, 4.8, 3.3),
        food("Ghee", 900.0, 0.0, 0.0, 100.0),

        // Protein staples
        food("Chicken Breast (cooked)", 165.0, 31.0, 0.0, 3.6),
        food("Whole Egg (boiled)", 155.0, 13.0, 1.1, 11.0),
        food("Egg White", 52.0, 11.0, 0.7, 0.2),
        food("Whey Protein (typical powder)", 400.0, 80.0, 8.0, 6.0),
        food("Tuna (canned in water)", 116.0, 26.0, 0.0, 0.8),
        food("Fish (rohu / tilapia, cooked)", 128.0, 26.0, 0.0, 2.7),

        // Fruit, nuts, starchy veg
        food("Banana", 89.0, 1.1, 23.0, 0.3),
        food("Apple", 52.0, 0.3, 14.0, 0.2),
        food("Peanut Butter", 588.0, 25.0, 20.0, 50.0),
        food("Almonds", 579.0, 21.0, 22.0, 50.0),
        food("Peanuts (roasted)", 585.0, 24.0, 21.0, 50.0),
        food("Sweet Potato (boiled)", 76.0, 1.4, 18.0, 0.1),
        food("Potato (boiled)", 87.0, 1.9, 20.0, 0.1)
    )

    fun search(query: String): List<FoodSearchResult> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        return all.filter { food -> terms.all { food.name.lowercase().contains(it) } }
    }
}
