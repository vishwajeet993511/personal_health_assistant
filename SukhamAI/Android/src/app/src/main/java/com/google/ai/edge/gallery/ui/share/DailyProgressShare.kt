/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.google.ai.edge.gallery.ui.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.ai.edge.gallery.ui.wearables.DailyProgress
import com.google.ai.edge.gallery.ui.wearables.WearableSnapshot

private const val WHATSAPP_PACKAGE = "com.whatsapp"

/**
 * Builds a WhatsApp-friendly daily progress message from the latest wearable snapshot and
 * launches the share intent. Falls back to a generic chooser if WhatsApp isn't installed.
 */
fun shareDailyProgressToWhatsApp(context: Context, snapshot: WearableSnapshot) {
  val message = buildDailyProgressMessage(snapshot)

  val whatsappIntent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, message)
      setPackage(WHATSAPP_PACKAGE)
    }

  try {
    context.startActivity(whatsappIntent)
  } catch (_: ActivityNotFoundException) {
    // WhatsApp not installed — fall back to the system share sheet so the user can still send
    // the update through SMS, email, etc.
    val fallback =
      Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, message)
        },
        "Share progress via",
      )
    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
      context.startActivity(fallback)
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(context, "No app available to share progress.", Toast.LENGTH_SHORT).show()
    }
  }
}

/** Generates the message body. Exposed for previewing/testing. */
fun buildDailyProgressMessage(snapshot: WearableSnapshot): String {
  val p: DailyProgress = snapshot.dailyProgress
  return buildString {
    appendLine("🌿 *Sukham Daily Check-in* — ${snapshot.dayOfWeek}")
    appendLine()
    appendLine("👟 Steps: ${"%,d".format(p.steps)}")
    appendLine("🔥 Calories burnt: ${"%,d".format(p.caloriesBurnt)} kcal")
    appendLine("🍽️ Calories eaten: ${"%,d".format(p.caloriesEaten)} kcal")
    appendLine("😴 Sleep: ${"%.1f".format(p.sleepHours)} h (${p.sleepWakeups} wake-ups)")
    appendLine("💧 Water: ${"%.1f".format(p.waterLiters)} L")
    appendLine("❤️ Heart rate: ${p.heartRateBpm} bpm")
    appendLine()
    appendLine("Health score: ${"%.0f".format(p.healthScore)}/100")
    appendLine()
    append("Tracking my journey with Sukham AI 🌿")
  }
}
