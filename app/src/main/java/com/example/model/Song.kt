package com.example.model

data class Song(
  val id: String,
  val file: String,
  val title: String,
  val artist: String = "Unknown Artist",
  val album: String = "Unknown Album",
  val track: Int = 0,
  val durationSec: Long = 0,
  val format: String = "FLAC 24-bit 96kHz",
  val pos: Int = 0,
  val isFavorite: Boolean = false,
  val genre: String = "Soundtrack",
  val year: String = "",
  val albumArtUrl: String? = null
) {
  val formattedDuration: String
    get() {
      if (durationSec <= 0) return "--:--"
      val minutes = durationSec / 60
      val seconds = durationSec % 60
      return "%d:%02d".format(minutes, seconds)
    }
}
