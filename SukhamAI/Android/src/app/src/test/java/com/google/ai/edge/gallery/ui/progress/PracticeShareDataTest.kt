package com.google.ai.edge.gallery.ui.progress

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class PracticeShareDataTest {
  private val today = LocalDate.of(2026, 10, 18)
  private fun session(type: ActivityType, status: ActivityStatus, date: LocalDate = today, ms: Long = 0) = ActivitySession(
    type = type, title = "Private title", startedAt = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
    timezone = "UTC", status = status, activeMs = ms, practiceByDay = if (ms > 0) mapOf(date to ms) else emptyMap(), feedback = "Private coaching notes")

  @Test fun partialPracticeContributesTimeWithoutInventingCompletion() {
    val data = practiceShareData(listOf(session(ActivityType.BREATHING, ActivityStatus.ENDED_EARLY, ms = 30_000)), false, today.minusDays(6), today)
    assertEquals("<1", data.firstValue)
    assertEquals("0", data.secondValue)
    assertTrue(data.hasActivity)
    assertFalse(data.toString().contains("Private"))
  }
  @Test fun reviewOnlyCardCountsCompletedReviewsNotPracticeMinutes() {
    val data = practiceShareData(listOf(session(ActivityType.PHOTO, ActivityStatus.COMPLETED), session(ActivityType.VIDEO, ActivityStatus.FAILED)), false, today.minusDays(6), today)
    assertEquals("1", data.firstValue)
    assertEquals("pose review", data.firstLabel)
    assertEquals("1", data.secondValue)
  }
  @Test fun weeklyCardUsesChosenDatesAndActualDailyPractice() {
    val start = today.minusDays(6)
    val data = practiceShareData(listOf(session(ActivityType.BREATHING, ActivityStatus.COMPLETED, start, 60_000), session(ActivityType.LIVE, ActivityStatus.COMPLETED, start.minusDays(1), 600_000)), true, start, today)
    assertEquals("1", data.firstValue)
    assertEquals("1", data.secondValue)
    assertEquals(listOf(true, false, false, false, false, false, false), data.activeDays)
  }
  @Test fun emptyHistoryCannotBeExportedAsAnAchievement() {
    assertFalse(practiceShareData(emptyList(), true, today.minusDays(6), today).hasActivity)
  }
}
