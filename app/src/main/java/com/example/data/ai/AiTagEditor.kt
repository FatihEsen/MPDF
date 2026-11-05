package com.example.data.ai

import android.util.Log
import com.example.model.Song
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

data class TagSuggestion(
  val title: String,
  val artist: String,
  val album: String,
  val genre: String,
  val year: String,
  val track: Int = 0,
  val albumArtDescription: String = "",
  val suggestedArtUrl: String? = null
)

class AiTagEditor {

  companion object {
    private const val TAG = "AiTagEditor"
  }

  // Model configured with gemini-4-argon with thinking enabled
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

  suspend fun suggestSingleTag(song: Song): TagSuggestion = withContext(Dispatchers.IO) {
    val prompt = """
      You are an expert music metadata librarian and audio archivist.
      Analyze this audio file metadata and provide pristine, accurate tags:
      - Current Title: "${song.title}"
      - Current Artist: "${song.artist}"
      - Current Album: "${song.album}"
      - File Path: "${song.file}"
      - Current Format: "${song.format}"

      Clean up file artifacts (such as track number prefixes, file extensions, bitrate markers, Japanese kana romanization or clean bilingual names).
      Deduce the official Artist, Album, Genre, Release Year, Track Number, and suggest aesthetic album art description.

      Respond ONLY with a valid JSON object matching this schema:
      {
        "title": "Clean Track Title",
        "artist": "Artist Name",
        "album": "Album Name",
        "genre": "Genre (e.g. Soundtrack, Classical, Lo-Fi, Piano)",
        "year": "Release Year (e.g. 2001)",
        "track": 1,
        "albumArtDescription": "Description of album cover aesthetic",
        "suggestedArtUrl": "Optional placeholder cover link or null"
      }
    """.trimIndent()

    try {
      val response = withTimeout(45_000L) {
        generativeModel.generateContent(prompt)
      }
      val text = response.text ?: throw IllegalStateException("Model returned empty response")
      parseSingleTagJson(text, song)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI Tag suggestion timed out", e)
      fallbackSingleTag(song)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI Tag suggestion failed", e)
      fallbackSingleTag(song)
    }
  }

  suspend fun suggestBatchTags(songs: List<Song>): List<TagSuggestion> = withContext(Dispatchers.IO) {
    if (songs.isEmpty()) return@withContext emptyList()

    val songListDescription = songs.mapIndexed { index, s ->
      "${index + 1}. Title: '${s.title}', Artist: '${s.artist}', Album: '${s.album}', File: '${s.file}'"
    }.joinToString("\n")

    val prompt = """
      You are an expert music metadata librarian and audio archivist.
      Clean and standardize tags for this batch of ${songs.size} tracks.
      Identify common Album, Artist, Genre, and Release Year, and properly order track numbers:

      $songListDescription

      Respond ONLY with a valid JSON array of objects with ${songs.size} elements:
      [
        {
          "title": "Clean Track Title",
          "artist": "Artist Name",
          "album": "Album Name",
          "genre": "Genre",
          "year": "Release Year",
          "track": 1,
          "albumArtDescription": "Album cover aesthetic",
          "suggestedArtUrl": null
        }
      ]
    """.trimIndent()

    try {
      val response = withTimeout(60_000L) {
        generativeModel.generateContent(prompt)
      }
      val text = response.text ?: throw IllegalStateException("Model returned empty response")
      parseBatchTagJson(text, songs)
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "AI Batch Tag suggestion timed out", e)
      songs.map { fallbackSingleTag(it) }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "AI Batch Tag suggestion failed", e)
      songs.map { fallbackSingleTag(it) }
    }
  }

  private fun parseSingleTagJson(raw: String, originalSong: Song): TagSuggestion {
    val cleanJson = raw.trim()
      .removePrefix("```json")
      .removePrefix("```")
      .removeSuffix("```")
      .trim()

    val obj = JSONObject(cleanJson)
    return TagSuggestion(
      title = obj.optString("title", originalSong.title),
      artist = obj.optString("artist", originalSong.artist),
      album = obj.optString("album", originalSong.album),
      genre = obj.optString("genre", originalSong.genre.ifBlank { "Soundtrack" }),
      year = obj.optString("year", originalSong.year.ifBlank { "2001" }),
      track = obj.optInt("track", originalSong.track),
      albumArtDescription = obj.optString("albumArtDescription", "Cozy Studio Ghibli artwork"),
      suggestedArtUrl = if (obj.has("suggestedArtUrl") && !obj.isNull("suggestedArtUrl")) obj.optString("suggestedArtUrl") else null
    )
  }

  private fun parseBatchTagJson(raw: String, originalSongs: List<Song>): List<TagSuggestion> {
    val cleanJson = raw.trim()
      .removePrefix("```json")
      .removePrefix("```")
      .removeSuffix("```")
      .trim()

    val array = JSONArray(cleanJson)
    val result = mutableListOf<TagSuggestion>()
    for (i in 0 until originalSongs.size) {
      val obj = array.optJSONObject(i)
      val orig = originalSongs[i]
      if (obj != null) {
        result.add(
          TagSuggestion(
            title = obj.optString("title", orig.title),
            artist = obj.optString("artist", orig.artist),
            album = obj.optString("album", orig.album),
            genre = obj.optString("genre", orig.genre.ifBlank { "Soundtrack" }),
            year = obj.optString("year", orig.year.ifBlank { "2001" }),
            track = obj.optInt("track", i + 1),
            albumArtDescription = obj.optString("albumArtDescription", "")
          )
        )
      } else {
        result.add(fallbackSingleTag(orig))
      }
    }
    return result
  }

  private fun fallbackSingleTag(song: Song): TagSuggestion {
    val cleanTitle = song.file.substringAfterLast('/').substringBeforeLast('.')
      .replace(Regex("^\\d+\\s*[-._]?\\s*"), "")
      .trim()

    val deducedArtist = when {
      song.file.contains("Hisaishi", ignoreCase = true) -> "Joe Hisaishi"
      song.file.contains("Sprout", ignoreCase = true) -> "Studio Sprout"
      song.file.contains("Philharmonic", ignoreCase = true) -> "Tokyo Philharmonic Orchestra"
      else -> song.artist
    }

    val deducedAlbum = when {
      song.file.contains("Spirited", ignoreCase = true) -> "Spirited Away OST"
      song.file.contains("Totoro", ignoreCase = true) -> "My Neighbor Totoro OST"
      song.file.contains("Howl", ignoreCase = true) -> "Howl's Moving Castle OST"
      song.file.contains("Kiki", ignoreCase = true) -> "Kiki's Delivery Service OST"
      song.file.contains("Mononoke", ignoreCase = true) -> "Princess Mononoke Symphonic Suite"
      song.file.contains("Autumn", ignoreCase = true) -> "Autumn Coffee & Tea"
      else -> song.album
    }

    return TagSuggestion(
      title = cleanTitle.ifBlank { song.title },
      artist = deducedArtist,
      album = deducedAlbum,
      genre = if (song.genre.isNotBlank()) song.genre else "Soundtrack / Orchestral",
      year = if (song.year.isNotBlank()) song.year else "2001",
      track = if (song.track > 0) song.track else 1,
      albumArtDescription = "Nostalgic Studio Ghibli warm watercolor vinyl artwork"
    )
  }
}
