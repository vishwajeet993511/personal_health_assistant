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

package com.google.ai.edge.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.google.ai.edge.gallery.proto.Theme

/**
 * Sukham Premium Zen-Tech Palette.
 * Matches the reference design with:
 * Light Mode:
 *   - ScreenBg: #F7EFD8 (Warm Almond / Cream Silk)
 *   - Primary:  #2E4E43 (Deep Forest Emerald Pine)
 * Dark Mode:
 *   - ScreenBg: #080908 (Deep Smoky Obsidian Black)
 *   - Primary:  #67BFA5 / #2E4E43 (Luminous Sage & Deep Forest Pine)
 */
@Immutable
data class SukhamPalette(
  val isDark: Boolean,
  val screenBg: Color,
  val cardBg: Color,
  val cardBorder: Color,
  val cardGlassBg: Color,
  val sagePrimary: Color,
  val sageContainer: Color,
  val sageDark: Color,
  val lavenderPrimary: Color,
  val lavenderSoft: Color,
  val lavenderBorder: Color,
  val streakAmber: Color,
  val streakGlow: Color,
  val tealAccent: Color,
  val tealContainer: Color,
  val titleBlack: Color,
  val bodyGray: Color,
  val mutedGray: Color,
  val chipBg: Color,
  val chipBorder: Color,
  val heroGradient: Brush,
  val navDockBg: Color,
  val navDockBorder: Color,
  val glowColor: Color,
)

val LightSukhamPalette = SukhamPalette(
  isDark = false,
  screenBg = Color(0xFFFAF7F0),           // Soft warm white, lighter than the old #F7EFD8
  cardBg = Color(0xFFFFFFFF),
  cardBorder = Color(0xFFE5DEC8),
  cardGlassBg = Color(0xEBFFFFFF),
  sagePrimary = Color(0xFF2E4E43),        // User reference color #2E4E43
  sageContainer = Color(0xFFDCE8E1),
  sageDark = Color(0xFF1D352D),
  lavenderPrimary = Color(0xFF5E548E),
  lavenderSoft = Color(0xFFEFEBF6),
  lavenderBorder = Color(0xFFD7D0E8),
  streakAmber = Color(0xFFDE5D09),
  streakGlow = Color(0xFFFAEBDB),
  tealAccent = Color(0xFF2E6B5D),
  tealContainer = Color(0xFFD9ECE6),
  titleBlack = Color(0xFF17211D),
  bodyGray = Color(0xFF53635C),
  mutedGray = Color(0xFF8A9A92),
  chipBg = Color(0xFFEFE8D3),
  chipBorder = Color(0xFFDDD2B8),
  heroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2E4E43), Color(0xFF3B6658)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
  ),
  navDockBg = Color(0xF2FFFFFF),
  navDockBorder = Color(0x332E4E43),
  glowColor = Color(0x402E4E43),
)

val DarkSukhamPalette = SukhamPalette(
  isDark = true,
  screenBg = Color(0xFF080908),           // User reference color #080908
  cardBg = Color(0xFF121714),
  cardBorder = Color(0xFF24362E),
  cardGlassBg = Color(0xD9141D19),
  sagePrimary = Color(0xFF67BFA5),        // Luminous glowing emerald sage
  sageContainer = Color(0xFF1C3A30),      // Deep forest container (#2E4E43 based)
  sageDark = Color(0xFF8CE4CA),
  lavenderPrimary = Color(0xFFA594DC),
  lavenderSoft = Color(0xFF251F38),
  lavenderBorder = Color(0xFF3C3358),
  streakAmber = Color(0xFFFF9438),
  streakGlow = Color(0xFF332014),
  tealAccent = Color(0xFF55B8A1),
  tealContainer = Color(0xFF173830),
  titleBlack = Color(0xFFF7EFD8),         // Cream Silk text for dark mode
  bodyGray = Color(0xFFA1B2A9),
  mutedGray = Color(0xFF6E7E76),
  chipBg = Color(0xFF161F1B),
  chipBorder = Color(0xFF283B32),
  heroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1A2F28), Color(0xFF0E1A16)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
  ),
  navDockBg = Color(0xDE101714),
  navDockBorder = Color(0x5567BFA5),
  glowColor = Color(0x4D67BFA5),
)

val LocalSukhamPalette = staticCompositionLocalOf { LightSukhamPalette }

@Composable
fun isSukhamDarkTheme(): Boolean {
  val themeOverride = ThemeSettings.themeOverride.value
  return (isSystemInDarkTheme() || themeOverride == Theme.THEME_DARK) &&
    themeOverride != Theme.THEME_LIGHT
}

/**
 * Global dynamic SukhamColors accessor.
 * Automatically recomposes on light/dark mode switch.
 */
object SukhamColors {
  val current: SukhamPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalSukhamPalette.current

  val isDark: Boolean @Composable @ReadOnlyComposable get() = current.isDark
  val ScreenBg: Color @Composable @ReadOnlyComposable get() = current.screenBg
  val CardBg: Color @Composable @ReadOnlyComposable get() = current.cardBg
  val CardBorder: Color @Composable @ReadOnlyComposable get() = current.cardBorder
  val CardGlassBg: Color @Composable @ReadOnlyComposable get() = current.cardGlassBg
  val SagePrimary: Color @Composable @ReadOnlyComposable get() = current.sagePrimary
  val SageContainer: Color @Composable @ReadOnlyComposable get() = current.sageContainer
  val SageDark: Color @Composable @ReadOnlyComposable get() = current.sageDark
  val LavenderPrimary: Color @Composable @ReadOnlyComposable get() = current.lavenderPrimary
  val LavenderSoft: Color @Composable @ReadOnlyComposable get() = current.lavenderSoft
  val LavenderBorder: Color @Composable @ReadOnlyComposable get() = current.lavenderBorder
  val StreakAmber: Color @Composable @ReadOnlyComposable get() = current.streakAmber
  val StreakGlow: Color @Composable @ReadOnlyComposable get() = current.streakGlow
  val TealAccent: Color @Composable @ReadOnlyComposable get() = current.tealAccent
  val TealContainer: Color @Composable @ReadOnlyComposable get() = current.tealContainer
  val TitleBlack: Color @Composable @ReadOnlyComposable get() = current.titleBlack
  val BodyGray: Color @Composable @ReadOnlyComposable get() = current.bodyGray
  val MutedGray: Color @Composable @ReadOnlyComposable get() = current.mutedGray
  val ChipBg: Color @Composable @ReadOnlyComposable get() = current.chipBg
  val ChipBorder: Color @Composable @ReadOnlyComposable get() = current.chipBorder
  val HeroGradient: Brush @Composable @ReadOnlyComposable get() = current.heroGradient
  val NavDockBg: Color @Composable @ReadOnlyComposable get() = current.navDockBg
  val NavDockBorder: Color @Composable @ReadOnlyComposable get() = current.navDockBorder
  val GlowColor: Color @Composable @ReadOnlyComposable get() = current.glowColor

  // Backward-compatible aliases for legacy callers
  val Peach = Color(0xFFFFCCBC)
  val LightBlue = Color(0xFFE1F5FE)
  val Purple: Color @Composable @ReadOnlyComposable get() = LavenderPrimary
  val PurpleDark: Color @Composable @ReadOnlyComposable get() = Color(0xFF5E35B1)
  val LavenderCard: Color @Composable @ReadOnlyComposable get() = LavenderSoft
  val LavenderDark: Color @Composable @ReadOnlyComposable get() = LavenderPrimary
  val Teal: Color @Composable @ReadOnlyComposable get() = TealAccent
  val LavenderBtn: Color @Composable @ReadOnlyComposable get() = LavenderSoft
  val LiveGreen: Color @Composable @ReadOnlyComposable get() = SagePrimary
  val AmberPrimary: Color @Composable @ReadOnlyComposable get() = StreakAmber
  val AmberSoft: Color @Composable @ReadOnlyComposable get() = StreakGlow
}
