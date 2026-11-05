package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Song
import com.example.ui.theme.MochaBase
import com.example.ui.theme.MochaCrust
import com.example.ui.theme.MochaGreen
import com.example.ui.theme.MochaMantle
import com.example.ui.theme.MochaMauve
import com.example.ui.theme.MochaPeach
import com.example.ui.theme.MochaRed
import com.example.ui.theme.MochaSubtext0
import com.example.ui.theme.MochaSurface0
import com.example.ui.theme.MochaSurface1
import com.example.ui.theme.MochaText
import com.example.util.AppStrings

@Composable
fun QueueScreen(
  queue: List<Song>,
  currentSongId: String?,
  selectedPositions: Set<Int>,
  strings: AppStrings,
  onSongClicked: (Int) -> Unit,
  onToggleSelection: (Int) -> Unit,
  onSelectAll: () -> Unit,
  onClearSelection: () -> Unit,
  onQueueNextSelected: () -> Unit,
  onClearQueue: () -> Unit,
  onSaveQueue: (String) -> Unit,
  onEditSongTags: (Song) -> Unit,
  onExploreLibraryClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showClearConfirmDialog by remember { mutableStateOf(false) }
  var showSavePlaylistDialog by remember { mutableStateOf(false) }
  var playlistNameInput by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MochaBase)
  ) {
    // Quick Actions Bar: Total Count, Icon-only Select All, Save, Queue Next, Trash Can Clear
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = if (selectedPositions.isNotEmpty()) "${selectedPositions.size} ${strings.selectedCount}" else "${queue.size} ${strings.tracksInQueue}",
        style = MaterialTheme.typography.bodyMedium,
        color = if (selectedPositions.isNotEmpty()) MochaPeach else MochaSubtext0,
        fontWeight = if (selectedPositions.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (queue.isNotEmpty()) {
          // Add Selected Behind Currently Playing Button (çalanın arkasına ekle)
          if (selectedPositions.isNotEmpty()) {
            IconButton(
              onClick = onQueueNextSelected,
              modifier = Modifier.size(36.dp).testTag("button_queue_next_header")
            ) {
              Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = strings.queueAfterPlaying,
                tint = MochaGreen,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(2.dp))
          }

          // Select All / Deselect (Icon Only)
          IconButton(
            onClick = {
              if (selectedPositions.size == queue.size) onClearSelection()
              else onSelectAll()
            },
            modifier = Modifier.size(36.dp).testTag("button_select_all")
          ) {
            Icon(
              imageVector = Icons.Default.SelectAll,
              contentDescription = if (selectedPositions.size == queue.size) strings.deselect else strings.selectAll,
              tint = if (selectedPositions.size == queue.size) MochaPeach else MochaMauve,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(2.dp))

          // Save Playlist (Icon Only)
          IconButton(
            onClick = {
              playlistNameInput = "Ghibli Favorites"
              showSavePlaylistDialog = true
            },
            modifier = Modifier.size(36.dp).testTag("button_save_playlist")
          ) {
            Icon(
              imageVector = Icons.Default.BookmarkBorder,
              contentDescription = strings.save,
              tint = MochaGreen,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(2.dp))

          // Clear Queue (Trash Bin Icon Only)
          IconButton(
            onClick = { showClearConfirmDialog = true },
            modifier = Modifier.size(36.dp).testTag("button_clear_queue")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = strings.clear,
              tint = MochaRed,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    if (queue.isEmpty()) {
      // Empty Queue State
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(80.dp)
              .clip(CircleShape)
              .background(MochaSurface0),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "🌱", fontSize = 38.sp)
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = strings.emptyQueueTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MochaText,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = strings.emptyQueueSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MochaSubtext0,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = onExploreLibraryClicked,
            colors = ButtonDefaults.buttonColors(containerColor = MochaMauve, contentColor = MochaCrust),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("button_empty_explore")
          ) {
            Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.browseLibrary)
          }
        }
      }
    } else {
      // Lazy List of Queue Items (single click selects, double click plays, no quality badge)
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .testTag("queue_track_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        itemsIndexed(queue, key = { _, song -> song.id }) { _, song ->
          val isCurrentSong = song.id == currentSongId
          val isSelected = selectedPositions.contains(song.pos)

          QueueTrackCard(
            song = song,
            isCurrent = isCurrentSong,
            isSelected = isSelected,
            onClick = { onToggleSelection(song.pos) },
            onDoubleClick = { onSongClicked(song.pos) },
            onEditTags = { onEditSongTags(song) },
            modifier = Modifier.testTag("queue_item_${song.pos}")
          )
        }
      }
    }
  }

  // Clear Confirmation Dialog
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text(text = strings.clearQueueTitle, color = MochaText) },
      text = {
        Text(
          text = strings.clearQueueConfirm,
          color = MochaSubtext0
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onClearQueue()
            showClearConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = MochaRed, contentColor = MochaCrust)
        ) {
          Text(strings.clearAll)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text(strings.cancel, color = MochaSubtext0)
        }
      },
      containerColor = MochaMantle,
      shape = RoundedCornerShape(20.dp)
    )
  }

  // Save Playlist Dialog
  if (showSavePlaylistDialog) {
    AlertDialog(
      onDismissRequest = { showSavePlaylistDialog = false },
      title = { Text(text = strings.saveQueueTitle, color = MochaText) },
      text = {
        Column {
          Text(text = strings.enterPlaylistName, color = MochaSubtext0)
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = playlistNameInput,
            onValueChange = { playlistNameInput = it },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("input_playlist_name")
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (playlistNameInput.isNotBlank()) {
              onSaveQueue(playlistNameInput.trim())
              showSavePlaylistDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MochaPeach, contentColor = MochaCrust),
          modifier = Modifier.testTag("button_confirm_save_playlist")
        ) {
          Text(strings.save)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSavePlaylistDialog = false }) {
          Text(strings.cancel, color = MochaSubtext0)
        }
      },
      containerColor = MochaMantle,
      shape = RoundedCornerShape(20.dp)
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueTrackCard(
  song: Song,
  isCurrent: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit,
  onDoubleClick: () -> Unit,
  onEditTags: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Distinctive color for selected items: vibrant Catppuccin Mauve tint
  // Playing track: warm Catppuccin Peach tint
  // Normal: MochaMantle
  val backgroundColor by animateColorAsState(
    targetValue = when {
      isSelected && isCurrent -> MochaMauve.copy(alpha = 0.32f)
      isSelected -> MochaMauve.copy(alpha = 0.24f)
      isCurrent -> MochaPeach.copy(alpha = 0.18f)
      else -> MochaMantle
    },
    label = "trackBgColor"
  )

  val borderColor = when {
    isSelected -> MochaMauve
    isCurrent -> MochaPeach
    else -> MochaSurface0
  }

  val borderWidth = if (isSelected || isCurrent) 1.5.dp else 1.dp

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
      .combinedClickable(
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        onLongClick = onClick
      ),
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    shape = RoundedCornerShape(14.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Sleek Status Circle (Checkmark when selected, Equalizer when playing, Number otherwise)
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(
            when {
              isSelected -> MochaMauve
              isCurrent -> MochaPeach.copy(alpha = 0.3f)
              else -> MochaSurface0
            }
          ),
        contentAlignment = Alignment.Center
      ) {
        if (isSelected) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = MochaCrust,
            modifier = Modifier.size(18.dp)
          )
        } else if (isCurrent) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Currently Playing",
            tint = MochaPeach,
            modifier = Modifier.size(18.dp)
          )
        } else {
          Text(
            text = (song.pos + 1).toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MochaSubtext0
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Song Metadata: Title, Artist, Album
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = song.title,
          style = MaterialTheme.typography.titleMedium,
          color = when {
            isSelected -> MochaMauve
            isCurrent -> MochaPeach
            else -> MochaText
          },
          fontWeight = if (isCurrent || isSelected) FontWeight.Bold else FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${song.artist} • ${song.album}",
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MochaText.copy(alpha = 0.85f) else MochaSubtext0,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Duration (Quality badge is removed per user request)
      Text(
        text = song.formattedDuration,
        style = MaterialTheme.typography.labelSmall,
        color = MochaSubtext0
      )

      Spacer(modifier = Modifier.width(4.dp))

      // Tag Editor Pencil Button for direct single-track editing
      IconButton(
        onClick = onEditTags,
        modifier = Modifier.size(32.dp).testTag("button_edit_tag_${song.id}")
      ) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Edit Tags",
          tint = if (isSelected) MochaMauve else MochaSubtext0,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
