package com.google.ai.edge.gallery.ui.progress

import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal data class PracticeShareData(
  val heading: String,
  val dateLabel: String,
  val firstValue: String,
  val firstLabel: String,
  val secondValue: String,
  val secondLabel: String,
  val activeDays: List<Boolean>,
  val dayLabels: List<String>,
  val defaultCaption: String,
  val hasActivity: Boolean,
)

internal fun practiceShareData(sessions: List<ActivitySession>, weekly: Boolean, weekStart: LocalDate, today: LocalDate): PracticeShareData {
  val from = if (weekly) weekStart else today
  val through = if (weekly) weekStart.plusDays(6) else today
  val summary = summarizeProgress(sessions, from, through)
  val reviews = summary.activities.filter { !it.type.isPractice && it.status == ActivityStatus.COMPLETED }
  val reviewDays = reviews.map { it.date }.toSet()
  val hasPractice = summary.practiceMs > 0
  val hasActivity = hasPractice || reviews.isNotEmpty()
  val format = DateTimeFormatter.ofPattern("d MMM")
  return PracticeShareData(
    heading = if (weekly) { if (hasPractice) "My week in motion" else "My week of learning" }
      else if (hasPractice) "A moment for myself" else "A moment to learn",
    dateLabel = if (weekly) "${from.format(format)} – ${through.format(format)} ${through.year}" else today.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
    firstValue = if (hasPractice) displayMinutes(summary.practiceMs) else reviews.size.toString(),
    firstLabel = if (hasPractice) "minutes practiced" else if (reviews.size == 1) "pose review" else "pose reviews",
    secondValue = if (weekly) (if (hasPractice) summary.activeDays else reviewDays.size).toString()
      else (summary.completedSessions + reviews.size).toString(),
    secondLabel = if (weekly) "active days" else "completed activities",
    activeDays = if (weekly) (0L..6L).map { offset ->
      val day = from.plusDays(offset)
      if (hasPractice) summary.daily[day]?.values?.sum()?.let { it > 0 } == true else day in reviewDays
    } else emptyList(),
    dayLabels = if (weekly) (0L..6L).map { from.plusDays(it).format(DateTimeFormatter.ofPattern("EEEEE")) } else emptyList(),
    defaultCaption = if (hasPractice) "Made time for myself." else "A little learning, every day.",
    hasActivity = hasActivity,
  )
}
