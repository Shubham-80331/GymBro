package com.gymbro.fitness.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OffNutriments(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null
)

@JsonClass(generateAdapter = true)
data class OffProduct(
    @Json(name = "product_name") val productName: String? = null,
    val brands: String? = null,
    val nutriments: OffNutriments? = null
)

@JsonClass(generateAdapter = true)
data class OffSearchResponse(
    val products: List<OffProduct>? = null
)

interface OpenFoodFactsApi {
    @GET("cgi/search.pl")
    suspend fun search(
        @Query("search_terms") terms: String,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("fields") fields: String = "product_name,brands,nutriments"
    ): OffSearchResponse
}

/** Nutrition per 100 g from the food database. */
data class FoodSearchResult(
    val name: String,
    val brand: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double
)

/** Open Food Facts: free, open food database (https://world.openfoodfacts.org). */
object FoodSearchClient {
    private val api: OpenFoodFactsApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            // Open Food Facts asks apps to identify themselves.
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "GymBro-Android/1.0 (com.gymbro.fitness)")
                        .build()
                )
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    suspend fun search(query: String): List<FoodSearchResult> =
        api.search(query.trim()).products.orEmpty().mapNotNull { it.toResult() }.distinctBy { it.name + it.brand }

    private fun OffProduct.toResult(): FoodSearchResult? {
        val name = productName?.trim().orEmpty()
        val n = nutriments ?: return null
        val kcal = n.energyKcal100g ?: return null
        if (name.isEmpty()) return null
        return FoodSearchResult(
            name = name,
            brand = brands?.split(',')?.firstOrNull()?.trim().orEmpty(),
            kcalPer100g = kcal,
            proteinPer100g = n.proteins100g ?: 0.0,
            carbsPer100g = n.carbohydrates100g ?: 0.0,
            fatPer100g = n.fat100g ?: 0.0
        )
    }
}
