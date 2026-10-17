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

package com.google.ai.edge.gallery.ui.yoga

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A joint the person should adjust, with everything needed to draw the fix.
 *
 * Coordinates are normalised image coordinates (0..1) so the same data draws correctly at any
 * display size. [rotationRad] is how far the [moving] end swings about [pivot] to reach the
 * target, measured in pixel space; null means "highlight the joint, but don't show a ghost"
 * (the limb points at the camera, so the flat image can't say which way to move it).
 */
data class JointCorrection(
  val label: String,
  val instruction: String,
  val fixedX: Float,
  val fixedY: Float,
  val pivotX: Float,
  val pivotY: Float,
  val movingX: Float,
  val movingY: Float,
  val rotationRad: Float?,
  val deltaDeg: Float,
)

/** Normalised bounding box of the visible skeleton, used to zoom in on the person. */
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

private val Wrong = Color(0xFFFF5252)
private val WrongDark = Color(0xFF8E1B1B)
private val Target = Color(0xFF69F0AE)
private val TargetDark = Color(0xFF1B7A4A)

private const val SPOTLIGHT_MS = 8_000

/**
 * Freezes the camera on the moment of analysis, zooms to the skeleton, and shows — with a
 * gently tilting, shaded 3D look — which joint to fix and where the limb should go. Calls
 * [onDone] after five seconds (or on tap) so the caller can drop back to the live camera.
 */
@Composable
fun CorrectionSpotlight(
  bitmap: Bitmap,
  corrections: List<JointCorrection>,
  bounds: NormRect?,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val zoom = remember { Animatable(0f) }
  val countdown = remember { Animatable(0f) }
  LaunchedEffect(Unit) { zoom.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
  LaunchedEffect(Unit) {
    countdown.animateTo(1f, tween(SPOTLIGHT_MS, easing = LinearEasing))
    onDone()
  }

  val transition = rememberInfiniteTransition(label = "spotlight")
  val pulse by
    transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "pulse")
  val sway by
    transition.animateFloat(
      0f, (2 * Math.PI).toFloat(),
      infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "sway",
    )
  // Rest, swing to the target, hold, repeat.
  val swing by
    transition.animateFloat(
      0f, 1f,
      infiniteRepeatable(
        keyframes {
          durationMillis = 2800
          0f at 0
          0f at 450
          1f at 1700
          1f at 2800
        }
      ),
      label = "swing",
    )

  // Zoom target: fit the skeleton with padding, never zooming out and capped so a close-up
  // person doesn't become a blur of pixels.
  val b = bounds ?: NormRect(0f, 0f, 1f, 1f)
  val bw = ((b.right - b.left) * 1.3f).coerceAtLeast(0.2f)
  val bh = ((b.bottom - b.top) * 1.3f).coerceAtLeast(0.2f)
  val zoomTarget = minOf(1f / bw, 1f / bh).coerceIn(1f, 2.4f)
  val cx = (b.left + b.right) / 2f
  val cy = (b.top + b.bottom) / 2f
  val winLeft = (cx - 0.5f / zoomTarget).coerceIn(0f, 1f - 1f / zoomTarget)
  val winTop = (cy - 0.5f / zoomTarget).coerceIn(0f, 1f - 1f / zoomTarget)

  Box(
    modifier = modifier.background(Color(0xFF0B0A10)).clickable(
      interactionSource = remember { MutableInteractionSource() }, indication = null,
    ) { onDone() },
    contentAlignment = Alignment.Center,
  ) {
    // The frame, tilted slightly in 3D so it feels like a floating card rather than a screenshot.
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
          .graphicsLayer {
            cameraDistance = 16f * density
            rotationY = sin(sway) * 6f * zoom.value
            rotationX = cos(sway) * 3f * zoom.value
          }
          .clipToBounds()
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize().graphicsLayer {
            val e = zoom.value
            val sc = 1f + (zoomTarget - 1f) * e
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = sc
            scaleY = sc
            translationX = -winLeft * e * sc * size.width
            translationY = -winTop * e * sc * size.height
          }
      ) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = "Paused pose",
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
          // Dim the photo so the coloured fix is what the eye lands on.
          drawRect(Color.Black.copy(alpha = 0.42f * zoom.value))
          corrections.forEachIndexed { i, c -> drawCorrection3d(c, i + 1, pulse, swing, zoomTarget) }
        }
      }
    }

    // Countdown sliver across the top.
    Box(
      modifier =
        Modifier.align(Alignment.TopStart)
          .fillMaxWidth(1f - countdown.value)
          .height(3.dp)
          .background(Target)
    )
    Text(
      "PAUSED \u00B7 ADJUST",
      color = Color.White,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.5.sp,
      modifier =
        Modifier.align(Alignment.TopStart)
          .padding(14.dp)
          .clip(RoundedCornerShape(50))
          .background(Color.Black.copy(alpha = 0.55f))
          .padding(horizontal = 10.dp, vertical = 5.dp),
    )

    // What to do, in words, over a soft gradient at the bottom.
    Column(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))
          )
          .padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      corrections.forEachIndexed { i, c ->
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(Wrong),
            contentAlignment = Alignment.Center,
          ) {
            Text("${i + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(Modifier.size(10.dp))
          Text(c.instruction, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
      }
      Text("Tap to return to camera", color = Color(0xFFB8B4C4), fontSize = 11.sp)
    }
  }
}

/** A limb drawn as a shaded tube: shadow, dark body, lit body, bright specular line. */
private fun DrawScope.tube(from: Offset, to: Offset, width: Float, base: Color, dark: Color, alpha: Float = 1f) {
  val light = Offset(-width * 0.14f, -width * 0.14f)
  drawLine(Color.Black.copy(alpha = 0.35f * alpha), from + Offset(width * 0.25f, width * 0.45f),
    to + Offset(width * 0.25f, width * 0.45f), width * 0.95f, StrokeCap.Round)
  drawLine(dark.copy(alpha = alpha), from, to, width, StrokeCap.Round)
  drawLine(base.copy(alpha = alpha), from + light * 0.4f, to + light * 0.4f, width * 0.68f, StrokeCap.Round)
  drawLine(Color.White.copy(alpha = 0.55f * alpha), from + light, to + light, width * 0.22f, StrokeCap.Round)
}

/** A joint drawn as a lit sphere. */
private fun DrawScope.sphere(center: Offset, r: Float, base: Color, dark: Color, alpha: Float = 1f) {
  drawCircle(Color.Black.copy(alpha = 0.3f * alpha), r, center + Offset(r * 0.3f, r * 0.5f))
  drawCircle(
    Brush.radialGradient(
      listOf(Color.White.copy(alpha = alpha), base.copy(alpha = alpha), dark.copy(alpha = alpha)),
      center = center + Offset(-r * 0.35f, -r * 0.35f),
      radius = r * 1.5f,
    ),
    r, center,
  )
}

/** A ring lying flat, drawn as a squashed ellipse so it reads as sitting on a surface. */
private fun DrawScope.flatRing(center: Offset, r: Float, color: Color, stroke: Float) {
  drawOval(color, Offset(center.x - r, center.y - r * 0.42f), Size(r * 2f, r * 0.84f), style = Stroke(stroke))
}

private fun DrawScope.drawCorrection3d(
  c: JointCorrection, number: Int, pulse: Float, swing: Float, zoom: Float,
) {
  val w = size.width
  val h = size.height
  // Sizes are specified for the zoomed view, so divide the zoom back out.
  val u = 1f / zoom
  val pivot = Offset(c.pivotX * w, c.pivotY * h)
  val fixed = Offset(c.fixedX * w, c.fixedY * h)
  val moving = Offset(c.movingX * w, c.movingY * h)
  val tubeW = 13.dp.toPx() * u

  // The wrong joint's two limb segments, glowing red.
  for (end in listOf(fixed, moving)) {
    drawLine(Wrong.copy(alpha = 0.22f), pivot, end, tubeW * 2.1f, StrokeCap.Round)
    tube(pivot, end, tubeW, Wrong, WrongDark)
  }

  c.rotationRad?.let { rot ->
    val v = moving - pivot
    val radius = hypot(v.x, v.y)
    val target = pivot + rotated(v, rot)
    val eased = swing * swing * (3f - 2f * swing)

    // Path the limb should travel.
    drawArc(
      color = Target.copy(alpha = 0.6f),
      startAngle = Math.toDegrees(atan2(v.y.toDouble(), v.x.toDouble())).toFloat(),
      sweepAngle = Math.toDegrees(rot.toDouble()).toFloat(),
      useCenter = false,
      topLeft = Offset(pivot.x - radius, pivot.y - radius),
      size = Size(radius * 2f, radius * 2f),
      style = Stroke(2.5.dp.toPx() * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f * u, 11f * u))),
    )
    // Landing pad: a flat glowing disc where the limb should end up.
    flatRing(target, 20.dp.toPx() * u, Target.copy(alpha = 0.9f), 2.5.dp.toPx() * u)
    flatRing(target, 11.dp.toPx() * u, Target.copy(alpha = 0.5f), 2.dp.toPx() * u)

    // Motion trail: fading afterimages behind the moving ghost limb.
    for (k in 5 downTo 1) {
      val tk = (eased - k * 0.07f).coerceAtLeast(0f)
      if (tk < eased) {
        tube(pivot, pivot + rotated(v, rot * tk), tubeW * 0.8f, Target, TargetDark, alpha = 0.07f * (6 - k))
      }
    }
    val current = pivot + rotated(v, rot * eased)
    tube(pivot, current, tubeW * 0.9f, Target, TargetDark, alpha = 0.95f)
    sphere(current, tubeW * 0.85f, Target, TargetDark)
  }

  // The joint itself: a lit sphere inside a pulsing flat ring.
  flatRing(pivot, (14.dp.toPx() + 26.dp.toPx() * pulse) * u, Wrong.copy(alpha = (1f - pulse) * 0.9f), 3.dp.toPx() * u)
  sphere(pivot, tubeW * 0.95f, Wrong, WrongDark)

  // Number badge.
  val badge = pivot + Offset(26.dp.toPx() * u, -26.dp.toPx() * u)
  drawCircle(Color.White, 12.dp.toPx() * u, badge)
  drawCircle(Wrong, 10.dp.toPx() * u, badge)
  val paint =
    android.graphics.Paint().apply {
      color = android.graphics.Color.WHITE
      textSize = 13.dp.toPx() * u
      isFakeBoldText = true
      isAntiAlias = true
      textAlign = android.graphics.Paint.Align.CENTER
    }
  drawIntoCanvas { it.nativeCanvas.drawText("$number", badge.x, badge.y + paint.textSize / 3f, paint) }
}

private fun rotated(v: Offset, rad: Float): Offset {
  val c = cos(rad)
  val s = sin(rad)
  return Offset(v.x * c - v.y * s, v.x * s + v.y * c)
}
