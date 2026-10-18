package com.google.ai.edge.gallery.ui.progress

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class ProgressModelsTest {
  @Test fun clockExcludesPauseAndBackgroundTime() {
    val clock = PracticeClock()
    clock.start(1000); clock.start(2000)
    assertEquals(3000L, clock.elapsed(4000))
    clock.pause(4000)
    assertEquals(3000L, clock.elapsed(9000))
    clock.start(10000)
    assertEquals(4500L, clock.elapsed(11500))
    clock.pause(11500); clock.pause(12000)
    assertEquals(4500L, clock.elapsed(18000))
  }
  @Test fun checkpointsCrossingMidnightAllocateEachDayCorrectly() {
    val end = ZonedDateTime.parse("2026-10-18T00:00:10+05:30[Asia/Kolkata]").toInstant().toEpochMilli()
    val split = splitPracticeTime(end, 30000, "Asia/Kolkata")
    assertEquals(20000L, split[LocalDate.parse("2026-10-17")])
    assertEquals(10000L, split[LocalDate.parse("2026-10-18")])
    assertEquals(30000L, split.values.sum())
    assertTrue(splitPracticeTime(end, 0, "Asia/Kolkata").isEmpty())
  }
  @Test fun daylightSavingDoesNotAssumeTwentyFourHourDays() {
    val end = ZonedDateTime.parse("2026-03-09T00:00:00-07:00[America/Los_Angeles]").toInstant().toEpochMilli()
    val split = splitPracticeTime(end, 23 * 3600000L, "America/Los_Angeles")
    assertEquals(mapOf(LocalDate.parse("2026-03-08") to 23 * 3600000L), split)
  }
  @Test fun reviewsDoNotInflatePracticeAndPartialSessionsContributeTime() {
    val day = LocalDate.parse("2026-10-18")
    val start = ZonedDateTime.parse("2026-10-18T08:00:00Z").toInstant().toEpochMilli()
    val sessions = listOf(
      ActivitySession(type = ActivityType.BREATHING, startedAt = start, timezone = "UTC", status = ActivityStatus.ENDED_EARLY, activeMs = 30000, practiceByDay = mapOf(day to 30000L)),
      ActivitySession(type = ActivityType.LIVE, startedAt = start, timezone = "UTC", status = ActivityStatus.COMPLETED, activeMs = 60000, practiceByDay = mapOf(day to 60000L)),
      ActivitySession(type = ActivityType.PHOTO, startedAt = start, timezone = "UTC", status = ActivityStatus.COMPLETED, activeMs = 120000, practiceByDay = mapOf(day to 120000L)),
    )
    val summary = summarizeProgress(sessions, day, day)
    assertEquals(90000L, summary.practiceMs)
    assertEquals(1, summary.activeDays)
    assertEquals(1, summary.completedSessions)
    assertEquals(3, summary.activities.size)
  }
  @Test fun periodFiltersTimeSegmentsIndependentlyFromSessionStart() {
    val day = LocalDate.parse("2026-10-18")
    val session = ActivitySession(type = ActivityType.BREATHING,
      startedAt = ZonedDateTime.parse("2026-10-17T23:59:40Z").toInstant().toEpochMilli(), timezone = "UTC",
      status = ActivityStatus.COMPLETED, activeMs = 60000,
      practiceByDay = mapOf(day.minusDays(1) to 20000L, day to 40000L))
    val summary = summarizeProgress(listOf(session), day, day)
    assertEquals(40000L, summary.practiceMs)
    assertEquals(1, summary.activeDays)
    assertEquals(0, summary.completedSessions)
    assertTrue(summary.activities.isEmpty())
  }
}
