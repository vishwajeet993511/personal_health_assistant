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
import android.util.Log
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.runtime.runtimeHelper
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

private const val TAG = "YogaCoachEngine"

// Image edge cap for the multimodal Gemma input. Pose detection still runs on the larger
// LANDMARK_INPUT_MAX_EDGE bitmap so accuracy isn't sacrificed.
internal const val GEMMA_INPUT_MAX_EDGE = 512
internal const val LANDMARK_INPUT_MAX_EDGE = 1024

// Soft caps so a runaway generation doesn't stall the whole session. In a repeating session a
// stuck frame is far more costly than in the one-shot photo flow (it blocks every later tick),
// so the time cap is tighter than the photo screen's 90 s.
internal const val MAX_RESPONSE_CHARS = 700
internal const val SESSION_MAX_RESPONSE_MS = 45_000L

/**
 * Builds the coaching prompt around the geometric ranking from [PoseClassifier].
 *
 * Naming is no longer an open generation task: the classifier has already measured the joint
 * angles, so the model is handed 2-3 ranked candidates and told to take the first unless the
 * image plainly contradicts it. That removes the long pose menu the prompt used to carry, which
 * matters because prefill dominates latency on-device and scales with prompt length.
 *
 * The model is also explicitly allowed to say nothing is wrong. Demanding a correction every
 * time makes it manufacture one, so a well-held pose gets picked at for no reason. The
 * classifier's own score is handed over as evidence: a high score means the joint angles are
 * close to the reference, which is real information about form quality rather than a guess.
 *
 * Falls back to asking the model to name the pose itself when the classifier found nothing
 * (no landmarks), so a missing skeleton degrades rather than breaking.
 */
internal fun buildYogaPrompt(
  candidates: List<PoseClassifier.PoseMatch>,
  /** Measured joint geometry in plain English, from [PoseClassifier.describeGeometry]. */
  geometry: List<String> = emptyList(),
  /** Where the skeleton departs from the top candidate's reference form. */
  deviations: List<String> = emptyList(),
): String = buildString {
  append("The cyan skeleton overlay marks a person's joints while doing yoga.\n\n")

  if (geometry.isNotEmpty()) {
    append("Measured from their joint positions:\n")
    geometry.forEach { append("  - ").append(it).append("\n") }
    append("\n")
  }

  if (candidates.isEmpty()) {
    append("Name the yoga pose you see.\n")
  } else {
    append("Poses whose body shape matches, closest first:\n")
    candidates.forEach { m ->
      append("  ").append(m.name)
      PoseCues.descriptionFor(m.name)?.let { append(" — ").append(it) }
      append("\n")
      // Cues for EVERY candidate, not just the top one: the model is told it may override the
      // ranking, so whichever it picks it needs that pose's checklist. Giving only the leader's
      // faults meant an override was judged against the wrong pose entirely.
      val cues = PoseCues.cuesFor(m.name)
      if (cues.isNotEmpty()) {
        append("     usual faults: ").append(cues.joinToString("; ")).append("\n")
      }
    }
    append("\nThis ranking comes from body shape alone and is usually right, but you can see ")
    append("the image and it cannot — you have the final say on which pose it is.\n")
  }

  if (deviations.isNotEmpty()) {
    append("\nCompared with a well-executed version of the closest pose:\n")
    deviations.forEach { append("  - ").append(it).append("\n") }
  } else if (candidates.isNotEmpty()) {
    append("\nEvery measured joint angle is close to the reference for the closest pose, so ")
    append("the form is probably good.\n")
  }

  append("\nOnly give a correction if something is genuinely wrong. If the alignment looks ")
  append("good, write exactly \"Fix: None, hold it\" and do not invent a problem. ")
  append("When you do correct something, name a specific body part and the direction to move ")
  append("it (for example \"drop the left hip down and back\"). Never give generic advice ")
  append("like \"engage your core\" or \"breathe deeply\". Speak gently, like a calm yoga ")
  append("teacher, and never score or grade the person.\n\n")
  append("Reply in exactly two lines, under 40 words total. Plain text only, no markdown, ")
  append("no dashes as separators:\n")
  append("Pose: <name>\n")
  append("Fix: <one specific, gently phrased correction, or \"None, hold it\">")
}

/**
 * What the voice coach says for a finished card: the correction only.
 *
 * The pose name and any score are for the screen; reading "Pose: Tree, Fix: ..." aloud sounds
 * like a form being filled in. A pose that needs nothing gets a quiet reassurance instead of
 * the literal "None, hold it".
 */
internal fun spokenCoachingText(raw: String): String {
  val fix =
    raw.lineSequence()
      .map { it.trim() }
      .firstOrNull { it.startsWith("Fix:", ignoreCase = true) }
      ?.substringAfter(':')
      ?.trim()
      .orEmpty()
  if (fix.isEmpty()) {
    // Unstructured text: drop pose and score lines, keep whatever is left.
    return raw.lineSequence()
      .map { it.trim() }
      .filterNot { it.startsWith("Pose:", true) || it.startsWith("Form:", true) }
      .joinToString(" ")
      .trim()
  }
  return if (fix.startsWith("none", ignoreCase = true)) "Lovely. Stay here, and keep breathing."
  else fix.trimEnd('.') + ". Then breathe, and stay with it."
}

/** Classifier score above which the skeleton is close enough to the template to call it good. */
private const val STRONG_MATCH_SCORE = 0.78f

/** Lifecycle of a single sampled frame as it moves through the pipeline. */
enum class CoachingStatus {
  /** Pose landmarks found, skeleton drawn, waiting on Gemma. */
  ANALYZING,
  /** Gemma finished; [CoachingCard.text] holds the coaching. */
  DONE,
  /** MediaPipe found no person in the frame — Gemma was never invoked. */
  NO_POSE,
  /** Pose detection or inference threw. */
  ERROR,
}

/**
 * One entry in the coaching timeline: the frame that was sampled, the skeleton drawn on it, and
 * whatever Gemma has said about it so far.
 *
 * @param timestampMs position this frame was taken from — elapsed session time in live mode,
 *   position within the clip in video mode. Drives the "0:40" label in the timeline.
 */
data class CoachingCard(
  val id: Long,
  val timestampMs: Long,
  val thumbnail: Bitmap?,
  val status: CoachingStatus,
  val text: String = "",
)

/**
 * Shared pose-coaching pipeline behind both the live-camera and uploaded-video modes.
 *
 * A frame goes through: downscale -> MediaPipe pose detection -> skeleton baked into the pixels
 * -> multimodal Gemma. The two modes differ only in how they feed frames in:
 *   - live camera calls [offerFrame], which **drops** a frame when a previous one is still being
 *     analyzed. On-device Gemma needs ~10-30 s per frame, so a strict 20 s wall-clock tick would
 *     otherwise build a backlog and the coaching would drift further behind the user the longer
 *     the session ran.
 *   - uploaded video calls [analyzeFrame], which queues instead. There is no real-time clock to
 *     fall behind, and a card for every sampled position is the point.
 *
 * Both paths share one [Mutex] so only a single inference is ever in flight — the underlying
 * LiteRT conversation is not safe to drive concurrently.
 */
class YogaCoachEngine(
  private val poseHelper: PoseLandmarkerHelper,
  private val scope: CoroutineScope,
) {
  private val _cards = MutableStateFlow<List<CoachingCard>>(emptyList())
  val cards: StateFlow<List<CoachingCard>> = _cards.asStateFlow()

  private val _busy = MutableStateFlow(false)
  val busy: StateFlow<Boolean> = _busy.asStateFlow()

  /** Ticks dropped by [offerFrame] because Gemma was still working. Surfaced in the UI. */
  private val _skippedTicks = MutableStateFlow(0)
  val skippedTicks: StateFlow<Int> = _skippedTicks.asStateFlow()

  private val lock = Mutex()
  private var nextId = 0L

  fun reset() {
    _cards.value = emptyList()
    _skippedTicks.value = 0
  }

  /**
   * Live-camera path. Analyzes [frame] only if the pipeline is idle; otherwise counts a skipped
   * tick and returns false immediately so the caller's 20 s cadence stays on its grid.
   */
  suspend fun offerFrame(
    model: Model,
    timestampMs: Long,
    frame: Bitmap,
    /** Ranking already computed by the caller's gating pass; recomputed here when null. */
    precomputed: List<PoseClassifier.PoseMatch>? = null,
  ): Boolean {
    if (!lock.tryLock()) {
      _skippedTicks.update { it + 1 }
      Log.d(TAG, "Dropping frame at ${timestampMs}ms — inference still in flight.")
      return false
    }
    try {
      analyze(model, timestampMs, frame, precomputed)
    } finally {
      lock.unlock()
    }
    return true
  }

  /** Uploaded-video path. Waits its turn rather than dropping the frame. */
  suspend fun analyzeFrame(model: Model, timestampMs: Long, frame: Bitmap) {
    lock.lock()
    try {
      analyze(model, timestampMs, frame, null)
    } finally {
      lock.unlock()
    }
  }

  private suspend fun analyze(
    model: Model,
    timestampMs: Long,
    frame: Bitmap,
    precomputed: List<PoseClassifier.PoseMatch>?,
  ) {
    _busy.value = true
    val id = nextId++
    try {
      val scaled = withContext(Dispatchers.Default) { downscaleForInference(frame, LANDMARK_INPUT_MAX_EDGE) }
      val result =
        withContext(Dispatchers.Default) {
          poseHelper.setup()
          poseHelper.detect(scaled)
        }
      if (result == null || result.landmarks().isEmpty()) {
        addCard(
          CoachingCard(
            id = id,
            timestampMs = timestampMs,
            thumbnail = scaled,
            status = CoachingStatus.NO_POSE,
            text = "No person detected in this frame.",
          )
        )
        return
      }

      val candidates =
        precomputed ?: withContext(Dispatchers.Default) { PoseClassifier.classify(result) }
      val geometry = withContext(Dispatchers.Default) { PoseClassifier.describeGeometry(result) }
      val deviations =
        candidates.firstOrNull()?.let { top ->
          withContext(Dispatchers.Default) { PoseClassifier.formDeviations(result, top.name) }
        } ?: emptyList()
      Log.d(
        TAG,
        "Pose candidates: " + candidates.joinToString { "${it.name}=%.2f".format(it.score) },
      )
      val annotated = withContext(Dispatchers.Default) { PoseOverlay.drawOverlay(scaled, result) }
      addCard(
        CoachingCard(
          id = id,
          timestampMs = timestampMs,
          thumbnail = annotated,
          status = CoachingStatus.ANALYZING,
        )
      )

      // Each frame is judged on its own. Without this the LiteRT conversation would accumulate
      // every previous frame and prompt, so context (and latency) would grow without bound over
      // a long session and the model would start comparing poses instead of coaching the
      // current one.
      val resetStart = System.currentTimeMillis()
      withContext(Dispatchers.IO) {
        model.runtimeHelper.resetConversation(
          model = model,
          supportImage = model.llmSupportImage,
        )
      }
      delay(300)
      val resetMs = System.currentTimeMillis() - resetStart
      Log.d(TAG, "TIMING resetConversation took ${resetMs}ms")

      val gemmaInput = withContext(Dispatchers.Default) { downscaleForInference(annotated, GEMMA_INPUT_MAX_EDGE) }
      val inferStart = System.currentTimeMillis()
      var firstTokenMs = -1L
      val text =
        runGemma(model, gemmaInput, buildYogaPrompt(candidates, geometry, deviations)) { partial ->
          if (firstTokenMs < 0) {
            firstTokenMs = System.currentTimeMillis() - inferStart
            Log.d(TAG, "TIMING first token after ${firstTokenMs}ms (prefill)")
          }
          updateCard(id) { it.copy(text = cleanCoachingText(partial)) }
        }
      Log.d(
        TAG,
        "TIMING frame done: reset=${resetMs}ms prefill=${firstTokenMs}ms" +
          " total=${System.currentTimeMillis() - resetStart}ms chars=${text.length}",
      )
      updateCard(id) { it.copy(status = CoachingStatus.DONE, text = cleanCoachingText(text)) }
    } catch (t: Throwable) {
      Log.e(TAG, "Analysis failed for frame at ${timestampMs}ms", t)
      val message = t.message ?: "Unknown error"
      if (_cards.value.any { it.id == id }) {
        updateCard(id) { it.copy(status = CoachingStatus.ERROR, text = message) }
      } else {
        addCard(
          CoachingCard(
            id = id,
            timestampMs = timestampMs,
            thumbnail = null,
            status = CoachingStatus.ERROR,
            text = message,
          )
        )
      }
    } finally {
      _busy.value = false
    }
  }

  /**
   * Bridges [com.google.ai.edge.gallery.runtime.LlmModelHelper.runInference]'s callback API into
   * a suspend function so the caller's mutex releases deterministically when generation ends —
   * including when it ends via the early-stop caps below.
   */
  private suspend fun runGemma(
    model: Model,
    image: Bitmap,
    prompt: String,
    onPartial: (String) -> Unit,
  ): String = suspendCancellableCoroutine { cont ->
    val builder = StringBuilder()
    val started = System.currentTimeMillis()
    val stopRequested = AtomicBoolean(false)
    val settled = AtomicBoolean(false)

    model.runtimeHelper.runInference(
      model = model,
      input = prompt,
      images = listOf(image),
      resultListener = { partial, done, _ ->
        builder.append(partial)
        onPartial(builder.toString())
        val tooLong = builder.length > MAX_RESPONSE_CHARS
        val tookTooLong = System.currentTimeMillis() - started > SESSION_MAX_RESPONSE_MS
        if ((tooLong || tookTooLong) && stopRequested.compareAndSet(false, true)) {
          Log.d(TAG, "Early-stopping generation (chars=${builder.length})")
          model.runtimeHelper.stopResponse(model)
        }
        if (done && settled.compareAndSet(false, true)) {
          cont.resume(builder.toString())
        }
      },
      // Registered once per model by the runtime rather than per call, so this is only a
      // backstop for a teardown mid-generation — the `done` and `onError` paths below are what
      // actually settle a normal frame. Guarded so a late fire can't resume twice.
      cleanUpListener = {
        if (settled.compareAndSet(false, true)) cont.resume(builder.toString())
      },
      onError = { message ->
        if (settled.compareAndSet(false, true)) {
          cont.resume(if (builder.isEmpty()) "Error: $message" else builder.toString())
        }
      },
      coroutineScope = scope,
    )

    cont.invokeOnCancellation {
      if (stopRequested.compareAndSet(false, true)) {
        runCatching { model.runtimeHelper.stopResponse(model) }
      }
    }
  }

  private fun addCard(card: CoachingCard) {
    _cards.update { it + card }
  }

  private fun updateCard(id: Long, transform: (CoachingCard) -> CoachingCard) {
    _cards.update { list -> list.map { if (it.id == id) transform(it) else it } }
  }
}

/**
 * Strips the markdown the model sprinkles into its answer.
 *
 * The coaching text is rendered as plain [androidx.compose.material3.Text], not markdown, so
 * `---` rules, `**bold**` and `- ` bullets show up as literal punctuation. Cheap to clean here
 * rather than pulling in a markdown renderer for three short lines.
 */
internal fun cleanCoachingText(raw: String): String =
  raw
    .lineSequence()
    .map { line ->
      line
        .trim()
        // Leading bullet/quote markers.
        .removePrefix("- ")
        .removePrefix("* ")
        .removePrefix("• ")
        .removePrefix("> ")
        // Inline emphasis and heading marks.
        .replace("**", "")
        .replace("__", "")
        .trimStart('#', ' ')
        .trim()
    }
    // Drop horizontal rules and any line that is nothing but punctuation.
    .filter { line -> line.isNotEmpty() && line.any { it.isLetterOrDigit() } }
    .joinToString("\n")
    .trim()

/** Scales [source] down so its longest edge is at most [maxEdge]. Always returns ARGB_8888. */
internal fun downscaleForInference(source: Bitmap, maxEdge: Int): Bitmap {
  val biggest = maxOf(source.width, source.height)
  if (biggest <= maxEdge) return source.copy(Bitmap.Config.ARGB_8888, false)
  val scale = maxEdge.toFloat() / biggest
  val w = (source.width * scale).toInt().coerceAtLeast(1)
  val h = (source.height * scale).toInt().coerceAtLeast(1)
  return Bitmap.createScaledBitmap(source, w, h, true)
}

/** Formats an elapsed/track position as `m:ss` for the timeline labels. */
internal fun formatTimestamp(ms: Long): String {
  val totalSeconds = ms / 1000
  return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
