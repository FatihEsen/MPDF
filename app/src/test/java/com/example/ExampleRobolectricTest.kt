package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.SettingsRepository
import com.example.util.AppLanguage
import com.example.util.Localization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MPDF", appName)
  }

  @Test
  fun `turkish tab titles are clean without parenthesis`() {
    val strings = Localization.get(AppLanguage.TURKISH)
    assertEquals("Sıra", strings.tabQueue)
    assertEquals("Kütüphane", strings.tabLibrary)
    assertEquals("Favoriler", strings.tabFavorites)
  }

  @Test
  fun `settings repository persists favorite tracks`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = SettingsRepository(context)
    val testSongId = "song-test-123"

    assertFalse(repo.isFavorite(testSongId))
    val isFav = repo.toggleFavorite(testSongId)
    assertTrue(isFav)
    assertTrue(repo.isFavorite(testSongId))

    val isUnfav = repo.toggleFavorite(testSongId)
    assertFalse(isUnfav)
    assertFalse(repo.isFavorite(testSongId))
  }
}
