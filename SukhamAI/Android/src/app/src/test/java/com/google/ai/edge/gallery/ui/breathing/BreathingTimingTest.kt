package com.google.ai.edge.gallery.ui.breathing

import org.junit.Assert.*
import org.junit.Test

class BreathingTimingTest {
  @Test fun inhaleHoldExhaleMatchAudioBoundaries() {
    assertEquals(BreathingPhase.INHALE, BreathingTiming.phaseAt(0))
    assertEquals(BreathingPhase.INHALE, BreathingTiming.phaseAt(3999))
    assertEquals(BreathingPhase.HOLD, BreathingTiming.phaseAt(4000))
    assertEquals(BreathingPhase.HOLD, BreathingTiming.phaseAt(7999))
    assertEquals(BreathingPhase.EXHALE, BreathingTiming.phaseAt(8000))
    assertEquals(BreathingPhase.EXHALE, BreathingTiming.phaseAt(11999))
    assertEquals(BreathingPhase.INHALE, BreathingTiming.phaseAt(12000))
    assertEquals(BreathingPhase.HOLD, BreathingTiming.phaseAt(244000))
  }

  @Test fun remainingTimeRoundsUpAndNeverGoesNegative() {
    assertEquals(60L, BreathingTiming.remainingSeconds(60000, 0))
    assertEquals(60L, BreathingTiming.remainingSeconds(60000, 1))
    assertEquals(59L, BreathingTiming.remainingSeconds(60000, 1000))
    assertEquals(1L, BreathingTiming.remainingSeconds(60000, 59999))
    assertEquals(0L, BreathingTiming.remainingSeconds(60000, 60000))
    assertEquals(0L, BreathingTiming.remainingSeconds(60000, 65000))
  }

  @Test fun circleHoldsStillAndTransitionsContinuously() {
    assertEquals(0f, BreathingTiming.expansionAt(0), 0.001f)
    assertEquals(1f, BreathingTiming.expansionAt(3999), 0.001f)
    assertEquals(1f, BreathingTiming.expansionAt(4000), 0.001f)
    assertEquals(1f, BreathingTiming.expansionAt(6000), 0.001f)
    assertEquals(1f, BreathingTiming.expansionAt(7999), 0.001f)
    assertEquals(1f, BreathingTiming.expansionAt(8000), 0.001f)
    assertEquals(0.5f, BreathingTiming.expansionAt(10000), 0.001f)
    assertEquals(0f, BreathingTiming.expansionAt(11999), 0.001f)
    assertEquals(0f, BreathingTiming.expansionAt(12000), 0.001f)
  }

  @Test fun everyDurationEndsAtACompleteBreathingCycle() {
    BreathingTiming.durations.forEach { minutes ->
      assertEquals(0L, minutes * 60000L % BreathingTiming.CYCLE_MS)
    }
  }
}
