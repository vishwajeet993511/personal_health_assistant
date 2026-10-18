package com.google.ai.edge.gallery.ui.llmchat

import com.google.ai.edge.gallery.ui.profile.model.UserProfile

/** Seeded demonstration values must never be treated as known personal facts by chat. */
internal fun editedWellnessProfileFields(profile: UserProfile): Map<String, Any> {
  val defaults = UserProfile()
  return buildMap {
    if (profile.name != defaults.name) put("name", profile.name.take(80))
    if (profile.targetFocus != defaults.targetFocus) put("focus", profile.targetFocus.take(160))
    if (profile.heightCm != defaults.heightCm) put("height_cm", profile.heightCm)
    if (profile.weightKg != defaults.weightKg) put("weight_kg", profile.weightKg)
    if (profile.gender != defaults.gender) put("gender", profile.gender.take(40))
  }
}
