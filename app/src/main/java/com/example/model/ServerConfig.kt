package com.example.model

import com.example.util.AppLanguage

data class ServerConfig(
  val host: String = "192.168.1.100",
  val port: Int = 6600,
  val password: String = "",
  val streamUrl: String = "http://192.168.1.100:8000/mpd.ogg",
  val isDemoMode: Boolean = true,
  val autoReconnect: Boolean = true,
  val enableLocalAudioStream: Boolean = false,
  val language: AppLanguage = AppLanguage.TURKISH
)

sealed interface ConnectionState {
  data object Disconnected : ConnectionState
  data class Connecting(val endpoint: String) : ConnectionState
  data class Connected(val endpoint: String, val latencyMs: Long? = null) : ConnectionState
  data class Error(val message: String) : ConnectionState
}
