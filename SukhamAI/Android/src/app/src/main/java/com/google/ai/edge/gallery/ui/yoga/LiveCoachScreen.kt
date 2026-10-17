/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.google.ai.edge.gallery.ui.yoga

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.roundToInt

private val Sage = Color(0xFF2E4E43)
private val SageDeep = Color(0xFF1D352D)
private val Cream = Color(0xFFF5F2E9)

/** Whether the coach can run yet, and if not, what the person is waiting on. */
internal sealed interface LiveReadiness {
  /** No model on disk and none downloading; the only place to get one is Profile. */
  object NeedsModel : LiveReadiness

  /** The one-time model download is running (it survives the app being closed). */
  class Downloading(val fraction: Float, val receivedMb: Int, val totalMb: Int) : LiveReadiness

  /** Downloaded; the model is being loaded into memory. */
  object WarmingUp : LiveReadiness

  /** Loading the model failed. */
  class Failed(val message: String) : LiveReadiness

  object Ready : LiveReadiness
}

/**
 * Closeness to the reference pose as a percentage, from the classifier's score.
 *
 * The score is `1 / (1 + d / 40)` where `d` is the weighted RMS joint-angle error in degrees, so
 * the degrees are recovered and mapped linearly: 0 degrees off is 100%, 60 or more is 0%. This is
 * measured geometry against the reference, never a number the language model made up.
 */
internal fun matchPercent(score: Float): Int {
  val degrees = 40f * (1f / score.coerceAtLeast(0.01f) - 1f)
  return (100f * (1f - degrees / 60f)).coerceIn(0f, 100f).roundToInt()
}

/**
 * The one line shown in the banner. Ordered so the most useful thing wins: the fix being shown
 * on the frozen frame, then fresh coaching, then whatever the gate is waiting for.
 */
internal fun liveBanner(
  holdState: PoseHoldDetector.State,
  holdProgress: Float,
  analysing: Boolean,
  spotlightFix: String?,
  coaching: Coaching?,
  coachingAgeMs: Long,
): String {
  if (spotlightFix != null) return spotlightFix
  if (holdState == PoseHoldDetector.State.NO_PERSON) return "Step into the frame"
  if (coaching != null && coachingAgeMs < COACHING_FRESH_MS && holdState != PoseHoldDetector.State.MOVING) {
    val text =
      when {
        !coaching.fixIsNone -> coaching.fix
        coaching.good.isNotEmpty() -> coaching.good
        else -> "Lovely form. Stay with it"
      }
    return text.trimEnd('.')
  }
  return when {
    analysing -> "Reading your pose…"
    holdState == PoseHoldDetector.State.MOVING -> "Settle into your pose"
    holdState == PoseHoldDetector.State.SETTLING -> "Hold steady"
    else -> "Move into a pose"
  }
}

/** How long a finished piece of coaching stays in the banner. */
private const val COACHING_FRESH_MS = 25_000L

/**
 * The whole live-coach experience: the camera fills the screen, a single line says what to do,
 * and a ring shows how close the pose is. Everything the coach decides is recorded in
 * [LiveCoachDebug] instead of being drawn here.
 */
@Composable
internal fun LiveCoachScreen(
  readiness: LiveReadiness,
  useFrontCamera: Boolean,
  onFlipCamera: () -> Unit,
  voiceOn: Boolean,
  onToggleVoice: () -> Unit,
  holdState: PoseHoldDetector.State,
  holdProgress: Float,
  matchPercent: Int?,
  poseName: String?,
  banner: String,
  spotlight: CoachingCard?,
  onSpotlightDone: () -> Unit,
  onFrame: (Bitmap) -> Unit,
  onPose: (PoseLandmarkerResult?) -> Unit,
  onClose: () -> Unit,
  onOpenProfile: () -> Unit,
  onRetry: () -> Unit,
) {
  // A coaching session is hands-free; the screen must not dim halfway through a hold.
  val view = LocalView.current
  DisposableEffect(Unit) {
    view.keepScreenOn = true
    onDispose { view.keepScreenOn = false }
  }
  // The screen is dark under the clock and battery, so they need light icons here.
  DisposableEffect(Unit) {
    val window = (view.context as? android.app.Activity)?.window
    val controller = window?.let { WindowCompat.getInsetsController(it, view) }
    val previous = controller?.isAppearanceLightStatusBars
    controller?.isAppearanceLightStatusBars = false
    onDispose { if (previous != null) controller.isAppearanceLightStatusBars = previous }
  }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E1512))) {
    if (readiness is LiveReadiness.Ready) {
      val selector =
        if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
      key(selector) {
        LivePosePreview(
          cameraSelector = selector,
          onFrame = onFrame,
          modifier = Modifier.fillMaxSize(),
          onPose = onPose,
        )
      }
      // Soft scrims so the banner and controls stay legible over any background.
      Box(
        Modifier.align(Alignment.TopCenter).fillMaxWidth().height(180.dp)
          .background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent)))
      )
      Box(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(240.dp)
          .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000))))
      )
    } else {
      StatusPanel(readiness, onOpenProfile, onRetry)
    }

    if (readiness is LiveReadiness.Ready) {
      spotlight?.thumbnail?.let { bmp ->
        CorrectionSpotlight(
          bitmap = bmp,
          corrections = spotlight.corrections,
          bounds = spotlight.skeletonBounds,
          onDone = onSpotlightDone,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    // While the freeze animation owns the screen it draws its own labels; tapping it returns.
    val frozen = spotlight?.thumbnail != null && readiness is LiveReadiness.Ready

    // Top: close, and the one line the coach wants you to hear.
    if (!frozen) Row(
      modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        onClick = onClose,
        shape = CircleShape,
        color = Color(0x66000000),
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
        }
      }
      Spacer(Modifier.width(12.dp))
      if (readiness is LiveReadiness.Ready) {
        Banner(text = banner, modifier = Modifier.weight(1f))
      }
    }

    // Bottom: flip camera, how close the pose is, voice on/off.
    if (readiness is LiveReadiness.Ready && !frozen) {
      Column(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
          .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (poseName != null && matchPercent != null) {
          Text(
            text = poseName,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 8f)),
          )
          Spacer(Modifier.height(10.dp))
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RoundButton(
            onClick = onFlipCamera,
            background = Cream,
            description = "Switch camera",
          ) { Icon(Icons.Outlined.Cameraswitch, null, tint = SageDeep) }

          ScoreRing(
            percent = matchPercent,
            holdProgress = holdProgress,
            settling = holdState == PoseHoldDetector.State.SETTLING,
          )

          RoundButton(
            onClick = onToggleVoice,
            background = Color(0x99000000),
            description = if (voiceOn) "Mute voice" else "Turn voice on",
          ) {
            Icon(
              if (voiceOn) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
              contentDescription = null,
              tint = Color.White,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Banner(text: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Sage.copy(alpha = 0.92f),
    shadowElevation = 6.dp,
    modifier = modifier,
  ) {
    Crossfade(targetState = text, animationSpec = tween(250), label = "banner") { t ->
      Text(
        text = t,
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
      )
    }
  }
}

@Composable
private fun RoundButton(
  onClick: () -> Unit,
  background: Color,
  description: String,
  content: @Composable () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = background,
    modifier = Modifier.size(52.dp),
  ) {
    Box(contentAlignment = Alignment.Center) { content() }
  }
}

/**
 * While holding a pose the ring fills with the hold; once a pose is recognised it shows how
 * close that pose is to the reference. Without a recognised pose it is just a calm placeholder.
 */
@Composable
private fun ScoreRing(percent: Int?, holdProgress: Float, settling: Boolean) {
  val target = if (settling) holdProgress else (percent ?: 0) / 100f
  val fill by animateFloatAsState(target, tween(450), label = "ring")
  val arc = if (settling) Color(0xFF7FB59F) else Color(0xFF2F8F6B)
  Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(Cream.copy(alpha = 0.92f))
      val stroke = 6.dp.toPx()
      val inset = stroke / 2f + 5.dp.toPx()
      val topLeft = Offset(inset, inset)
      val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
      drawArc(Sage.copy(alpha = 0.16f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
      drawArc(arc, -90f, 360f * fill, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
    if (percent != null) {
      Text("$percent%", color = SageDeep, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    } else {
      Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = Sage.copy(alpha = 0.6f))
    }
  }
}

/** Everything shown instead of the camera while the coach is not ready. */
@Composable
private fun StatusPanel(
  readiness: LiveReadiness,
  onOpenProfile: () -> Unit,
  onRetry: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 36.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    when (readiness) {
      is LiveReadiness.Downloading -> {
        CircularProgressIndicator(
          progress = { readiness.fraction.coerceIn(0f, 1f) },
          color = Color(0xFF7FB59F),
          trackColor = Color(0x22FFFFFF),
          strokeWidth = 5.dp,
          modifier = Modifier.size(84.dp),
        )
        Spacer(Modifier.height(28.dp))
        PanelText("Downloading your coach")
        PanelHint(
          "%.1f of %.1f GB. This happens once, and it keeps going if you leave the app."
            .format(readiness.receivedMb / 1024f, readiness.totalMb / 1024f)
        )
      }
      LiveReadiness.WarmingUp -> {
        CircularProgressIndicator(color = Color(0xFF7FB59F), strokeWidth = 5.dp, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(28.dp))
        PanelText("Warming up your coach")
        PanelHint("A few seconds the first time.")
      }
      is LiveReadiness.Failed -> {
        PanelText("The coach couldn't start")
        PanelHint(readiness.message)
        Spacer(Modifier.height(24.dp))
        PillButton("Try again", onRetry)
      }
      else -> {
        PanelText("Your coach needs its AI model")
        PanelHint("Download it once from your Profile. Every feature uses it.")
        Spacer(Modifier.height(24.dp))
        PillButton("Open Profile", onOpenProfile)
      }
    }
  }
}

@Composable
private fun PanelText(text: String) {
  Text(text, color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
}

@Composable
private fun PanelHint(text: String) {
  Spacer(Modifier.height(8.dp))
  Text(text, color = Cream.copy(alpha = 0.65f), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier.clip(RoundedCornerShape(50)).background(Cream).clickable(onClick = onClick)
      .padding(horizontal = 28.dp, vertical = 14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(label, color = SageDeep, fontSize = 15.sp, fontWeight = FontWeight.Medium)
  }
}
