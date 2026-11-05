package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.ui.theme.MochaBase
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
fun TopBar(
  connectionState: ConnectionState,
  isUpdatingDb: Boolean,
  queueCount: Int,
  activeTab: Int,
  searchQuery: String,
  strings: com.example.util.AppStrings,
  onTabSelected: (Int) -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onUpdateDbClicked: () -> Unit,
  onAiCuratorClicked: () -> Unit,
  onSettingsClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "dbScanRotation")
  val rotationAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotationAngle"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MochaMantle)
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Header Row with Branding & Quick Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Branding
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag("app_branding")
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MochaSurface0),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "🌱", fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "MPDF",
            style = MaterialTheme.typography.titleLarge,
            color = MochaPeach,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Quick Actions
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Connection status pill
        ConnectionPill(
          connectionState = connectionState,
          onClick = onSettingsClicked
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Database scan button
        IconButton(
          onClick = onUpdateDbClicked,
          modifier = Modifier
            .testTag("button_db_update")
            .size(38.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Update MPD Database",
            tint = if (isUpdatingDb) MochaPeach else MochaSubtext0,
            modifier = if (isUpdatingDb) Modifier.rotate(rotationAngle) else Modifier
          )
        }

        // Sprout AI Curator button
        IconButton(
          onClick = onAiCuratorClicked,
          modifier = Modifier
            .testTag("button_sprout_ai")
            .size(38.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Sprout AI Curator",
            tint = MochaGreen
          )
        }

        // Settings button
        IconButton(
          onClick = onSettingsClicked,
          modifier = Modifier
            .testTag("button_settings")
            .size(38.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Server Settings",
            tint = MochaSubtext0
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Search / Filter Input
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChanged,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("search_filter_input"),
      placeholder = {
        Text(
          text = when (activeTab) {
            0 -> strings.searchPlaceholderQueue
            1 -> strings.searchPlaceholderLibrary
            else -> "${strings.tabFavorites}..."
          },
          color = MochaSubtext0.copy(alpha = 0.7f),
          style = MaterialTheme.typography.bodyMedium
        )
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          tint = MochaSubtext0,
          modifier = Modifier.size(20.dp)
        )
      },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(
            onClick = { onSearchQueryChanged("") },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear search",
              tint = MochaSubtext0
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MochaSurface0,
        unfocusedContainerColor = MochaSurface0,
        focusedBorderColor = MochaMauve,
        unfocusedBorderColor = MochaSurface1,
        focusedTextColor = MochaText,
        unfocusedTextColor = MochaText
      )
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Navigation Tabs (Queue, Library, Favorites)
    TabRow(
      selectedTabIndex = activeTab,
      containerColor = MochaMantle,
      contentColor = MochaMauve,
      indicator = { tabPositions ->
        if (activeTab in tabPositions.indices) {
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
            height = 3.dp,
            color = MochaMauve
          )
        }
      },
      divider = {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MochaSurface0)
        )
      }
    ) {
      // Tab 0: Queue (Sıra)
      Tab(
        selected = activeTab == 0,
        onClick = { onTabSelected(0) },
        modifier = Modifier.testTag("tab_queue"),
        text = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = strings.tabQueue,
              fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
              color = if (activeTab == 0) MochaMauve else MochaSubtext0
            )
            if (queueCount > 0) {
              Spacer(modifier = Modifier.width(6.dp))
              BadgedBox(badge = {
                Badge(
                  containerColor = if (activeTab == 0) MochaMauve else MochaSurface1,
                  contentColor = MochaBase
                ) {
                  Text(text = queueCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }) { }
            }
          }
        }
      )

      // Tab 1: Library (Kütüphane)
      Tab(
        selected = activeTab == 1,
        onClick = { onTabSelected(1) },
        modifier = Modifier.testTag("tab_library"),
        text = {
          Text(
            text = strings.tabLibrary,
            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
            color = if (activeTab == 1) MochaMauve else MochaSubtext0
          )
        }
      )

      // Tab 2: Favorites (Favoriler)
      Tab(
        selected = activeTab == 2,
        onClick = { onTabSelected(2) },
        modifier = Modifier.testTag("tab_favorites"),
        text = {
          Text(
            text = strings.tabFavorites,
            fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium,
            color = if (activeTab == 2) MochaPeach else MochaSubtext0
          )
        }
      )
    }

  }
}

@Composable
private fun ConnectionPill(
  connectionState: ConnectionState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val (dotColor, label) = when (connectionState) {
    is ConnectionState.Connected -> Pair(
      MochaGreen,
      if (connectionState.endpoint.contains("Demo")) "Demo" else connectionState.endpoint
    )
    is ConnectionState.Connecting -> Pair(MochaYellow, "Connecting...")
    is ConnectionState.Disconnected -> Pair(MochaRed, "Offline")
    is ConnectionState.Error -> Pair(MochaRed, "Error")
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(MochaSurface0)
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 5.dp)
      .testTag("connection_status_pill")
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(dotColor)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MochaText,
      maxLines = 1
    )
  }
}
