package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CuratorRecommendation
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
import com.example.ui.viewmodel.AiCuratorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SproutAiSheet(
  sheetState: SheetState,
  uiState: AiCuratorUiState,
  onCurateRequested: (String) -> Unit,
  onEnqueueAllRecommendations: (List<CuratorRecommendation>) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var promptInput by remember { mutableStateOf("") }

  val sampleChips = listOf(
    "Cozy rainy evening with Ghibli piano",
    "Warm autumn coffee & tape lo-fi",
    "Orchestral Hi-Res studio journey",
    "Late night tranquil acoustic strings"
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MochaMantle,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 8.dp)
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
              .size(38.dp)
              .clip(CircleShape)
              .background(MochaGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "🌱", fontSize = 20.sp)
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Sprout AI Curator",
                style = MaterialTheme.typography.titleLarge,
                color = MochaGreen,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(MochaMauve.copy(alpha = 0.2f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "Gemini 4 Argon • High Thinking",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  color = MochaMauve,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
            Text(
              text = "Deep acoustic reasoning for MPD playlists",
              style = MaterialTheme.typography.bodySmall,
              color = MochaSubtext0
            )
          }
        }

        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MochaSubtext0)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Sample Vibe Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        for (chip in sampleChips) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(MochaSurface0)
              .clickable {
                promptInput = chip
                onCurateRequested(chip)
              }
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(text = chip, fontSize = 12.sp, color = MochaPeach)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Input Field & Send Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = promptInput,
          onValueChange = { promptInput = it },
          placeholder = {
            Text(
              text = "Describe your mood, vibe, or listening scene...",
              color = MochaSubtext0.copy(alpha = 0.7f),
              style = MaterialTheme.typography.bodyMedium
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MochaGreen,
            unfocusedBorderColor = MochaSurface1,
            focusedTextColor = MochaText,
            unfocusedTextColor = MochaText,
            focusedContainerColor = MochaSurface0,
            unfocusedContainerColor = MochaSurface0
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("input_curator_prompt")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = {
            if (promptInput.isNotBlank()) {
              onCurateRequested(promptInput.trim())
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MochaGreen, contentColor = MochaCrust),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.height(52.dp).testTag("button_curate_submit")
        ) {
          Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Response Content Area
      when (uiState) {
        is AiCuratorUiState.Idle -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MochaSurface0)
              .padding(20.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MochaGreen,
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Ask Sprout for Musical Insights",
                style = MaterialTheme.typography.titleMedium,
                color = MochaText,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Sprout reasons deeply over your library, queue, and current track to craft harmonically balanced playlists.",
                style = MaterialTheme.typography.bodySmall,
                color = MochaSubtext0,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
        is AiCuratorUiState.Thinking -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MochaSurface0)
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = MochaGreen, modifier = Modifier.size(36.dp))
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = "🌱 Sprout is deep in thought...",
                style = MaterialTheme.typography.titleMedium,
                color = MochaGreen,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Analyzing musical keys, tempo curves, acoustic resonances, and Studio Ghibli arrangements using Gemini 4 Argon.",
                style = MaterialTheme.typography.bodySmall,
                color = MochaSubtext0,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }
        is AiCuratorUiState.Error -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MochaRed.copy(alpha = 0.1f))
              .border(1.dp, MochaRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
              .padding(16.dp)
          ) {
            Column {
              Text(
                text = "Curation Notice",
                style = MaterialTheme.typography.titleMedium,
                color = MochaRed,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = uiState.message, style = MaterialTheme.typography.bodySmall, color = MochaText)
            }
          }
        }
        is AiCuratorUiState.Success -> {
          val resp = uiState.response
          Column(modifier = Modifier.fillMaxWidth()) {
            // Vibe Title Card
            Card(
              colors = CardDefaults.cardColors(containerColor = MochaSurface0),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = resp.vibeTitle,
                  style = MaterialTheme.typography.titleLarge,
                  color = MochaPeach,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                // High Thinking Insights Callout
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MochaMantle)
                    .padding(10.dp)
                ) {
                  Row(verticalAlignment = Alignment.Top) {
                    Icon(
                      imageVector = Icons.Default.Spa,
                      contentDescription = null,
                      tint = MochaGreen,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = resp.thinkingInsights,
                      style = MaterialTheme.typography.bodySmall,
                      color = MochaSubtext0,
                      lineHeight = 18.sp
                    )
                  }
                }

                if (resp.cozyTip.isNotBlank()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.Lightbulb,
                      contentDescription = null,
                      tint = MochaYellow,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = resp.cozyTip,
                      style = MaterialTheme.typography.labelSmall,
                      color = MochaYellow
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Enqueue All Tracks
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${resp.recommendations.size} Curated Tracks",
                style = MaterialTheme.typography.titleMedium,
                color = MochaText,
                fontWeight = FontWeight.Bold
              )
              Button(
                onClick = {
                  onEnqueueAllRecommendations(resp.recommendations)
                  onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MochaPeach, contentColor = MochaCrust),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("button_curator_enqueue_all")
              ) {
                Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add All to MPD Queue", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recommendation Items
            for (rec in resp.recommendations) {
              Card(
                colors = CardDefaults.cardColors(containerColor = MochaSurface0),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(34.dp)
                      .clip(CircleShape)
                      .background(MochaMauve.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.MusicNote,
                      contentDescription = null,
                      tint = MochaMauve,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = rec.title,
                      style = MaterialTheme.typography.titleSmall,
                      color = MochaText,
                      fontWeight = FontWeight.SemiBold
                    )
                    Text(
                      text = "${rec.artist} • ${rec.album}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MochaSubtext0
                    )
                    if (rec.reason.isNotBlank()) {
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = rec.reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MochaPeach,
                        fontSize = 11.sp
                      )
                    }
                  }
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(MochaMantle)
                      .padding(horizontal = 6.dp, vertical = 3.dp)
                  ) {
                    Text(
                      text = rec.suggestedFormat,
                      fontSize = 9.sp,
                      color = MochaGreen,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
