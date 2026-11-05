package com.example.model

data class CuratorRecommendation(
  val title: String,
  val artist: String,
  val album: String = "",
  val reason: String = "",
  val suggestedFormat: String = "FLAC 24-bit 96kHz"
)

data class CuratorResponse(
  val vibeTitle: String,
  val thinkingInsights: String = "",
  val recommendations: List<CuratorRecommendation> = emptyList(),
  val cozyTip: String = ""
)
