package com.example.data.mpd

import com.example.model.ConnectionState
import com.example.model.LibraryEntry
import com.example.model.LibraryFolder
import com.example.model.LibraryTrack
import com.example.model.MpdStatus
import com.example.model.PlaybackState
import com.example.model.ServerConfig
import com.example.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class DemoMpdClient(
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : MpdClient {

  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _status = MutableStateFlow(MpdStatus())
  override val status: StateFlow<MpdStatus> = _status.asStateFlow()

  private val _currentSong = MutableStateFlow<Song?>(null)
  override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

  private val _queue = MutableStateFlow<List<Song>>(emptyList())
  override val queue: StateFlow<List<Song>> = _queue.asStateFlow()

  private var playbackTickerJob: Job? = null
  private var lastVolumeBeforeMute: Int = 80

  // Master demo catalog
  private val masterCatalog: List<Song> = listOf(
    Song(
      id = "demo_1",
      file = "Studio Ghibli/Spirited Away/01 - One Summer's Day.flac",
      title = "One Summer's Day (あの夏へ)",
      artist = "Joe Hisaishi",
      album = "Spirited Away OST",
      track = 1,
      durationSec = 191,
      format = "FLAC 24-bit 96kHz",
      isFavorite = true
    ),
    Song(
      id = "demo_2",
      file = "Studio Ghibli/Totoro/02 - Path of the Wind.flac",
      title = "Path of the Wind (風のとおり道)",
      artist = "Joe Hisaishi",
      album = "My Neighbor Totoro OST",
      track = 2,
      durationSec = 224,
      format = "FLAC 24-bit 96kHz",
      isFavorite = true
    ),
    Song(
      id = "demo_3",
      file = "Studio Ghibli/Kiki/03 - A Town with an Ocean View.dsf",
      title = "A Town with an Ocean View (海の見える街)",
      artist = "Joe Hisaishi",
      album = "Kiki's Delivery Service OST",
      track = 3,
      durationSec = 180,
      format = "DSD 2.8MHz (DSD64)",
      isFavorite = false
    ),
    Song(
      id = "demo_4",
      file = "Studio Ghibli/Howl/04 - Merry-Go-Round of Life.flac",
      title = "Merry-Go-Round of Life (人生のメリーゴーランド)",
      artist = "Joe Hisaishi",
      album = "Howl's Moving Castle OST",
      track = 4,
      durationSec = 238,
      format = "FLAC 24-bit 192kHz",
      isFavorite = true
    ),
    Song(
      id = "demo_5",
      file = "Studio Ghibli/Mononoke/05 - The Legend of Ashitaka.flac",
      title = "The Legend of Ashitaka (アシタカせっ記)",
      artist = "Joe Hisaishi",
      album = "Princess Mononoke Symphonic Suite",
      track = 5,
      durationSec = 348,
      format = "FLAC 24-bit 96kHz",
      isFavorite = false
    ),
    Song(
      id = "demo_6",
      file = "Studio Ghibli/Porco Rosso/06 - The Bygone Days.mp3",
      title = "The Bygone Days (帰らざる日々)",
      artist = "Joe Hisaishi",
      album = "Porco Rosso OST",
      track = 6,
      durationSec = 212,
      format = "MP3 320kbps",
      isFavorite = false
    ),
    Song(
      id = "demo_7",
      file = "Studio Ghibli/Spirited Away/07 - Always With Me.flac",
      title = "Always With Me (いつも何度でも)",
      artist = "Youmi Kimura",
      album = "Spirited Away OST",
      track = 7,
      durationSec = 217,
      format = "FLAC 16-bit 44.1kHz",
      isFavorite = true
    ),
    Song(
      id = "demo_8",
      file = "Lo-Fi & Cozy Beats/Autumn Coffee/01 - Rain in Kamakura.flac",
      title = "Rain in Kamakura",
      artist = "Studio Sprout",
      album = "Autumn Coffee & Tea",
      track = 1,
      durationSec = 164,
      format = "FLAC 24-bit 48kHz",
      isFavorite = false
    ),
    Song(
      id = "demo_9",
      file = "Lo-Fi & Cozy Beats/Autumn Coffee/02 - Soot Sprite Grooves.flac",
      title = "Soot Sprite Grooves",
      artist = "Studio Sprout",
      album = "Autumn Coffee & Tea",
      track = 2,
      durationSec = 178,
      format = "FLAC 24-bit 48kHz",
      isFavorite = false
    ),
    Song(
      id = "demo_10",
      file = "Hi-Res Masters/Orchestral/01 - Summer Grasses & Wind.flac",
      title = "Summer Grasses & Wind",
      artist = "Tokyo Philharmonic Orchestra",
      album = "Ghibli in the Forest Live",
      track = 1,
      durationSec = 295,
      format = "FLAC 24-bit 96kHz",
      isFavorite = true
    )
  )

  init {
    initializeDefaultQueue()
  }

  private fun initializeDefaultQueue() {
    val initialQueue = masterCatalog.take(6).mapIndexed { index, song ->
      song.copy(pos = index)
    }
    _queue.value = initialQueue
    if (initialQueue.isNotEmpty()) {
      _currentSong.value = initialQueue[0]
      _status.value = _status.value.copy(
        songId = initialQueue[0].id,
        songPos = 0,
        totalSeconds = initialQueue[0].durationSec,
        elapsedSeconds = 0,
        state = PlaybackState.PAUSE
      )
    }
  }

  override fun connect(config: ServerConfig) {
    scope.launch {
      _connectionState.value = ConnectionState.Connecting("Demo Server (MPD 0.23)")
      delay(300)
      _connectionState.value = ConnectionState.Connected("Demo Server (MPD 0.23)", latencyMs = 6)
      startTicker()
    }
  }

  override fun disconnect() {
    _connectionState.value = ConnectionState.Disconnected
    stopTicker()
  }

  private fun startTicker() {
    playbackTickerJob?.cancel()
    playbackTickerJob = scope.launch {
      while (isActive) {
        delay(1000)
        val curStatus = _status.value
        if (curStatus.state == PlaybackState.PLAY) {
          val nextElapsed = curStatus.elapsedSeconds + 1
          if (nextElapsed >= curStatus.totalSeconds && curStatus.totalSeconds > 0) {
            handleSongEnded()
          } else {
            _status.value = curStatus.copy(elapsedSeconds = nextElapsed)
          }
        }
      }
    }
  }

  private fun stopTicker() {
    playbackTickerJob?.cancel()
    playbackTickerJob = null
  }

  private fun handleSongEnded() {
    val curStatus = _status.value
    val curQueue = _queue.value

    if (curStatus.consume && curStatus.songPos in curQueue.indices) {
      val mutable = curQueue.toMutableList()
      mutable.removeAt(curStatus.songPos)
      _queue.value = mutable.mapIndexed { idx, s -> s.copy(pos = idx) }
    }

    if (curStatus.single) {
      _status.value = curStatus.copy(elapsedSeconds = 0)
      return
    }

    next()
  }

  override fun play(pos: Int?) {
    val curQueue = _queue.value
    if (curQueue.isEmpty()) return

    val targetPos = pos ?: if (_status.value.songPos >= 0) _status.value.songPos else 0
    if (targetPos in curQueue.indices) {
      val targetSong = curQueue[targetPos]
      _currentSong.value = targetSong
      _status.value = _status.value.copy(
        state = PlaybackState.PLAY,
        songId = targetSong.id,
        songPos = targetPos,
        totalSeconds = targetSong.durationSec,
        elapsedSeconds = if (pos != null) 0 else _status.value.elapsedSeconds
      )
    }
  }

  override fun pause(pause: Boolean?) {
    val curStatus = _status.value
    val shouldPause = pause ?: (curStatus.state == PlaybackState.PLAY)
    _status.value = curStatus.copy(
      state = if (shouldPause) PlaybackState.PAUSE else PlaybackState.PLAY
    )
  }

  override fun stop() {
    _status.value = _status.value.copy(
      state = PlaybackState.STOP,
      elapsedSeconds = 0
    )
  }

  override fun next() {
    val curQueue = _queue.value
    if (curQueue.isEmpty()) return

    val curPos = _status.value.songPos
    val nextPos = if (_status.value.random) {
      if (curQueue.size > 1) {
        val candidates = curQueue.indices.filter { it != curPos }
        candidates.random()
      } else 0
    } else {
      if (curPos + 1 < curQueue.size) curPos + 1
      else if (_status.value.repeat) 0
      else {
        stop()
        return
      }
    }
    play(nextPos)
  }

  override fun previous() {
    val curQueue = _queue.value
    if (curQueue.isEmpty()) return

    val curStatus = _status.value
    if (curStatus.elapsedSeconds > 3) {
      _status.value = curStatus.copy(elapsedSeconds = 0)
      return
    }

    val prevPos = if (curStatus.songPos > 0) curStatus.songPos - 1 else if (curStatus.repeat) curQueue.size - 1 else 0
    play(prevPos)
  }

  override fun seek(seconds: Long) {
    val curStatus = _status.value
    val clamped = seconds.coerceIn(0L, curStatus.totalSeconds)
    _status.value = curStatus.copy(elapsedSeconds = clamped)
  }

  override fun setVolume(volume: Int) {
    val clamped = volume.coerceIn(0, 100)
    _status.value = _status.value.copy(volume = clamped, isMuted = clamped == 0)
    if (clamped > 0) {
      lastVolumeBeforeMute = clamped
    }
  }

  override fun toggleMute() {
    val cur = _status.value
    if (cur.isMuted || cur.volume == 0) {
      val restored = if (lastVolumeBeforeMute > 0) lastVolumeBeforeMute else 80
      _status.value = cur.copy(volume = restored, isMuted = false)
    } else {
      lastVolumeBeforeMute = cur.volume
      _status.value = cur.copy(volume = 0, isMuted = true)
    }
  }

  override fun setRandom(enable: Boolean) {
    _status.value = _status.value.copy(random = enable)
  }

  override fun setRepeat(enable: Boolean) {
    _status.value = _status.value.copy(repeat = enable)
  }

  override fun setSingle(enable: Boolean) {
    _status.value = _status.value.copy(single = enable)
  }

  override fun setConsume(enable: Boolean) {
    _status.value = _status.value.copy(consume = enable)
  }

  override fun deleteFromQueue(positions: List<Int>) {
    val current = _queue.value.toMutableList()
    val sortedDesc = positions.sortedDescending()
    for (pos in sortedDesc) {
      if (pos in current.indices) {
        current.removeAt(pos)
      }
    }
    val reindexed = current.mapIndexed { idx, s -> s.copy(pos = idx) }
    _queue.value = reindexed

    val curSongPos = _status.value.songPos
    if (curSongPos in positions) {
      if (reindexed.isNotEmpty()) {
        val newPos = curSongPos.coerceAtMost(reindexed.size - 1)
        play(newPos)
      } else {
        stop()
        _currentSong.value = null
        _status.value = _status.value.copy(songId = null, songPos = -1)
      }
    } else {
      val removedBefore = positions.count { it < curSongPos }
      val newPos = (curSongPos - removedBefore).coerceAtLeast(0)
      _status.value = _status.value.copy(songPos = newPos)
    }
  }

  override fun moveInQueue(fromPos: Int, toPos: Int) {
    val current = _queue.value.toMutableList()
    if (fromPos in current.indices && toPos in current.indices) {
      val item = current.removeAt(fromPos)
      current.add(toPos, item)
      val reindexed = current.mapIndexed { idx, s -> s.copy(pos = idx) }
      _queue.value = reindexed

      val curPos = _status.value.songPos
      val newCurPos = when (curPos) {
        fromPos -> toPos
        in (minOf(fromPos, toPos)..maxOf(fromPos, toPos)) -> {
          if (fromPos < toPos) curPos - 1 else curPos + 1
        }
        else -> curPos
      }
      _status.value = _status.value.copy(songPos = newCurPos)
    }
  }

  override fun queueNext(positions: List<Int>) {
    val curQueue = _queue.value
    if (curQueue.isEmpty()) return
    val curPos = _status.value.songPos
    val insertPos = (curPos + 1).coerceAtMost(curQueue.size)

    val selectedSongs = positions.sorted().mapNotNull { curQueue.getOrNull(it) }
    val remainingSongs = curQueue.filterIndexed { index, _ -> index !in positions }

    val mutable = remainingSongs.toMutableList()
    val adjustedInsertPos = insertPos.coerceAtMost(mutable.size)
    mutable.addAll(adjustedInsertPos, selectedSongs)

    val reindexed = mutable.mapIndexed { idx, s -> s.copy(pos = idx) }
    _queue.value = reindexed

    val newCurrentIndex = reindexed.indexOfFirst { it.id == _currentSong.value?.id }
    if (newCurrentIndex >= 0) {
      _status.value = _status.value.copy(songPos = newCurrentIndex)
    }
  }

  override fun clearQueue() {
    stop()
    _queue.value = emptyList()
    _currentSong.value = null
    _status.value = _status.value.copy(
      songId = null,
      songPos = -1,
      elapsedSeconds = 0,
      totalSeconds = 0
    )
  }

  override fun saveQueue(playlistName: String) {
    // In demo mode, simulate saving playlist
  }

  override suspend fun listDirectory(path: String): List<LibraryEntry> {
    delay(50) // simulate tiny directory listing latency
    val cleanPath = path.trim().trim('/')

    if (cleanPath.isEmpty()) {
      return listOf(
        LibraryFolder(path = "Studio Ghibli", name = "Studio Ghibli", trackCount = 7, subfolderCount = 5),
        LibraryFolder(path = "Lo-Fi & Cozy Beats", name = "Lo-Fi & Cozy Beats", trackCount = 2, subfolderCount = 1),
        LibraryFolder(path = "Hi-Res Masters", name = "Hi-Res Masters", trackCount = 1, subfolderCount = 1)
      )
    }

    if (cleanPath.equals("Studio Ghibli", ignoreCase = true)) {
      return listOf(
        LibraryFolder(path = "Studio Ghibli/Spirited Away", name = "Spirited Away (千と千尋の神隠し)", trackCount = 2),
        LibraryFolder(path = "Studio Ghibli/Totoro", name = "My Neighbor Totoro (となりのトトロ)", trackCount = 1),
        LibraryFolder(path = "Studio Ghibli/Howl", name = "Howl's Moving Castle (ハウルの動く城)", trackCount = 1),
        LibraryFolder(path = "Studio Ghibli/Kiki", name = "Kiki's Delivery Service (魔女の宅急便)", trackCount = 1),
        LibraryFolder(path = "Studio Ghibli/Mononoke", name = "Princess Mononoke (もののけ姫)", trackCount = 1),
        LibraryFolder(path = "Studio Ghibli/Porco Rosso", name = "Porco Rosso (紅の豚)", trackCount = 1)
      )
    }

    if (cleanPath.equals("Lo-Fi & Cozy Beats", ignoreCase = true)) {
      return listOf(
        LibraryFolder(path = "Lo-Fi & Cozy Beats/Autumn Coffee", name = "Autumn Coffee & Tea", trackCount = 2)
      )
    }

    if (cleanPath.equals("Hi-Res Masters", ignoreCase = true)) {
      return listOf(
        LibraryFolder(path = "Hi-Res Masters/Orchestral", name = "Orchestral Masters", trackCount = 1)
      )
    }

    // Matching track listings
    val matchingSongs = masterCatalog.filter { it.file.startsWith(cleanPath) }
    return matchingSongs.map { song ->
      LibraryTrack(
        path = song.file,
        name = song.title,
        song = song
      )
    }
  }

  override fun enqueue(uri: String) {
    val song = masterCatalog.find { it.file == uri } ?: Song(
      id = UUID.randomUUID().toString(),
      file = uri,
      title = uri.substringAfterLast('/'),
      artist = "Local Artist",
      album = "MPD Library",
      durationSec = 210,
      format = "FLAC 24-bit 96kHz"
    )
    val cur = _queue.value.toMutableList()
    val newSong = song.copy(
      id = UUID.randomUUID().toString(),
      pos = cur.size
    )
    cur.add(newSong)
    _queue.value = cur

    if (_currentSong.value == null) {
      _currentSong.value = newSong
      _status.value = _status.value.copy(
        songId = newSong.id,
        songPos = 0,
        totalSeconds = newSong.durationSec
      )
    }
  }

  override fun playUri(uri: String) {
    val cur = _queue.value.toMutableList()
    val song = masterCatalog.find { it.file == uri } ?: Song(
      id = UUID.randomUUID().toString(),
      file = uri,
      title = uri.substringAfterLast('/'),
      artist = "Local Artist",
      album = "MPD Library",
      durationSec = 210,
      format = "FLAC 24-bit 96kHz"
    )
    val newSong = song.copy(
      id = UUID.randomUUID().toString(),
      pos = cur.size
    )
    cur.add(newSong)
    _queue.value = cur
    play(cur.size - 1)
  }

  override fun enqueueDirectory(dirUri: String) {
    val matching = masterCatalog.filter { it.file.startsWith(dirUri) }
    val cur = _queue.value.toMutableList()
    for (m in matching) {
      cur.add(m.copy(id = UUID.randomUUID().toString(), pos = cur.size))
    }
    _queue.value = cur
    if (_currentSong.value == null && cur.isNotEmpty()) {
      _currentSong.value = cur[0]
      _status.value = _status.value.copy(
        songId = cur[0].id,
        songPos = 0,
        totalSeconds = cur[0].durationSec
      )
    }
  }

  override fun updateDatabase() {
    scope.launch {
      _status.value = _status.value.copy(isUpdatingDb = true)
      delay(2000)
      _status.value = _status.value.copy(isUpdatingDb = false)
    }
  }

  override suspend fun ping(): Long {
    delay(10)
    return 6L
  }
}
