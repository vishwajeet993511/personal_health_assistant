package com.google.ai.edge.gallery.ui.progress

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.concurrent.Executors

/** Process-owned serial writer: final checkpoints survive leaving a composable. */
internal class ProgressRepository private constructor(context: Context) {
  private val database = ProgressDatabase(context.applicationContext)
  private val scope = CoroutineScope(SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())
  private val mutableSessions = MutableStateFlow<List<ActivitySession>>(emptyList())
  val sessions = mutableSessions.asStateFlow()
  private val mutableLoading = MutableStateFlow(true)
  val loading = mutableLoading.asStateFlow()
  private val mutableError = MutableStateFlow<String?>(null)
  val error = mutableError.asStateFlow()

  init {
    scope.launch {
      try {
        // Only runs once per process: unfinished records belong to the previous process.
        database.writableDatabase.execSQL("UPDATE activity_sessions SET status = 'INTERRUPTED' WHERE status IN ('IN_PROGRESS','PAUSED')")
        refresh()
      } catch (e: Exception) { report(e) }
      finally { mutableLoading.value = false }
    }
  }
  fun save(session: ActivitySession) {
    scope.launch {
      try { database.save(session); refresh() }
      catch (e: Exception) { report(e) }
    }
  }
  fun retry() { scope.launch { try { refresh() } catch (e: Exception) { report(e) } } }
  private fun refresh() { mutableSessions.value = database.read(); mutableError.value = null }
  private fun report(e: Exception) { Log.e("ProgressHistory", "Unable to access local history", e); mutableError.value = "Unable to load or save activity history. Please try again." }
  companion object {
    @Volatile private var instance: ProgressRepository? = null
    fun get(context: Context): ProgressRepository = instance ?: synchronized(this) {
      instance ?: ProgressRepository(context).also { instance = it }
    }
  }
}

internal class ProgressDatabase(context: Context, name: String = "sukham_progress.db") : SQLiteOpenHelper(context, name, null, 1) {
  override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL("""CREATE TABLE activity_sessions (
      id TEXT PRIMARY KEY, type TEXT NOT NULL, title TEXT NOT NULL,
      started_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, timezone TEXT NOT NULL,
      status TEXT NOT NULL, active_ms INTEGER NOT NULL, planned_ms INTEGER NOT NULL,
      analyzed_ms INTEGER NOT NULL, feedback TEXT NOT NULL)""")
    db.execSQL("CREATE INDEX activity_by_date ON activity_sessions(started_at)")
    db.execSQL("""CREATE TABLE practice_days (
      session_id TEXT NOT NULL REFERENCES activity_sessions(id) ON DELETE CASCADE,
      day TEXT NOT NULL, active_ms INTEGER NOT NULL, PRIMARY KEY(session_id, day))""")
    db.execSQL("""CREATE TABLE session_poses (
      session_id TEXT NOT NULL REFERENCES activity_sessions(id) ON DELETE CASCADE,
      pose TEXT NOT NULL, count INTEGER NOT NULL, PRIMARY KEY(session_id, pose))""")
  }
  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

  fun save(session: ActivitySession) {
    val db = writableDatabase
    db.beginTransaction()
    try {
      var previousMs = 0L
      var previousStatus: ActivityStatus? = null
      db.rawQuery("SELECT active_ms,status FROM activity_sessions WHERE id=?", arrayOf(session.id)).use {
        if (it.moveToFirst()) { previousMs = it.getLong(0); previousStatus = ActivityStatus.valueOf(it.getString(1)) }
      }
      val terminal = setOf(ActivityStatus.COMPLETED, ActivityStatus.ENDED_EARLY, ActivityStatus.FAILED)
      if (previousStatus in terminal && previousStatus != session.status) { db.setTransactionSuccessful(); return }
      val active = if (session.type.isPractice) session.activeMs.coerceAtLeast(previousMs) else 0L
      val values = ContentValues().apply {
        put("id", session.id); put("type", session.type.name); put("title", session.title)
        put("started_at", session.startedAt); put("updated_at", session.updatedAt); put("timezone", session.timezone)
        put("status", session.status.name); put("active_ms", active); put("planned_ms", session.plannedMs)
        put("analyzed_ms", session.analyzedMs); put("feedback", session.feedback)
      }
      // UPDATE preserves children; REPLACE would delete daily history via the foreign key.
      if (db.update("activity_sessions", values, "id=?", arrayOf(session.id)) == 0) db.insertOrThrow("activity_sessions", null, values)
      splitPracticeTime(session.updatedAt, active - previousMs, session.timezone).forEach { (day, ms) ->
        db.execSQL("INSERT OR IGNORE INTO practice_days(session_id,day,active_ms) VALUES(?,?,0)", arrayOf(session.id, day.toString()))
        db.execSQL("UPDATE practice_days SET active_ms=active_ms+? WHERE session_id=? AND day=?", arrayOf<Any>(ms, session.id, day.toString()))
      }
      db.delete("session_poses", "session_id=?", arrayOf(session.id))
      session.poseCounts.filter { it.key.isNotBlank() && it.value > 0 }.forEach { (pose, count) ->
        db.insertOrThrow("session_poses", null, ContentValues().apply { put("session_id", session.id); put("pose", pose); put("count", count) })
      }
      db.setTransactionSuccessful()
    } finally { db.endTransaction() }
  }
  fun read(): List<ActivitySession> {
    val db = readableDatabase
    val days = mutableMapOf<String, MutableMap<LocalDate, Long>>()
    db.rawQuery("SELECT session_id,day,active_ms FROM practice_days", null).use { c ->
      while(c.moveToNext()) days.getOrPut(c.getString(0)) { mutableMapOf() }[LocalDate.parse(c.getString(1))] = c.getLong(2)
    }
    val poses = mutableMapOf<String, MutableMap<String, Int>>()
    db.rawQuery("SELECT session_id,pose,count FROM session_poses", null).use { c ->
      while(c.moveToNext()) poses.getOrPut(c.getString(0)) { mutableMapOf() }[c.getString(1)] = c.getInt(2)
    }
    return db.rawQuery("SELECT id,type,title,started_at,updated_at,timezone,status,active_ms,planned_ms,analyzed_ms,feedback FROM activity_sessions ORDER BY started_at DESC", null).use { c ->
      buildList {
        while(c.moveToNext()) add(ActivitySession(c.getString(0),ActivityType.valueOf(c.getString(1)),c.getString(2),c.getLong(3),c.getLong(4),c.getString(5),ActivityStatus.valueOf(c.getString(6)),c.getLong(7),c.getLong(8),c.getLong(9),c.getString(10),poses[c.getString(0)].orEmpty(),days[c.getString(0)].orEmpty()))
      }
    }
  }
}
