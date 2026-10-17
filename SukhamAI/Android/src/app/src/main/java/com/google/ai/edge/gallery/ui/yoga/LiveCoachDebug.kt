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

import android.util.Log

/**
 * Everything the live coach decides, kept in memory instead of on screen.
 *
 * The live screen is deliberately bare: one cue, one ring. The detail that explains *why* it said
 * what it said lives here, in plain variables, so a debugger can read [frames] and [analyses]
 * directly, and [dump] writes the same thing to logcat (`adb logcat -s LiveCoachDebug`) when a
 * session ends.
 *
 * Bounded ring buffers; nothing leaves the device.
 */
object LiveCoachDebug {

  private const val TAG = "LiveCoachDebug"
  private const val MAX_FRAMES = 400
  private const val MAX_ANALYSES = 60

  /** One preview detection, as seen by the hold gate and the live ring. */
  data class Frame(
    val atMs: Long,
    val holdState: String,
    val holdProgress: Float,
    val topPose: String?,
    /** Raw classifier score 0..1 for [topPose], before any display smoothing. */
    val matchScore: Float?,
    /** What the ring showed, 0..100, after smoothing. Null when the pose was unknown. */
    val shownPercent: Int?,
  )

  /** One full analysis: the measurements, the prompt, and what the model said back. */
  data class Analysis(
    val cardId: Long,
    val atMs: Long,
    val candidates: List<String>,
    val confident: Boolean,
    val geometry: List<String>,
    val deviations: List<String>,
    val corrections: List<String>,
    val aligned: List<String>,
    val prompt: String,
    val gemmaRaw: String,
    val spoken: String,
    val resetMs: Long,
    val firstTokenMs: Long,
    val totalMs: Long,
  )

  private val lock = Any()
  private val frameBuffer = ArrayDeque<Frame>()
  private val analysisBuffer = ArrayDeque<Analysis>()

  @Volatile var sessionStartedAtMs: Long = 0L
  @Volatile var modelName: String = ""

  /** Snapshot copies, safe to read from any thread. */
  val frames: List<Frame> get() = synchronized(lock) { frameBuffer.toList() }
  val analyses: List<Analysis> get() = synchronized(lock) { analysisBuffer.toList() }

  fun startSession(model: String) {
    synchronized(lock) {
      frameBuffer.clear()
      analysisBuffer.clear()
    }
    sessionStartedAtMs = System.currentTimeMillis()
    modelName = model
  }

  fun recordFrame(frame: Frame) =
    synchronized(lock) {
      frameBuffer.addLast(frame)
      while (frameBuffer.size > MAX_FRAMES) frameBuffer.removeFirst()
    }

  fun recordAnalysis(analysis: Analysis) {
    synchronized(lock) {
      analysisBuffer.addLast(analysis)
      while (analysisBuffer.size > MAX_ANALYSES) analysisBuffer.removeFirst()
    }
    Log.d(
      TAG,
      "ANALYSIS #${analysis.cardId} pose=${analysis.candidates.firstOrNull()} " +
        "confident=${analysis.confident} aligned=${analysis.aligned} " +
        "fix=${analysis.corrections} spoken=\"${analysis.spoken}\" " +
        "timing(reset=${analysis.resetMs} firstToken=${analysis.firstTokenMs} total=${analysis.totalMs})",
    )
  }

  /** The whole session as text. Called when the live screen closes. */
  fun dump(): String =
    synchronized(lock) {
      buildString {
        appendLine("=== LiveCoachDebug model=$modelName started=$sessionStartedAtMs ===")
        appendLine("-- analyses (${analysisBuffer.size}) --")
        analysisBuffer.forEach { a ->
          appendLine("#${a.cardId} t+${a.atMs - sessionStartedAtMs}ms confident=${a.confident}")
          appendLine("  candidates: ${a.candidates}")
          appendLine("  geometry:   ${a.geometry}")
          appendLine("  deviations: ${a.deviations}")
          appendLine("  fix:        ${a.corrections}")
          appendLine("  working:    ${a.aligned}")
          appendLine("  gemma:      ${a.gemmaRaw.replace("\n", " | ")}")
          appendLine("  spoken:     ${a.spoken}")
          appendLine("  timing:     reset=${a.resetMs} firstToken=${a.firstTokenMs} total=${a.totalMs}")
        }
        appendLine("-- frames (${frameBuffer.size}, last 40) --")
        frameBuffer.toList().takeLast(40).forEach { f ->
          appendLine(
            "t+${f.atMs - sessionStartedAtMs}ms ${f.holdState} hold=${"%.2f".format(f.holdProgress)} " +
              "pose=${f.topPose} score=${f.matchScore?.let { "%.2f".format(it) }} shown=${f.shownPercent}"
          )
        }
      }
    }

  fun logDump() {
    dump().lineSequence().forEach { Log.d(TAG, it) }
  }
}
