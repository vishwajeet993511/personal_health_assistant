/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.google.ai.edge.gallery.ui.wearables

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.runtime.runtimeHelper
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "WearablesAnalysisScreen"
private const val WEARABLES_ASSET = "wearables_records.csv"

private const val MAX_AI_RESPONSE_CHARS = 700
private const val MAX_AI_RESPONSE_MS = 90_000L

// =====================================================================================
// Domain model + repository
// =====================================================================================

/** Single wearable snapshot parsed from one CSV row. */
data class WearableSnapshot(
  val userId: String,
  val age: Int,
  val gender: String,
  val weightKg: Double,
  val heightCm: Double,
  val medicalConditions: String,
  val onMedication: Boolean,
  val smoker: Boolean,
  val alcoholConsumption: String,
  val timestamp: String,
  val dayOfWeek: String,
  val sleepDuration: Double,
  val deepSleep: Double,
  val remSleep: Double,
  val wakeups: Int,
  val snoring: Boolean,
  val heartRate: Int,
  val spo2: Double,
  val ecg: String,
  val caloriesIntake: Int,
  val waterLiters: Double,
  val stressLevel: String,
  val mood: String,
  val skinTempC: Double,
  val bodyFatPct: Double,
  val muscleMassPct: Double,
  val healthScore: Double,
  val anomaly: Int,
) {
  val bmi: Double = weightKg / (heightCm / 100.0).pow(2.0)

  val bmiCategory: String =
    when {
      bmi < 18.5 -> "Underweight"
      bmi < 25.0 -> "Healthy"
      bmi < 30.0 -> "Overweight"
      bmi < 35.0 -> "Obese · Class I"
      bmi < 40.0 -> "Obese · Class II"
      else -> "Obese · Class III"
    }

  val bmiVerdict: Verdict =
    when {
      bmi in 18.5..24.9 -> Verdict.GOOD
      bmi in 25.0..29.9 -> Verdict.WARN
      else -> Verdict.BAD
    }

  val lightSleep: Double = (sleepDuration - deepSleep - remSleep).coerceAtLeast(0.0)

  val sleepVerdict: Verdict =
    when {
      sleepDuration in 7.0..9.0 && wakeups <= 1 -> Verdict.GOOD
      sleepDuration in 6.0..9.5 -> Verdict.WARN
      else -> Verdict.BAD
    }

  val hydrationTarget = 2.5
  val hydrationPct: Float = (waterLiters / hydrationTarget).coerceIn(0.0, 1.0).toFloat()
  val hydrationVerdict: Verdict =
    when {
      waterLiters >= 2.5 -> Verdict.GOOD
      waterLiters >= 2.0 -> Verdict.WARN
      else -> Verdict.BAD
    }

  val hrVerdict: Verdict =
    when {
      heartRate in 60..78 -> Verdict.GOOD
      heartRate in 79..88 -> Verdict.WARN
      else -> Verdict.BAD
    }
  val spo2Verdict: Verdict =
    when {
      spo2 >= 97.0 -> Verdict.GOOD
      spo2 >= 95.0 -> Verdict.WARN
      else -> Verdict.BAD
    }

  /**
   * Daily-progress summary used by the Share Progress flow. Steps/calories-burnt aren't in the
   * raw CSV, so we estimate them from weight + intake. Replace with live sensor values when a
   * step counter is integrated.
   */
  val dailyProgress: DailyProgress
    get() {
      // Mifflin-St Jeor BMR ≈ 10·kg + 6.25·cm − 5·age (gender-neutral mid value), times an
      // activity factor of 1.35 for "lightly active" — gives a sane calories-burnt estimate.
      val bmr = 10 * weightKg + 6.25 * heightCm - 5 * age
      val caloriesBurnt = (bmr * 1.35).roundToInt()
      // Rough step estimate: ~25 % of calories-burnt scaled to steps. Yields ~4–6 k for the
      // sample row, which reads as a realistic "low-active day".
      val steps = (caloriesBurnt * 2.5).roundToInt()
      return DailyProgress(
        steps = steps,
        caloriesBurnt = caloriesBurnt,
        caloriesEaten = caloriesIntake,
        sleepHours = sleepDuration,
        sleepWakeups = wakeups,
        waterLiters = waterLiters,
        heartRateBpm = heartRate,
        healthScore = healthScore,
      )
    }

  /** Local hardcoded insights — kept so the screen renders even before Gemma is ready. */
  val localInsights: List<Insight>
    get() =
      listOf(
        Insight(
          severity = bmiVerdict,
          title = "BMI · ${"%.1f".format(bmi)} ($bmiCategory)",
          body =
            "Weight $weightKg kg / height ${heightCm.roundToInt()} cm. Combined with " +
              medicalConditions.lowercase() +
              " this is the highest-impact lever. A −5% weight loss meaningfully improves A1C.",
        ),
        Insight(
          severity = Verdict.BAD,
          title = "Lifestyle stack",
          body =
            "$medicalConditions · ${if (smoker) "smoker" else "non-smoker"} · " +
              "$alcoholConsumption alcohol. Cutting nicotine and scaling alcohol to " +
              "≤2 drinks/week is the highest-yield intervention.",
        ),
        Insight(
          severity = sleepVerdict,
          title = "Sleep fragmented",
          body =
            "${sleepDuration.format(1)} h total with $wakeups wake-ups. Aim for a 23:00 " +
              "lights-out and avoid alcohol within 3 h of bed.",
        ),
        Insight(
          severity = hydrationVerdict,
          title = "Hydration ${waterLiters.format(2)} L (target ${hydrationTarget.format(1)} L)",
          body =
            "Under-hydrated by ≈${(hydrationTarget - waterLiters).coerceAtLeast(0.0).format(1)} L " +
              "— relevant for diabetics where dehydration can spike fasting glucose.",
        ),
        Insight(
          severity = spo2Verdict,
          title = "SpO₂ ${spo2.format(1)}%",
          body =
            "Paired with $wakeups wake-ups, worth a screening for obstructive sleep apnea, " +
              "especially given BMI.",
        ),
        Insight(
          severity = Verdict.GOOD,
          title = "ECG $ecg, snoring ${if (snoring) "yes" else "no"}",
          body =
            "Useful baselines to keep an eye on as weight, alcohol and sleep change.",
        ),
      )

  /** Compact prompt body for Gemma. Stays under ~120 tokens to keep prefill fast. */
  fun toLlmPrompt(): String =
    buildString {
      appendLine("You are SUKHAM AI, an on-device wellness coach.")
      appendLine("Analyze this wearable snapshot and reply in under 80 words as 4 bullets:")
      appendLine("- Risk: top medical risk and why")
      appendLine("- Action: one specific change to make this week")
      appendLine("- Sleep: one cue to improve sleep tonight")
      appendLine("- Watch: which metric to track next")
      appendLine()
      appendLine("Snapshot ($timestamp, $dayOfWeek):")
      appendLine(
        "- ${age}y $gender · ${weightKg.format(1)} kg / ${heightCm.roundToInt()} cm · " +
          "BMI ${bmi.format(1)} ($bmiCategory)"
      )
      appendLine(
        "- Conditions: $medicalConditions" +
          (if (onMedication) " (on meds)" else "") +
          " · ${if (smoker) "Smoker" else "Non-smoker"} · alcohol $alcoholConsumption"
      )
      appendLine(
        "- Sleep ${sleepDuration.format(1)}h: deep ${deepSleep.format(1)}h, REM " +
          "${remSleep.format(1)}h, ${wakeups} wake-ups, snoring " +
          "${if (snoring) "yes" else "no"}"
      )
      appendLine(
        "- HR ${heartRate} bpm, SpO₂ ${spo2.format(1)}%, ECG $ecg, skin " +
          "${skinTempC.format(1)}°C"
      )
      appendLine(
        "- Intake: ${caloriesIntake} kcal, water ${waterLiters.format(2)} L · stress " +
          "$stressLevel · mood $mood"
      )
      appendLine(
        "- Composition: body fat ${bodyFatPct.format(1)}%, muscle ${muscleMassPct.format(1)}%"
      )
      append("- Health score ${healthScore.format(1)}/100, anomaly flag $anomaly")
    }
}

/** Reads the bundled CSV and returns the latest record. */
suspend fun loadLatestWearableSnapshot(context: Context): WearableSnapshot? =
  withContext(Dispatchers.IO) {
    runCatching {
        context.assets.open(WEARABLES_ASSET).bufferedReader().use { reader ->
          val rows = reader.readLines().filter { it.isNotBlank() }
          require(rows.size >= 2) { "wearables_records.csv must have a header + at least one row" }
          val last = rows.last().split(",")
          WearableSnapshot(
            userId = last[0],
            age = last[1].toInt(),
            gender = last[2],
            weightKg = last[3].toDouble(),
            heightCm = last[4].toDouble(),
            medicalConditions = last[5],
            onMedication = last[6].equals("Yes", ignoreCase = true),
            smoker = last[7].equals("Yes", ignoreCase = true),
            alcoholConsumption = last[8],
            timestamp = last[9],
            dayOfWeek = last[10],
            sleepDuration = last[11].toDouble(),
            deepSleep = last[12].toDouble(),
            remSleep = last[13].toDouble(),
            wakeups = last[14].toInt(),
            snoring = last[15].equals("Yes", ignoreCase = true),
            heartRate = last[16].toInt(),
            spo2 = last[17].toDouble(),
            ecg = last[18],
            caloriesIntake = last[19].toDouble().toInt(),
            waterLiters = last[20].toDouble(),
            stressLevel = last[21],
            mood = last[22],
            skinTempC = last[23].toDouble(),
            bodyFatPct = last[24].toDouble(),
            muscleMassPct = last[25].toDouble(),
            healthScore = last[26].toDouble(),
            anomaly = last[27].toInt(),
          )
        }
      }
      .onFailure { Log.e(TAG, "Failed to parse $WEARABLES_ASSET", it) }
      .getOrNull()
  }

// =====================================================================================
// Visual helpers
// =====================================================================================

private object WearableColors {
  val ScreenBg = Color(0xFFF5F4EF)
  val CardSurface = Color.White
  val Title = Color(0xFF1A1A1A)
  val Body = Color(0xFF5C5C66)
  val Soft = Color(0xFFEDE7F6)
  val Purple = Color(0xFF7E57C2)
  val PurpleDark = Color(0xFF5E35B1)
  val Teal = Color(0xFF26A69A)
  val Peach = Color(0xFFFFAB91)
  val Sun = Color(0xFFFFB300)
  val Mint = Color(0xFF80CBC4)
  val Sky = Color(0xFF81D4FA)
  val Track = Color(0xFFE0DCEA)
  val Good = Color(0xFF43A047)
  val Warn = Color(0xFFFB8C00)
  val Bad = Color(0xFFE53935)
}

enum class Verdict(val label: String, val color: Color) {
  GOOD("Healthy", WearableColors.Good),
  WARN("Watch", WearableColors.Warn),
  BAD("Action", WearableColors.Bad),
}

data class Insight(val severity: Verdict, val title: String, val body: String)

/** Daily progress snapshot for the Share Progress flow. */
data class DailyProgress(
  val steps: Int,
  val caloriesBurnt: Int,
  val caloriesEaten: Int,
  val sleepHours: Double,
  val sleepWakeups: Int,
  val waterLiters: Double,
  val heartRateBpm: Int,
  val healthScore: Double,
)

// =====================================================================================
// Top-level screen
// =====================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearablesAnalysisScreen(
  modelManagerViewModel: ModelManagerViewModel,
  onNavigateUp: () -> Unit,
) {
  val context = LocalContext.current

  var snapshot by remember { mutableStateOf<WearableSnapshot?>(null) }
  LaunchedEffect(Unit) { snapshot = loadLatestWearableSnapshot(context) }

  Scaffold(
    containerColor = WearableColors.ScreenBg,
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Wearables Insights",
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
            containerColor = WearableColors.ScreenBg,
            titleContentColor = WearableColors.Title,
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
            "Loading latest record…",
            style = MaterialTheme.typography.bodyMedium.copy(color = WearableColors.Body),
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
      HeroProfileCard(snap)
      VitalsGrid(snap)
      SleepBreakdownCard(snap)
      BodyCompositionCard(snap)
      LifestyleRiskCard(snap)
      AskSukhamAiCard(snapshot = snap, modelManagerViewModel = modelManagerViewModel)
      InsightsCard(snap.localInsights)
      Spacer(Modifier.height(8.dp))
    }
  }
}

// ----------------------------------------------------------------------------------- Hero card

@Composable
private fun HeroProfileCard(snap: WearableSnapshot) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = WearableColors.Soft),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HealthScoreDial(score = snap.healthScore.toFloat(), modifier = Modifier.size(110.dp))
      Spacer(Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          "User ${snap.userId}",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = WearableColors.Title,
          ),
        )
        Text(
          "${snap.age} y/o · ${snap.gender} · ${snap.weightKg.format(1)} kg · " +
            "${snap.heightCm.roundToInt()} cm",
          style = MaterialTheme.typography.bodySmall.copy(color = WearableColors.Body),
        )
        Spacer(Modifier.height(10.dp))
        VerdictChip(
          label = "BMI ${snap.bmi.format(1)} · ${snap.bmiCategory}",
          verdict = snap.bmiVerdict,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          "Snapshot: ${snap.dayOfWeek}, ${snap.timestamp}",
          style = MaterialTheme.typography.labelSmall.copy(color = WearableColors.Body),
        )
      }
    }
  }
}

@Composable
private fun HealthScoreDial(score: Float, modifier: Modifier = Modifier) {
  val fraction = (score / 100f).coerceIn(0f, 1f)
  val color =
    when {
      score >= 75f -> WearableColors.Good
      score >= 55f -> WearableColors.Warn
      else -> WearableColors.Bad
    }
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val stroke = 14.dp.toPx()
      val arcSize = Size(size.width - stroke, size.height - stroke)
      val arcOffset = Offset(stroke / 2f, stroke / 2f)
      drawArc(
        color = WearableColors.Track,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = arcOffset,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
      drawArc(
        color = color,
        startAngle = 135f,
        sweepAngle = 270f * fraction,
        useCenter = false,
        topLeft = arcOffset,
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        "${score.roundToInt()}",
        style =
          MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = WearableColors.Title,
            fontSize = 28.sp,
          ),
      )
      Text(
        "Health",
        style = MaterialTheme.typography.labelSmall.copy(color = WearableColors.Body),
      )
    }
  }
}

// ------------------------------------------------------------------------------- Vitals grid

@Composable
private fun VitalsGrid(snap: WearableSnapshot) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    VitalTile(
      icon = Icons.Outlined.Favorite,
      tint = WearableColors.Bad,
      label = "Heart Rate",
      value = "${snap.heartRate}",
      unit = "bpm",
      verdict = snap.hrVerdict,
      modifier = Modifier.weight(1f),
    )
    VitalTile(
      icon = Icons.Outlined.Bolt,
      tint = WearableColors.Sky,
      label = "SpO₂",
      value = snap.spo2.format(1),
      unit = "%",
      verdict = snap.spo2Verdict,
      modifier = Modifier.weight(1f),
    )
  }
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    VitalTile(
      icon = Icons.Outlined.Thermostat,
      tint = WearableColors.Peach,
      label = "Skin Temp",
      value = snap.skinTempC.format(1),
      unit = "°C",
      verdict = Verdict.GOOD,
      modifier = Modifier.weight(1f),
    )
    VitalTile(
      icon = Icons.Outlined.MonitorHeart,
      tint = WearableColors.Mint,
      label = "ECG",
      value = snap.ecg,
      unit = "",
      verdict = Verdict.GOOD,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun VitalTile(
  icon: ImageVector,
  tint: Color,
  label: String,
  value: String,
  unit: String,
  verdict: Verdict,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.height(120.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = WearableColors.CardSurface),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(32.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(tint.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
          label,
          style = MaterialTheme.typography.labelMedium.copy(color = WearableColors.Body),
        )
      }
      Spacer(Modifier.weight(1f))
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          value,
          style =
            MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.Bold,
              color = WearableColors.Title,
            ),
        )
        if (unit.isNotEmpty()) {
          Spacer(Modifier.width(4.dp))
          Text(
            unit,
            style = MaterialTheme.typography.bodySmall.copy(color = WearableColors.Body),
            modifier = Modifier.padding(bottom = 6.dp),
          )
        }
      }
      Spacer(Modifier.height(4.dp))
      VerdictChip(label = verdict.label, verdict = verdict, dense = true)
    }
  }
}

// ----------------------------------------------------------------------------- Sleep card

@Composable
private fun SleepBreakdownCard(snap: WearableSnapshot) {
  SectionCard(
    icon = Icons.Outlined.Bedtime,
    iconTint = WearableColors.PurpleDark,
    title = "Sleep",
    headline = "${snap.sleepDuration.format(1)} h total",
    headlineSuffix = "(target 7–9 h)",
    verdict = snap.sleepVerdict,
  ) {
    StackedBar(
      segments =
        listOf(
          BarSegment("Deep", snap.deepSleep.toFloat(), WearableColors.PurpleDark),
          BarSegment("REM", snap.remSleep.toFloat(), WearableColors.Purple),
          BarSegment("Light/Awake", snap.lightSleep.toFloat(), WearableColors.Mint),
        )
    )
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      MiniStat("Deep", "${snap.deepSleep.format(1)} h")
      MiniStat("REM", "${snap.remSleep.format(1)} h")
      MiniStat("Wake-ups", "${snap.wakeups}")
      MiniStat("Snoring", if (snap.snoring) "Yes" else "No")
    }
  }
}

private data class BarSegment(val label: String, val value: Float, val color: Color)

@Composable
private fun StackedBar(segments: List<BarSegment>) {
  val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
  Column {
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(16.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(WearableColors.Track),
    ) {
      segments.forEach { seg ->
        val w = seg.value / total
        if (w > 0f) {
          Box(modifier = Modifier.weight(w).fillMaxSize().background(seg.color))
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      segments.forEach { seg ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(seg.color)
          )
          Spacer(Modifier.width(4.dp))
          Text(
            seg.label,
            style = MaterialTheme.typography.labelSmall.copy(color = WearableColors.Body),
          )
        }
      }
    }
  }
}

// --------------------------------------------------------------------- Body composition / fuel

@Composable
private fun BodyCompositionCard(snap: WearableSnapshot) {
  SectionCard(
    icon = Icons.Outlined.Spa,
    iconTint = WearableColors.Teal,
    title = "Body & Fuel",
    headline = "${snap.caloriesIntake} kcal · ${snap.waterLiters.format(2)} L water",
    headlineSuffix = null,
    verdict = snap.hydrationVerdict,
  ) {
    LabeledBar(
      label = "Hydration",
      icon = Icons.Outlined.WaterDrop,
      tint = WearableColors.Sky,
      fraction = snap.hydrationPct,
      valueText = "${snap.waterLiters.format(2)} / ${snap.hydrationTarget.format(1)} L",
    )
    Spacer(Modifier.height(10.dp))
    LabeledBar(
      label = "Body fat",
      icon = Icons.Outlined.LocalFireDepartment,
      tint = WearableColors.Sun,
      fraction = (snap.bodyFatPct / 40.0).toFloat().coerceIn(0f, 1f),
      valueText = "${snap.bodyFatPct.format(1)} %",
    )
    Spacer(Modifier.height(10.dp))
    LabeledBar(
      label = "Muscle mass",
      icon = Icons.Outlined.Bolt,
      tint = WearableColors.Teal,
      fraction = (snap.muscleMassPct / 70.0).toFloat().coerceIn(0f, 1f),
      valueText = "${snap.muscleMassPct.format(1)} %",
    )
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      MiniStat("Calories", "${snap.caloriesIntake} kcal")
      MiniStat("Stress", snap.stressLevel)
      MiniStat("Mood", snap.mood)
    }
  }
}

@Composable
private fun LabeledBar(
  label: String,
  icon: ImageVector,
  tint: Color,
  fraction: Float,
  valueText: String,
) {
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(6.dp))
      Text(
        label,
        style = MaterialTheme.typography.labelLarge.copy(color = WearableColors.Body),
      )
      Spacer(Modifier.weight(1f))
      Text(
        valueText,
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = WearableColors.Title,
          ),
      )
    }
    Spacer(Modifier.height(6.dp))
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(10.dp)
          .clip(RoundedCornerShape(5.dp))
          .background(WearableColors.Track),
    ) {
      Box(
        modifier =
          Modifier.fillMaxHeight()
            .fillMaxWidth(fraction.coerceIn(0f, 1f))
            .background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.7f), tint))),
      )
    }
  }
}

// --------------------------------------------------------------------------- Lifestyle card

@Composable
private fun LifestyleRiskCard(snap: WearableSnapshot) {
  SectionCard(
    icon = Icons.Outlined.Mood,
    iconTint = WearableColors.Peach,
    title = "Lifestyle factors",
    headline = "Stacked risk",
    headlineSuffix = null,
    verdict = Verdict.BAD,
  ) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      RiskPill(snap.medicalConditions, true)
      RiskPill("Medication", snap.onMedication)
      RiskPill("Smoker", snap.smoker)
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      RiskPill("Alcohol · ${snap.alcoholConsumption}", true)
      RiskPill("Stress · ${snap.stressLevel}", true)
      RiskPill("Anomaly", snap.anomaly != 0)
    }
  }
}

@Composable
private fun RiskPill(label: String, flagged: Boolean) {
  val bg = if (flagged) WearableColors.Bad.copy(alpha = 0.12f) else WearableColors.Good.copy(alpha = 0.14f)
  val fg = if (flagged) WearableColors.Bad else WearableColors.Good
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bg,
  ) {
    Text(
      label,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      style =
        MaterialTheme.typography.labelMedium.copy(color = fg, fontWeight = FontWeight.SemiBold),
    )
  }
}

// =====================================================================================
// Ask SUKHAM AI card — feeds the live snapshot into the text-only Gemma and streams the reply.
// =====================================================================================

@Composable
private fun AskSukhamAiCard(
  snapshot: WearableSnapshot,
  modelManagerViewModel: ModelManagerViewModel,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uiState by modelManagerViewModel.uiState.collectAsState()

  // Use the text-only chat task (LLM_CHAT) — the wearable snapshot is text, no need for the
  // vision tower's memory cost.
  val chatTask: Task? = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT)
  val chatModel: Model? = chatTask?.let { modelManagerViewModel.resolveModelForTask(it) }

  val downloadStatus = chatModel?.let { uiState.modelDownloadStatus[it.name]?.status }
  val initStatus = chatModel?.let { uiState.modelInitializationStatus[it.name]?.status }

  var aiResponse by remember { mutableStateOf("") }
  var aiInFlight by remember { mutableStateOf(false) }
  var pendingInference by remember { mutableStateOf(false) }
  var earlyStopFired by remember { mutableStateOf(false) }
  var startedAt by remember { mutableStateOf(0L) }

  // Pre-warm the text Gemma in the background — same idempotent pattern as YogaPoseScreen.
  LaunchedEffect(chatModel?.name, uiState.loadingModelAllowlist) {
    val task = chatTask
    val model = chatModel
    if (
      task != null &&
        model != null &&
        !uiState.loadingModelAllowlist &&
        downloadStatus == ModelDownloadStatusType.SUCCEEDED &&
        initStatus != ModelInitializationStatusType.INITIALIZED
    ) {
      Log.d(TAG, "Pre-warming text Gemma for wearable analysis.")
      modelManagerViewModel.initializeModel(context = context, task = task, model = model)
    }
  }

  // Reactive inference runner: once the user asks + the model reports INITIALIZED, kick off
  // generation. Avoids depending on initializeModel's onDone (it silently skips while warming).
  LaunchedEffect(pendingInference, initStatus) {
    if (
      pendingInference &&
        initStatus == ModelInitializationStatusType.INITIALIZED &&
        chatModel != null &&
        !aiInFlight
    ) {
      val model = chatModel
      pendingInference = false
      aiInFlight = true
      earlyStopFired = false
      startedAt = System.currentTimeMillis()
      val prompt = snapshot.toLlmPrompt()
      Log.d(TAG, "Running wearable analysis on ${prompt.length} chars.")
      model.runtimeHelper.runInference(
        model = model,
        input = prompt,
        images = emptyList(),
        resultListener = { partial, done, _ ->
          aiResponse += partial
          val tooLong = aiResponse.length > MAX_AI_RESPONSE_CHARS
          val tookTooLong = System.currentTimeMillis() - startedAt > MAX_AI_RESPONSE_MS
          if (!earlyStopFired && (tooLong || tookTooLong)) {
            earlyStopFired = true
            Log.d(
              TAG,
              "Early-stopping wearable analysis (chars=${aiResponse.length}," +
                " elapsedMs=${System.currentTimeMillis() - startedAt})",
            )
            model.runtimeHelper.stopResponse(model)
          }
          if (done) aiInFlight = false
        },
        cleanUpListener = { aiInFlight = false },
        onError = { msg ->
          aiInFlight = false
          aiResponse = "Error: $msg"
        },
        coroutineScope = scope,
      )
    }
  }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = WearableColors.CardSurface),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(WearableColors.Purple.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = WearableColors.PurpleDark,
            modifier = Modifier.size(20.dp),
          )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            "Ask SUKHAM AI",
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = WearableColors.Title,
              ),
          )
          Text(
            "On-device Gemma 2B reads the live snapshot above.",
            style = MaterialTheme.typography.labelSmall.copy(color = WearableColors.Body),
          )
        }
      }
      Spacer(Modifier.height(14.dp))
      Button(
        modifier = Modifier.fillMaxWidth().height(54.dp),
        enabled = !aiInFlight && !pendingInference && chatModel != null,
        colors =
          ButtonDefaults.buttonColors(containerColor = WearableColors.PurpleDark),
        onClick = {
          val task = chatTask
          val model = chatModel
          if (task == null || model == null) {
            Toast.makeText(
                context,
                "On-device Gemma not available yet. Open the chat once to install it.",
                Toast.LENGTH_LONG,
              )
              .show()
            return@Button
          }
          if (downloadStatus != ModelDownloadStatusType.SUCCEEDED) {
            Toast.makeText(
                context,
                "Download Gemma from \"Chat with SUKHAM AI\" first.",
                Toast.LENGTH_LONG,
              )
              .show()
            return@Button
          }
          aiResponse = ""
          pendingInference = true
          modelManagerViewModel.initializeModel(context = context, task = task, model = model)
        },
      ) {
        if (pendingInference || aiInFlight) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = Color.White,
            strokeWidth = 2.dp,
          )
          Spacer(Modifier.width(10.dp))
          val label =
            when {
              pendingInference && initStatus != ModelInitializationStatusType.INITIALIZED ->
                "Warming up Gemma…"
              aiResponse.isBlank() -> "Reading snapshot…"
              else -> "Analyzing…"
            }
          Text(label, color = Color.White)
        } else {
          Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White)
          Spacer(Modifier.width(8.dp))
          Text(
            "Analyze wearable data by SUKHAM AI",
            style =
              MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
              ),
          )
        }
      }
      if (aiResponse.isNotBlank()) {
        Spacer(Modifier.height(14.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = WearableColors.Soft,
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              "SUKHAM analysis",
              style =
                MaterialTheme.typography.labelMedium.copy(
                  color = WearableColors.PurpleDark,
                  fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.height(6.dp))
            Text(
              aiResponse,
              style =
                MaterialTheme.typography.bodyMedium.copy(
                  color = WearableColors.Title,
                ),
            )
          }
        }
      }
    }
  }
}

// ------------------------------------------------------------------------- Static insights

@Composable
private fun InsightsCard(insights: List<Insight>) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = WearableColors.CardSurface),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier =
            Modifier.size(36.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(WearableColors.Purple.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            Icons.Outlined.Bolt,
            contentDescription = null,
            tint = WearableColors.PurpleDark,
            modifier = Modifier.size(20.dp),
          )
        }
        Spacer(Modifier.width(12.dp))
        Column {
          Text(
            "Rule-based insights",
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = WearableColors.Title,
              ),
          )
          Text(
            "Derived directly from the snapshot above",
            style = MaterialTheme.typography.labelSmall.copy(color = WearableColors.Body),
          )
        }
      }
      Spacer(Modifier.height(14.dp))
      insights.forEachIndexed { idx, insight ->
        InsightRow(insight)
        if (idx != insights.lastIndex) {
          Spacer(Modifier.height(12.dp))
        }
      }
    }
  }
}

@Composable
private fun InsightRow(insight: Insight) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier =
        Modifier.size(10.dp)
          .clip(RoundedCornerShape(5.dp))
          .background(insight.severity.color),
    )
    Spacer(Modifier.width(10.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        insight.title,
        style =
          MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = WearableColors.Title,
          ),
      )
      Spacer(Modifier.height(2.dp))
      Text(
        insight.body,
        style = MaterialTheme.typography.bodySmall.copy(color = WearableColors.Body),
      )
    }
  }
}

// ----------------------------------------------------------------------------- shared bits

@Composable
private fun SectionCard(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  headline: String,
  headlineSuffix: String?,
  verdict: Verdict,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = WearableColors.CardSurface),
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            title,
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = WearableColors.Title,
              ),
          )
          Row {
            Text(
              headline,
              style =
                MaterialTheme.typography.bodySmall.copy(
                  color = WearableColors.Title,
                  fontWeight = FontWeight.Medium,
                ),
            )
            if (headlineSuffix != null) {
              Spacer(Modifier.width(6.dp))
              Text(
                headlineSuffix,
                style = MaterialTheme.typography.bodySmall.copy(color = WearableColors.Body),
              )
            }
          }
        }
        VerdictChip(label = verdict.label, verdict = verdict, dense = true)
      }
      Spacer(Modifier.height(14.dp))
      content()
    }
  }
}

@Composable
private fun VerdictChip(label: String, verdict: Verdict, dense: Boolean = false) {
  Surface(
    shape = RoundedCornerShape(if (dense) 10.dp else 14.dp),
    color = verdict.color.copy(alpha = 0.14f),
  ) {
    Text(
      label,
      modifier =
        Modifier.padding(
          horizontal = if (dense) 8.dp else 12.dp,
          vertical = if (dense) 4.dp else 6.dp,
        ),
      style =
        MaterialTheme.typography.labelMedium.copy(
          color = verdict.color,
          fontWeight = FontWeight.SemiBold,
        ),
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun MiniStat(label: String, value: String) {
  Column {
    Text(
      label,
      style = MaterialTheme.typography.labelSmall.copy(color = WearableColors.Body),
    )
    Text(
      value,
      style =
        MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.SemiBold,
          color = WearableColors.Title,
        ),
    )
  }
}

// ----------------------------------------------------------------------------- helpers

private fun Double.format(digits: Int): String = "%.${digits}f".format(this)
