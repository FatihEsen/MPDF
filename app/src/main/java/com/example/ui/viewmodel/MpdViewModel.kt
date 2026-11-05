package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiTagEditor
import com.example.data.ai.SproutAiCurator
import com.example.data.ai.TagSuggestion
import com.example.data.audio.StreamAudioPlayer
import com.example.data.mpd.DemoMpdClient
import com.example.data.mpd.MpdClient
import com.example.data.mpd.TcpMpdClient
import com.example.data.preferences.SettingsRepository
import com.example.model.ConnectionState
import com.example.model.CuratorRecommendation
import com.example.model.CuratorResponse
import com.example.model.LibraryEntry
import com.example.model.LibraryFolder
import com.example.model.MpdStatus
import com.example.model.PlaybackState
import com.example.model.ServerConfig
import com.example.model.Song
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Localization
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AiCuratorUiState {
  data object Idle : AiCuratorUiState
  data object Thinking : AiCuratorUiState
  data class Success(val response: CuratorResponse) : AiCuratorUiState
  data class Error(val message: String) : AiCuratorUiState
}

class MpdViewModel(application: Application) : AndroidViewModel(application) {

  private val settingsRepo = SettingsRepository(application)
  val serverConfig: StateFlow<ServerConfig> = settingsRepo.config

  val strings: StateFlow<AppStrings> = serverConfig
    .map { Localization.get(it.language) }
    .stateIn(viewModelScope, SharingStarted.Eagerly, Localization.get(serverConfig.value.language))

  private val streamPlayer = StreamAudioPlayer(application)
  val streamState = streamPlayer.streamState

  private val demoClient = DemoMpdClient(viewModelScope)
  private val tcpClient = TcpMpdClient(viewModelScope)

  // Active MPD client delegate
  private var activeClient: MpdClient = if (serverConfig.value.isDemoMode) demoClient else tcpClient

  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _status = MutableStateFlow(MpdStatus())
  val status: StateFlow<MpdStatus> = _status.asStateFlow()

  private val _currentSong = MutableStateFlow<Song?>(null)
  val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

  private val _rawQueue = MutableStateFlow<List<Song>>(emptyList())
  val rawQueue: StateFlow<List<Song>> = _rawQueue.asStateFlow()

  // Favorites & Preferences
  val favoriteIds: StateFlow<Set<String>> = settingsRepo.favoriteIds
  private val _savedFavoriteSongs = MutableStateFlow<Map<String, Song>>(emptyMap())

  // Tab & Search state
  val activeTab = MutableStateFlow(0) // 0: Queue, 1: Library, 2: Favorites
  val searchQuery = MutableStateFlow("")

  // Filtered queue based on search
  val queue: StateFlow<List<Song>> = combine(_rawQueue, searchQuery, favoriteIds) { q, query, favIds ->
    val mapped = q.map { it.copy(isFavorite = favIds.contains(it.id) || favIds.contains(it.file)) }
    if (query.isBlank()) mapped
    else mapped.filter {
      it.title.contains(query, ignoreCase = true) ||
          it.artist.contains(query, ignoreCase = true) ||
          it.album.contains(query, ignoreCase = true)
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Filtered favorites based on search
  val favorites: StateFlow<List<Song>> = combine(_savedFavoriteSongs, _rawQueue, favoriteIds, searchQuery) { savedMap, q, favIds, query ->
    val pool = mutableMapOf<String, Song>()
    // Add saved favorite songs
    for ((k, s) in savedMap) {
      if (favIds.contains(k) || favIds.contains(s.file) || favIds.contains(s.id)) {
        pool[s.id] = s.copy(isFavorite = true)
      }
    }
    // Add any queued songs that are favorited
    for (s in q) {
      if (favIds.contains(s.id) || favIds.contains(s.file)) {
        pool[s.id] = s.copy(isFavorite = true)
      }
    }
    val list = pool.values.toList()
    if (query.isBlank()) list
    else list.filter {
      it.title.contains(query, ignoreCase = true) ||
          it.artist.contains(query, ignoreCase = true) ||
          it.album.contains(query, ignoreCase = true)
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Queue Multi-selection (positions in queue)
  val selectedPositions = MutableStateFlow<Set<Int>>(emptySet())

  // Tag Editor state
  val editingSong = MutableStateFlow<Song?>(null)
  val isBatchTagEditorOpen = MutableStateFlow(false)
  private val aiTagEditor = AiTagEditor()

  // Library state
  val currentLibraryPath = MutableStateFlow("")
  val libraryEntries = MutableStateFlow<List<LibraryEntry>>(emptyList())
  val isLibraryLoading = MutableStateFlow(false)

  // Sprout AI Curator
  private val sproutCurator = SproutAiCurator()
  private val _aiCuratorState = MutableStateFlow<AiCuratorUiState>(AiCuratorUiState.Idle)
  val aiCuratorState: StateFlow<AiCuratorUiState> = _aiCuratorState.asStateFlow()

  // Feedback Snackbar Message
  val snackbarMessage = MutableStateFlow<String?>(null)

  init {
    switchClient(serverConfig.value.isDemoMode)
    connectCurrent()
    loadLibrary("")

    // Synchronize local audio stream with playback status if enabled
    viewModelScope.launch {
      status.collect { s ->
        val cfg = serverConfig.value
        if (cfg.enableLocalAudioStream && cfg.streamUrl.isNotBlank()) {
          if (s.state == PlaybackState.PLAY) {
            streamPlayer.resume()
          } else if (s.state == PlaybackState.PAUSE) {
            streamPlayer.pause()
          } else {
            streamPlayer.stop()
          }
        }
      }
    }
  }

  private fun switchClient(isDemo: Boolean) {
    activeClient.disconnect()
    activeClient = if (isDemo) demoClient else tcpClient

    viewModelScope.launch {
      activeClient.connectionState.collect { _connectionState.value = it }
    }
    viewModelScope.launch {
      activeClient.status.collect { _status.value = it }
    }
    viewModelScope.launch {
      activeClient.currentSong.collect { _currentSong.value = it }
    }
    viewModelScope.launch {
      activeClient.queue.collect { _rawQueue.value = it }
    }
  }

  fun connectCurrent() {
    activeClient.connect(serverConfig.value)
  }

  fun updateSettings(newConfig: ServerConfig) {
    val previousMode = serverConfig.value.isDemoMode
    settingsRepo.updateConfig(newConfig)

    if (newConfig.isDemoMode != previousMode) {
      switchClient(newConfig.isDemoMode)
    }
    connectCurrent()

    if (newConfig.enableLocalAudioStream && newConfig.streamUrl.isNotBlank()) {
      streamPlayer.startStream(newConfig.streamUrl)
    } else {
      streamPlayer.stop()
    }

    loadLibrary(currentLibraryPath.value)
    showSnackbar(if (newConfig.language == AppLanguage.TURKISH) "Ayarlar güncellendi" else "Settings updated successfully")
  }

  fun setLanguage(lang: AppLanguage) {
    val updated = serverConfig.value.copy(language = lang)
    settingsRepo.updateConfig(updated)
  }

  suspend fun testPing(): Long {
    return activeClient.ping()
  }

  // Transport Controls
  fun play(pos: Int? = null) {
    activeClient.play(pos)
  }

  fun pause() {
    activeClient.pause()
  }

  fun togglePlayPause() {
    val curState = _status.value.state
    if (curState == PlaybackState.PLAY) {
      activeClient.pause(true)
    } else {
      activeClient.play()
    }
  }

  fun stop() {
    activeClient.stop()
  }

  fun next() {
    activeClient.next()
  }

  fun previous() {
    activeClient.previous()
  }

  fun seekTo(seconds: Long) {
    activeClient.seek(seconds)
  }

  fun setVolume(vol: Int) {
    activeClient.setVolume(vol)
  }

  fun toggleMute() {
    activeClient.toggleMute()
  }

  fun toggleShuffle() {
    activeClient.setRandom(!_status.value.random)
  }

  fun toggleRepeat() {
    activeClient.setRepeat(!_status.value.repeat)
  }

  fun toggleSingle() {
    activeClient.setSingle(!_status.value.single)
  }

  fun toggleConsume() {
    activeClient.setConsume(!_status.value.consume)
  }

  // Queue Multi-selection & Batch Actions
  fun toggleSelection(pos: Int) {
    val cur = selectedPositions.value.toMutableSet()
    if (cur.contains(pos)) cur.remove(pos) else cur.add(pos)
    selectedPositions.value = cur
  }

  fun clearSelection() {
    selectedPositions.value = emptySet()
  }

  fun selectAll() {
    selectedPositions.value = _rawQueue.value.indices.toSet()
  }

  fun moveUpSelected() {
    val selected = selectedPositions.value.sorted()
    if (selected.isEmpty() || selected.first() == 0) return

    val newPositions = mutableSetOf<Int>()
    for (pos in selected) {
      activeClient.moveInQueue(pos, pos - 1)
      newPositions.add(pos - 1)
    }
    selectedPositions.value = newPositions
    showSnackbar("${selected.size} ${strings.value.selectedCount}")
  }

  fun moveDownSelected() {
    val selected = selectedPositions.value.sortedDescending()
    val queueSize = _rawQueue.value.size
    if (selected.isEmpty() || selected.first() >= queueSize - 1) return

    val newPositions = mutableSetOf<Int>()
    for (pos in selected) {
      activeClient.moveInQueue(pos, pos + 1)
      newPositions.add(pos + 1)
    }
    selectedPositions.value = newPositions
    showSnackbar("${selected.size} ${strings.value.selectedCount}")
  }

  fun queueNextSelected() {
    val selected = selectedPositions.value.toList()
    if (selected.isEmpty()) return
    activeClient.queueNext(selected)
    clearSelection()
    showSnackbar(strings.value.queueAfterPlaying)
  }

  fun deleteSelected() {
    val selected = selectedPositions.value.toList()
    if (selected.isEmpty()) return
    activeClient.deleteFromQueue(selected)
    clearSelection()
    showSnackbar("${selected.size} ${strings.value.selectedCount}")
  }

  fun clearQueue() {
    activeClient.clearQueue()
    clearSelection()
    showSnackbar(strings.value.clear)
  }

  fun saveQueue(playlistName: String) {
    if (playlistName.isNotBlank()) {
      activeClient.saveQueue(playlistName)
      showSnackbar("Saved playlist '$playlistName'")
    }
  }

  // Favorites Operations
  fun toggleFavorite(song: Song) {
    val isNowFav = settingsRepo.toggleFavorite(song.id)
    val map = _savedFavoriteSongs.value.toMutableMap()
    if (isNowFav) {
      map[song.id] = song.copy(isFavorite = true)
    } else {
      map.remove(song.id)
      map.remove(song.file)
    }
    _savedFavoriteSongs.value = map

    // Update rawQueue
    val updatedQueue = _rawQueue.value.map {
      if (it.id == song.id || it.file == song.file) it.copy(isFavorite = isNowFav) else it
    }
    _rawQueue.value = updatedQueue

    // Update currentSong
    if (_currentSong.value?.id == song.id || _currentSong.value?.file == song.file) {
      _currentSong.value = _currentSong.value?.copy(isFavorite = isNowFav)
    }

    val isTurkish = serverConfig.value.language == AppLanguage.TURKISH
    val msg = if (isNowFav) {
      if (isTurkish) "Favorilere eklendi: ${song.title}" else "Added to favorites: ${song.title}"
    } else {
      if (isTurkish) "Favorilerden çıkarıldı: ${song.title}" else "Removed from favorites: ${song.title}"
    }
    showSnackbar(msg)
  }

  fun playFavorite(song: Song) {
    val currentQ = _rawQueue.value
    val existingIndex = currentQ.indexOfFirst { it.id == song.id || it.file == song.file }
    if (existingIndex >= 0) {
      play(existingIndex)
    } else {
      playTrackNow(song)
    }
  }

  fun enqueueAllFavorites() {
    val favs = favorites.value
    if (favs.isEmpty()) return
    for (f in favs) {
      activeClient.enqueue(f.file)
    }
    val isTurkish = serverConfig.value.language == AppLanguage.TURKISH
    showSnackbar(if (isTurkish) "${favs.size} favori parça sıraya eklendi" else "Enqueued ${favs.size} favorites into MPD")
  }

  // Tag Editor Operations
  fun openTagEditorForSong(song: Song) {
    editingSong.value = song
  }

  fun closeTagEditor() {
    editingSong.value = null
  }

  fun openBatchTagEditor() {
    if (selectedPositions.value.isNotEmpty()) {
      isBatchTagEditorOpen.value = true
    }
  }

  fun closeBatchTagEditor() {
    isBatchTagEditorOpen.value = false
  }

  suspend fun suggestAiTags(song: Song): TagSuggestion {
    return aiTagEditor.suggestSingleTag(song)
  }

  suspend fun suggestAiBatchTags(songs: List<Song>): List<TagSuggestion> {
    return aiTagEditor.suggestBatchTags(songs)
  }

  fun updateSongTags(updatedSong: Song) {
    val current = _rawQueue.value.toMutableList()
    val index = current.indexOfFirst { it.id == updatedSong.id }
    if (index >= 0) {
      current[index] = updatedSong
      _rawQueue.value = current
      if (_currentSong.value?.id == updatedSong.id) {
        _currentSong.value = updatedSong
      }
      showSnackbar(if (serverConfig.value.language == AppLanguage.TURKISH) "Etiketler kaydedildi" else "Tags saved successfully")
    }
    closeTagEditor()
  }

  fun updateBatchTags(
    artist: String,
    album: String,
    genre: String,
    year: String,
    albumArtUrl: String?
  ) {
    val selectedIdxs = selectedPositions.value
    val current = _rawQueue.value.toMutableList()
    for (idx in selectedIdxs) {
      if (idx in current.indices) {
        val s = current[idx]
        current[idx] = s.copy(
          artist = artist.ifBlank { s.artist },
          album = album.ifBlank { s.album },
          genre = genre.ifBlank { s.genre },
          year = year.ifBlank { s.year },
          albumArtUrl = albumArtUrl ?: s.albumArtUrl
        )
      }
    }
    _rawQueue.value = current
    val currentSongId = _currentSong.value?.id
    if (currentSongId != null) {
      current.find { it.id == currentSongId }?.let { _currentSong.value = it }
    }
    clearSelection()
    closeBatchTagEditor()
    showSnackbar(if (serverConfig.value.language == AppLanguage.TURKISH) "${selectedIdxs.size} parçanın etiketleri güncellendi" else "Updated tags for ${selectedIdxs.size} tracks")
  }

  // Library Navigation
  fun loadLibrary(path: String) {
    viewModelScope.launch {
      isLibraryLoading.value = true
      currentLibraryPath.value = path
      try {
        val entries = activeClient.listDirectory(path)
        libraryEntries.value = entries
      } catch (e: Exception) {
        libraryEntries.value = emptyList()
      } finally {
        isLibraryLoading.value = false
      }
    }
  }

  fun navigateUpLibrary() {
    val path = currentLibraryPath.value
    if (path.isBlank()) return
    val parent = if (path.contains('/')) path.substringBeforeLast('/') else ""
    loadLibrary(parent)
  }

  fun enqueueTrack(song: Song) {
    activeClient.enqueue(song.file)
    showSnackbar("Enqueued: ${song.title}")
  }

  fun playTrackNow(song: Song) {
    activeClient.playUri(song.file)
    showSnackbar("Playing: ${song.title}")
  }

  fun enqueueFolder(folder: LibraryFolder) {
    activeClient.enqueueDirectory(folder.path)
    showSnackbar("Added folder: ${folder.name}")
  }

  fun triggerDatabaseUpdate() {
    activeClient.updateDatabase()
    showSnackbar(if (serverConfig.value.language == AppLanguage.TURKISH) "Veritabanı taraması başlatıldı" else "Database update scan initiated")
  }

  // Sprout AI Curator
  fun curateMusic(userPrompt: String) {
    viewModelScope.launch {
      _aiCuratorState.value = AiCuratorUiState.Thinking
      try {
        val response = sproutCurator.curateMusic(
          userPrompt = userPrompt,
          currentQueue = _rawQueue.value,
          nowPlaying = _currentSong.value
        )
        _aiCuratorState.value = AiCuratorUiState.Success(response)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _aiCuratorState.value = AiCuratorUiState.Error(e.localizedMessage ?: "Curation failed")
      }
    }
  }

  fun resetCurator() {
    _aiCuratorState.value = AiCuratorUiState.Idle
  }

  fun enqueueCuratorRecommendations(recommendations: List<CuratorRecommendation>) {
    for (rec in recommendations) {
      val uri = "AI Curation/${rec.artist} - ${rec.title}.flac"
      activeClient.enqueue(uri)
    }
    showSnackbar(if (serverConfig.value.language == AppLanguage.TURKISH) "${recommendations.size} parça MPD sırasına eklendi" else "Enqueued ${recommendations.size} curated tracks into MPD")
  }

  fun showSnackbar(msg: String) {
    snackbarMessage.value = msg
  }

  fun dismissSnackbar() {
    snackbarMessage.value = null
  }

  override fun onCleared() {
    super.onCleared()
    streamPlayer.release()
    demoClient.disconnect()
    tcpClient.disconnect()
  }
}
