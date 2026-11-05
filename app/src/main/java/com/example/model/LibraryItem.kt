package com.example.model

sealed interface LibraryEntry {
  val path: String
  val name: String
}

data class LibraryFolder(
  override val path: String,
  override val name: String,
  val trackCount: Int = 0,
  val subfolderCount: Int = 0
) : LibraryEntry

data class LibraryTrack(
  override val path: String,
  override val name: String,
  val song: Song
) : LibraryEntry
