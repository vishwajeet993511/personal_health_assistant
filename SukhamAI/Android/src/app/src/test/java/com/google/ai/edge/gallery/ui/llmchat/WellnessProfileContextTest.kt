package com.google.ai.edge.gallery.ui.llmchat

import com.google.ai.edge.gallery.ui.profile.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WellnessProfileContextTest {
  @Test fun demonstrationProfileDoesNotBecomePersonalFacts() {
    assertTrue(editedWellnessProfileFields(UserProfile()).isEmpty())
  }

  @Test fun personalizationIncludesOnlyEditedFields() {
    val profile = UserProfile(name = "Priya", targetFocus = "Balance", heightCm = 165)
    assertEquals(mapOf("name" to "Priya", "focus" to "Balance", "height_cm" to 165), editedWellnessProfileFields(profile))
  }

  @Test fun scoresAndDeviceFieldsAreNeverSentAsPersonalization() {
    val profile = UserProfile(streakDays = 100, formScore = 99, connectedDevice = "Private device", isReadAloudEnabled = false)
    assertTrue(editedWellnessProfileFields(profile).isEmpty())
  }
}
