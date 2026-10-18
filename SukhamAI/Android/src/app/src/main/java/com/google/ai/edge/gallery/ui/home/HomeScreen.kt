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


package com.google.ai.edge.gallery.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.ui.common.tos.AppTosDialog
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.theme.SukhamColors
import kotlin.math.absoluteValue

/** One page of the home carousel: a photograph, what it does, and the single action on it. */
private data class HeroCard(
  val title: String,
  val subtitle: String,
  val cta: String,
  @DrawableRes val image: Int,
  val imageAlignment: Alignment,
  /** Zooms the photo and slides it up, so the subject sits clear of the text below it. */
  val liftSubject: Boolean = false,
  val onClick: () -> Unit,
)

/**
 * Coaching and reviews in a carousel, with a dedicated programs card beneath it.
 */
@Composable
fun HomeScreen(
  tosViewModel: TosViewModel,
  onLiveClicked: () -> Unit,
  onAiClicked: () -> Unit,
  onVideoClicked: () -> Unit,
  onPhotoClicked: () -> Unit,
  onProfileClicked: () -> Unit,
  onProgramsClicked: () -> Unit,
  onBreathingClicked: () -> Unit,
  onProgressClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }

  val cards =
    listOf(
      HeroCard(
        title = "Live Coach",
        subtitle = "Real-time guidance on your posture, spoken to you as you move",
        cta = "Start a live session",
        image = R.drawable.live_exercise,
        imageAlignment = Alignment.Center,
        liftSubject = true,
        onClick = onLiveClicked,
      ),
      HeroCard(
        title = "Video Review",
        subtitle = "Upload a recording and see exactly where to refine",
        cta = "Review a video",
        image = R.drawable.video_motion_audit_hero,
        imageAlignment = Alignment.Center,
        onClick = onVideoClicked,
      ),
      HeroCard(
        title = "Photo Review",
        subtitle = "A quick check of your pose from a single photo",
        cta = "Review a photo",
        image = R.drawable.yoga_tips,
        imageAlignment = Alignment.Center,
        onClick = onPhotoClicked,
      ),
    )
  val pagerState = rememberPagerState(pageCount = { cards.size })

  if (!showTosDialog) {
    Box(modifier = modifier.fillMaxSize().background(SukhamColors.ScreenBg)) {
      Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Box(
          modifier = Modifier.fillMaxWidth().height(64.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "SUKHAM",
            color = SukhamColors.SagePrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
          )
        }

        // Leave room underneath for the floating nav so the carousel never sits behind it.
        BoxWithConstraints(
          modifier = Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().padding(bottom = 108.dp)
        ) {
          val sidePadding = 28.dp
          val dotsBlock = 44.dp
          val cardWidth = maxWidth - sidePadding * 2
          val programsBlock = 236.dp
          val cardHeight = minOf(cardWidth * 1.5f, maxHeight - dotsBlock - programsBlock)
            .coerceAtLeast(340.dp)
          Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            HorizontalPager(
              state = pagerState,
              contentPadding = PaddingValues(horizontal = sidePadding),
              pageSpacing = 14.dp,
              modifier = Modifier.fillMaxWidth().height(cardHeight),
            ) { page ->
              // Neighbouring cards sit slightly back and fade, so the centred one leads.
              val distance =
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                  .absoluteValue
                  .coerceIn(0f, 1f)
              HeroCardView(
                card = cards[page],
                animationActive = pagerState.currentPage == page,
                modifier =
                  Modifier.fillMaxSize().graphicsLayer {
                    val scale = lerp(0.92f, 1f, 1f - distance)
                    scaleX = scale
                    scaleY = scale
                    alpha = lerp(0.65f, 1f, 1f - distance)
                  },
              )
            }
            Spacer(Modifier.height(18.dp))
            PageDots(count = cards.size, current = pagerState.currentPage)
            Spacer(Modifier.height(22.dp))
            BreathingCard(onClick = onBreathingClicked, modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding))
            Spacer(Modifier.height(12.dp))
            ProgramsCard(
              onClick = onProgramsClicked,
              modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
            )
            Spacer(Modifier.height(16.dp))
          }
        }
      }

      SukhamBottomNav(
        currentTab = SukhamNavTab.HOME,
        onLiveClick = onAiClicked,
        onProfileClick = onProfileClicked,
        onProgramsClick = onProgramsClicked,
        onProgressClick = onProgressClicked,
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }

  if (showTosDialog) {
    AppTosDialog(
      onTosAccepted = {
        showTosDialog = false
        tosViewModel.acceptTos()
      }
    )
  }
}

/** A quiet, camera-free practice alongside the movement tools. */
@Composable
private fun BreathingCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(24.dp)
  Row(
    modifier = modifier.clip(shape).background(SukhamColors.CardBg)
      .border(1.dp, SukhamColors.CardBorder, shape).clickable(onClick = onClick).padding(18.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(Modifier.size(40.dp).background(SukhamColors.SagePrimary.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
      Icon(Icons.Outlined.Air, null, tint = SukhamColors.SagePrimary, modifier = Modifier.size(24.dp))
    }
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text("Breathe & Relax", color = SukhamColors.TitleBlack, fontSize = 19.sp, fontWeight = FontWeight.Medium)
      Text("A little space to slow down.", color = SukhamColors.BodyGray, fontSize = 12.sp)
      Text("1, 2, 3 or 5 minutes", color = SukhamColors.SagePrimary, fontSize = 11.sp)
    }
    Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Open breathing", tint = SukhamColors.SagePrimary, modifier = Modifier.size(20.dp))
  }
}

/** A permanent entry into programs, independent of the carousel position. */
@Composable
private fun ProgramsCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val shape = RoundedCornerShape(24.dp)
  val contentColor = if (SukhamColors.isDark) SukhamColors.TitleBlack else SukhamColors.ScreenBg
  val arrowColor = if (SukhamColors.isDark) SukhamColors.SageContainer else SukhamColors.SagePrimary
  Row(
    modifier = modifier
      .shadow(8.dp, shape, ambientColor = SukhamColors.CardBg, spotColor = SukhamColors.CardBg)
      .clip(shape)
      .background(SukhamColors.CardBg)
      .border(1.dp, SukhamColors.SagePrimary.copy(alpha = 0.22f), shape)
      .clickable(onClick = onClick)
      .padding(20.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier = Modifier.size(52.dp)
        .background(SukhamColors.SagePrimary.copy(alpha = 0.10f), RoundedCornerShape(17.dp))
        .border(1.dp, SukhamColors.CardBorder, RoundedCornerShape(17.dp)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(R.drawable.lotus_logo),
        contentDescription = null,
        tint = SukhamColors.SagePrimary,
        modifier = Modifier.size(30.dp),
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
      Text(
        "Programs",
        color = SukhamColors.TitleBlack,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
      )
      Text(
        "Discover your next practice",
        color = SukhamColors.BodyGray,
        fontSize = 12.sp,
        lineHeight = 17.sp,
      )
    }
    Icon(
      Icons.AutoMirrored.Outlined.ArrowForward,
      contentDescription = null,
      tint = SukhamColors.SagePrimary,
      modifier = Modifier.size(20.dp),
    )
  }
}

@Composable
private fun HeroCardView(card: HeroCard, modifier: Modifier = Modifier, animationActive: Boolean = false) {
  val shape = RoundedCornerShape(30.dp)
  Box(
    modifier =
      modifier
        .shadow(elevation = 14.dp, shape = shape, clip = false, ambientColor = Color(0x33000000), spotColor = Color(0x44000000))
        .clip(shape)
        .clickable(onClick = card.onClick)
  ) {
    if (card.image == R.drawable.video_motion_audit_hero) {
      AnimatedReviewImage(active = animationActive, modifier = Modifier.fillMaxSize())
    } else Image(
      painter = painterResource(card.image),
      contentDescription = card.title,
      contentScale = ContentScale.Crop,
      alignment = card.imageAlignment,
      modifier =
        Modifier.fillMaxSize().graphicsLayer {
          if (card.liftSubject) {
            // 1.2x about the centre, then up by 10% of the height: the bottom edge stays covered.
            scaleX = 1.2f
            scaleY = 1.2f
            translationY = -size.height * 0.10f
          }
        },
    )
    // A warm dark wash across the lower half; the text sits on it, so it reads on any photo.
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xE6221C17))
          )
    )
    Column(
      modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(card.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Medium)
      Spacer(Modifier.height(6.dp))
      Text(
        card.subtitle,
        color = Color.White.copy(alpha = 0.78f),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(18.dp))
      Box(
        modifier =
          Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFFE6E8D8)),
        contentAlignment = Alignment.Center,
      ) {
        Text(card.cta, color = Color(0xFF1F2A24), fontSize = 16.sp, fontWeight = FontWeight.Medium)
      }
    }
  }
}

@Composable
private fun PageDots(count: Int, current: Int) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    repeat(count) { i ->
      val selected = i == current
      val width by animateDpAsState(if (selected) 20.dp else 7.dp, tween(250), label = "dotWidth")
      Box(
        modifier =
          Modifier.height(7.dp).width(width).clip(CircleShape)
            .background(if (selected) SukhamColors.TitleBlack else SukhamColors.MutedGray.copy(alpha = 0.35f))
      )
    }
  }
}

enum class SukhamNavTab {
  HOME, PROGRAMS, AI, PROGRESS, PROFILE,
}

/** Main destinations with shared selection and accessibility semantics. */
@Composable
fun SukhamBottomNav(
  currentTab: SukhamNavTab = SukhamNavTab.HOME,
  onHomeClick: () -> Unit = {},
  onProgramsClick: () -> Unit = {},
  onProgressClick: () -> Unit = {},
  onLiveClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth().height(66.dp),
      shape = RoundedCornerShape(30.dp),
      color = SukhamColors.NavDockBg,
      shadowElevation = 14.dp,
      border = androidx.compose.foundation.BorderStroke(0.8.dp, SukhamColors.CardBorder),
    ) {
      Row(
        modifier = Modifier.fillMaxSize().selectableGroup().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        NavIcon(Icons.Outlined.Home, "Home", currentTab == SukhamNavTab.HOME, onHomeClick, Modifier.weight(1f))
        NavIcon(Icons.Outlined.Layers, "Programs", currentTab == SukhamNavTab.PROGRAMS, onProgramsClick, Modifier.weight(1f))
        AiLiveButton(selected = currentTab == SukhamNavTab.AI, onClick = onLiveClick, modifier = Modifier.weight(1f))
        NavIcon(Icons.Outlined.BarChart, "Progress", currentTab == SukhamNavTab.PROGRESS, onProgressClick, Modifier.weight(1f))
        NavIcon(Icons.Outlined.Person, "Profile", currentTab == SukhamNavTab.PROFILE, onProfileClick, Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun NavIcon(
  icon: ImageVector, description: String, selected: Boolean, onClick: () -> Unit,
  modifier: Modifier = Modifier, enabled: Boolean = true,
) {
  val tint by animateColorAsState(
    if (selected) SukhamColors.SagePrimary else SukhamColors.MutedGray,
    tween(200), label = "navTint",
  )
  Column(
    modifier = modifier.height(60.dp).clip(RoundedCornerShape(18.dp))
      .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = { if (!selected) onClick() })
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(Modifier.height(44.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
      if (selected) Box(Modifier.size(38.dp).background(SukhamColors.SageContainer, CircleShape))
      Icon(icon, description, tint = tint, modifier = Modifier.size(23.dp))
    }
  }
}

/** The centre "Live coach" button: an AI-branded orb with a slowly rotating gradient halo. */
@Composable
private fun AiLiveButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "aiOrb")
  val angle by
    transition.animateFloat(0f, 360f, infiniteRepeatable(tween(4500, easing = LinearEasing)), label = "angle")
  val glow by
    transition.animateFloat(
      0.35f, 0.75f,
      infiniteRepeatable(tween(1800), RepeatMode.Reverse),
      label = "glow",
    )
  val violet = Color(0xFF8E6CF0)
  val teal = Color(0xFF2EC4B6)
  val pink = Color(0xFFE56BB0)
  Column(
    modifier =
      modifier.height(60.dp).clip(RoundedCornerShape(18.dp))
        .selectable(selected = selected, role = Role.Tab, onClick = { if (!selected) onClick() })
        .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.height(44.dp).fillMaxWidth()) {
      // Soft pulsing aura.
      Box(
        Modifier.size(44.dp)
          .graphicsLayer { alpha = glow }
          .background(Brush.radialGradient(listOf(violet.copy(alpha = 0.55f), Color.Transparent)), CircleShape)
      )
      // Rotating sweep ring.
      Box(
        Modifier.size(42.dp)
          .graphicsLayer { rotationZ = angle }
          .background(Brush.sweepGradient(listOf(violet, teal, pink, violet)), CircleShape)
      )
      // Dark core with the sparkle.
      Box(
        Modifier.size(37.dp).background(Color(0xFF14111F), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Outlined.AutoAwesome,
          contentDescription = "AI live coach",
          tint = Color.White,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}
