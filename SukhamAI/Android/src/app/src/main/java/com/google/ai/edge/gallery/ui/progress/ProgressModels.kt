package com.google.ai.edge.gallery.ui.progress

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

internal enum class ActivityType(val title: String, val isPractice: Boolean) {
  PROGRAM("Programs", true), LIVE("Live Coach", true), PHOTO("Photo Review", false),
  VIDEO("Video Review", false), BREATHING("Breathing", true),
}
internal enum class ActivityStatus { IN_PROGRESS, PAUSED, COMPLETED, ENDED_EARLY, INTERRUPTED, FAILED }
internal data class ActivitySession(
  val id: String = UUID.randomUUID().toString(),
  val type: ActivityType,
  val title: String = type.title,
  val startedAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = startedAt,
  val timezone: String = ZoneId.systemDefault().id,
  val status: ActivityStatus = ActivityStatus.IN_PROGRESS,
  val activeMs: Long = 0,
  val plannedMs: Long = 0,
  val analyzedMs: Long = 0,
  val feedback: String = "",
  val poseCounts: Map<String, Int> = emptyMap(),
  val practiceByDay: Map<LocalDate, Long> = emptyMap(),
) {
  val date: LocalDate get() = Instant.ofEpochMilli(startedAt).atZone(ZoneId.of(timezone)).toLocalDate()
}

/** Monotonic clock: paused/background time is never practice time. */
internal class PracticeClock {
  private var accumulated = 0L
  private var runningSince: Long? = null
  fun start(now: Long) { if (runningSince == null) runningSince = now }
  fun pause(now: Long) { accumulated = elapsed(now); runningSince = null }
  fun elapsed(now: Long): Long = accumulated + (runningSince?.let { (now - it).coerceAtLeast(0) } ?: 0)
  fun reset() { accumulated = 0; runningSince = null }
}

internal fun splitPracticeTime(endAt: Long, durationMs: Long, timezone: String): Map<LocalDate, Long> {
  val zone = ZoneId.of(timezone)
  var cursor = endAt - durationMs.coerceAtLeast(0)
  val result = linkedMapOf<LocalDate, Long>()
  while (cursor < endAt) {
    val day = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
    val next = minOf(endAt, day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
    result[day] = (result[day] ?: 0) + next - cursor
    cursor = next
  }
  return result
}

internal data class ProgressSummary(
  val practiceMs: Long,
  val activeDays: Int,
  val completedSessions: Int,
  val activities: List<ActivitySession>,
  val daily: Map<LocalDate, Map<ActivityType, Long>>,
)
internal fun summarizeProgress(sessions: List<ActivitySession>, from: LocalDate, through: LocalDate): ProgressSummary {
  val daily = sortedMapOf<LocalDate, MutableMap<ActivityType, Long>>()
  sessions.filter { it.type.isPractice }.forEach { session ->
    session.practiceByDay.filterKeys { it >= from && it <= through }.forEach { (day, ms) ->
      if (ms > 0) {
        val values = daily.getOrPut(day) { mutableMapOf() }
        values[session.type] = (values[session.type] ?: 0) + ms
      }
    }
  }
  val activities = sessions.filter { it.date >= from && it.date <= through }
  return ProgressSummary(daily.values.sumOf { it.values.sum() }, daily.size,
    activities.count { it.type.isPractice && it.status == ActivityStatus.COMPLETED }, activities, daily)
}
