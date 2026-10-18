package com.google.ai.edge.gallery.ui.breathing

import kotlin.math.PI
import kotlin.math.cos

internal enum class BreathingPhase { INHALE, HOLD, EXHALE }

/** Shared clock for the animation, phase labels and twelve-second audio loop. */
internal object BreathingTiming {
  const val PHASE_MS = 4_000L
  const val CYCLE_MS = PHASE_MS * 3
  val durations = listOf(1, 2, 3, 5)

  fun phaseAt(elapsedMs: Long): BreathingPhase = when (elapsedMs % CYCLE_MS) {
    in 0 until PHASE_MS -> BreathingPhase.INHALE
    in PHASE_MS until PHASE_MS * 2 -> BreathingPhase.HOLD
    else -> BreathingPhase.EXHALE
  }

  fun phaseFraction(elapsedMs: Long): Float =
    (elapsedMs % PHASE_MS).toFloat() / PHASE_MS

  fun expansionAt(elapsedMs: Long): Float {
    val eased = (0.5 - 0.5 * cos(PI * phaseFraction(elapsedMs))).toFloat()
    return when (phaseAt(elapsedMs)) {
      BreathingPhase.INHALE -> eased
      BreathingPhase.HOLD -> 1f
      BreathingPhase.EXHALE -> 1f - eased
    }
  }

  fun remainingSeconds(durationMs: Long, elapsedMs: Long): Long =
    ((durationMs - elapsedMs).coerceAtLeast(0) + 999) / 1_000
}
