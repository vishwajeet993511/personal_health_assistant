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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.google.ai.edge.gallery.ui.home.SukhamBottomNav
import com.google.ai.edge.gallery.ui.home.SukhamNavTab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.wearables.WearableSnapshot
import com.google.ai.edge.gallery.ui.wearables.loadLatestWearableSnapshot

import com.google.ai.edge.gallery.ui.theme.SukhamColors
import com.google.ai.edge.gallery.ui.theme.isSukhamDarkTheme

// MARK: - Zen-Tech Theme Palette
private object SukhamInsightColors {
  val ScreenBg: Color @Composable get() = SukhamColors.ScreenBg
  val CardBg: Color @Composable get() = SukhamColors.CardBg
  val CardBorder: Color @Composable get() = SukhamColors.CardBorder
  val TitleBlack: Color @Composable get() = SukhamColors.TitleBlack
  val BodyGray: Color @Composable get() = SukhamColors.BodyGray

  val SagePrimary: Color @Composable get() = SukhamColors.SagePrimary
  val SageContainer: Color @Composable get() = SukhamColors.SageContainer
  val SageDark: Color @Composable get() = SukhamColors.SageDark

  val LavenderPrimary: Color @Composable get() = SukhamColors.LavenderPrimary
  val LavenderSoft: Color @Composable get() = SukhamColors.LavenderSoft

  val AmberPrimary: Color @Composable get() = SukhamColors.StreakAmber
  val AmberSoft: Color @Composable get() = SukhamColors.StreakGlow

  val ChipBg: Color @Composable get() = SukhamColors.ChipBg
}

enum class InsightFilter(val label: String) {
  ALL("All Highlights"),
  SPINE("Spine & Posture"),
  BALANCE("Kinetic Balance"),
  ASANA("Asana Mastery"),
  VITALS("Sleep & Vitals"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
  onNavigateUp: () -> Unit,
  onOpenWearablesAnalysis: () -> Unit,
  onOpenImageChat: () -> Unit,
  onOpenYogaCoach: () -> Unit,
  onHomeClicked: () -> Unit = onNavigateUp,
  onProgramsClicked: () -> Unit = {},
  onProfileClicked: () -> Unit = {},
) {
  val context = LocalContext.current
  var snapshot by remember { mutableStateOf<WearableSnapshot?>(null) }
  var selectedFilter by remember { mutableStateOf(InsightFilter.ALL) }

  LaunchedEffect(Unit) {
    snapshot = loadLatestWearableSnapshot(context)
  }

  Scaffold(
    containerColor = SukhamInsightColors.ScreenBg,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              "Health & Mobility Insights",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamInsightColors.TitleBlack,
                fontSize = 17.5.sp,
              ),
            )
            Text(
              "AI Kinetic & Biometric Synthesis",
              style = MaterialTheme.typography.bodySmall.copy(
                color = SukhamInsightColors.BodyGray,
                fontSize = 11.5.sp,
              ),
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
              tint = SukhamInsightColors.TitleBlack,
            )
          }
        },
        actions = {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = SukhamInsightColors.SageContainer,
            border = BorderStroke(1.dp, SukhamInsightColors.SagePrimary.copy(alpha = 0.25f)),
            modifier = Modifier.padding(end = 16.dp),
          ) {
            Text(
              text = "Oct 7 – 13",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamInsightColors.SageDark,
                fontSize = 11.sp,
              ),
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SukhamInsightColors.ScreenBg,
        ),
      )
    },
    bottomBar = {
      SukhamBottomNav(
        currentTab = SukhamNavTab.INSIGHTS,
        onHomeClick = onHomeClicked,
        onProgramsClick = onProgramsClicked,
        onProfileClick = onProfileClicked,
        onCentralClick = onOpenImageChat,
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Weekly Consistency Calendar Strip
      WeeklyConsistencyStrip()

      // 2. Hero Card: Biological Mobility Age
      MobilityAgeHeroCard()

      // 3. Featured Cross-Modal Synthesis (Wearables x Vision AI)
      CrossModalDiscoveryCard()

      // 4. Interactive Filter Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        InsightFilter.values().forEach { filter ->
          FilterChip(
            selected = selectedFilter == filter,
            onClick = { selectedFilter = filter },
            label = {
              Text(
                filter.label,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 12.sp,
                ),
              )
            },
            shape = RoundedCornerShape(20.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = SukhamInsightColors.SagePrimary,
              selectedLabelColor = SukhamColors.ScreenBg,
              containerColor = SukhamInsightColors.ChipBg,
              labelColor = SukhamInsightColors.BodyGray,
            ),
            border = BorderStroke(
              1.dp,
              if (selectedFilter == filter) SukhamInsightColors.SagePrimary else SukhamInsightColors.CardBorder,
            ),
          )
        }
      }

      // Filterable Sections
      val showBalance = selectedFilter == InsightFilter.ALL || selectedFilter == InsightFilter.BALANCE
      val showAsana = selectedFilter == InsightFilter.ALL || selectedFilter == InsightFilter.ASANA
      val showSpine = selectedFilter == InsightFilter.ALL || selectedFilter == InsightFilter.SPINE
      val showVitals = selectedFilter == InsightFilter.ALL || selectedFilter == InsightFilter.VITALS

      if (showBalance) {
        // 5. Lateral Symmetry & Balance Radar Card
        LateralSymmetryCard()
      }

      if (showAsana) {
        // 6. Pocket Yoga 6-Category Mastery Breakdown
        PocketYogaMasteryOverviewCard(onExplorePrograms = onOpenYogaCoach)
      }

      if (showSpine || selectedFilter == InsightFilter.ALL) {
        // 7. Daily AI Actionable Prescription Card
        DailyPrescriptionCard(onStartPrescription = onOpenYogaCoach)
      }

      if (showVitals) {
        // 8. Wearables & Lab Biometrics Quick Hub
        WearablesBiometricsQuickCard(onOpenWearables = onOpenWearablesAnalysis, onOpenReports = onOpenImageChat)
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

// -----------------------------------------------------------------------------
// Component 1: Weekly Consistency Strip
// -----------------------------------------------------------------------------
@Composable
private fun WeeklyConsistencyStrip() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamInsightColors.CardBg),
    border = BorderStroke(1.dp, SukhamInsightColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val days = listOf("M", "T", "W", "T", "F", "S", "S")
      days.forEachIndexed { index, day ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Text(
            text = day,
            style = MaterialTheme.typography.labelSmall.copy(
              color = SukhamInsightColors.BodyGray,
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp,
            ),
          )

          when {
            index < 5 -> {
              Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = SukhamInsightColors.SageContainer,
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = SukhamInsightColors.SagePrimary,
                    modifier = Modifier.size(15.dp),
                  )
                }
              }
            }
            index == 5 -> {
              Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = SukhamInsightColors.SagePrimary,
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "12",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      fontSize = 11.sp,
                    ),
                  )
                }
              }
            }
            else -> {
              Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = SukhamInsightColors.ChipBg,
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "13",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = SukhamInsightColors.TitleBlack,
                      fontSize = 11.sp,
                    ),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// Component 2: Hero Biological Mobility Age Card
// -----------------------------------------------------------------------------
@Composable
private fun MobilityAgeHeroCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamInsightColors.CardBg),
    border = BorderStroke(1.dp, SukhamInsightColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(SukhamInsightColors.CardBg)
        .padding(18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Circular Radial Ring Gauge (88 Index)
        var startAnim by remember { mutableStateOf(false) }
        val ringProgress by animateFloatAsState(
          targetValue = if (startAnim) 0.88f else 0f,
          animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
          label = "mobilityGauge"
        )
        LaunchedEffect(Unit) {
          startAnim = true
        }

        Box(
          modifier = Modifier.size(86.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            progress = { ringProgress },
            modifier = Modifier.fillMaxSize(),
            color = SukhamInsightColors.SagePrimary,
            trackColor = SukhamInsightColors.SageContainer,
            strokeWidth = 7.dp,
            strokeCap = StrokeCap.Round,
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${(ringProgress * 100).toInt()}",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = SukhamInsightColors.SageDark,
                fontSize = 24.sp,
                lineHeight = 24.sp,
              ),
            )
            Text(
              text = "INDEX",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamInsightColors.BodyGray,
                letterSpacing = 0.5.sp,
                fontSize = 9.sp,
              ),
            )
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = SukhamInsightColors.SageContainer,
          ) {
            Text(
              text = "KINETIC BIOMARKER",
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamInsightColors.SageDark,
                fontSize = 9.5.sp,
                letterSpacing = 0.6.sp,
              ),
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Mobility Age: 24 yrs",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              color = SukhamInsightColors.TitleBlack,
              fontSize = 18.sp,
            ),
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "You are 4 years younger in joint mobility & spinal flexibility than your chronological age (28).",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamInsightColors.BodyGray,
              fontSize = 11.5.sp,
              lineHeight = 16.sp,
            ),
          )
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// Component 3: Cross-Modal Discovery Card
// -----------------------------------------------------------------------------
@Composable
private fun CrossModalDiscoveryCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamInsightColors.CardBg),
    border = BorderStroke(1.dp, SukhamInsightColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(SukhamInsightColors.CardBg)
        .padding(18.dp),
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SukhamInsightColors.LavenderSoft,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Outlined.ElectricBolt,
                contentDescription = null,
                tint = SukhamInsightColors.LavenderPrimary,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "CROSS-MODAL DISCOVERY",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = SukhamInsightColors.LavenderPrimary,
                  fontSize = 10.sp,
                  letterSpacing = 0.5.sp,
                ),
              )
            }
          }

          Text(
            text = "+14% Stability",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              color = SukhamInsightColors.SagePrimary,
              fontSize = 11.5.sp,
            ),
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "High Sleep Recovery (>85%) strongly boosted your 10s Pose Stability today.",
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamInsightColors.TitleBlack,
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
          ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Metric Comparison Box
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = SukhamInsightColors.ChipBg,
          border = BorderStroke(1.dp, SukhamInsightColors.CardBorder),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Overnight HRV & Sleep",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = SukhamInsightColors.BodyGray,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Medium,
                ),
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "88% (Restorative)",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = SukhamInsightColors.TitleBlack,
                  fontSize = 13.5.sp,
                ),
              )
            }

            Box(
              modifier = Modifier
                .width(1.dp)
                .height(34.dp)
                .background(SukhamInsightColors.CardBorder),
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "10s Hold Equilibrium",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = SukhamInsightColors.BodyGray,
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Medium,
                ),
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "94% (Near Zero Sway)",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = SukhamInsightColors.TitleBlack,
                  fontSize = 13.5.sp,
                ),
              )
            }
          }
        }
      }
    }
  }
}

// -----------------------------------------------------------------------------
// Component 4: Lateral Symmetry Radar Card
// -----------------------------------------------------------------------------
@Composable
private fun LateralSymmetryCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamInsightColors.CardBg),
    border = BorderStroke(1.dp, SukhamInsightColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Lateral Symmetry Radar",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamInsightColors.TitleBlack,
            fontSize = 15.5.sp,
          ),
        )
        Text(
          text = "Left vs. Right",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamInsightColors.SagePrimary,
            fontSize = 11.5.sp,
          ),
        )
      }

      Text(
        text = "Evaluated across 10-second unilateral holds (Tree, Warrior II, Side Plank).",
        style = MaterialTheme.typography.bodySmall.copy(
          color = SukhamInsightColors.BodyGray,
          fontSize = 11.5.sp,
        ),
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Left Side Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Left Side",
          modifier = Modifier.width(70.dp),
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamInsightColors.TitleBlack,
            fontSize = 12.sp,
          ),
        )
        LinearProgressIndicator(
          progress = { 0.47f },
          modifier = Modifier
            .weight(1f)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = SukhamInsightColors.SagePrimary,
          trackColor = SukhamInsightColors.ChipBg,
          strokeCap = StrokeCap.Round,
        )
        Text(
          text = "47%",
          modifier = Modifier.width(42.dp),
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamInsightColors.TitleBlack,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
          ),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Right Side Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Right Side",
          modifier = Modifier.width(70.dp),
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamInsightColors.TitleBlack,
            fontSize = 12.sp,
          ),
        )
        LinearProgressIndicator(
          progress = { 0.53f },
          modifier = Modifier
            .weight(1f)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = SukhamInsightColors.SagePrimary,
          trackColor = SukhamInsightColors.ChipBg,
          strokeCap = StrokeCap.Round,
        )
        Text(
          text = "53%",
          modifier = Modifier.width(42.dp),
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamInsightColors.TitleBlack,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
          ),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Tip Card
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = SukhamInsightColors.AmberSoft,
        border = BorderStroke(1.dp, SukhamInsightColors.AmberPrimary.copy(alpha = 0.2f)),
      ) {
        Text(
          text = "💡 Tip: Mild right-hip dominance detected (+6%). Reinforce left glute medius holds during unilateral practice.",
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          style = MaterialTheme.typography.bodySmall.copy(
            color = SukhamInsightColors.AmberPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 15.sp,
          ),
        )
      }
    }
  }
}

// -----------------------------------------------------------------------------
// Component 5: Pocket Yoga 6-Category Mastery Breakdown
// -----------------------------------------------------------------------------
@Composable
private fun PocketYogaMasteryOverviewCard(onExplorePrograms: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamInsightColors.CardBg),
    border = BorderStroke(1.dp, SukhamInsightColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Asana Dictionary Mastery",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamInsightColors.TitleBlack,
              fontSize = 15.sp,
            ),
          )
          Text(
            text = "Pocket Yoga Curriculum",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamInsightColors.BodyGray,
              fontSize = 11.sp,
            ),
          )
        }
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = SukhamInsightColors.SageContainer,
        ) {
          Text(
            text = "21/29 (86%)",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamInsightColors.SageDark,
              fontSize = 11.sp,
            ),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 6 Category Grid
      val categories = listOf(
        Triple("Standing", 0.88f, SukhamInsightColors.SagePrimary),
        Triple("Seated", 0.74f, SukhamInsightColors.SagePrimary),
        Triple("Supine", 0.95f, SukhamInsightColors.SagePrimary),
        Triple("Prone", 0.60f, SukhamInsightColors.AmberPrimary),
        Triple("Support", 0.52f, SukhamInsightColors.AmberPrimary),
        Triple("Inversions", 0.40f, SukhamInsightColors.AmberPrimary),
      )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in categories.indices step 2) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CategoryProgressCell(categories[i], modifier = Modifier.weight(1f))
            if (i + 1 < categories.size) {
              CategoryProgressCell(categories[i + 1], modifier = Modifier.weight(1f))
            } else {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onExplorePrograms),
        color = SukhamInsightColors.SageContainer,
        shape = RoundedCornerShape(12.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Open Asana Dictionary & Audits",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamInsightColors.SageDark,
              fontSize = 12.5.sp,
            ),
          )
          Icon(
            Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = SukhamInsightColors.SageDark,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun CategoryProgressCell(
  data: Triple<String, Float, Color>,
  modifier: Modifier = Modifier,
) {
  val (name, progress, barColor) = data
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = SukhamInsightColors.ChipBg,
    border = BorderStroke(1.dp, SukhamInsightColors.CardBorder),
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = name,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamInsightColors.TitleBlack,
            fontSize = 11.5.sp,
          ),
        )
        Text(
          text = "${(progress * 100).toInt()}%",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = barColor,
            fontSize = 11.5.sp,
          ),
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = barColor,
        trackColor = SukhamInsightColors.ChipBg,
        strokeCap = StrokeCap.Round,
      )
    }
  }
}

// -----------------------------------------------------------------------------
// Component 6: Daily AI Prescription Action Card
// -----------------------------------------------------------------------------
@Composable
private fun DailyPrescriptionCard(onStartPrescription: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSukhamDarkTheme()) Color(0xFF1B382F) else Color(0xFF2E4E43)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.2f),
      ) {
        Text(
          text = "DAILY AI PRESCRIPTION",
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 9.5.sp,
            letterSpacing = 0.6.sp,
          ),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "3-Min Cervical Spine Reset",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          fontSize = 17.sp,
        ),
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Based on detected neck forward tilt, decompress with Cat-Cow and Sphinx pose before afternoon desk work.",
        style = MaterialTheme.typography.bodySmall.copy(
          color = Color.White.copy(alpha = 0.9f),
          fontSize = 12.sp,
          lineHeight = 16.5.sp,
        ),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = onStartPrescription,
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp),
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isSukhamDarkTheme()) Color(0xFF67BFA5) else Color.White
        ),
      ) {
        Text(
          text = "Start 3-Minute Routine",
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = if (isSukhamDarkTheme()) Color(0xFF080908) else Color(0xFF2E4E43),
            fontSize = 13.5.sp,
          ),
        )
      }
    }
  }
}

// -----------------------------------------------------------------------------
// Component 7: Wearables & Lab Biometrics Quick Hub
// -----------------------------------------------------------------------------
@Composable
private fun WearablesBiometricsQuickCard(
  onOpenWearables: () -> Unit,
  onOpenReports: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamInsightColors.CardBg),
    border = BorderStroke(1.dp, SukhamInsightColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Biometrics & Lab Integration",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = SukhamInsightColors.TitleBlack,
          fontSize = 15.5.sp,
        ),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Wearable sensors and blood biomarker tracking linked to motion performance.",
        style = MaterialTheme.typography.bodySmall.copy(
          color = SukhamInsightColors.BodyGray,
          fontSize = 11.5.sp,
        ),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenWearables),
          color = SukhamInsightColors.SageContainer,
          shape = RoundedCornerShape(14.dp),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Outlined.Watch, contentDescription = null, tint = SukhamInsightColors.SageDark)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Wearables",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = SukhamInsightColors.SageDark,
                ),
              )
              Text(
                text = "HRV & Sleep",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = SukhamInsightColors.BodyGray,
                  fontSize = 10.sp,
                ),
              )
            }
          }
        }

        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenReports),
          color = SukhamInsightColors.LavenderSoft,
          shape = RoundedCornerShape(14.dp),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Outlined.MonitorHeart, contentDescription = null, tint = SukhamInsightColors.LavenderPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Lab Reports",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = SukhamInsightColors.LavenderPrimary,
                ),
              )
              Text(
                text = "hs-CRP & Biomarkers",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = SukhamInsightColors.BodyGray,
                  fontSize = 10.sp,
                ),
              )
            }
          }
        }
      }
    }
  }
}
