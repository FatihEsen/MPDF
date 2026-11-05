package com.example.model

enum class PlaybackState {
  PLAY,
  PAUSE,
  STOP
}

data class MpdStatus(
  val state: PlaybackState = PlaybackState.STOP,
  val songId: String? = null,
  val songPos: Int = -1,
  val elapsedSeconds: Long = 0,
  val totalSeconds: Long = 0,
  val volume: Int = 80,
  val isMuted: Boolean = false,
  val random: Boolean = false,
  val repeat: Boolean = false,
  val single: Boolean = false,
  val consume: Boolean = false,
  val bitrateKbps: Int = 1411,
  val audioFormat: String = "44100:16:2",
  val isUpdatingDb: Boolean = false
) {
  val formattedElapsed: String
    get() {
      val minutes = elapsedSeconds / 60
      val seconds = elapsedSeconds % 60
      return "%d:%02d".format(minutes, seconds)
    }

  val formattedTotal: String
    get() {
      if (totalSeconds <= 0) return "--:--"
      val minutes = totalSeconds / 60
      val seconds = totalSeconds % 60
      return "%d:%02d".format(minutes, seconds)
    }

  val progressFraction: Float
    get() {
      if (totalSeconds <= 0) return 0f
      return (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }
}
