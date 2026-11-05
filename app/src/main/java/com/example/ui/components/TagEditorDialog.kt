package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.ai.TagSuggestion
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
import kotlinx.coroutines.launch

@Composable
fun SingleTagEditorDialog(
  song: Song,
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSave: (Song) -> Unit,
  onSuggestAiTags: suspend (Song) -> TagSuggestion,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var title by remember { mutableStateOf(song.title) }
  var artist by remember { mutableStateOf(song.artist) }
  var album by remember { mutableStateOf(song.album) }
  var genre by remember { mutableStateOf(song.genre) }
  var year by remember { mutableStateOf(song.year) }
  var trackNum by remember { mutableIntStateOf(song.track) }
  var albumArtUrl by remember { mutableStateOf(song.albumArtUrl ?: "") }

  var isAiLoading by remember { mutableStateOf(false) }
  var aiInsights by remember { mutableStateOf<String?>(null) }

  // Sample curated artwork placeholders for album art options
  val presetArtworks = listOf(
    "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300&q=80",
    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&q=80",
    "https://images.unsplash.com/photo-1445307806294-bff7f67ff225?w=300&q=80"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = modifier
        .fillMaxWidth(0.92f)
        .clip(RoundedCornerShape(24.dp)),
      color = MochaMantle,
      shape = RoundedCornerShape(24.dp),
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MochaMauve.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MochaMauve,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = strings.tagEditorTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MochaText,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = song.file.substringAfterLast('/'),
                style = MaterialTheme.typography.bodySmall,
                color = MochaSubtext0,
                maxLines = 1
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MochaSubtext0)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Compact AI Auto-Tag Action Card
        Card(
          colors = CardDefaults.cardColors(containerColor = MochaSurface0),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MochaGreen,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "🌱 Sprout AI Auto-Tag",
                style = MaterialTheme.typography.titleSmall,
                color = MochaGreen,
                fontWeight = FontWeight.Bold
              )
            }
            Button(
              onClick = {
                scope.launch {
                  isAiLoading = true
                  try {
                    val suggestion = onSuggestAiTags(song)
                    title = suggestion.title
                    artist = suggestion.artist
                    album = suggestion.album
                    genre = suggestion.genre
                    year = suggestion.year
                    trackNum = suggestion.track
                    aiInsights = suggestion.albumArtDescription
                    if (!suggestion.suggestedArtUrl.isNullOrBlank()) {
                      albumArtUrl = suggestion.suggestedArtUrl
                    }
                  } finally {
                    isAiLoading = false
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MochaGreen, contentColor = MochaCrust),
              shape = RoundedCornerShape(10.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("button_ai_auto_tag")
            ) {
              if (isAiLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MochaCrust, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analiz...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.aiAutoTag, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        aiInsights?.let { insights ->
          Spacer(modifier = Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(MochaSurface0)
              .padding(8.dp)
          ) {
            Text(
              text = "✨ AI Note: $insights",
              style = MaterialTheme.typography.labelSmall,
              color = MochaPeach
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Form Fields
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text(strings.trackTitleLabel) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MochaPeach,
            unfocusedBorderColor = MochaSurface1,
            focusedTextColor = MochaText,
            unfocusedTextColor = MochaText
          ),
          modifier = Modifier.fillMaxWidth().testTag("input_tag_title")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text(strings.artistLabel) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            modifier = Modifier.weight(1f).testTag("input_tag_artist")
          )

          OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text(strings.albumLabel) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            modifier = Modifier.weight(1f).testTag("input_tag_album")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = genre,
            onValueChange = { genre = it },
            label = { Text(strings.genreLabel) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            modifier = Modifier.weight(0.5f).testTag("input_tag_genre")
          )

          OutlinedTextField(
            value = year,
            onValueChange = { year = it },
            label = { Text(strings.yearLabel) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            modifier = Modifier.weight(0.25f).testTag("input_tag_year")
          )

          OutlinedTextField(
            value = if (trackNum > 0) trackNum.toString() else "",
            onValueChange = { trackNum = it.toIntOrNull() ?: 0 },
            label = { Text(strings.trackNumLabel) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            modifier = Modifier.weight(0.25f).testTag("input_tag_track")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Album Art Section
        Text(
          text = strings.albumArtLabel,
          style = MaterialTheme.typography.titleMedium,
          color = MochaMauve,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Cover Thumbnail Preview
          Box(
            modifier = Modifier
              .size(60.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MochaSurface0)
              .border(1.dp, MochaSurface1, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            if (albumArtUrl.isNotBlank()) {
              AsyncImage(
                model = albumArtUrl,
                contentDescription = "Cover Art Preview",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
              )
            } else {
              Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = MochaSubtext0)
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          OutlinedTextField(
            value = albumArtUrl,
            onValueChange = { albumArtUrl = it },
            placeholder = { Text("https://... or preset") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaMauve,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            modifier = Modifier.weight(1f).testTag("input_tag_art_url")
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save & Cancel Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = MochaSubtext0)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val updated = song.copy(
                title = title.trim(),
                artist = artist.trim(),
                album = album.trim(),
                genre = genre.trim(),
                year = year.trim(),
                track = trackNum,
                albumArtUrl = albumArtUrl.trim().ifBlank { null }
              )
              onSave(updated)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MochaPeach, contentColor = MochaCrust),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("button_save_tags")
          ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.saveTags, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun BatchTagEditorDialog(
  selectedSongs: List<Song>,
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSaveBatch: (artist: String, album: String, genre: String, year: String, albumArtUrl: String?) -> Unit,
  onSuggestAiBatchTags: suspend (List<Song>) -> List<TagSuggestion>,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var commonArtist by remember { mutableStateOf(selectedSongs.firstOrNull()?.artist ?: "") }
  var commonAlbum by remember { mutableStateOf(selectedSongs.firstOrNull()?.album ?: "") }
  var commonGenre by remember { mutableStateOf(selectedSongs.firstOrNull()?.genre ?: "") }
  var commonYear by remember { mutableStateOf(selectedSongs.firstOrNull()?.year ?: "") }
  var commonArtUrl by remember { mutableStateOf("") }
  var isAiLoading by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = modifier
        .fillMaxWidth(0.92f)
        .clip(RoundedCornerShape(24.dp)),
      color = MochaMantle,
      shape = RoundedCornerShape(24.dp),
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MochaMauve.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = MochaMauve,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = strings.batchTagEditorTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MochaText,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${selectedSongs.size} ${strings.selectedCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MochaPeach
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MochaSubtext0)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Compact AI Batch Harmonization Action Card
        Card(
          colors = CardDefaults.cardColors(containerColor = MochaSurface0),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MochaGreen,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "🌱 AI Harmonization",
                style = MaterialTheme.typography.titleSmall,
                color = MochaGreen,
                fontWeight = FontWeight.Bold
              )
            }
            Button(
              onClick = {
                scope.launch {
                  isAiLoading = true
                  try {
                    val suggestions = onSuggestAiBatchTags(selectedSongs)
                    if (suggestions.isNotEmpty()) {
                      commonArtist = suggestions.first().artist
                      commonAlbum = suggestions.first().album
                      commonGenre = suggestions.first().genre
                      commonYear = suggestions.first().year
                    }
                  } finally {
                    isAiLoading = false
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = MochaGreen, contentColor = MochaCrust),
              shape = RoundedCornerShape(10.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("button_ai_batch_auto_tag")
            ) {
              if (isAiLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MochaCrust, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analiz...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.aiAutoTag, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = commonArtist,
          onValueChange = { commonArtist = it },
          label = { Text(strings.artistLabel) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MochaPeach,
            unfocusedBorderColor = MochaSurface1,
            focusedTextColor = MochaText,
            unfocusedTextColor = MochaText
          ),
          modifier = Modifier.fillMaxWidth().testTag("input_batch_artist")
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = commonAlbum,
          onValueChange = { commonAlbum = it },
          label = { Text(strings.albumLabel) },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MochaPeach,
            unfocusedBorderColor = MochaSurface1,
            focusedTextColor = MochaText,
            unfocusedTextColor = MochaText
          ),
          modifier = Modifier.fillMaxWidth().testTag("input_batch_album")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = commonGenre,
            onValueChange = { commonGenre = it },
            label = { Text(strings.genreLabel) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            modifier = Modifier.weight(1f).testTag("input_batch_genre")
          )

          OutlinedTextField(
            value = commonYear,
            onValueChange = { commonYear = it },
            label = { Text(strings.yearLabel) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            modifier = Modifier.weight(1f).testTag("input_batch_year")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = commonArtUrl,
          onValueChange = { commonArtUrl = it },
          label = { Text(strings.albumArtLabel) },
          placeholder = { Text("https://...") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MochaMauve,
            unfocusedBorderColor = MochaSurface1,
            focusedTextColor = MochaText,
            unfocusedTextColor = MochaText
          ),
          modifier = Modifier.fillMaxWidth().testTag("input_batch_art_url")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Save & Cancel Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text(strings.cancel, color = MochaSubtext0)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              onSaveBatch(
                commonArtist.trim(),
                commonAlbum.trim(),
                commonGenre.trim(),
                commonYear.trim(),
                commonArtUrl.trim().ifBlank { null }
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MochaPeach, contentColor = MochaCrust),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("button_save_batch_tags")
          ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.saveTags, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
