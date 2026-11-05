package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sensors
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ServerConfig
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
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.Localization
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
  initialConfig: ServerConfig,
  strings: AppStrings,
  onDismiss: () -> Unit,
  onSave: (ServerConfig) -> Unit,
  onTestPing: suspend () -> Long,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var host by remember { mutableStateOf(initialConfig.host) }
  var portStr by remember { mutableStateOf(initialConfig.port.toString()) }
  var password by remember { mutableStateOf(initialConfig.password) }
  var streamUrl by remember { mutableStateOf(initialConfig.streamUrl) }
  var isDemoMode by remember { mutableStateOf(initialConfig.isDemoMode) }
  var autoReconnect by remember { mutableStateOf(initialConfig.autoReconnect) }
  var enableLocalAudioStream by remember { mutableStateOf(initialConfig.enableLocalAudioStream) }
  var selectedLanguage by remember { mutableStateOf(initialConfig.language) }

  // Dynamically reflect language in dialog UI as user taps buttons
  val currentStrings = Localization.get(selectedLanguage)

  var isTestingPing by remember { mutableStateOf(false) }
  var pingResult by remember { mutableStateOf<String?>(null) }
  var pingSuccess by remember { mutableStateOf(false) }

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
                .background(MochaSurface0),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = MochaPeach,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = currentStrings.settingsTitle,
              style = MaterialTheme.typography.titleLarge,
              color = MochaText,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp).testTag("button_close_settings")
          ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MochaSubtext0)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Switcher Section (Türkçe & English)
        Card(
          colors = CardDefaults.cardColors(containerColor = MochaSurface0),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MochaMauve,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = currentStrings.languageLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MochaText,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Turkish Button
              val isTr = selectedLanguage == AppLanguage.TURKISH
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isTr) MochaMauve else MochaMantle)
                  .border(1.dp, if (isTr) MochaMauve else MochaSurface1, RoundedCornerShape(12.dp))
                  .clickable { selectedLanguage = AppLanguage.TURKISH }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = "🇹🇷", fontSize = 16.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Türkçe",
                    fontWeight = FontWeight.Bold,
                    color = if (isTr) MochaCrust else MochaText,
                    fontSize = 13.sp
                  )
                }
              }

              // English Button
              val isEn = selectedLanguage == AppLanguage.ENGLISH
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isEn) MochaMauve else MochaMantle)
                  .border(1.dp, if (isEn) MochaMauve else MochaSurface1, RoundedCornerShape(12.dp))
                  .clickable { selectedLanguage = AppLanguage.ENGLISH }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = "🇬🇧", fontSize = 16.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "English",
                    fontWeight = FontWeight.Bold,
                    color = if (isEn) MochaCrust else MochaText,
                    fontSize = 13.sp
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Demo Server Switch Banner
        Card(
          colors = CardDefaults.cardColors(containerColor = MochaSurface0),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = currentStrings.demoModeTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MochaGreen,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = currentStrings.demoModeSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MochaSubtext0
              )
            }
            Switch(
              checked = isDemoMode,
              onCheckedChange = { isDemoMode = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MochaGreen,
                checkedTrackColor = MochaGreen.copy(alpha = 0.3f),
                uncheckedThumbColor = MochaSubtext0,
                uncheckedTrackColor = MochaSurface1
              ),
              modifier = Modifier.testTag("switch_demo_mode")
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real MPD Server Connection Fields
        Text(
          text = currentStrings.tcpConnectionSection,
          style = MaterialTheme.typography.labelLarge,
          color = MochaMauve,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Host Input
        OutlinedTextField(
          value = host,
          onValueChange = { host = it },
          label = { Text(currentStrings.serverHostLabel) },
          placeholder = { Text("192.168.1.100 or raspberrypi.local") },
          singleLine = true,
          enabled = !isDemoMode,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MochaPeach,
            unfocusedBorderColor = MochaSurface1,
            focusedTextColor = MochaText,
            unfocusedTextColor = MochaText
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("input_server_host")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Port & Password Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = portStr,
            onValueChange = { portStr = it },
            label = { Text(currentStrings.portLabel) },
            placeholder = { Text("6600") },
            singleLine = true,
            enabled = !isDemoMode,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(0.4f).testTag("input_server_port")
          )

          OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(currentStrings.passwordLabel) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isDemoMode,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaPeach,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(0.6f).testTag("input_server_password")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ping Latency Test Button & Result
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          OutlinedButton(
            onClick = {
              scope.launch {
                isTestingPing = true
                pingResult = null
                try {
                  val latency = onTestPing()
                  pingResult = "Pong! ${latency}ms latency"
                  pingSuccess = true
                } catch (e: Exception) {
                  pingResult = "Failed: ${e.message}"
                  pingSuccess = false
                } finally {
                  isTestingPing = false
                }
              }
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("button_ping_test")
          ) {
            if (isTestingPing) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MochaPeach)
              Spacer(modifier = Modifier.width(8.dp))
            } else {
              Icon(imageVector = Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp), tint = MochaPeach)
              Spacer(modifier = Modifier.width(6.dp))
            }
            Text(currentStrings.testConnectionPing, color = MochaPeach, fontSize = 12.sp)
          }

          pingResult?.let { result ->
            Text(
              text = result,
              style = MaterialTheme.typography.bodySmall,
              color = if (pingSuccess) MochaGreen else MochaRed,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Local Audio Streaming Option (ExoPlayer HTTP Stream)
        Text(
          text = currentStrings.localAudioSection,
          style = MaterialTheme.typography.labelLarge,
          color = MochaMauve,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = currentStrings.streamAudioTitle,
              style = MaterialTheme.typography.titleMedium,
              color = MochaText
            )
            Text(
              text = currentStrings.streamAudioSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MochaSubtext0
            )
          }
          Switch(
            checked = enableLocalAudioStream,
            onCheckedChange = { enableLocalAudioStream = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = MochaMauve,
              checkedTrackColor = MochaMauve.copy(alpha = 0.3f),
              uncheckedThumbColor = MochaSubtext0,
              uncheckedTrackColor = MochaSurface1
            ),
            modifier = Modifier.testTag("switch_local_audio")
          )
        }

        if (enableLocalAudioStream) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = streamUrl,
            onValueChange = { streamUrl = it },
            label = { Text(currentStrings.httpStreamUrlLabel) },
            placeholder = { Text("http://192.168.1.100:8000/mpd.ogg") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MochaMauve,
              unfocusedBorderColor = MochaSurface1,
              focusedTextColor = MochaText,
              unfocusedTextColor = MochaText
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("input_stream_url")
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons: Cancel & Save
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text(currentStrings.cancel, color = MochaSubtext0)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val port = portStr.toIntOrNull() ?: 6600
              val newCfg = ServerConfig(
                host = host.trim(),
                port = port,
                password = password,
                streamUrl = streamUrl.trim(),
                isDemoMode = isDemoMode,
                autoReconnect = autoReconnect,
                enableLocalAudioStream = enableLocalAudioStream,
                language = selectedLanguage
              )
              onSave(newCfg)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MochaPeach, contentColor = MochaCrust),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("button_save_settings")
          ) {
            Text(currentStrings.saveAndConnect, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
