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

package com.google.ai.edge.gallery.ui.profile.model

/** Domain model representing the user's health profile in Sukham AI. */
data class UserProfile(
  val id: Long = 1L,
  val name: String = "Alex Morgan",
  val tagline: String = "Yoga & Strength Enthusiast",
  val heightCm: Int = 178,
  val weightKg: Int = 72,
  val gender: String = "Male",
  val targetFocus: String = "Posture & Alignment",
  val streakDays: Int = 6,
  val formScore: Int = 84,
  val connectedDevice: String = "Galaxy Watch Active",
  val isDeviceConnected: Boolean = true,
  val isReadAloudEnabled: Boolean = true,
)
