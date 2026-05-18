/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.google.ai.edge.gallery.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.ui.wearables.Insight
import com.google.ai.edge.gallery.ui.wearables.Verdict
import com.google.ai.edge.gallery.ui.wearables.WearableSnapshot
import com.google.ai.edge.gallery.ui.wearables.loadLatestWearableSnapshot
import kotlin.math.roundToInt

private object InsightColors {
  val ScreenBg = Color(0xFFF5F4EF)
  val Card = Color.White
  val Title = Color(0xFF1A1A1A)
  val Body = Color(0xFF5C5C66)
  val Purple = Color(0xFF7E57C2)
  val PurpleDark = Color(0xFF5E35B1)
  val Soft = Color(0xFFEDE7F6)
  val PeachSoft = Color(0xFFFFE0B2)
  val MintSoft = Color(0xFFB2DFDB)
  val Good = Color(0xFF43A047)
  val Warn = Color(0xFFFB8C00)
  val Bad = Color(0xFFE53935)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
  onNavigateUp: () -> Unit,
  onOpenWearablesAnalysis: () -> Unit,
  onOpenImageChat: () -> Unit,
  onOpenYogaCoach: () -> Unit,
) {
  val context = LocalContext.current
  var snapshot by remember { mutableStateOf<WearableSnapshot?>(null) }
  LaunchedEffect(Unit) { snapshot = loadLatestWearableSnapshot(context) }

  Scaffold(
    containerColor = InsightColors.ScreenBg,
    topBar = {
      TopAppBar(
        title = {
          Text(
            "My Insights",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = InsightColors.ScreenBg,
            titleContentColor = InsightColors.Title,
          ),
      )
    },
  ) { padding ->
    val snap = snapshot
    if (snap == null) {
      Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(Modifier.height(12.dp))
          Text(
            "Reading your records…",
            style = MaterialTheme.typography.bodyMedium.copy(color = InsightColors.Body),
          )
        }
      }
      return@Scaffold
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      TodaySummaryCard(snap)
      WearablesAnalysisSection(snap = snap, onOpenFullAnalysis = onOpenWearablesAnalysis)
      PersonalFilesSection(onOpenImageChat = onOpenImageChat)
      YogaHistorySection(onOpenYoga = onOpenYogaCoach)
      Spacer(Modifier.height(8.dp))
    }
  }
}

// ----------------------------------------------------------------------- Today summary card

@Composable
private fun TodaySummaryCard(snap: WearableSnapshot) {
  val progress = snap.dailyProgress
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = InsightColors.Soft),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "Today's snapshot",
            style =
              MaterialTheme.typography.labelMedium.copy(
                color = InsightColors.Body,
                fontWeight = FontWeight.SemiBold,
              ),
          )
          Spacer(Modifier.height(2.dp))
          Text(
            "${snap.dayOfWeek} · ${snap.timestamp}",
            style = MaterialTheme.typography.titleMedium.copy(
              color = InsightColors.Title,
              fontWeight = FontWeight.Bold,
            ),
          )
        }
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = scoreColor(progress.healthScore.toFloat()).copy(alpha = 0.18f),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              "${progress.healthScore.roundToInt()}",
              style =
                MaterialTheme.typography.titleLarge.copy(
                  color = scoreColor(progress.healthScore.toFloat()),
                  fontWeight = FontWeight.Bold,
                ),
            )
            Text(
              "Health",
              style = MaterialTheme.typography.labelSmall.copy(color = InsightColors.Body),
            )
          }
        }
      }
      Spacer(Modifier.height(14.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SummaryTile(
          icon = Icons.Outlined.Favorite,
          tint = InsightColors.Bad,
          label = "Heart rate",
          value = "${snap.heartRate} bpm",
          modifier = Modifier.weight(1f),
        )
        SummaryTile(
          icon = Icons.Outlined.Bedtime,
          tint = InsightColors.PurpleDark,
          label = "Sleep",
          value = "${"%.1f".format(snap.sleepDuration)} h",
          modifier = Modifier.weight(1f),
        )
      }
      Spacer(Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SummaryTile(
          icon = Icons.Outlined.WaterDrop,
          tint = Color(0xFF29B6F6),
          label = "Water",
          value = "${"%.1f".format(snap.waterLiters)} L",
          modifier = Modifier.weight(1f),
        )
        SummaryTile(
          icon = Icons.Outlined.MonitorHeart,
          tint = Color(0xFF26A69A),
          label = "ECG",
          value = snap.ecg,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun SummaryTile(
  icon: ImageVector,
  tint: Color,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = InsightColors.Card,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
      }
      Spacer(Modifier.width(10.dp))
      Column {
        Text(
          label,
          style = MaterialTheme.typography.labelSmall.copy(color = InsightColors.Body),
        )
        Text(
          value,
          style =
            MaterialTheme.typography.bodyMedium.copy(
              color = InsightColors.Title,
              fontWeight = FontWeight.SemiBold,
            ),
        )
      }
    }
  }
}

private fun scoreColor(score: Float): Color =
  when {
    score >= 75f -> InsightColors.Good
    score >= 55f -> InsightColors.Warn
    else -> InsightColors.Bad
  }

// --------------------------------------------------------------- Wearables analysis section

@Composable
private fun WearablesAnalysisSection(snap: WearableSnapshot, onOpenFullAnalysis: () -> Unit) {
  SectionContainer(
    title = "Wearables Analysis",
    subtitle = "Past rule-based reviews on this snapshot",
    icon = Icons.Outlined.MonitorHeart,
    iconTint = InsightColors.PurpleDark,
  ) {
    snap.localInsights.take(4).forEachIndexed { idx, insight ->
      InsightRow(insight)
      if (idx != 3 && idx != snap.localInsights.lastIndex) {
        Spacer(Modifier.height(12.dp))
      }
    }
    Spacer(Modifier.height(14.dp))
    LinkRow(label = "View full wearables analysis", onClick = onOpenFullAnalysis)
  }
}

@Composable
private fun InsightRow(insight: Insight) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier =
        Modifier.size(10.dp)
          .clip(RoundedCornerShape(5.dp))
          .background(verdictColor(insight.severity)),
    )
    Spacer(Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        insight.title,
        style =
          MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = InsightColors.Title,
          ),
      )
      Spacer(Modifier.height(2.dp))
      Text(
        insight.body,
        style = MaterialTheme.typography.bodySmall.copy(color = InsightColors.Body),
      )
    }
  }
}

private fun verdictColor(v: Verdict): Color =
  when (v) {
    Verdict.GOOD -> InsightColors.Good
    Verdict.WARN -> InsightColors.Warn
    Verdict.BAD -> InsightColors.Bad
  }

// ----------------------------------------------------------------- Personal files section

@Composable
private fun PersonalFilesSection(onOpenImageChat: () -> Unit) {
  SectionContainer(
    title = "Personal Files Analysis",
    subtitle = "Reports, scans and photos you've analyzed",
    icon = Icons.Outlined.Description,
    iconTint = Color(0xFFEF6C00),
  ) {
    EmptyStateRow(
      headline = "No reports analyzed yet",
      body =
        "Upload a scan or lab report inside the chat — SUKHAM will read it on-device and log " +
          "the analysis here for future reference.",
      ctaLabel = "Upload a report",
      onCta = onOpenImageChat,
      tint = Color(0xFFEF6C00),
    )
  }
}

// ----------------------------------------------------------------- Yoga history section

@Composable
private fun YogaHistorySection(onOpenYoga: () -> Unit) {
  SectionContainer(
    title = "Yoga Coaching",
    subtitle = "Past pose feedback sessions",
    icon = Icons.Outlined.SelfImprovement,
    iconTint = Color(0xFF7CB342),
  ) {
    EmptyStateRow(
      headline = "No coaching sessions yet",
      body =
        "Snap a photo of yourself in a yoga pose to get on-device form feedback. Sessions you " +
          "save will appear here.",
      ctaLabel = "Start a session",
      onCta = onOpenYoga,
      tint = Color(0xFF7CB342),
    )
  }
}

// ----------------------------------------------------------------------------- shared bits

@Composable
private fun SectionContainer(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = InsightColors.Card),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(iconTint.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
          Text(
            title,
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = InsightColors.Title,
              ),
          )
          Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall.copy(color = InsightColors.Body),
          )
        }
      }
      Spacer(Modifier.height(14.dp))
      content()
    }
  }
}

@Composable
private fun EmptyStateRow(
  headline: String,
  body: String,
  ctaLabel: String,
  onCta: () -> Unit,
  tint: Color,
) {
  Column {
    Text(
      headline,
      style =
        MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.SemiBold,
          color = InsightColors.Title,
        ),
    )
    Spacer(Modifier.height(4.dp))
    Text(
      body,
      style = MaterialTheme.typography.bodySmall.copy(color = InsightColors.Body),
    )
    Spacer(Modifier.height(12.dp))
    LinkRow(label = ctaLabel, onClick = onCta, accent = tint)
  }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit, accent: Color = InsightColors.PurpleDark) {
  Surface(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    color = accent.copy(alpha = 0.10f),
    shape = RoundedCornerShape(14.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        label,
        modifier = Modifier.weight(1f),
        style =
          MaterialTheme.typography.bodyMedium.copy(
            color = accent,
            fontWeight = FontWeight.SemiBold,
          ),
      )
      Icon(
        Icons.AutoMirrored.Rounded.ArrowForward,
        contentDescription = null,
        tint = accent,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}
