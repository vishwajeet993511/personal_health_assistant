package com.google.ai.edge.gallery.ui.progress

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ProgressDatabaseTest {
  private val context = InstrumentationRegistry.getInstrumentation().targetContext
  private val name = "progress-test-${System.nanoTime()}.db"
  private lateinit var database: ProgressDatabase
  @Before fun create() { database = ProgressDatabase(context, name) }
  @After fun cleanup() { database.close(); context.deleteDatabase(name) }

  @Test fun checkpointsAreIdempotentAndKeepEarlierDailyTotals() {
    val base = ActivitySession(type = ActivityType.BREATHING, startedAt = 1792281600000L,
      updatedAt = 1792281605000L, timezone = "UTC", activeMs = 5000, plannedMs = 60000)
    database.save(base); database.save(base)
    val completed = base.copy(activeMs = 60000, updatedAt = base.updatedAt + 55000,
      status = ActivityStatus.COMPLETED, poseCounts = mapOf("Tree Pose" to 1))
    database.save(completed); database.save(completed)
    database.save(completed.copy(status = ActivityStatus.PAUSED, activeMs = 10000))
    val records = database.read()
    assertEquals(1, records.size)
    assertEquals(ActivityStatus.COMPLETED, records.single().status)
    assertEquals(60000L, records.single().activeMs)
    assertEquals(60000L, records.single().practiceByDay.values.sum())
    assertEquals(mapOf("Tree Pose" to 1), records.single().poseCounts)
    database.close(); database = ProgressDatabase(context, name)
    assertEquals(60000L, database.read().single().practiceByDay.values.sum())
  }
  @Test fun reviewNeverAddsPracticeMinutesAndKeepsFeedback() {
    database.save(ActivitySession(type = ActivityType.PHOTO, timezone = "UTC", status = ActivityStatus.COMPLETED,
      activeMs = 300000, feedback = "Pose: Tree Pose", poseCounts = mapOf("Tree Pose" to 1)))
    val review = database.read().single()
    assertEquals(0L, review.activeMs)
    assertTrue(review.practiceByDay.isEmpty())
    assertEquals("Pose: Tree Pose", review.feedback)
  }
  @Test fun endedEarlySessionCannotBeAccidentallyCountedAsCompleted() {
    val partial = ActivitySession(type = ActivityType.BREATHING, status = ActivityStatus.ENDED_EARLY,
      activeMs = 7000, plannedMs = 60000)
    database.save(partial)
    database.save(partial.copy(status = ActivityStatus.COMPLETED))
    assertEquals(ActivityStatus.ENDED_EARLY, database.read().single().status)
  }
}
