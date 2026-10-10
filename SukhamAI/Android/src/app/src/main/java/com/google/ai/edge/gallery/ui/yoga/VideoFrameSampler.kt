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

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

private const val TAG = "VideoFrameSampler"

/** Hard cap on frames pulled from one clip, so a long upload can't run for an hour at a booth. */
private const val MAX_SAMPLED_FRAMES = 40

/** Frames we aim to pull from any clip, regardless of its length. */
private const val TARGET_FRAMES = 5

private const val MIN_INTERVAL_MS = 4_000L
private const val MAX_INTERVAL_MS = 20_000L

/**
 * Pulls evenly spaced still frames out of a user-supplied video.
 *
 * Frames are decoded lazily, one at a time, rather than all up front: a 5-minute clip at one
 * frame per 20 s is only 15 bitmaps, but each is multi-megabyte at source resolution and the
 * analysis pipeline is ~20 s per frame anyway, so there is nothing to gain from holding them all
 * in memory at once.
 */
object VideoFrameSampler {

  /** A frame and the position in the clip it came from. */
  data class SampledFrame(val timestampMs: Long, val bitmap: Bitmap)

  /**
   * Sampling interval for a clip of [durationMs], chosen to land near [TARGET_FRAMES] frames.
   *
   * A fixed 20 s interval was fine when live mode shared it, but video mode is the only user
   * now, and a fixed interval scales badly at both ends: a 21 s clip yields 2 frames while a
   * 10-minute one yields 30. Aiming for a frame count instead keeps any clip to a comparable
   * amount of analysis, clamped so we never thrash on very short clips or skim very long ones.
   */
  fun intervalFor(durationMs: Long): Long {
    if (durationMs <= 0L) return MAX_INTERVAL_MS
    return (durationMs / TARGET_FRAMES).coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
  }

  /** Video duration in ms, or 0 if it can't be read. */
  fun durationMs(context: Context, uri: Uri): Long =
    retriever(context, uri)?.use { r ->
      r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } ?: 0L

  /**
   * Positions that will be sampled from a clip of [durationMs], one every [intervalMs].
   *
   * Always includes 0 so a clip shorter than one interval still yields a single frame, and is
   * capped at [MAX_SAMPLED_FRAMES]. Exposed separately from [forEachFrame] so the UI can show a
   * total ("analyzing 3/7") before any decoding starts.
   */
  fun samplePositions(durationMs: Long, intervalMs: Long): List<Long> {
    if (durationMs <= 0L) return listOf(0L)
    val positions = mutableListOf<Long>()
    var t = 0L
    while (t < durationMs && positions.size < MAX_SAMPLED_FRAMES) {
      positions.add(t)
      t += intervalMs
    }
    return positions.ifEmpty { listOf(0L) }
  }

  /**
   * Decodes the frame at each of [positions] and hands it to [onFrame] before moving on.
   *
   * [onFrame] is suspending and is awaited, which is what keeps only one decoded bitmap alive at
   * a time. Positions whose frame fails to decode are skipped rather than aborting the run — a
   * single bad seek shouldn't kill a demo.
   */
  suspend fun forEachFrame(
    context: Context,
    uri: Uri,
    positions: List<Long>,
    maxEdge: Int = LANDMARK_INPUT_MAX_EDGE,
    onFrame: suspend (SampledFrame) -> Unit,
  ) {
    val r = retriever(context, uri) ?: throw IllegalStateException("Unable to read this video.")
    r.use {
      for (positionMs in positions) {
        val bitmap =
          runCatching {
              // OPTION_CLOSEST_SYNC snaps to the nearest keyframe, which is far cheaper than an
              // exact seek and is plenty for a pose held over several seconds.
              it.getScaledFrameAtTime(
                positionMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                maxEdge,
                maxEdge,
              )
            }
            .getOrNull()
        if (bitmap == null) {
          Log.w(TAG, "No frame decoded at ${positionMs}ms; skipping.")
          continue
        }
        onFrame(SampledFrame(timestampMs = positionMs, bitmap = bitmap))
      }
    }
  }

  private fun retriever(context: Context, uri: Uri): MediaMetadataRetriever? =
    runCatching {
        MediaMetadataRetriever().apply { setDataSource(context, uri) }
      }
      .onFailure { Log.e(TAG, "setDataSource failed for $uri", it) }
      .getOrNull()
}
