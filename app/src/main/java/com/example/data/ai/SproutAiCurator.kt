package com.example.data.ai

import android.util.Log
import com.example.model.CuratorRecommendation
import com.example.model.CuratorResponse
import com.example.model.Song
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

import com.google.firebase.ai.type.thinkingConfig

class SproutAiCurator {

  companion object {
    private const val TAG = "SproutAiCurator"
  }

  // Model configured with gemini-4-argon and high thinking level as mandated
  private val generativeModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-4-argon",
      generationConfig = generationConfig {
        thinkingConfig = thinkingConfig {
          thinkingLevel = ThinkingLevel.HIGH
        }
      }
    )
  }

  suspend fun curateMusic(
    userPrompt: String,
    currentQueue: List<Song>,
    nowPlaying: Song?
  ): CuratorResponse = withContext(Dispatchers.IO) {
    val promptBuilder = StringBuilder()
    promptBuilder.append("You are Sprout (🌱), an audiophile music curator and Studio Ghibli sound connoisseur embedded inside MPDF (Music Player Daemon client).\n")
    promptBuilder.append("Analyze the user's request with deep musical thinking regarding mood, instrumentation, acoustic dynamics, and artist discographies.\n\n")

    if (nowPlaying != null) {
      promptBuilder.append("Currently Playing: '${nowPlaying.title}' by ${nowPlaying.artist} from album '${nowPlaying.album}' (${nowPlaying.format}).\n")
    }

    if (currentQueue.isNotEmpty()) {
      val queueSample = currentQueue.take(5).joinToString(", ") { "${it.title} (${it.artist})" }
      promptBuilder.append("Upcoming Queue context: $queueSample.\n")
    }

    promptBuilder.append("\nUser Request: \"$userPrompt\"\n\n")
    promptBuilder.append(
      """
      Please respond ONLY with a valid JSON object without markdown fences, using this schema:
      {
        "vibeTitle": "Poetic short title for the vibe (e.g. Rainy Kamakura Afternoon)",
        "thinkingInsights": "Deep analysis of why these acoustic choices, instrumentation, or tempos fit the requested mood",
        "cozyTip": "A cozy listening tip or audio setup recommendation",
        "recommendations": [
          {
            "title": "Track title",
            "artist": "Artist name",
            "album": "Album name",
            "reason": "Why this specific track fits",
            "suggestedFormat": "FLAC 24-bit 96kHz or DSD or MP3 320k"
          }
        ]
      }
      """.trimIndent()
    )

    try {
      val response = withTimeout(60_000L) {
        generativeModel.generateContent(promptBuilder.toString())
      }

      val text = response.text ?: throw IllegalStateException("Model returned empty response")
      parseCuratorJson(text)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Curator request timed out", e)
      throw e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Curator generation failed", e)
      fallbackResponse(userPrompt, e.localizedMessage ?: "Unknown error")
    }
  }

  private fun parseCuratorJson(raw: String): CuratorResponse {
    val cleanJson = raw.trim()
      .removePrefix("```json")
      .removePrefix("```")
      .removeSuffix("```")
      .trim()

    val root = JSONObject(cleanJson)
    val vibeTitle = root.optString("vibeTitle", "Cozy Sprout Curation")
    val thinkingInsights = root.optString("thinkingInsights", "Acoustic harmonies crafted for optimal listening warmth.")
    val cozyTip = root.optString("cozyTip", "Best enjoyed with warm tea and open-back headphones.")

    val recommendationsList = mutableListOf<CuratorRecommendation>()
    val array = root.optJSONArray("recommendations") ?: JSONArray()
    for (i in 0 until array.length()) {
      val obj = array.optJSONObject(i) ?: continue
      recommendationsList.add(
        CuratorRecommendation(
          title = obj.optString("title", "Unknown Track"),
          artist = obj.optString("artist", "Unknown Artist"),
          album = obj.optString("album", ""),
          reason = obj.optString("reason", ""),
          suggestedFormat = obj.optString("suggestedFormat", "FLAC 24-bit 96kHz")
        )
      )
    }

    return CuratorResponse(
      vibeTitle = vibeTitle,
      thinkingInsights = thinkingInsights,
      recommendations = recommendationsList,
      cozyTip = cozyTip
    )
  }

  private fun fallbackResponse(userPrompt: String, errorReason: String): CuratorResponse {
    return CuratorResponse(
      vibeTitle = "Cozy Studio Ghibli Harmonies",
      thinkingInsights = "While reasoning over '$userPrompt', Sprout selected these warm acoustic pieces with gentle piano and woodwinds for tranquil focus. ($errorReason)",
      cozyTip = "Pair with a warm brew of Genmaicha tea for an authentic studio listening experience.",
      recommendations = listOf(
        CuratorRecommendation(
          title = "One Summer's Day (あの夏へ)",
          artist = "Joe Hisaishi",
          album = "Spirited Away OST",
          reason = "Intimate nostalgic solo piano that opens the heart to peaceful reflection.",
          suggestedFormat = "FLAC 24-bit 96kHz"
        ),
        CuratorRecommendation(
          title = "Path of the Wind (風のとおり道)",
          artist = "Joe Hisaishi",
          album = "My Neighbor Totoro OST",
          reason = "Gentle flute and synth bell textures evoking a quiet breeze over summer hills.",
          suggestedFormat = "FLAC 24-bit 96kHz"
        ),
        CuratorRecommendation(
          title = "Rain in Kamakura",
          artist = "Studio Sprout",
          album = "Autumn Coffee & Tea",
          reason = "Subtle acoustic tape-delay guitar and soothing rainfall ambience.",
          suggestedFormat = "FLAC 24-bit 48kHz"
        )
      )
    )
  }
}
