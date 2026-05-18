/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.google.ai.edge.gallery.ui.programs

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private object ProgramColors {
  val ScreenBg = Color(0xFFF5F4EF)
  val Card = Color.White
  val Title = Color(0xFF1A1A1A)
  val Body = Color(0xFF5C5C66)
  val Purple = Color(0xFF7E57C2)
  val PurpleDark = Color(0xFF5E35B1)
  val Soft = Color(0xFFEDE7F6)
  val Peach = Color(0xFFFFAB91)
  val Mint = Color(0xFF80CBC4)
  val Sun = Color(0xFFFFB300)
  val Sky = Color(0xFF81D4FA)
}

private data class ProgramItem(
  val icon: ImageVector,
  val tint: Color,
  val title: String,
  val subtitle: String,
  val detail: String,
)

private val FOOD_ITEMS =
  listOf(
    ProgramItem(
      icon = Icons.Outlined.Restaurant,
      tint = ProgramColors.Peach,
      title = "Low-GI breakfast",
      subtitle = "Steel-cut oats · berries · chia · walnuts",
      detail = "≈ 350 kcal · 12 g protein · suits Diabetes management.",
    ),
    ProgramItem(
      icon = Icons.Outlined.LocalCafe,
      tint = ProgramColors.Sun,
      title = "Mid-morning tea",
      subtitle = "Cinnamon green tea, no sugar",
      detail = "Cinnamon may help post-meal glucose response.",
    ),
    ProgramItem(
      icon = Icons.Outlined.Restaurant,
      tint = ProgramColors.Mint,
      title = "Lunch plate",
      subtitle = "Grilled paneer · quinoa · sautéed greens",
      detail = "High-protein, fibre-rich, ≈ 520 kcal.",
    ),
    ProgramItem(
      icon = Icons.Outlined.Restaurant,
      tint = ProgramColors.Purple,
      title = "Dinner",
      subtitle = "Moong dal soup · cucumber salad · 2 phulkas",
      detail = "Eat by 19:30 to support sleep and morning glucose.",
    ),
    ProgramItem(
      icon = Icons.Outlined.WaterDrop,
      tint = ProgramColors.Sky,
      title = "Hydration plan",
      subtitle = "8 × 300 ml glasses spread through the day",
      detail = "Add a pinch of salt + lemon to one glass post-workout.",
    ),
  )

private val EXERCISE_ITEMS =
  listOf(
    ProgramItem(
      icon = Icons.Outlined.WbSunny,
      tint = ProgramColors.Sun,
      title = "Morning · Surya Namaskar",
      subtitle = "6 rounds, slow tempo",
      detail = "Warms up the body and gently raises heart rate.",
    ),
    ProgramItem(
      icon = Icons.Outlined.DirectionsRun,
      tint = ProgramColors.Peach,
      title = "Brisk walk",
      subtitle = "30 min after lunch · 100 steps/min",
      detail = "Post-meal walking blunts the glucose spike.",
    ),
    ProgramItem(
      icon = Icons.Outlined.SelfImprovement,
      tint = ProgramColors.Mint,
      title = "Yoga · stress release",
      subtitle = "Balasana · Setu Bandh · Viparita Karani",
      detail = "Hold each pose for 5 slow breaths, evening before bed.",
    ),
    ProgramItem(
      icon = Icons.Outlined.DirectionsRun,
      tint = ProgramColors.Purple,
      title = "Strength · 2× per week",
      subtitle = "Body-weight squats · push-ups · rows · plank",
      detail = "3 rounds · 8–12 reps · 60 s rest. Builds muscle, helps insulin sensitivity.",
    ),
    ProgramItem(
      icon = Icons.Outlined.NightsStay,
      tint = ProgramColors.PurpleDark,
      title = "Wind-down stretch",
      subtitle = "10 min mobility before bed",
      detail = "Hip openers + neck rolls to reduce night wake-ups.",
    ),
  )

private val ROUTINE_ITEMS =
  listOf(
    ProgramItem(
      icon = Icons.Outlined.WbSunny,
      tint = ProgramColors.Sun,
      title = "06:30 · Hydrate + 5-min sun",
      subtitle = "500 ml warm water · 5 min daylight on skin",
      detail = "Anchors circadian rhythm and helps morning alertness.",
    ),
    ProgramItem(
      icon = Icons.Outlined.SelfImprovement,
      tint = ProgramColors.Mint,
      title = "07:00 · Yoga + breath work",
      subtitle = "20 min Surya Namaskar + Bhramari",
      detail = "Lowers resting heart rate over 4–6 weeks.",
    ),
    ProgramItem(
      icon = Icons.Outlined.Restaurant,
      tint = ProgramColors.Peach,
      title = "08:00 · Low-GI breakfast",
      subtitle = "See Food tab for plate",
      detail = "Eat within 1 h of waking.",
    ),
    ProgramItem(
      icon = Icons.Outlined.AccessTime,
      tint = ProgramColors.Purple,
      title = "13:30 · Post-lunch walk",
      subtitle = "30 min brisk walk",
      detail = "Cuts post-meal glucose by ≈ 17 %.",
    ),
    ProgramItem(
      icon = Icons.Outlined.AccessTime,
      tint = ProgramColors.Sky,
      title = "18:00 · Hydration check",
      subtitle = "Top up to 2.5 L target",
      detail = "Refill bottle for the evening.",
    ),
    ProgramItem(
      icon = Icons.Outlined.NightsStay,
      tint = ProgramColors.PurpleDark,
      title = "22:30 · Wind-down",
      subtitle = "Phone away · 10 min stretch · dim lights",
      detail = "Aim for 8 h in bed, lights-out by 23:00.",
    ),
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(onNavigateUp: () -> Unit) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Food", "Exercise", "Daily Routine")
  val items =
    when (selectedTab) {
      0 -> FOOD_ITEMS
      1 -> EXERCISE_ITEMS
      else -> ROUTINE_ITEMS
    }
  val tabHeadline =
    when (selectedTab) {
      0 -> "Food recommendations"
      1 -> "Exercise recommendations"
      else -> "Smart daily routine"
    }
  val tabSubtitle =
    when (selectedTab) {
      0 ->
        "On-device suggestions matched to your profile — Diabetes-friendly, low-GI, " +
          "easy to source."
      1 ->
        "Mix of mobility, cardio and strength designed around your current sleep and stress " +
          "levels."
      else ->
        "A sample day SUKHAM would build for you. Personalised plans come online once your " +
          "history is rich enough."
    }

  Scaffold(
    containerColor = ProgramColors.ScreenBg,
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Programs",
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
            containerColor = ProgramColors.ScreenBg,
            titleContentColor = ProgramColors.Title,
          ),
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding),
    ) {
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = ProgramColors.ScreenBg,
        contentColor = ProgramColors.PurpleDark,
        indicator = { positions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
            color = ProgramColors.PurpleDark,
          )
        },
      ) {
        tabs.forEachIndexed { idx, title ->
          Tab(
            selected = idx == selectedTab,
            onClick = { selectedTab = idx },
            text = {
              Text(
                title,
                style =
                  MaterialTheme.typography.labelLarge.copy(
                    fontWeight =
                      if (idx == selectedTab) FontWeight.Bold else FontWeight.Medium,
                  ),
              )
            },
          )
        }
      }

      Column(
        modifier =
          Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = ProgramColors.Soft,
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              tabHeadline,
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = ProgramColors.Title,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
              tabSubtitle,
              style = MaterialTheme.typography.bodySmall.copy(color = ProgramColors.Body),
            )
            Spacer(Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = ProgramColors.PurpleDark.copy(alpha = 0.12f),
            ) {
              Text(
                "Coming soon · personalised generation",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    color = ProgramColors.PurpleDark,
                    fontWeight = FontWeight.SemiBold,
                  ),
              )
            }
          }
        }

        items.forEach { item -> ProgramRow(item) }
        Spacer(Modifier.height(20.dp))
      }
    }
  }
}

@Composable
private fun ProgramRow(item: ProgramItem) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = ProgramColors.Card),
  ) {
    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(item.tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(20.dp))
      }
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          item.title,
          style =
            MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.SemiBold,
              color = ProgramColors.Title,
            ),
        )
        Text(
          item.subtitle,
          style = MaterialTheme.typography.bodySmall.copy(color = ProgramColors.Body),
        )
        Spacer(Modifier.height(4.dp))
        Text(
          item.detail,
          style =
            MaterialTheme.typography.labelSmall.copy(
              color = ProgramColors.Body,
              fontWeight = FontWeight.Medium,
            ),
        )
      }
    }
  }
}
