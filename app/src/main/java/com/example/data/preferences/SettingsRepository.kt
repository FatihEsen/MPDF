package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ServerConfig
import com.example.util.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

  companion object {
    private const val PREFS_NAME = "mpdf_settings"
    private const val KEY_HOST = "key_host"
    private const val KEY_PORT = "key_port"
    private const val KEY_PASSWORD = "key_password"
    private const val KEY_STREAM_URL = "key_stream_url"
    private const val KEY_DEMO_MODE = "key_demo_mode"
    private const val KEY_AUTO_RECONNECT = "key_auto_reconnect"
    private const val KEY_ENABLE_STREAM = "key_enable_stream"
    private const val KEY_LANGUAGE = "key_language"
    private const val KEY_FAVORITES = "key_favorites"
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _config = MutableStateFlow(loadConfig())
  val config: StateFlow<ServerConfig> = _config.asStateFlow()

  private val _favoriteIds = MutableStateFlow<Set<String>>(loadFavorites())
  val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

  private fun loadFavorites(): Set<String> {
    return prefs.getStringSet(KEY_FAVORITES, emptySet())?.toSet() ?: emptySet()
  }

  fun isFavorite(idOrFile: String): Boolean {
    return _favoriteIds.value.contains(idOrFile)
  }

  fun toggleFavorite(idOrFile: String): Boolean {
    val current = _favoriteIds.value.toMutableSet()
    val isFavNow = if (current.contains(idOrFile)) {
      current.remove(idOrFile)
      false
    } else {
      current.add(idOrFile)
      true
    }
    prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    _favoriteIds.value = current
    return isFavNow
  }

  private fun loadConfig(): ServerConfig {
    val langCode = prefs.getString(KEY_LANGUAGE, "tr") ?: "tr"
    val language = if (langCode == "en") AppLanguage.ENGLISH else AppLanguage.TURKISH
    return ServerConfig(
      host = prefs.getString(KEY_HOST, "192.168.1.100") ?: "192.168.1.100",
      port = prefs.getInt(KEY_PORT, 6600),
      password = prefs.getString(KEY_PASSWORD, "") ?: "",
      streamUrl = prefs.getString(KEY_STREAM_URL, "http://192.168.1.100:8000/mpd.ogg") ?: "http://192.168.1.100:8000/mpd.ogg",
      isDemoMode = prefs.getBoolean(KEY_DEMO_MODE, true),
      autoReconnect = prefs.getBoolean(KEY_AUTO_RECONNECT, true),
      enableLocalAudioStream = prefs.getBoolean(KEY_ENABLE_STREAM, false),
      language = language
    )
  }

  fun updateConfig(newConfig: ServerConfig) {
    prefs.edit()
      .putString(KEY_HOST, newConfig.host)
      .putInt(KEY_PORT, newConfig.port)
      .putString(KEY_PASSWORD, newConfig.password)
      .putString(KEY_STREAM_URL, newConfig.streamUrl)
      .putBoolean(KEY_DEMO_MODE, newConfig.isDemoMode)
      .putBoolean(KEY_AUTO_RECONNECT, newConfig.autoReconnect)
      .putBoolean(KEY_ENABLE_STREAM, newConfig.enableLocalAudioStream)
      .putString(KEY_LANGUAGE, newConfig.language.code)
      .apply()
    _config.value = newConfig
  }
}

