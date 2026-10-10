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
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.abs
import kotlin.math.sqrt

private const val TAG = "PoseHoldDetector"

/** Mean per-landmark movement (normalised image units) below which the body counts as still. */
private const val STILLNESS_THRESHOLD = 0.018f

/** How long the body must stay still before the pose counts as held. */
private const val HOLD_MS = 2_000L

/** Minimum [PoseClassifier] score to spend an inference on. Below this we're guessing. */
private const val MIN_CONFIDENCE = 0.45f

/** Rate ceiling — never analyse more often than this, however much they move around. */
private const val MIN_GAP_MS = 15_000L

/** Allow re-coaching the *same* pose after this long, so a long hold isn't ignored forever. */
private const val SAME_POSE_REPEAT_MS = 90_000L

/** Upper bound on frames pooled for the median, so a long hold cannot grow the buffer forever. */
private const val MAX_POOLED_FRAMES = 12

/**
 * Re-coach the same pose immediately if its match score moves by more than this.
 *
 * A right-way/wrong-way demonstration holds *one* pose twice — a clean Warrior II and a
 * deliberately sloppy one. Both carry the same name, so a name-only "has it changed" test stays
 * silent for the half that matters. The classifier score is exactly the signal that separates
 * them, so a material swing in it counts as a new pose worth coaching.
 */
private const val FORM_CHANGE_DELTA = 0.08f

/**
 * Decides *when* a live frame is worth spending ~30 s of on-device inference on.
 *
 * A fixed timer fires on a schedule unrelated to what the person is doing, so it routinely
 * spends a full inference on a blurred transition or an empty room and then confidently names
 * it. This instead waits for three things at once:
 *
 *  1. **Settled** — per-landmark movement under [STILLNESS_THRESHOLD] for [HOLD_MS]. Coaching a
 *     held shape is the only thing that makes sense; nobody critiques a transition.
 *  2. **Changed** — a different pose from the one last coached (unless [SAME_POSE_REPEAT_MS] has
 *     passed), so we don't burn 30 s repeating ourselves while they hold Tree.
 *  3. **Confident** — [PoseClassifier]'s best score clears [MIN_CONFIDENCE], so we skip frames
 *     where the person is half out of frame or in something we have no template for.
 *
 * All three run off the pose detection the preview already performs every ~400 ms, so this adds
 * gating rather than computation.
 */
class PoseHoldDetector {

  /** What the detector is waiting on — drives the on-screen hint during a session. */
  enum class State {
    NO_PERSON,
    /** Someone is there, but on the floor — we only coach standing poses. */
    NOT_STANDING,
    MOVING,
    SETTLING,
    READY,
    COOLDOWN,
  }

  data class Decision(
    val state: State,
    /** Fraction of [HOLD_MS] elapsed, 0..1 — drives the settle indicator. */
    val holdProgress: Float,
    /** Non-null only on the frame where all three gates pass. */
    val trigger: List<PoseClassifier.PoseMatch>?,
    val topPoseName: String?,
  )

  /**
   * Feature vectors collected while the body is still, pooled at trigger time.
   *
   * MediaPipe's world landmarks — the depth axis especially — carry enough per-frame noise to
   * reorder two near-neighbour poses, which is why the same held pose could be reported as two
   * different asanas on consecutive triggers. Taking a per-feature median across the hold window
   * removes that jitter, and it is free: these frames are already being detected for the
   * stillness test.
   */
  private val heldFeatures = mutableListOf<FloatArray>()

  private var previous: List<Pair<Float, Float>>? = null
  private var stillSinceMs = 0L
  private var lastAnalysedPose: String? = null
  private var lastAnalysedScore = 0f
  private var lastAnalysedAtMs = 0L

  fun reset() {
    heldFeatures.clear()
    previous = null
    stillSinceMs = 0L
    lastAnalysedPose = null
    lastAnalysedScore = 0f
    lastAnalysedAtMs = 0L
  }

  /**
   * Feeds one preview detection in. Call on every preview frame; cheap.
   *
   * @param engineBusy true while an inference is already running — we never trigger on top of
   *   one, so the caller's skip-if-busy path stays a backstop rather than the normal route.
   */
  fun onFrame(
    result: PoseLandmarkerResult?,
    nowMs: Long,
    engineBusy: Boolean,
  ): Decision {
    val landmarks = result?.landmarks()?.firstOrNull()
    if (landmarks == null || landmarks.isEmpty()) {
      previous = null
      stillSinceMs = 0L
      return Decision(State.NO_PERSON, 0f, null, null)
    }

    if (!PoseClassifier.isStandingPose(result)) {
      previous = null
      stillSinceMs = 0L
      heldFeatures.clear()
      return Decision(State.NOT_STANDING, 0f, null, null)
    }

    val current = landmarks.map { it.x() to it.y() }
    val prev = previous
    previous = current

    if (prev == null || prev.size != current.size) {
      stillSinceMs = 0L
      return Decision(State.MOVING, 0f, null, null)
    }

    // Mean Euclidean displacement across all joints, in normalised image units.
    var total = 0f
    for (i in current.indices) {
      val dx = current[i].first - prev[i].first
      val dy = current[i].second - prev[i].second
      total += sqrt(dx * dx + dy * dy)
    }
    val movement = total / current.size

    if (movement > STILLNESS_THRESHOLD) {
      stillSinceMs = 0L
      heldFeatures.clear()
      return Decision(State.MOVING, 0f, null, null)
    }

    if (stillSinceMs == 0L) stillSinceMs = nowMs
    PoseClassifier.featuresOf(result)?.let { f ->
      heldFeatures += f
      // Cap the window so a very long hold does not grow without bound.
      if (heldFeatures.size > MAX_POOLED_FRAMES) heldFeatures.removeAt(0)
    }
    val heldMs = nowMs - stillSinceMs
    val progress = (heldMs.toFloat() / HOLD_MS).coerceIn(0f, 1f)
    if (heldMs < HOLD_MS) {
      return Decision(State.SETTLING, progress, null, null)
    }

    // Settled. Rank the pooled median rather than this single frame, then apply the cheap gates
    // before committing to an expensive inference.
    val pooled = medianFeatures()
    val candidates =
      if (pooled != null) PoseClassifier.classifyFeatures(pooled) else PoseClassifier.classify(result)
    val top = candidates.firstOrNull()
    val topName = top?.name

    if (engineBusy) return Decision(State.COOLDOWN, 1f, null, topName)

    if (top == null || top.score < MIN_CONFIDENCE) {
      Log.d(TAG, "Settled but low confidence (${top?.score}); not analysing.")
      return Decision(State.COOLDOWN, 1f, null, topName)
    }

    val sinceLast = nowMs - lastAnalysedAtMs
    if (lastAnalysedAtMs != 0L && sinceLast < MIN_GAP_MS) {
      return Decision(State.COOLDOWN, 1f, null, topName)
    }
    val samePose = topName == lastAnalysedPose
    val formChanged = abs(top.score - lastAnalysedScore) >= FORM_CHANGE_DELTA
    if (samePose && !formChanged && sinceLast < SAME_POSE_REPEAT_MS) {
      return Decision(State.COOLDOWN, 1f, null, topName)
    }

    lastAnalysedPose = topName
    lastAnalysedScore = top.score
    lastAnalysedAtMs = nowMs
    Log.d(TAG, "Pooled ${heldFeatures.size} frames for classification.")
    heldFeatures.clear()
    // Require a fresh settle before the next trigger, so one long hold fires exactly once.
    stillSinceMs = 0L
    Log.d(
      TAG,
      "TRIGGER pose=$topName score=${top.score} movement=$movement" +
        " samePose=$samePose formChanged=$formChanged",
    )
    return Decision(State.READY, 1f, candidates, topName)
  }
  /** Per-feature median across the hold window; null until we have a couple of frames. */
  private fun medianFeatures(): FloatArray? {
    if (heldFeatures.size < 2) return heldFeatures.firstOrNull()
    val n = heldFeatures[0].size
    return FloatArray(n) { i ->
      val column = heldFeatures.map { it[i] }.sorted()
      val mid = column.size / 2
      if (column.size % 2 == 1) column[mid] else (column[mid - 1] + column[mid]) / 2f
    }
  }

}
