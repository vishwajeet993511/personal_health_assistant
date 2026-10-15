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

package com.google.ai.edge.gallery.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.ui.profile.data.UserProfileRepository
import com.google.ai.edge.gallery.ui.profile.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for Profile Screen. */
data class ProfileUiState(
  val profile: UserProfile = UserProfile(),
  val isEditing: Boolean = false,
  val currentTheme: Theme = Theme.THEME_AUTO,
  val isLoading: Boolean = false,
  val userFeedbackMessage: String? = null,
)

/**
 * MVVM ViewModel for Profile Screen.
 * Manages user profile state, SQLite database updates, and theme switches.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
  private val profileRepository: UserProfileRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUiState())
  val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

  init {
    loadProfile()
  }

  fun loadProfile() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val profile = profileRepository.loadProfile()
      val theme = profileRepository.readTheme()
      com.google.ai.edge.gallery.ui.theme.ThemeSettings.themeOverride.value = theme
      _uiState.update {
        it.copy(
          profile = profile,
          currentTheme = theme,
          isLoading = false
        )
      }
    }
  }

  fun toggleEditMode() {
    _uiState.update { it.copy(isEditing = !it.isEditing) }
  }

  fun saveProfile(
    name: String,
    tagline: String,
    heightCm: Int,
    weightKg: Int,
    gender: String,
    targetFocus: String,
  ) {
    viewModelScope.launch {
      val validGender = when (gender.trim().lowercase()) {
        "female" -> "Female"
        "other" -> "Other"
        else -> "Male"
      }
      val updatedProfile = _uiState.value.profile.copy(
        name = name.trim().ifEmpty { "Alex Morgan" },
        tagline = tagline.trim().ifEmpty { "Yoga & Strength Enthusiast" },
        heightCm = heightCm.coerceIn(100, 250),
        weightKg = weightKg.coerceIn(30, 300),
        gender = validGender,
        targetFocus = targetFocus.trim().ifEmpty { "Posture & Alignment" },
      )
      val saved = profileRepository.saveProfile(updatedProfile)
      _uiState.update {
        it.copy(
          profile = updatedProfile,
          isEditing = false,
          userFeedbackMessage = if (saved) "Profile saved to local database" else "Failed to save profile"
        )
      }
    }
  }

  fun updateTheme(theme: Theme) {
    com.google.ai.edge.gallery.ui.theme.ThemeSettings.themeOverride.value = theme
    profileRepository.saveTheme(theme)
    _uiState.update { it.copy(currentTheme = theme, userFeedbackMessage = "Theme updated") }
  }

  fun toggleReadAloud(enabled: Boolean) {
    viewModelScope.launch {
      val success = profileRepository.setReadAloudEnabled(enabled)
      if (success) {
        _uiState.update {
          it.copy(
            profile = it.profile.copy(isReadAloudEnabled = enabled),
            userFeedbackMessage = if (enabled) "Voice Read Aloud turned ON" else "Voice Read Aloud turned OFF"
          )
        }
      }
    }
  }

  fun clearFeedbackMessage() {
    _uiState.update { it.copy(userFeedbackMessage = null) }
  }
}
