package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.configureAppCheck
import com.example.ui.components.BatchTagEditorDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SingleTagEditorDialog
import com.example.ui.components.SproutAiSheet
import com.example.ui.components.StickyBottomPlayer
import com.example.ui.components.TopBar
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.theme.MochaBase
import com.example.ui.theme.MpdfTheme
import com.example.ui.viewmodel.MpdViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check with launch intent before UI/ViewModels
    configureAppCheck(this, intent)

    enableEdgeToEdge()

    setContent {
      MpdfTheme {
        MpdfApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpdfApp(
  viewModel: MpdViewModel = viewModel()
) {
  val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
  val status by viewModel.status.collectAsStateWithLifecycle()
  val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
  val queue by viewModel.queue.collectAsStateWithLifecycle()
  val rawQueue by viewModel.rawQueue.collectAsStateWithLifecycle()
  val favorites by viewModel.favorites.collectAsStateWithLifecycle()
  val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedPositions by viewModel.selectedPositions.collectAsStateWithLifecycle()
  val libraryEntries by viewModel.libraryEntries.collectAsStateWithLifecycle()
  val currentLibraryPath by viewModel.currentLibraryPath.collectAsStateWithLifecycle()
  val isLibraryLoading by viewModel.isLibraryLoading.collectAsStateWithLifecycle()
  val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
  val strings by viewModel.strings.collectAsStateWithLifecycle()
  val editingSong by viewModel.editingSong.collectAsStateWithLifecycle()
  val isBatchTagEditorOpen by viewModel.isBatchTagEditorOpen.collectAsStateWithLifecycle()
  val aiCuratorState by viewModel.aiCuratorState.collectAsStateWithLifecycle()
  val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showAiCuratorSheet by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Show Snackbar on messages
  LaunchedEffect(snackbarMsg) {
    snackbarMsg?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissSnackbar()
    }
  }

  // Handle Android Back Navigation
  BackHandler(enabled = selectedPositions.isNotEmpty() || (activeTab == 1 && currentLibraryPath.isNotBlank()) || activeTab != 0) {
    if (selectedPositions.isNotEmpty()) {
      viewModel.clearSelection()
    } else if (activeTab == 1 && currentLibraryPath.isNotBlank()) {
      viewModel.navigateUpLibrary()
    } else if (activeTab != 0) {
      viewModel.activeTab.value = 0
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MochaBase,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopBar(
        connectionState = connectionState,
        isUpdatingDb = status.isUpdatingDb,
        queueCount = rawQueue.size,
        activeTab = activeTab,
        searchQuery = searchQuery,
        strings = strings,
        onTabSelected = { viewModel.activeTab.value = it },
        onSearchQueryChanged = { viewModel.searchQuery.value = it },
        onUpdateDbClicked = { viewModel.triggerDatabaseUpdate() },
        onAiCuratorClicked = { showAiCuratorSheet = true },
        onSettingsClicked = { showSettingsDialog = true },
        modifier = Modifier.statusBarsPadding()
      )
    },
    bottomBar = {
      val isFav = currentSong?.isFavorite == true ||
          (currentSong != null && (favoriteIds.contains(currentSong?.id) || favoriteIds.contains(currentSong?.file)))

      StickyBottomPlayer(
        status = status,
        currentSong = currentSong,
        selectedCount = selectedPositions.size,
        strings = strings,
        onPlayPauseClicked = { viewModel.togglePlayPause() },
        onStopClicked = { viewModel.stop() },
        onNextClicked = { viewModel.next() },
        onPreviousClicked = { viewModel.previous() },
        onSeekTo = { viewModel.seekTo(it) },
        onVolumeChanged = { viewModel.setVolume(it) },
        onToggleMute = { viewModel.toggleMute() },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onToggleRepeat = { viewModel.toggleRepeat() },
        onToggleSingle = { viewModel.toggleSingle() },
        onToggleConsume = { viewModel.toggleConsume() },
        onMoveUpSelected = { viewModel.moveUpSelected() },
        onMoveDownSelected = { viewModel.moveDownSelected() },
        onQueueNextSelected = { viewModel.queueNextSelected() },
        onDeleteSelected = { viewModel.deleteSelected() },
        onOpenBatchTagEditor = { viewModel.openBatchTagEditor() },
        onClearSelection = { viewModel.clearSelection() },
        isCurrentSongFavorite = isFav,
        onToggleFavorite = { viewModel.toggleFavorite(it) }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (activeTab) {
        0 -> {
          QueueScreen(
            queue = queue,
            currentSongId = currentSong?.id,
            selectedPositions = selectedPositions,
            strings = strings,
            onSongClicked = { viewModel.play(it) },
            onToggleSelection = { viewModel.toggleSelection(it) },
            onSelectAll = { viewModel.selectAll() },
            onClearSelection = { viewModel.clearSelection() },
            onQueueNextSelected = { viewModel.queueNextSelected() },
            onClearQueue = { viewModel.clearQueue() },
            onSaveQueue = { viewModel.saveQueue(it) },
            onEditSongTags = { viewModel.openTagEditorForSong(it) },
            onExploreLibraryClicked = { viewModel.activeTab.value = 1 }
          )
        }
        1 -> {
          LibraryScreen(
            currentPath = currentLibraryPath,
            entries = libraryEntries,
            isLoading = isLibraryLoading,
            onFolderClicked = { viewModel.loadLibrary(it.path) },
            onNavigateTo = { viewModel.loadLibrary(it) },
            onEnqueueTrack = { viewModel.enqueueTrack(it) },
            onPlayTrackNow = { viewModel.playTrackNow(it) },
            onEnqueueFolder = { viewModel.enqueueFolder(it) }
          )
        }
        2 -> {
          FavoritesScreen(
            favorites = favorites,
            currentSongId = currentSong?.id,
            strings = strings,
            onSongClicked = { viewModel.playFavorite(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onEnqueueAllFavorites = { viewModel.enqueueAllFavorites() },
            onNavigateToQueue = { viewModel.activeTab.value = 0 }
          )
        }
      }
    }
  }

  // Settings Dialog Modal (with English / Türkçe language switcher)
  if (showSettingsDialog) {
    SettingsDialog(
      initialConfig = serverConfig,
      strings = strings,
      onDismiss = { showSettingsDialog = false },
      onSave = { viewModel.updateSettings(it) },
      onTestPing = { viewModel.testPing() }
    )
  }

  // Single Song Tag Editor Dialog (with AI auto-tag & album art)
  editingSong?.let { targetSong ->
    SingleTagEditorDialog(
      song = targetSong,
      strings = strings,
      onDismiss = { viewModel.closeTagEditor() },
      onSave = { viewModel.updateSongTags(it) },
      onSuggestAiTags = { viewModel.suggestAiTags(it) }
    )
  }

  // Batch Tag Editor Dialog (with AI batch harmonization & album art)
  if (isBatchTagEditorOpen) {
    val selectedSongs = selectedPositions.mapNotNull { rawQueue.getOrNull(it) }
    BatchTagEditorDialog(
      selectedSongs = selectedSongs,
      strings = strings,
      onDismiss = { viewModel.closeBatchTagEditor() },
      onSaveBatch = { artist, album, genre, year, albumArtUrl ->
        viewModel.updateBatchTags(artist, album, genre, year, albumArtUrl)
      },
      onSuggestAiBatchTags = { viewModel.suggestAiBatchTags(it) }
    )
  }

  // Sprout AI Curator Sheet
  if (showAiCuratorSheet) {
    SproutAiSheet(
      sheetState = sheetState,
      uiState = aiCuratorState,
      onCurateRequested = { viewModel.curateMusic(it) },
      onEnqueueAllRecommendations = { viewModel.enqueueCuratorRecommendations(it) },
      onDismiss = { showAiCuratorSheet = false }
    )
  }
}
