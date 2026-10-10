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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.google.ai.edge.gallery.ui.common.LiveCameraView
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
      onFrame(bitmap)
      imageProxy.close()
    },
    preferredSize = 720,
    renderPreview = false,
    cameraSelector = cameraSelector,
  )

  LaunchedEffect(cameraSelector) {
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
      shown = frame.asImageBitmap()
      shownSize = IntSize(frame.width, frame.height)
      landmarks = result
      onPose(result)
      delay(PREVIEW_POSE_INTERVAL_MS)
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

    val stroke = maxOf(4f, minOf(cw, ch) * 0.006f)
    val dot = maxOf(6f, minOf(cw, ch) * 0.009f)
    PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
      val a = pose[connection.start()]
      val b = pose[connection.end()]
      drawLine(
        color = Color(0xFF00E5FF),
        start = toCanvas(a.x(), a.y()),
        end = toCanvas(b.x(), b.y()),
        strokeWidth = stroke,
      )
    }
    pose.forEach { lm -> drawCircle(color = Color(0xFFFFD740), radius = dot, center = toCanvas(lm.x(), lm.y())) }
  }
}
