package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LibraryEntry
import com.example.model.LibraryFolder
import com.example.model.LibraryTrack
import com.example.model.Song
import com.example.ui.theme.MochaBase
import com.example.ui.theme.MochaCrust
import com.example.ui.theme.MochaGreen
import com.example.ui.theme.MochaMantle
import com.example.ui.theme.MochaMauve
import com.example.ui.theme.MochaPeach
import com.example.ui.theme.MochaSubtext0
import com.example.ui.theme.MochaSurface0
import com.example.ui.theme.MochaSurface1
import com.example.ui.theme.MochaText
import com.example.ui.theme.MochaYellow

@Composable
fun LibraryScreen(
  currentPath: String,
  entries: List<LibraryEntry>,
  isLoading: Boolean,
  onFolderClicked: (LibraryFolder) -> Unit,
  onNavigateTo: (String) -> Unit,
  onEnqueueTrack: (Song) -> Unit,
  onPlayTrackNow: (Song) -> Unit,
  onEnqueueFolder: (LibraryFolder) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MochaBase)
  ) {
    // Breadcrumb Trail Navigation Bar
    BreadcrumbsBar(
      currentPath = currentPath,
      onNavigateTo = onNavigateTo
    )

    if (isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = MochaPeach)
      }
    } else if (entries.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MochaSubtext0,
            modifier = Modifier.size(54.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Folder is Empty",
            style = MaterialTheme.typography.titleMedium,
            color = MochaText
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "No music files or subfolders found in this directory.",
            style = MaterialTheme.typography.bodySmall,
            color = MochaSubtext0
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .testTag("library_entry_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(entries, key = { it.path }) { entry ->
          when (entry) {
            is LibraryFolder -> {
              FolderItemCard(
                folder = entry,
                onClick = { onFolderClicked(entry) },
                onEnqueueFolder = { onEnqueueFolder(entry) }
              )
            }
            is LibraryTrack -> {
              TrackItemCard(
                track = entry,
                onEnqueue = { onEnqueueTrack(entry.song) },
                onPlayNow = { onPlayTrackNow(entry.song) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BreadcrumbsBar(
  currentPath: String,
  onNavigateTo: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val segments = if (currentPath.isBlank()) emptyList() else currentPath.split('/')
  val scrollState = rememberScrollState()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(MochaMantle)
      .horizontalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Root Segment
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .clickable { onNavigateTo("") }
        .padding(horizontal = 6.dp, vertical = 4.dp)
        .testTag("breadcrumb_root")
    ) {
      Icon(
        imageVector = Icons.Default.Home,
        contentDescription = "Library Root",
        tint = if (currentPath.isBlank()) MochaPeach else MochaSubtext0,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "Music",
        style = MaterialTheme.typography.labelLarge,
        color = if (currentPath.isBlank()) MochaPeach else MochaSubtext0,
        fontWeight = if (currentPath.isBlank()) FontWeight.Bold else FontWeight.Normal
      )
    }

    var accumulatedPath = ""
    for ((index, segment) in segments.withIndex()) {
      accumulatedPath = if (accumulatedPath.isBlank()) segment else "$accumulatedPath/$segment"
      val pathForClick = accumulatedPath
      val isLast = index == segments.size - 1

      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MochaSurface1,
        modifier = Modifier.size(16.dp)
      )

      Text(
        text = segment,
        style = MaterialTheme.typography.labelLarge,
        color = if (isLast) MochaPeach else MochaSubtext0,
        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable { onNavigateTo(pathForClick) }
          .padding(horizontal = 6.dp, vertical = 4.dp)
      )
    }
  }
}

@Composable
private fun FolderItemCard(
  folder: LibraryFolder,
  onClick: () -> Unit,
  onEnqueueFolder: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(1.dp, MochaSurface0, RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .testTag("folder_card_${folder.name}"),
    colors = CardDefaults.cardColors(containerColor = MochaMantle),
    shape = RoundedCornerShape(14.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(MochaMauve.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Folder,
          contentDescription = null,
          tint = MochaMauve,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.titleMedium,
          color = MochaText,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (folder.trackCount > 0) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${folder.trackCount} tracks",
            style = MaterialTheme.typography.bodySmall,
            color = MochaSubtext0
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Enqueue Entire Folder Action
      OutlinedButton(
        onClick = onEnqueueFolder,
        modifier = Modifier.height(34.dp).testTag("button_enqueue_folder_${folder.name}"),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp)
      ) {
        Icon(
          imageVector = Icons.Default.PlaylistAdd,
          contentDescription = "Add Folder to Queue",
          tint = MochaGreen,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add All", fontSize = 11.sp, color = MochaGreen)
      }
    }
  }
}

@Composable
private fun TrackItemCard(
  track: LibraryTrack,
  onEnqueue: () -> Unit,
  onPlayNow: () -> Unit,
  modifier: Modifier = Modifier
) {
  val song = track.song

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(1.dp, MochaSurface0, RoundedCornerShape(14.dp))
      .clickable(onClick = onPlayNow)
      .testTag("track_card_${song.title}"),
    colors = CardDefaults.cardColors(containerColor = MochaMantle),
    shape = RoundedCornerShape(14.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(MochaSurface0),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.MusicNote,
          contentDescription = null,
          tint = MochaPeach,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = song.title,
          style = MaterialTheme.typography.titleMedium,
          color = MochaText,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${song.artist} • ${song.formattedDuration}",
            style = MaterialTheme.typography.bodySmall,
            color = MochaSubtext0,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(MochaSurface0)
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text(
              text = song.format,
              fontSize = 8.sp,
              color = MochaGreen,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Direct Enqueue Button
      IconButton(
        onClick = onEnqueue,
        modifier = Modifier.size(34.dp).testTag("button_enqueue_${song.id}")
      ) {
        Icon(
          imageVector = Icons.Default.PlaylistAdd,
          contentDescription = "Enqueue Track",
          tint = MochaMauve,
          modifier = Modifier.size(20.dp)
        )
      }

      // Play Now Button
      IconButton(
        onClick = onPlayNow,
        modifier = Modifier.size(34.dp).testTag("button_play_now_${song.id}")
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Play Now",
          tint = MochaPeach,
          modifier = Modifier.size(22.dp)
        )
      }
    }
  }
}
