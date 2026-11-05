package com.example.data.mpd

import com.example.model.ConnectionState
import com.example.model.LibraryEntry
import com.example.model.MpdStatus
import com.example.model.ServerConfig
import com.example.model.Song
import kotlinx.coroutines.flow.StateFlow

interface MpdClient {
  val connectionState: StateFlow<ConnectionState>
  val status: StateFlow<MpdStatus>
  val currentSong: StateFlow<Song?>
  val queue: StateFlow<List<Song>>

  fun connect(config: ServerConfig)
  fun disconnect()

  fun play(pos: Int? = null)
  fun pause(pause: Boolean? = null)
  fun stop()
  fun next()
  fun previous()
  fun seek(seconds: Long)
  fun setVolume(volume: Int)
  fun toggleMute()

  fun setRandom(enable: Boolean)
  fun setRepeat(enable: Boolean)
  fun setSingle(enable: Boolean)
  fun setConsume(enable: Boolean)

  fun deleteFromQueue(positions: List<Int>)
  fun moveInQueue(fromPos: Int, toPos: Int)
  fun queueNext(positions: List<Int>)
  fun clearQueue()
  fun saveQueue(playlistName: String)

  suspend fun listDirectory(path: String): List<LibraryEntry>
  fun enqueue(uri: String)
  fun playUri(uri: String)
  fun enqueueDirectory(dirUri: String)
  fun updateDatabase()
  suspend fun ping(): Long
}
