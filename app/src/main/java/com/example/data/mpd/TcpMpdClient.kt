package com.example.data.mpd

import android.util.Log
import com.example.model.ConnectionState
import com.example.model.LibraryEntry
import com.example.model.LibraryFolder
import com.example.model.LibraryTrack
import com.example.model.MpdStatus
import com.example.model.PlaybackState
import com.example.model.ServerConfig
import com.example.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min

class TcpMpdClient(
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : MpdClient {

  companion object {
    private const val TAG = "TcpMpdClient"
    private const val CONNECT_TIMEOUT_MS = 5000
    private const val SOCKET_TIMEOUT_MS = 10000
    private const val IDLE_SOCKET_TIMEOUT_MS = 0 // block indefinitely for idle events
  }

  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _status = MutableStateFlow(MpdStatus())
  override val status: StateFlow<MpdStatus> = _status.asStateFlow()

  private val _currentSong = MutableStateFlow<Song?>(null)
  override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

  private val _queue = MutableStateFlow<List<Song>>(emptyList())
  override val queue: StateFlow<List<Song>> = _queue.asStateFlow()

  private var serverConfig: ServerConfig? = null
  private var connectionSupervisorJob: Job? = null
  private var idleJob: Job? = null

  // Dedicated command socket with mutex for sequential thread-safe MPD commands
  private val cmdMutex = Mutex()
  private var cmdSocket: Socket? = null
  private var cmdReader: BufferedReader? = null
  private var cmdWriter: BufferedWriter? = null

  // Dedicated idle socket for asynchronous push events
  private var idleSocket: Socket? = null
  private var idleReader: BufferedReader? = null
  private var idleWriter: BufferedWriter? = null

  override fun connect(config: ServerConfig) {
    serverConfig = config
    connectionSupervisorJob?.cancel()
    connectionSupervisorJob = scope.launch {
      var attempt = 0
      while (isActive) {
        try {
          val endpoint = "${config.host}:${config.port}"
          _connectionState.value = ConnectionState.Connecting(endpoint)

          setupCommandSocket(config)
          val startPing = System.currentTimeMillis()
          refreshAllState()
          val latency = System.currentTimeMillis() - startPing

          _connectionState.value = ConnectionState.Connected(endpoint, latency)
          attempt = 0 // reset backoff on successful connection

          startIdleLoop(config)
          break
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.w(TAG, "Connection attempt failed: ${e.message}")
          _connectionState.value = ConnectionState.Error("Connection failed: ${e.localizedMessage}")
          closeSockets()
          if (!config.autoReconnect) break
          attempt++
          val backoffMs = min(1000L * (1L shl min(attempt, 5)), 15000L)
          delay(backoffMs)
        }
      }
    }
  }

  override fun disconnect() {
    connectionSupervisorJob?.cancel()
    idleJob?.cancel()
    closeSockets()
    _connectionState.value = ConnectionState.Disconnected
  }

  private fun closeSockets() {
    try { cmdSocket?.close() } catch (_: Exception) {}
    try { idleSocket?.close() } catch (_: Exception) {}
    cmdSocket = null
    cmdReader = null
    cmdWriter = null
    idleSocket = null
    idleReader = null
    idleWriter = null
  }

  private suspend fun setupCommandSocket(config: ServerConfig) = withContext(Dispatchers.IO) {
    cmdMutex.withLock {
      closeCommandSocket()
      val socket = Socket()
      socket.connect(InetSocketAddress(config.host, config.port), CONNECT_TIMEOUT_MS)
      socket.soTimeout = SOCKET_TIMEOUT_MS
      val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
      val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

      val banner = reader.readLine()
      if (banner == null || !banner.startsWith("OK MPD")) {
        socket.close()
        throw IllegalStateException("Invalid MPD handshake: $banner")
      }

      if (config.password.isNotBlank()) {
        writer.write("password \"${escapeMpd(config.password)}\"\n")
        writer.flush()
        val authResp = readResponseLines(reader)
        if (authResp.any { it.startsWith("ACK") }) {
          socket.close()
          throw IllegalStateException("MPD Password Authentication Failed")
        }
      }

      cmdSocket = socket
      cmdReader = reader
      cmdWriter = writer
    }
  }

  private fun closeCommandSocket() {
    try { cmdSocket?.close() } catch (_: Exception) {}
    cmdSocket = null
    cmdReader = null
    cmdWriter = null
  }

  private fun startIdleLoop(config: ServerConfig) {
    idleJob?.cancel()
    idleJob = scope.launch(Dispatchers.IO) {
      while (isActive) {
        try {
          val socket = Socket()
          socket.connect(InetSocketAddress(config.host, config.port), CONNECT_TIMEOUT_MS)
          socket.soTimeout = IDLE_SOCKET_TIMEOUT_MS
          val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
          val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

          val banner = reader.readLine()
          if (banner == null || !banner.startsWith("OK MPD")) {
            socket.close()
            delay(2000)
            continue
          }

          if (config.password.isNotBlank()) {
            writer.write("password \"${escapeMpd(config.password)}\"\n")
            writer.flush()
            readResponseLines(reader)
          }

          idleSocket = socket
          idleReader = reader
          idleWriter = writer

          while (isActive && !socket.isClosed) {
            writer.write("idle player mixer options playlist database\n")
            writer.flush()

            val idleEvents = mutableListOf<String>()
            while (isActive) {
              val line = reader.readLine() ?: break
              if (line.startsWith("changed: ")) {
                idleEvents.add(line.substringAfter("changed: ").trim())
              } else if (line == "OK") {
                break
              } else if (line.startsWith("ACK")) {
                break
              }
            }

            if (idleEvents.isNotEmpty()) {
              handleIdleEvents(idleEvents)
            }
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.d(TAG, "Idle listener loop interrupted: ${e.message}")
          delay(2000)
        }
      }
    }
  }

  private suspend fun handleIdleEvents(events: List<String>) {
    for (event in events) {
      when (event) {
        "player", "mixer", "options" -> refreshStatus()
        "playlist" -> {
          refreshQueue()
          refreshStatus()
        }
        "database" -> refreshStatus()
      }
    }
  }

  private suspend fun sendCommand(command: String): List<String> = withContext(Dispatchers.IO) {
    cmdMutex.withLock {
      val writer = cmdWriter
      val reader = cmdReader
      val cfg = serverConfig
      if (writer == null || reader == null || cfg == null) {
        throw IllegalStateException("Command socket not connected")
      }

      writer.write("$command\n")
      writer.flush()
      readResponseLines(reader)
    }
  }

  private fun readResponseLines(reader: BufferedReader): List<String> {
    val lines = mutableListOf<String>()
    while (true) {
      val line = reader.readLine() ?: break
      if (line == "OK") {
        break
      }
      if (line.startsWith("ACK")) {
        lines.add(line)
        break
      }
      lines.add(line)
    }
    return lines
  }

  private suspend fun refreshAllState() {
    refreshStatus()
    refreshCurrentSong()
    refreshQueue()
  }

  private suspend fun refreshStatus() {
    try {
      val lines = sendCommand("status")
      var state = PlaybackState.STOP
      var songId: String? = null
      var songPos = -1
      var elapsed = 0L
      var total = 0L
      var volume = 80
      var random = false
      var repeat = false
      var single = false
      var consume = false
      var bitrate = 0
      var audio = "44100:16:2"
      var updatingDb = false

      for (line in lines) {
        val key = line.substringBefore(':').trim()
        val value = line.substringAfter(':').trim()
        when (key) {
          "state" -> state = when (value) {
            "play" -> PlaybackState.PLAY
            "pause" -> PlaybackState.PAUSE
            else -> PlaybackState.STOP
          }
          "songid" -> songId = value
          "song" -> songPos = value.toIntOrNull() ?: -1
          "elapsed" -> elapsed = value.toDoubleOrNull()?.toLong() ?: 0L
          "duration" -> total = value.toDoubleOrNull()?.toLong() ?: 0L
          "volume" -> volume = (value.toIntOrNull() ?: 80).coerceIn(0, 100)
          "random" -> random = value == "1"
          "repeat" -> repeat = value == "1"
          "single" -> single = value == "1"
          "consume" -> consume = value == "1"
          "bitrate" -> bitrate = value.toIntOrNull() ?: 0
          "audio" -> audio = value
          "updating_db" -> updatingDb = true
        }
      }

      _status.value = _status.value.copy(
        state = state,
        songId = songId,
        songPos = songPos,
        elapsedSeconds = elapsed,
        totalSeconds = total,
        volume = volume,
        isMuted = volume == 0,
        random = random,
        repeat = repeat,
        single = single,
        consume = consume,
        bitrateKbps = bitrate,
        audioFormat = audio,
        isUpdatingDb = updatingDb
      )

      refreshCurrentSong()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to refresh status", e)
    }
  }

  private suspend fun refreshCurrentSong() {
    try {
      val lines = sendCommand("currentsong")
      if (lines.isEmpty()) {
        _currentSong.value = null
        return
      }
      _currentSong.value = parseSong(lines)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to refresh current song", e)
    }
  }

  private suspend fun refreshQueue() {
    try {
      val lines = sendCommand("playlistinfo")
      val songs = mutableListOf<Song>()
      val currentSongLines = mutableListOf<String>()

      for (line in lines) {
        if (line.startsWith("file: ") && currentSongLines.isNotEmpty()) {
          parseSong(currentSongLines)?.let { songs.add(it) }
          currentSongLines.clear()
        }
        currentSongLines.add(line)
      }
      if (currentSongLines.isNotEmpty()) {
        parseSong(currentSongLines)?.let { songs.add(it) }
      }

      _queue.value = songs
    } catch (e: Exception) {
      Log.e(TAG, "Failed to refresh playlist info", e)
    }
  }

  private fun parseSong(lines: List<String>): Song? {
    var file: String? = null
    var id = ""
    var title: String? = null
    var artist = "Unknown Artist"
    var album = "Unknown Album"
    var track = 0
    var time = 0L
    var format = "FLAC 24-bit 96kHz"
    var pos = 0

    for (line in lines) {
      val key = line.substringBefore(':').trim()
      val value = line.substringAfter(':').trim()
      when (key) {
        "file" -> file = value
        "Id" -> id = value
        "Title" -> title = value
        "Artist" -> artist = value
        "Album" -> album = value
        "Track" -> track = value.substringBefore('/').toIntOrNull() ?: 0
        "Time" -> time = value.toLongOrNull() ?: 0L
        "duration" -> if (time == 0L) time = value.toDoubleOrNull()?.toLong() ?: 0L
        "Pos" -> pos = value.toIntOrNull() ?: 0
        "Format" -> format = value
      }
    }

    if (file == null) return null
    if (format == "FLAC 24-bit 96kHz" && file.endsWith(".mp3", ignoreCase = true)) {
      format = "MP3 320kbps"
    } else if (file.endsWith(".dsf", ignoreCase = true) || file.endsWith(".dff", ignoreCase = true)) {
      format = "DSD 2.8MHz"
    }

    return Song(
      id = if (id.isNotBlank()) id else file,
      file = file,
      title = title ?: file.substringAfterLast('/'),
      artist = artist,
      album = album,
      track = track,
      durationSec = time,
      format = format,
      pos = pos
    )
  }

  private fun escapeMpd(param: String): String {
    return param.replace("\\", "\\\\").replace("\"", "\\\"")
  }

  override fun play(pos: Int?) {
    scope.launch {
      try {
        if (pos != null) {
          sendCommand("play $pos")
        } else {
          sendCommand("play")
        }
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "play command error", e)
      }
    }
  }

  override fun pause(pause: Boolean?) {
    scope.launch {
      try {
        if (pause == null) {
          sendCommand("pause")
        } else {
          sendCommand("pause ${if (pause) 1 else 0}")
        }
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "pause command error", e)
      }
    }
  }

  override fun stop() {
    scope.launch {
      try {
        sendCommand("stop")
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "stop command error", e)
      }
    }
  }

  override fun next() {
    scope.launch {
      try {
        sendCommand("next")
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "next command error", e)
      }
    }
  }

  override fun previous() {
    scope.launch {
      try {
        sendCommand("previous")
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "previous command error", e)
      }
    }
  }

  override fun seek(seconds: Long) {
    scope.launch {
      try {
        sendCommand("seekcur $seconds")
        _status.value = _status.value.copy(elapsedSeconds = seconds)
      } catch (e: Exception) {
        Log.e(TAG, "seek command error", e)
      }
    }
  }

  override fun setVolume(volume: Int) {
    val clamped = volume.coerceIn(0, 100)
    scope.launch {
      try {
        sendCommand("setvol $clamped")
        _status.value = _status.value.copy(volume = clamped, isMuted = clamped == 0)
      } catch (e: Exception) {
        Log.e(TAG, "setvol command error", e)
      }
    }
  }

  override fun toggleMute() {
    val cur = _status.value
    val targetVol = if (cur.isMuted || cur.volume == 0) 80 else 0
    setVolume(targetVol)
  }

  override fun setRandom(enable: Boolean) {
    scope.launch {
      try {
        sendCommand("random ${if (enable) 1 else 0}")
        _status.value = _status.value.copy(random = enable)
      } catch (e: Exception) {
        Log.e(TAG, "random command error", e)
      }
    }
  }

  override fun setRepeat(enable: Boolean) {
    scope.launch {
      try {
        sendCommand("repeat ${if (enable) 1 else 0}")
        _status.value = _status.value.copy(repeat = enable)
      } catch (e: Exception) {
        Log.e(TAG, "repeat command error", e)
      }
    }
  }

  override fun setSingle(enable: Boolean) {
    scope.launch {
      try {
        sendCommand("single ${if (enable) 1 else 0}")
        _status.value = _status.value.copy(single = enable)
      } catch (e: Exception) {
        Log.e(TAG, "single command error", e)
      }
    }
  }

  override fun setConsume(enable: Boolean) {
    scope.launch {
      try {
        sendCommand("consume ${if (enable) 1 else 0}")
        _status.value = _status.value.copy(consume = enable)
      } catch (e: Exception) {
        Log.e(TAG, "consume command error", e)
      }
    }
  }

  override fun deleteFromQueue(positions: List<Int>) {
    scope.launch {
      try {
        val sortedDesc = positions.sortedDescending()
        for (pos in sortedDesc) {
          sendCommand("delete $pos")
        }
        refreshQueue()
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "delete command error", e)
      }
    }
  }

  override fun moveInQueue(fromPos: Int, toPos: Int) {
    scope.launch {
      try {
        sendCommand("move $fromPos $toPos")
        refreshQueue()
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "move command error", e)
      }
    }
  }

  override fun queueNext(positions: List<Int>) {
    scope.launch {
      try {
        val curPos = _status.value.songPos
        var targetPos = curPos + 1
        val sorted = positions.sorted()
        for (pos in sorted) {
          sendCommand("move $pos $targetPos")
          targetPos++
        }
        refreshQueue()
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "queueNext command error", e)
      }
    }
  }

  override fun clearQueue() {
    scope.launch {
      try {
        sendCommand("clear")
        refreshQueue()
        refreshStatus()
      } catch (e: Exception) {
        Log.e(TAG, "clear command error", e)
      }
    }
  }

  override fun saveQueue(playlistName: String) {
    scope.launch {
      try {
        sendCommand("save \"${escapeMpd(playlistName)}\"")
      } catch (e: Exception) {
        Log.e(TAG, "save playlist command error", e)
      }
    }
  }

  override suspend fun listDirectory(path: String): List<LibraryEntry> = withContext(Dispatchers.IO) {
    try {
      val param = if (path.isBlank()) "\"\"" else "\"${escapeMpd(path)}\""
      val lines = sendCommand("lsinfo $param")
      val entries = mutableListOf<LibraryEntry>()
      var currentSongLines = mutableListOf<String>()

      for (line in lines) {
        if (line.startsWith("directory: ")) {
          val dirPath = line.substringAfter("directory: ").trim()
          val dirName = dirPath.substringAfterLast('/')
          entries.add(LibraryFolder(path = dirPath, name = dirName))
        } else if (line.startsWith("file: ")) {
          if (currentSongLines.isNotEmpty()) {
            parseSong(currentSongLines)?.let {
              entries.add(LibraryTrack(path = it.file, name = it.title, song = it))
            }
            currentSongLines.clear()
          }
          currentSongLines.add(line)
        } else if (currentSongLines.isNotEmpty()) {
          currentSongLines.add(line)
        }
      }

      if (currentSongLines.isNotEmpty()) {
        parseSong(currentSongLines)?.let {
          entries.add(LibraryTrack(path = it.file, name = it.title, song = it))
        }
      }

      entries
    } catch (e: Exception) {
      Log.e(TAG, "listDirectory error", e)
      emptyList()
    }
  }

  override fun enqueue(uri: String) {
    scope.launch {
      try {
        sendCommand("add \"${escapeMpd(uri)}\"")
        refreshQueue()
      } catch (e: Exception) {
        Log.e(TAG, "add command error", e)
      }
    }
  }

  override fun playUri(uri: String) {
    scope.launch {
      try {
        sendCommand("add \"${escapeMpd(uri)}\"")
        refreshQueue()
        val queueSize = _queue.value.size
        if (queueSize > 0) {
          sendCommand("play ${queueSize - 1}")
          refreshStatus()
        }
      } catch (e: Exception) {
        Log.e(TAG, "playUri command error", e)
      }
    }
  }

  override fun enqueueDirectory(dirUri: String) {
    enqueue(dirUri)
  }

  override fun updateDatabase() {
    scope.launch {
      try {
        sendCommand("update")
        _status.value = _status.value.copy(isUpdatingDb = true)
      } catch (e: Exception) {
        Log.e(TAG, "update command error", e)
      }
    }
  }

  override suspend fun ping(): Long = withContext(Dispatchers.IO) {
    val start = System.currentTimeMillis()
    sendCommand("ping")
    System.currentTimeMillis() - start
  }
}
