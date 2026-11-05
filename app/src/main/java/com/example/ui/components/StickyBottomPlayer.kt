package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MpdStatus
import com.example.model.PlaybackState
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
import com.example.ui.theme.MochaYellow

@Composable
fun StickyBottomPlayer(
  status: MpdStatus,
  currentSong: Song?,
  selectedCount: Int,
  strings: com.example.util.AppStrings,
  onPlayPauseClicked: () -> Unit,
  onStopClicked: () -> Unit,
  onNextClicked: () -> Unit,
  onPreviousClicked: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onVolumeChanged: (Int) -> Unit,
  onToggleMute: () -> Unit,
  onToggleShuffle: () -> Unit,
  onToggleRepeat: () -> Unit,
  onToggleSingle: () -> Unit,
  onToggleConsume: () -> Unit,
  // Batch actions
  onMoveUpSelected: () -> Unit,
  onMoveDownSelected: () -> Unit,
  onQueueNextSelected: () -> Unit,
  onDeleteSelected: () -> Unit,
  onOpenBatchTagEditor: () -> Unit,
  onClearSelection: () -> Unit,
  isCurrentSongFavorite: Boolean = false,
  onToggleFavorite: (Song) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isPlaying = status.state == PlaybackState.PLAY

  // Vinyl rotation animation
  val infiniteTransition = rememberInfiniteTransition(label = "vinylRotation")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "vinylSpin"
  )

  var isUserDraggingSeek by remember { mutableStateOf(false) }
  var dragSeekFraction by remember { mutableFloatStateOf(0f) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
    color = MochaMantle,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    tonalElevation = 8.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Contextual Batch Action Bar when items are selected
      AnimatedVisibility(visible = selectedCount > 0) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
          colors = CardDefaults.cardColors(containerColor = MochaSurface0),
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "$selectedCount ${strings.selectedCount}",
              style = MaterialTheme.typography.titleMedium,
              color = MochaPeach,
              fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = onOpenBatchTagEditor,
                modifier = Modifier.size(36.dp).testTag("batch_edit_tags")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Edit Tags",
                  tint = MochaPeach
                )
              }
              IconButton(
                onClick = onMoveUpSelected,
                modifier = Modifier.size(36.dp).testTag("batch_move_up")
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowUp,
                  contentDescription = "Move Up",
                  tint = MochaMauve
                )
              }
              IconButton(
                onClick = onMoveDownSelected,
                modifier = Modifier.size(36.dp).testTag("batch_move_down")
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = "Move Down",
                  tint = MochaMauve
                )
              }
              IconButton(
                onClick = onQueueNextSelected,
                modifier = Modifier.size(36.dp).testTag("batch_queue_next")
              ) {
                Icon(
                  imageVector = Icons.Default.PlaylistPlay,
                  contentDescription = strings.queueAfterPlaying,
                  tint = MochaGreen
                )
              }
              IconButton(
                onClick = onDeleteSelected,
                modifier = Modifier.size(36.dp).testTag("batch_delete")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete Selected",
                  tint = MochaRed
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = strings.cancelSelection,
                color = MochaSubtext0,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onClearSelection)
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      }

      // ROW 1: Vinyl Disc, Track Details, Quality Badge, Heart, Volume Slider
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Rotating Vinyl Record Deck
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(MochaCrust),
          contentAlignment = Alignment.Center
        ) {
          VinylCanvas(
            modifier = Modifier
              .size(54.dp)
              .rotate(if (isPlaying) rotation else 0f)
          )
          // Center Sprout Emblem
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(MochaPeach),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "🌱", fontSize = 10.sp)
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Song Title, Artist, and Format Tag
        Column(
          modifier = Modifier
            .weight(1f)
            .testTag("now_playing_info")
        ) {
          Text(
            text = currentSong?.title ?: strings.noTrackPlaying,
            style = MaterialTheme.typography.titleMedium,
            color = MochaText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = currentSong?.let { "${it.artist} • ${it.album}" } ?: strings.selectSongPrompt,
              style = MaterialTheme.typography.bodySmall,
              color = MochaSubtext0,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )
            if (currentSong != null) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(MochaSurface0)
                  .padding(horizontal = 5.dp, vertical = 2.dp)
              ) {
                Text(
                  text = currentSong.format,
                  fontSize = 9.sp,
                  color = MochaPeach,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Favorite Heart Button
        IconButton(
          onClick = { currentSong?.let { onToggleFavorite(it) } },
          modifier = Modifier.size(36.dp).testTag("button_now_playing_fav")
        ) {
          Icon(
            imageVector = if (isCurrentSongFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorite",
            tint = if (isCurrentSongFavorite) MochaPeach else MochaSubtext0,
            modifier = Modifier.size(20.dp)
          )
        }

        // Mini Volume Controller
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.width(100.dp)
        ) {
          IconButton(
            onClick = onToggleMute,
            modifier = Modifier.size(28.dp).testTag("button_mute")
          ) {
            Icon(
              imageVector = if (status.isMuted || status.volume == 0) Icons.Default.VolumeMute
              else if (status.volume < 50) Icons.Default.VolumeDown
              else Icons.Default.VolumeUp,
              contentDescription = "Mute Toggle",
              tint = if (status.isMuted) MochaRed else MochaSubtext0,
              modifier = Modifier.size(16.dp)
            )
          }
          Slider(
            value = status.volume.toFloat(),
            onValueChange = { onVolumeChanged(it.toInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f).testTag("slider_volume"),
            colors = SliderDefaults.colors(
              thumbColor = MochaMauve,
              activeTrackColor = MochaMauve,
              inactiveTrackColor = MochaSurface1
            )
          )
        }
      }

      // Seek Scrubber Timeline with Elapsed / Total Counters
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isUserDraggingSeek) {
            val dragSec = (dragSeekFraction * status.totalSeconds).toLong()
            "%d:%02d".format(dragSec / 60, dragSec % 60)
          } else status.formattedElapsed,
          style = MaterialTheme.typography.labelSmall,
          color = MochaSubtext0,
          modifier = Modifier.width(36.dp)
        )

        Slider(
          value = if (isUserDraggingSeek) dragSeekFraction else status.progressFraction,
          onValueChange = {
            isUserDraggingSeek = true
            dragSeekFraction = it
          },
          onValueChangeFinished = {
            val targetSeconds = (dragSeekFraction * status.totalSeconds).toLong()
            onSeekTo(targetSeconds)
            isUserDraggingSeek = false
          },
          modifier = Modifier
            .weight(1f)
            .height(28.dp)
            .testTag("slider_seek"),
          colors = SliderDefaults.colors(
            thumbColor = MochaPeach,
            activeTrackColor = MochaPeach,
            inactiveTrackColor = MochaSurface0
          )
        )

        Text(
          text = status.formattedTotal,
          style = MaterialTheme.typography.labelSmall,
          color = MochaSubtext0,
          modifier = Modifier.width(36.dp)
        )
      }

      // ROW 2: Complete Transport Controls & MPD Modes
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Mode Toggles Group
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Shuffle
          ModeToggle(
            isActive = status.random,
            onClick = onToggleShuffle,
            icon = Icons.Default.Shuffle,
            desc = "Shuffle (z)",
            testTag = "toggle_shuffle"
          )
          // Repeat All
          ModeToggle(
            isActive = status.repeat,
            onClick = onToggleRepeat,
            icon = Icons.Default.Repeat,
            desc = "Repeat (r)",
            testTag = "toggle_repeat"
          )
          // Repeat Single Song
          ModeToggle(
            isActive = status.single,
            onClick = onToggleSingle,
            icon = Icons.Default.RepeatOne,
            desc = "Single (y)",
            testTag = "toggle_single"
          )
          // Consume Mode
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(if (status.consume) MochaPeach.copy(alpha = 0.2f) else Color.Transparent)
              .clickable(onClick = onToggleConsume)
              .testTag("toggle_consume"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "CNS",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (status.consume) MochaPeach else MochaSubtext0
            )
          }
        }

        // Core Transport Controls (Previous, Play/Pause, Stop, Next)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          IconButton(
            onClick = onPreviousClicked,
            modifier = Modifier.size(40.dp).testTag("button_prev")
          ) {
            Icon(
              imageVector = Icons.Default.SkipPrevious,
              contentDescription = "Previous (b)",
              tint = MochaText,
              modifier = Modifier.size(24.dp)
            )
          }

          FilledIconButton(
            onClick = onPlayPauseClicked,
            modifier = Modifier.size(50.dp).testTag("button_play_pause"),
            colors = IconButtonDefaults.filledIconButtonColors(
              containerColor = MochaPeach,
              contentColor = MochaCrust
            )
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              modifier = Modifier.size(28.dp)
            )
          }

          IconButton(
            onClick = onStopClicked,
            modifier = Modifier.size(40.dp).testTag("button_stop")
          ) {
            Icon(
              imageVector = Icons.Default.Stop,
              contentDescription = "Stop",
              tint = MochaSubtext0,
              modifier = Modifier.size(22.dp)
            )
          }

          IconButton(
            onClick = onNextClicked,
            modifier = Modifier.size(40.dp).testTag("button_next")
          ) {
            Icon(
              imageVector = Icons.Default.SkipNext,
              contentDescription = "Next (n)",
              tint = MochaText,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ModeToggle(
  isActive: Boolean,
  onClick: () -> Unit,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  desc: String,
  testTag: String
) {
  IconButton(
    onClick = onClick,
    modifier = Modifier.size(34.dp).testTag(testTag)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = desc,
      tint = if (isActive) MochaMauve else MochaSubtext0,
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
private fun VinylCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2

    // Outer Vinyl Body with Radial Sheen
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFF2B2B38), Color(0xFF14141E), Color(0xFF0F0F17)),
        center = center,
        radius = radius
      ),
      radius = radius,
      center = center
    )

    // Concentric Vinyl Grooves
    val grooveColor = Color(0xFF38384D)
    for (step in 1..4) {
      val r = radius * (0.35f + step * 0.13f)
      drawCircle(
        color = grooveColor.copy(alpha = 0.4f),
        radius = r,
        center = center,
        style = Stroke(width = 1f)
      )
    }

    // Center Label Disc
    drawCircle(
      color = Color(0xFF1E1E2E),
      radius = radius * 0.35f,
      center = center
    )
  }
}
