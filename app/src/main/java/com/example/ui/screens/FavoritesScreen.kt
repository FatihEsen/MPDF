package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.util.AppStrings

@Composable
fun FavoritesScreen(
  favorites: List<Song>,
  currentSongId: String?,
  strings: AppStrings,
  onSongClicked: (Song) -> Unit,
  onToggleFavorite: (Song) -> Unit,
  onEnqueueAllFavorites: () -> Unit,
  onNavigateToQueue: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MochaBase)
  ) {
    // Header Bar: Total Count & "Enqueue All"
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "${favorites.size} ${strings.tabFavorites}",
        style = MaterialTheme.typography.bodyMedium,
        color = MochaSubtext0
      )

      if (favorites.isNotEmpty()) {
        Button(
          onClick = onEnqueueAllFavorites,
          colors = ButtonDefaults.buttonColors(containerColor = MochaPeach, contentColor = MochaCrust),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("button_enqueue_all_favorites")
        ) {
          Icon(
            imageVector = Icons.Default.PlaylistAdd,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = strings.addAllToMpdQueue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    if (favorites.isEmpty()) {
      // Empty Favorites State
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
            Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = null,
              tint = MochaPeach,
              modifier = Modifier.size(42.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = strings.emptyFavoritesTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MochaText,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = strings.emptyFavoritesSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MochaSubtext0,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = onNavigateToQueue,
            colors = ButtonDefaults.buttonColors(containerColor = MochaMauve, contentColor = MochaCrust),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("button_empty_fav_queue")
          ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.tabQueue)
          }
        }
      }
    } else {
      // Favorites List
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .testTag("favorites_track_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp, top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        itemsIndexed(favorites, key = { _, song -> song.id }) { index, song ->
          val isCurrentSong = song.id == currentSongId

          FavoriteTrackCard(
            song = song,
            index = index,
            isCurrent = isCurrentSong,
            onClick = { onSongClicked(song) },
            onToggleFavorite = { onToggleFavorite(song) },
            modifier = Modifier.testTag("fav_item_${song.id}")
          )
        }
      }
    }
  }
}

@Composable
private fun FavoriteTrackCard(
  song: Song,
  index: Int,
  isCurrent: Boolean,
  onClick: () -> Unit,
  onToggleFavorite: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(
        width = if (isCurrent) 1.5.dp else 1.dp,
        color = if (isCurrent) MochaPeach else MochaSurface0,
        shape = RoundedCornerShape(14.dp)
      )
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrent) MochaPeach.copy(alpha = 0.18f) else MochaMantle
    ),
    shape = RoundedCornerShape(14.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Index or playing equalizer
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(if (isCurrent) MochaPeach.copy(alpha = 0.3f) else MochaSurface0),
        contentAlignment = Alignment.Center
      ) {
        if (isCurrent) {
          Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = "Currently Playing",
            tint = MochaPeach,
            modifier = Modifier.size(18.dp)
          )
        } else {
          Text(
            text = (index + 1).toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MochaSubtext0
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Song Details
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = song.title,
          style = MaterialTheme.typography.titleMedium,
          color = if (isCurrent) MochaPeach else MochaText,
          fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${song.artist} • ${song.album}",
          style = MaterialTheme.typography.bodySmall,
          color = MochaSubtext0,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Duration
      Text(
        text = song.formattedDuration,
        style = MaterialTheme.typography.labelSmall,
        color = MochaSubtext0
      )

      Spacer(modifier = Modifier.width(6.dp))

      // Favorite toggle button (red/peach filled heart)
      IconButton(
        onClick = onToggleFavorite,
        modifier = Modifier.size(36.dp).testTag("fav_heart_button_${song.id}")
      ) {
        Icon(
          imageVector = Icons.Default.Favorite,
          contentDescription = "Remove from Favorites",
          tint = MochaPeach,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}
