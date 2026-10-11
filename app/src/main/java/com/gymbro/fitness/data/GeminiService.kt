package com.gymbro.fitness.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit
import retrofit2.HttpException
import com.gymbro.fitness.BuildConfig

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null // "user" or "model"; omitted for the system instruction
)

/** One message of the coach conversation, oldest first. */
data class CoachTurn(val fromUser: Boolean, val text: String)

data class CoachReply(val text: String, val isError: Boolean = false)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private const val MAX_HISTORY_TURNS = 20

    suspend fun generateGymBroResponse(conversation: List<CoachTurn>, profileContext: String): CoachReply {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            return CoachReply(
                "The AI coach isn't set up in this build yet. Your workouts and nutrition tracking still work normally.",
                isError = true
            )
        }

        val systemInstructionText = """
            You are "GymBro", an energetic, supportive, and knowledgeable virtual gym partner, workout coach, and nutrition advisor.
            Your tone is motivational and casual (terms like "bro", "PR", "beast mode" are fine), but your training and nutrition advice must be evidence-based and specific.
            Keep advice practical and focused on safety, progressive overload, and hitting daily macros. Use the user's logged data below when it is relevant.
            You are not a doctor: for pain, injury, medical conditions, or eating disorders, tell the user to see a qualified professional.
            Here is the user's context:
            $profileContext
        """.trimIndent()

        // Gemini expects the conversation to start with a user turn.
        val turns = conversation.takeLast(MAX_HISTORY_TURNS).dropWhile { !it.fromUser }
        val request = GeminiGenerateRequest(
            contents = turns.map { turn ->
                GeminiContent(
                    parts = listOf(GeminiPart(text = turn.text)),
                    role = if (turn.fromUser) "user" else "model"
                )
            },
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText)))
        )

        return try {
            val response = api.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                CoachReply("I couldn't come up with an answer to that one. Try asking it a different way.", isError = true)
            } else {
                CoachReply(text)
            }
        } catch (e: HttpException) {
            val message = when (e.code()) {
                429 -> "Lots of people are asking questions right now. Give it a minute and try again."
                400 -> "I couldn't process that message. Try rephrasing it."
                401, 403 -> "The AI coach isn't available in this build right now."
                else -> "The coach is having trouble right now. Please try again shortly."
            }
            CoachReply(message, isError = true)
        } catch (e: IOException) {
            CoachReply("Can't reach the coach. Check your internet connection and try again.", isError = true)
        } catch (e: Exception) {
            CoachReply("Something went wrong on my side. Please try again.", isError = true)
        }
    }
}
