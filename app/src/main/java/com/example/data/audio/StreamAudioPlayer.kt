package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StreamAudioPlayer(private val context: Context) {

  companion object {
    private const val TAG = "StreamAudioPlayer"
  }

  sealed interface StreamState {
    data object Idle : StreamState
    data object Preparing : StreamState
    data object Playing : StreamState
    data object Paused : StreamState
    data class Error(val message: String) : StreamState
  }

  private val _streamState = MutableStateFlow<StreamState>(StreamState.Idle)
  val streamState: StateFlow<StreamState> = _streamState.asStateFlow()

  private var mediaPlayer: MediaPlayer? = null
  private var currentUrl: String? = null
  private var isMuted: Boolean = false

  fun startStream(url: String) {
    if (url.isBlank()) return
    release()
    try {
      _streamState.value = StreamState.Preparing
      currentUrl = url
      val player = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        )
        setDataSource(url)
        setOnPreparedListener {
          it.start()
          _streamState.value = StreamState.Playing
          applyVolume()
        }
        setOnErrorListener { _, what, extra ->
          Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
          _streamState.value = StreamState.Error("Stream playback error ($what, $extra)")
          true
        }
        prepareAsync()
      }
      mediaPlayer = player
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start stream", e)
      _streamState.value = StreamState.Error(e.localizedMessage ?: "Failed to start stream")
    }
  }

  fun pause() {
    try {
      mediaPlayer?.let {
        if (it.isPlaying) {
          it.pause()
          _streamState.value = StreamState.Paused
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error pausing stream", e)
    }
  }

  fun resume() {
    try {
      mediaPlayer?.let {
        it.start()
        _streamState.value = StreamState.Playing
      } ?: currentUrl?.let { startStream(it) }
    } catch (e: Exception) {
      Log.e(TAG, "Error resuming stream", e)
    }
  }

  fun stop() {
    release()
    _streamState.value = StreamState.Idle
  }

  fun setMuted(muted: Boolean) {
    isMuted = muted
    applyVolume()
  }

  private fun applyVolume() {
    val vol = if (isMuted) 0f else 1.0f
    try {
      mediaPlayer?.setVolume(vol, vol)
    } catch (e: Exception) {
      Log.e(TAG, "Error setting volume", e)
    }
  }

  fun release() {
    try {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    } catch (_: Exception) {}
    mediaPlayer = null
  }
}
