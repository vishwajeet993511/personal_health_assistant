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

package com.google.ai.edge.gallery.ui.profile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.ai.edge.gallery.ui.profile.model.UserProfile

/**
 * Local SQLite database helper managing the user profile table for Sukham AI.
 * Persists personal health parameters, biometrics, goals, and connection settings locally.
 */
class UserProfileDatabaseHelper(context: Context) :
  SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

  companion object {
    private const val DATABASE_NAME = "sukham_user_profile.db"
    private const val DATABASE_VERSION = 3

    const val TABLE_PROFILE = "user_profile"
    const val COL_ID = "id"
    const val COL_NAME = "name"
    const val COL_TAGLINE = "tagline"
    const val COL_HEIGHT_CM = "height_cm"
    const val COL_WEIGHT_KG = "weight_kg"
    const val COL_GENDER = "gender"
    const val COL_TARGET_FOCUS = "target_focus"
    const val COL_STREAK_DAYS = "streak_days"
    const val COL_FORM_SCORE = "form_score"
    const val COL_CONNECTED_DEVICE = "connected_device"
    const val COL_IS_CONNECTED = "is_connected"
    const val COL_IS_READ_ALOUD = "is_read_aloud"
  }

  override fun onCreate(db: SQLiteDatabase) {
    val createTableSql = """
      CREATE TABLE $TABLE_PROFILE (
        $COL_ID INTEGER PRIMARY KEY,
        $COL_NAME TEXT NOT NULL,
        $COL_TAGLINE TEXT NOT NULL,
        $COL_HEIGHT_CM INTEGER NOT NULL,
        $COL_WEIGHT_KG INTEGER NOT NULL,
        $COL_GENDER TEXT NOT NULL,
        $COL_TARGET_FOCUS TEXT NOT NULL,
        $COL_STREAK_DAYS INTEGER NOT NULL,
        $COL_FORM_SCORE INTEGER NOT NULL,
        $COL_CONNECTED_DEVICE TEXT NOT NULL,
        $COL_IS_CONNECTED INTEGER NOT NULL,
        $COL_IS_READ_ALOUD INTEGER NOT NULL DEFAULT 1
      )
    """.trimIndent()
    db.execSQL(createTableSql)

    // Seed initial default profile record (Alex Morgan)
    val defaultValues = ContentValues().apply {
      put(COL_ID, 1L)
      put(COL_NAME, "Alex Morgan")
      put(COL_TAGLINE, "Yoga & Strength Enthusiast")
      put(COL_HEIGHT_CM, 178)
      put(COL_WEIGHT_KG, 72)
      put(COL_GENDER, "Male")
      put(COL_TARGET_FOCUS, "Posture & Alignment")
      put(COL_STREAK_DAYS, 6)
      put(COL_FORM_SCORE, 84)
      put(COL_CONNECTED_DEVICE, "Galaxy Watch Active")
      put(COL_IS_CONNECTED, 1)
      put(COL_IS_READ_ALOUD, 1)
    }
    db.insert(TABLE_PROFILE, null, defaultValues)
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    if (oldVersion < 2) {
      try {
        db.execSQL("ALTER TABLE $TABLE_PROFILE ADD COLUMN $COL_GENDER TEXT NOT NULL DEFAULT 'Male'")
      } catch (e: Exception) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROFILE")
        onCreate(db)
        return
      }
    }
    if (oldVersion < 3) {
      try {
        db.execSQL("ALTER TABLE $TABLE_PROFILE ADD COLUMN $COL_IS_READ_ALOUD INTEGER NOT NULL DEFAULT 1")
      } catch (e: Exception) {
        // Safe fallback if column already exists
      }
    }
  }

  fun getProfile(): UserProfile {
    val db = readableDatabase
    val cursor = db.query(
      TABLE_PROFILE,
      null,
      "$COL_ID = ?",
      arrayOf("1"),
      null,
      null,
      null
    )

    return cursor.use {
      if (it.moveToFirst()) {
        val name = it.getString(it.getColumnIndexOrThrow(COL_NAME))
        val tagline = it.getString(it.getColumnIndexOrThrow(COL_TAGLINE))
        val heightCm = it.getInt(it.getColumnIndexOrThrow(COL_HEIGHT_CM))
        val weightKg = it.getInt(it.getColumnIndexOrThrow(COL_WEIGHT_KG))
        val genderIndex = it.getColumnIndex(COL_GENDER)
        val gender = if (genderIndex != -1 && !it.isNull(genderIndex)) {
          it.getString(genderIndex)
        } else {
          "Male"
        }
        val targetFocus = it.getString(it.getColumnIndexOrThrow(COL_TARGET_FOCUS))
        val streakDays = it.getInt(it.getColumnIndexOrThrow(COL_STREAK_DAYS))
        val formScore = it.getInt(it.getColumnIndexOrThrow(COL_FORM_SCORE))
        val connectedDevice = it.getString(it.getColumnIndexOrThrow(COL_CONNECTED_DEVICE))
        val isConnected = it.getInt(it.getColumnIndexOrThrow(COL_IS_CONNECTED)) == 1
        val readAloudIndex = it.getColumnIndex(COL_IS_READ_ALOUD)
        val isReadAloud = if (readAloudIndex != -1 && !it.isNull(readAloudIndex)) {
          it.getInt(readAloudIndex) == 1
        } else {
          true
        }

        UserProfile(
          id = 1L,
          name = name,
          tagline = tagline,
          heightCm = heightCm,
          weightKg = weightKg,
          gender = gender,
          targetFocus = targetFocus,
          streakDays = streakDays,
          formScore = formScore,
          connectedDevice = connectedDevice,
          isDeviceConnected = isConnected,
          isReadAloudEnabled = isReadAloud,
        )
      } else {
        UserProfile()
      }
    }
  }

  fun updateProfile(profile: UserProfile): Boolean {
    val db = writableDatabase
    val values = ContentValues().apply {
      put(COL_NAME, profile.name)
      put(COL_TAGLINE, profile.tagline)
      put(COL_HEIGHT_CM, profile.heightCm)
      put(COL_WEIGHT_KG, profile.weightKg)
      put(COL_GENDER, profile.gender)
      put(COL_TARGET_FOCUS, profile.targetFocus)
      put(COL_STREAK_DAYS, profile.streakDays)
      put(COL_FORM_SCORE, profile.formScore)
      put(COL_CONNECTED_DEVICE, profile.connectedDevice)
      put(COL_IS_CONNECTED, if (profile.isDeviceConnected) 1 else 0)
      put(COL_IS_READ_ALOUD, if (profile.isReadAloudEnabled) 1 else 0)
    }

    val rowsAffected = db.update(TABLE_PROFILE, values, "$COL_ID = ?", arrayOf("1"))
    if (rowsAffected == 0) {
      values.put(COL_ID, 1L)
      db.insertWithOnConflict(TABLE_PROFILE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
    return true
  }
}
