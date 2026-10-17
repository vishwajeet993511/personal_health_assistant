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
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.google.ai.edge.gallery.ui.common.LiveCameraView
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Preview-only pose refresh rate. Fast enough to track a person, slow enough to leave the CPU
 * to Gemma, which is doing the actual coaching in parallel. */
private const val PREVIEW_POSE_INTERVAL_MS = 400L

/** How often the drawn skeleton is refreshed; independent of the slower hold-gate cadence. */
private const val SKELETON_INTERVAL_MS = 100L

/**
 * Live camera preview with the skeleton drawn over it.
 *
 * Renders the frames itself (`renderPreview = false` on [LiveCameraView]) rather than letting
 * LiveCameraView draw them, because the skeleton has to land in exactly the same center-crop
 * transform as the image underneath it — reproducing that mapping against someone else's
 * internal canvas would be guesswork. Both are drawn here, from the same numbers.
 *
 * The pose used for the overlay comes from a **separate** [PoseLandmarkerHelper] instance from
 * the one the coaching engine uses. MediaPipe landmarkers are not safe to call concurrently, and
 * the preview runs continuously while the engine is mid-frame.
 *
 * @param onFrame receives every camera frame, for the caller's sampling loop.
 * @param onPose receives each throttled pose detection (null when nobody is found). The caller
 *   uses these for hold/stillness gating; they come from the preview's own detector, so this
 *   costs nothing beyond what the overlay already computes.
 */
@Composable
fun LivePosePreview(
  cameraSelector: CameraSelector,
  onFrame: (Bitmap) -> Unit,
  modifier: Modifier = Modifier,
  onPose: (PoseLandmarkerResult?) -> Unit = {},
) {
  val context = LocalContext.current
  val previewHelper = remember { PoseLandmarkerHelper(context.applicationContext) }
  // The detection loop below outlives recompositions; without this it would keep calling the
  // handler from the first composition, with whatever state that one had captured.
  val currentOnPose by rememberUpdatedState(onPose)
  DisposableEffect(Unit) { onDispose { previewHelper.close() } }

  // Latest frame lives outside Compose state so the 30fps camera callback doesn't drive
  // recomposition; the throttled loop below promotes one frame at a time into state.
  val pending = remember { AtomicReference<Bitmap?>(null) }
  var shown by remember { mutableStateOf<ImageBitmap?>(null) }
  var shownSize by remember { mutableStateOf(IntSize.Zero) }
  var landmarks by remember { mutableStateOf<PoseLandmarkerResult?>(null) }

  LiveCameraView(
    onBitmap = { bitmap, imageProxy ->
      pending.set(bitmap)
      // The picture follows the camera frame by frame; only the skeleton waits on detection.
      shown = bitmap.asImageBitmap()
      shownSize = IntSize(bitmap.width, bitmap.height)
      onFrame(bitmap)
      imageProxy.close()
    },
    preferredSize = 720,
    renderPreview = false,
    cameraSelector = cameraSelector,
  )

  LaunchedEffect(cameraSelector) {
    var lastGateMs = 0L
    while (isActive) {
      val frame = pending.get()
      if (frame == null) {
        delay(50)
        continue
      }
      val result =
        withContext(Dispatchers.Default) {
          previewHelper.setup()
          runCatching { previewHelper.detect(frame) }.getOrNull()
        }
      landmarks = result
      // The hold gate was tuned against detections ~400 ms apart, so it still gets them at that
      // pace even though the skeleton now refreshes faster.
      val now = System.currentTimeMillis()
      if (now - lastGateMs >= PREVIEW_POSE_INTERVAL_MS) {
        lastGateMs = now
        currentOnPose(result)
      }
      delay(SKELETON_INTERVAL_MS)
    }
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val image = shown ?: return@Canvas
    val bw = shownSize.width.toFloat()
    val bh = shownSize.height.toFloat()
    if (bw <= 0f || bh <= 0f) return@Canvas
    val cw = size.width
    val ch = size.height

    // Center-crop: scale so the shorter relative axis fills, then take the middle of the rest.
    val scale = if (bw / bh > cw / ch) ch / bh else cw / bw
    val srcLeft = (bw - cw / scale) / 2f
    val srcTop = (bh - ch / scale) / 2f

    drawImage(
      image = image,
      srcOffset = IntOffset(srcLeft.toInt(), srcTop.toInt()),
      srcSize = IntSize((cw / scale).toInt(), (ch / scale).toInt()),
      dstOffset = IntOffset.Zero,
      dstSize = IntSize(cw.toInt(), ch.toInt()),
    )

    val pose = landmarks?.landmarks()?.firstOrNull() ?: return@Canvas
    // Normalized landmark -> source pixel -> canvas pixel, through the same crop above.
    fun toCanvas(nx: Float, ny: Float) =
      Offset((nx * bw - srcLeft) * scale, (ny * bh - srcTop) * scale)

    drawGlowingSkeleton(pose, ::toCanvas, base = maxOf(2f, minOf(cw, ch) * 0.003f))
  }
}

/** The joints that make up the body figure; face, fingers and toes are left out as clutter. */
private val BODY_JOINTS = setOf(11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)

/** Hand and foot tips, so limbs end in a point instead of stopping dead at the wrist/ankle. */
private val EXTRA_SEGMENTS = listOf(15 to 19, 16 to 20, 27 to 31, 28 to 32)

private val GlowGreen = Color(0xFF3DFF8A)
private val CoreGreen = Color(0xFFC8FFDD)

/**
 * Draws the figure as a soft green neon: a wide faint halo, a brighter band, then a thin bright
 * core. Three strokes of the same line is what reads as "glowing" without any blur filter.
 */
private fun DrawScope.drawGlowingSkeleton(
  pose: List<NormalizedLandmark>,
  toCanvas: (Float, Float) -> Offset,
  base: Float,
) {
  fun seen(i: Int) = pose.getOrNull(i)?.let { it.visibility().orElse(1f) >= 0.4f } == true
  fun pt(i: Int) = toCanvas(pose[i].x(), pose[i].y())

  val segments = mutableListOf<Pair<Int, Int>>()
  PoseLandmarker.POSE_LANDMARKS.forEach { c ->
    if (c.start() in BODY_JOINTS && c.end() in BODY_JOINTS) segments += c.start() to c.end()
  }
  segments += EXTRA_SEGMENTS
  val drawn = segments.filter { (a, b) -> seen(a) && seen(b) }

  // Neck: shoulder midpoint up to the nose, which stands in for the head.
  val neck =
    if (seen(0) && seen(11) && seen(12)) {
      val sm = Offset((pt(11).x + pt(12).x) / 2f, (pt(11).y + pt(12).y) / 2f)
      sm to pt(0)
    } else null

  fun pass(widthMul: Float, color: Color) {
    drawn.forEach { (a, b) ->
      drawLine(color, pt(a), pt(b), strokeWidth = base * widthMul, cap = StrokeCap.Round)
    }
    neck?.let { (from, to) ->
      drawLine(color, from, to, strokeWidth = base * widthMul, cap = StrokeCap.Round)
    }
  }
  pass(3.4f, GlowGreen.copy(alpha = 0.12f))
  pass(1.8f, GlowGreen.copy(alpha = 0.36f))
  pass(0.8f, CoreGreen)

  // Joint dots: a halo and a white centre.
  (BODY_JOINTS + 0).filter { seen(it) }.forEach { i ->
    val c = pt(i)
    drawCircle(GlowGreen.copy(alpha = 0.45f), radius = base * 1.9f, center = c)
    drawCircle(Color.White, radius = base * 0.95f, center = c)
  }
}
