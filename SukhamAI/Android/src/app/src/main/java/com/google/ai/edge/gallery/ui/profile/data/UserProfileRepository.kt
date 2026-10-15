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

import android.content.Context
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.ui.profile.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing user profile persistence in the local SQLite database
 * and theme preferences via DataStoreRepository.
 */
@Singleton
class UserProfileRepository @Inject constructor(
  @ApplicationContext private val context: Context,
  private val dataStoreRepository: DataStoreRepository,
) {
  private val dbHelper = UserProfileDatabaseHelper(context)
  private val _profileFlow = MutableStateFlow(UserProfile())
  val profileFlow: StateFlow<UserProfile> = _profileFlow.asStateFlow()

  suspend fun loadProfile(): UserProfile = withContext(Dispatchers.IO) {
    val profile = dbHelper.getProfile()
    _profileFlow.value = profile
    profile
  }

  suspend fun saveProfile(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
    val success = dbHelper.updateProfile(profile)
    if (success) {
      _profileFlow.value = profile
    }
    success
  }

  suspend fun setReadAloudEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
    val current = dbHelper.getProfile()
    val updated = current.copy(isReadAloudEnabled = enabled)
    val success = dbHelper.updateProfile(updated)
    if (success) {
      _profileFlow.value = updated
    }
    success
  }

  fun readTheme(): Theme {
    return dataStoreRepository.readTheme()
  }

  fun saveTheme(theme: Theme) {
    dataStoreRepository.saveTheme(theme)
  }
}
