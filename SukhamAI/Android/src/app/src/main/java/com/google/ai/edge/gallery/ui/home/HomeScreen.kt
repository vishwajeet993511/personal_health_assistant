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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mms
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.ai.edge.gallery.GalleryTopAppBar
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.AppBarAction
import com.google.ai.edge.gallery.data.AppBarActionType
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.CategoryInfo
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.RevealingText
import com.google.ai.edge.gallery.ui.common.SwipingText
import com.google.ai.edge.gallery.ui.common.TaskIcon
import com.google.ai.edge.gallery.ui.common.buildTrackableUrlAnnotatedString
import com.google.ai.edge.gallery.ui.common.rememberDelayedAnimationProgress
import com.google.ai.edge.gallery.ui.common.tos.AppTosDialog
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.share.shareDailyProgressToWhatsApp
import com.google.ai.edge.gallery.ui.theme.SukhamColors
import com.google.ai.edge.gallery.ui.theme.customColors
import com.google.ai.edge.gallery.ui.theme.homePageTitleStyle
import com.google.ai.edge.gallery.ui.wearables.loadLatestWearableSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "AGHomeScreen"

private const val TASK_COUNT_ANIMATION_DURATION = 250
private const val ANIMATION_INIT_DELAY = 0L
private const val TOP_APP_BAR_ANIMATION_DURATION = 600
private const val TITLE_FIRST_LINE_ANIMATION_DURATION = 600
private const val TITLE_SECOND_LINE_ANIMATION_DURATION = 600
private const val TITLE_SECOND_LINE_ANIMATION_DURATION2 = 800
private const val TITLE_SECOND_LINE_ANIMATION_START =
  ANIMATION_INIT_DELAY + (TITLE_FIRST_LINE_ANIMATION_DURATION * 0.5).toInt()
private const val TASK_LIST_ANIMATION_START = TITLE_SECOND_LINE_ANIMATION_START + 110
private const val TASK_CARD_ANIMATION_DELAY_OFFSET = 100
private const val TASK_CARD_ANIMATION_DURATION = 600
private const val CONTENT_COMPOSABLES_ANIMATION_DURATION = 1200
private const val CONTENT_COMPOSABLES_OFFSET_Y = 16

/** Navigation destination data */
private object HomeScreenDestination {
  @StringRes val titleRes = R.string.app_name
}

private val PREDEFINED_CATEGORY_ORDER =
  listOf(Category.HEALTH.id, Category.LLM.id, Category.EXPERIMENTAL.id)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modelManagerViewModel: ModelManagerViewModel,
  tosViewModel: TosViewModel,
  navigateToTaskScreen: (Task) -> Unit,
  navigateToModelScreen: (Task, Model) -> Unit,
  onModelsClicked: () -> Unit,
  onLiveExerciseClicked: () -> Unit = {},
  onVideoAnalysisClicked: () -> Unit = {},
  onYogaClicked: () -> Unit = {},
  onWearablesClicked: () -> Unit = {},
  onInsightsClicked: () -> Unit = {},
  onProgramsClicked: () -> Unit = {},
  onProfileClicked: () -> Unit = {},
  enableAnimation: Boolean,
  modifier: Modifier = Modifier,
  gm4: Boolean = false,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  val dbHelper = remember(context) { com.google.ai.edge.gallery.ui.profile.data.UserProfileDatabaseHelper(context) }
  var profileName by remember { mutableStateOf("Alex") }
  LaunchedEffect(Unit) {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
      val fullName = dbHelper.getProfile().name
      profileName = fullName.split(" ").firstOrNull() ?: "Alex"
    }
  }

  // SUKHAM AI now standardises on the multimodal "Ask Image" chat — same 2B Gemma but with
  // the vision tower enabled. resolveModelForTask refuses to escalate to the 4B variant so we
  // stay within the S20+ memory envelope while still supporting image inputs.
  val imageChatTask = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_ASK_IMAGE)

  fun openChatForTask(task: Task?) {
    val resolvedTask = task ?: return
    val model = modelManagerViewModel.resolveModelForTask(resolvedTask)
    if (model != null) {
      modelManagerViewModel.selectModel(model)
      if (!modelManagerViewModel.isModelDownloaded(model)) {
        modelManagerViewModel.downloadModel(resolvedTask, model)
      }
      navigateToModelScreen(resolvedTask, model)
    } else {
      navigateToTaskScreen(resolvedTask)
    }
  }

  // The header card, bottom-nav central button, and wellness tiles all open the image chat.
  fun openImageChat() = openChatForTask(imageChatTask)
  // Scan Documents: resolve at click time so we don't capture a null task before allowlist loads.
  fun openScanDocsChat() {
    val task =
      modelManagerViewModel.getTaskById(BuiltInTaskId.SCAN_DOCS)
        ?: modelManagerViewModel.getCustomTaskByTaskId(BuiltInTaskId.SCAN_DOCS)?.task
    if (task != null) {
      openChatForTask(task)
    } else {
      // Fallback: image chat still supports photo/PDF upload if Scan Docs isn't registered yet.
      openImageChat()
    }
  }

  // Loads today's snapshot from assets and fires the WhatsApp share intent.
  fun onShareDailyProgress() {
    scope.launch {
      val snapshot = loadLatestWearableSnapshot(context)
      if (snapshot == null) {
        android.widget.Toast.makeText(
            context,
            "No wearable records found yet.",
            android.widget.Toast.LENGTH_SHORT,
          )
          .show()
        return@launch
      }
      shareDailyProgressToWhatsApp(context, snapshot)
    }
  }

  // Show home screen content when TOS has been accepted.
  if (!showTosDialog) {
      val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

      BackHandler(drawerState.isOpen) { scope.launch { drawerState.close() } }

      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalDrawerSheet {
            Column(modifier = Modifier.padding(16.dp)) {
              Row(modifier = Modifier.fillMaxWidth()) {
                SquareDrawerItem(
                  label = stringResource(R.string.drawer_settings_label),
                  description = stringResource(R.string.drawer_settings_description),
                  icon = Icons.Rounded.Settings,
                  onClick = {
                    showSettingsDialog = true
                    scope.launch { drawerState.close() }
                  },
                  modifier = Modifier.weight(1f),
                  iconBrush =
                    linearGradient(
                      colors =
                        listOf(
                          MaterialTheme.customColors.taskBgGradientColors[2][0],
                          MaterialTheme.customColors.taskBgGradientColors[2][1],
                        )
                    ),
                )
                Spacer(modifier = Modifier.width(16.dp))
                SquareDrawerItem(
                  label = stringResource(R.string.drawer_models_label),
                  description = stringResource(R.string.drawer_models_description),
                  icon = Icons.AutoMirrored.Rounded.ListAlt,
                  onClick = {
                    scope.launch { drawerState.close() }
                    scope.launch {
                      delay(50)
                      onModelsClicked()
                    }
                  },
                  modifier = Modifier.weight(1f),
                  iconBrush =
                    linearGradient(
                      colors =
                        listOf(
                          MaterialTheme.customColors.taskBgGradientColors[1][0],
                          MaterialTheme.customColors.taskBgGradientColors[1][1],
                        )
                    ),
                )
              }
            }
          }
        },
        gesturesEnabled = drawerState.isOpen,
      ) {
        Scaffold(
          containerColor = SukhamColors.ScreenBg,
          topBar = {
            SukhamHeader(
              name = profileName,
              onAvatarClick = onProfileClicked,
              onMenuClick = { scope.launch { drawerState.open() } },
            )
          },
          bottomBar = {
            SukhamBottomNav(
              currentTab = SukhamNavTab.HOME,
              onCentralClick = { openImageChat() },
              onInsightsClick = onInsightsClicked,
              onProgramsClick = onProgramsClicked,
              onProfileClick = onProfileClicked,
            )
          }
        ) { innerPadding ->
          Column(
            modifier =
              Modifier.fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // 1. Personal Progress Dashboard (Concentric Gauge, 7-Day Consistency, Spine Align)
            PersonalProgressDashboard(
              formScore = 84,
              spineAlignImprovement = 6,
            )

            // 1.5 Smart AI Recommendations Carousel
            SmartRecommendationsCarousel(
              onViewAllInsights = onInsightsClicked,
              onStartSpineReset = onYogaClicked,
              onStartBalanceAudit = onProgramsClicked,
              onStartSleepWinddown = onProgramsClicked,
              onStartCoreMobility = onProgramsClicked,
            )

            // 2. Hero Action 1: Live AI Motion Coach (Gym, Dance, Yoga)
            LiveAiCoachHeroCard(
              onClick = { onLiveExerciseClicked() }
            )

            // 3. Hero Action 2: Video & Photo Form Analysis (Async Audit)
            VideoAnalysisHeroCard(
              onClick = { onVideoAnalysisClicked() }
            )

            // 4. Multimodal Chat Card (Ask Image / Voice / Text)
            if (imageChatTask != null) {
              SukhamMainChatCard(onClick = { openImageChat() })
            }

            // 5. Secondary Quick-Action Row: Wearables & Documents Analysis
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              SukhamGridCard(
                title = "Wearables",
                subtitle = "Biometrics & sleep",
                imageRes = R.drawable.wearables_biometric_art,
                modifier = Modifier.weight(1f),
                actionText = "Insights",
                actionColor = SukhamColors.SageContainer,
                actionTextColor = SukhamColors.SageDark,
                onClick = { onWearablesClicked() }
              )
              SukhamGridCard(
                title = "Health Reports",
                subtitle = "Lab tests & vitals",
                imageRes = R.drawable.health_reports_art,
                modifier = Modifier.weight(1f),
                actionText = "Upload",
                actionColor = SukhamColors.LavenderSoft,
                actionTextColor = SukhamColors.LavenderPrimary,
                onClick = { openScanDocsChat() }
              )
            }

            // 6. Share Progress Card
            SukhamFooterCard(onClick = { onShareDailyProgress() })

            Spacer(modifier = Modifier.height(40.dp))
          }
        }
    }
  }

  // Show TOS dialog for users to accept.
  if (showTosDialog) {
    AppTosDialog(
      onTosAccepted = {
        showTosDialog = false
        tosViewModel.acceptTos()
      }
    )
  }

  // Settings dialog.
  if (showSettingsDialog) {
    SettingsDialog(
      curThemeOverride = modelManagerViewModel.readThemeOverride(),
      modelManagerViewModel = modelManagerViewModel,
      onDismissed = { showSettingsDialog = false },
    )
  }
}

@Composable
fun SukhamHeader(
  name: String = "Alex",
  streakDays: Int = 6,
  onMenuClick: () -> Unit = {},
  onAvatarClick: () -> Unit = {},
  onNotificationClick: () -> Unit = {},
) {
  val streakTransition = rememberInfiniteTransition(label = "streakGlow")
  val streakScale by streakTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "streakScale"
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Row: Menu Button on left, Notification & Glowing Avatar on right
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Menu drawer button in frosted pill
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(SukhamColors.ChipBg)
          .border(1.dp, SukhamColors.CardBorder, CircleShape)
          .clickable(onClick = onMenuClick),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Menu,
          contentDescription = "Menu",
          tint = SukhamColors.TitleBlack,
          modifier = Modifier.size(20.dp)
        )
      }

      // Right: Streak pill + Notification + Profile avatar with glowing border
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Streak Badge
        Surface(
          color = SukhamColors.StreakGlow,
          shape = RoundedCornerShape(percent = 50),
          border = BorderStroke(0.8.dp, SukhamColors.StreakAmber.copy(alpha = 0.35f)),
          modifier = Modifier.graphicsLayer {
            scaleX = streakScale
            scaleY = streakScale
          }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "$streakDays-Day Streak 🔥",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamColors.TitleBlack,
                fontSize = 11.5.sp
              )
            )
          }
        }

        // Notification button in frosted pill
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SukhamColors.ChipBg)
            .border(1.dp, SukhamColors.CardBorder, CircleShape)
            .clickable(onClick = onNotificationClick),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            tint = SukhamColors.TitleBlack,
            modifier = Modifier.size(20.dp)
          )
        }

        // Avatar with subtle glowing sage ring
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .border(1.8.dp, SukhamColors.SagePrimary, CircleShape)
            .clickable(onClick = onAvatarClick),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(R.drawable.user_avatar_alex),
            contentDescription = "Profile",
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape),
            contentScale = ContentScale.Crop
          )
        }
      }
    }

    // Elegant greeting headline matching Screenshot 1
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 2.dp)
    ) {
      Text(
        text = "Hello, $name!",
        style = MaterialTheme.typography.bodyMedium.copy(
          color = SukhamColors.BodyGray,
          fontSize = 14.5.sp,
          fontWeight = FontWeight.Medium
        )
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "—What practice are you excited to begin today?",
        style = MaterialTheme.typography.headlineSmall.copy(
          color = SukhamColors.TitleBlack,
          fontSize = 20.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = (-0.3).sp,
          lineHeight = 26.sp
        )
      )
    }
  }
}

@Composable
fun ConcentricFormScoreGauge(
  score: Int = 84,
  modifier: Modifier = Modifier,
  sizeDp: Dp = 100.dp,
) {
  val trackColor = SukhamColors.CardBorder
  var startAnim by remember { mutableStateOf(false) }
  val progress by animateFloatAsState(
    targetValue = if (startAnim) score / 100f else 0f,
    animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
    label = "gaugeSweep"
  )
  LaunchedEffect(Unit) {
    startAnim = true
  }

  Box(
    modifier = modifier.size(sizeDp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
      val strokeOuter = 7.dp.toPx()
      val strokeInner = 5.dp.toPx()

      // Outer track
      drawArc(
        color = trackColor,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
      )

      // Outer gradient: Green -> Lime -> Amber -> Coral
      val outerGradient = Brush.sweepGradient(
        0.0f to Color(0xFF43A047),
        0.28f to Color(0xFF8BC34A),
        0.58f to Color(0xFFFFB300),
        0.82f to Color(0xFFFF7043),
        1.0f to Color(0xFFE53935)
      )
      val outerSweep = 270f * progress
      drawArc(
        brush = outerGradient,
        startAngle = 135f,
        sweepAngle = outerSweep,
        useCenter = false,
        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
      )

      // Outer milestone indicators
      val radiusOuter = (size.minDimension - strokeOuter) / 2f
      val center = Offset(size.width / 2f, size.height / 2f)

      // Indicator 1: ring handle near start
      val rad1 = Math.toRadians(150.0)
      val dot1Center = Offset(
        (center.x + radiusOuter * Math.cos(rad1)).toFloat(),
        (center.y + radiusOuter * Math.sin(rad1)).toFloat()
      )
      drawCircle(
        color = Color.White,
        radius = 3.5.dp.toPx(),
        center = dot1Center
      )
      drawCircle(
        color = Color(0xFF43A047),
        radius = 2.dp.toPx(),
        center = dot1Center
      )

      // Indicator 2: solid coral dot near end of arc
      val rad2 = Math.toRadians((135.0 + outerSweep.toDouble()))
      val dot2Center = Offset(
        (center.x + radiusOuter * Math.cos(rad2)).toFloat(),
        (center.y + radiusOuter * Math.sin(rad2)).toFloat()
      )
      drawCircle(
        color = Color(0xFFFF5722),
        radius = 3.dp.toPx(),
        center = dot2Center
      )

      // Inner track
      val innerPadding = strokeOuter + 4.dp.toPx()
      val innerSize = Size(size.width - innerPadding * 2, size.height - innerPadding * 2)
      val innerTopLeft = Offset(innerPadding, innerPadding)

      drawArc(
        color = trackColor,
        startAngle = 160f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = innerTopLeft,
        size = innerSize,
        style = Stroke(width = strokeInner, cap = StrokeCap.Round)
      )

      // Inner gradient: Magenta -> Purple -> Cyan -> Amber
      val innerGradient = Brush.sweepGradient(
        0.0f to Color(0xFFAB47BC),
        0.35f to Color(0xFF673AB7),
        0.65f to Color(0xFF00ACC1),
        1.0f to Color(0xFFFFB300)
      )
      val innerSweep = 220f * (progress * 0.9f).coerceAtLeast(0f)
      drawArc(
        brush = innerGradient,
        startAngle = 160f,
        sweepAngle = innerSweep,
        useCenter = false,
        topLeft = innerTopLeft,
        size = innerSize,
        style = Stroke(width = strokeInner, cap = StrokeCap.Round)
      )
    }

    // Centered Form Score text
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.ExtraBold,
          color = SukhamColors.TitleBlack,
          fontSize = 18.sp
        )
      )
      Text(
        text = "Form Score",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Medium,
          color = SukhamColors.BodyGray,
          fontSize = 9.sp
        )
      )
    }
  }
}

@Composable
fun DayConsistencyCapsule(
  day: String,
  isCompleted: Boolean,
  modifier: Modifier = Modifier,
  heightDp: Dp = 52.dp,
) {
  val capsuleBg = SukhamColors.ChipBg
  val checkGreen = SukhamColors.SagePrimary

  Column(
    modifier = modifier
      .widthIn(min = 16.dp, max = 22.dp)
      .height(heightDp)
      .background(capsuleBg, RoundedCornerShape(percent = 50))
      .border(0.8.dp, SukhamColors.ChipBorder, RoundedCornerShape(percent = 50))
      .padding(vertical = 3.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = day,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        color = SukhamColors.TitleBlack,
        fontSize = 10.sp
      ),
      modifier = Modifier.padding(top = 1.dp)
    )

    if (isCompleted) {
      Box(
        modifier = Modifier
          .size(16.dp)
          .background(checkGreen, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Check,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(10.dp)
        )
      }
    } else {
      Spacer(modifier = Modifier.size(16.dp))
    }
  }
}

@Composable
fun PersonalProgressDashboard(
  modifier: Modifier = Modifier,
  formScore: Int = 84,
  spineAlignImprovement: Int = 6,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamColors.CardBg),
    border = BorderStroke(1.dp, SukhamColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      val isCompact = maxWidth < 360.dp
      val gaugeSize = if (isCompact) 86.dp else 98.dp

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left Column: Concentric Arc Gauge
          ConcentricFormScoreGauge(
            score = formScore,
            sizeDp = gaugeSize,
          )

          Spacer(modifier = Modifier.width(6.dp))

          // Right Column: 7-Day Consistency (flexibly adapts to width)
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "7-Day Consistency",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamColors.TitleBlack,
                fontSize = if (isCompact) 13.sp else 14.sp
              ),
              maxLines = 1
            )

            // 7 capsules: M, T, W, T, F, S, S (each with weight(1f) to guarantee screen fit)
            val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
            val completedDays = listOf(true, true, true, true, true, false, false)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              weekDays.forEachIndexed { index, day ->
                Box(
                  modifier = Modifier.weight(1f),
                  contentAlignment = Alignment.Center
                ) {
                  DayConsistencyCapsule(
                    day = day,
                    isCompleted = completedDays.getOrElse(index) { false },
                    heightDp = if (isCompact) 48.dp else 52.dp
                  )
                }
              }
            }
          }
        }

        // Bottom Row: Spine Align improvement indicator
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 2.dp, top = 1.dp)
        ) {
          Text(
            buildAnnotatedString {
              withStyle(SpanStyle(color = SukhamColors.TitleBlack, fontWeight = FontWeight.Medium, fontSize = 12.sp)) {
                append("Spine Align: ")
              }
              withStyle(SpanStyle(color = SukhamColors.SagePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)) {
                append("+$spineAlignImprovement%")
              }
              withStyle(SpanStyle(color = SukhamColors.BodyGray, fontWeight = FontWeight.Normal, fontSize = 12.sp)) {
                append(" this week")
              }
            }
          )
        }
      }
    }
  }
}

@Composable
fun LiveAiCoachHeroCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val liveTransition = rememberInfiniteTransition(label = "livePulse")
  val liveDotAlpha by liveTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(750, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "liveDotAlpha"
  )
  val liveDotScale by liveTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(750, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "liveDotScale"
  )
  val playPulse by liveTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "playPulse"
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamColors.CardGlassBg),
    border = BorderStroke(1.2.dp, SukhamColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Top badges: LIVE COACH · 30 min on left, Bookmark / Save on right
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = SukhamColors.SageContainer,
          shape = RoundedCornerShape(percent = 50),
          border = BorderStroke(0.8.dp, SukhamColors.SagePrimary.copy(alpha = 0.3f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .graphicsLayer {
                  alpha = liveDotAlpha
                  scaleX = liveDotScale
                  scaleY = liveDotScale
                }
                .background(SukhamColors.SagePrimary, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "((•)) LIVE COACH · 30 MIN",
              style = MaterialTheme.typography.labelSmall.copy(
                color = SukhamColors.SageDark,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.5.sp
              )
            )
          }
        }

        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(SukhamColors.ChipBg)
            .border(1.dp, SukhamColors.CardBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = "Active",
            tint = SukhamColors.SagePrimary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Middle: Main Title, Description & Visual
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Yoga & Kinetic Motion",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamColors.TitleBlack,
              fontSize = 18.5.sp
            )
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "Real-time AI posture tracking, kinetic balance & spinal alignment coaching",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamColors.BodyGray,
              fontSize = 11.5.sp,
              lineHeight = 15.5.sp
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            DisciplineChip(icon = Icons.Outlined.FitnessCenter, label = "Gym")
            DisciplineChip(icon = Icons.Outlined.MusicNote, label = "Dance")
            DisciplineChip(icon = Icons.Outlined.SelfImprovement, label = "Yoga")
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
          modifier = Modifier
            .size(90.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.2.dp, SukhamColors.CardBorder, RoundedCornerShape(18.dp)),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(R.drawable.live_ai_coach_hero),
            contentDescription = "Live AI Coach",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }
      }

      // Bottom Docked Action Pill (matching reference screenshot bottom pill)
      Surface(
        color = SukhamColors.SageContainer,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SukhamColors.SagePrimary.copy(alpha = 0.25f)),
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onClick)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Experiencing Genuine Flow",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamColors.SageDark,
                fontSize = 13.sp
              )
            )
            Text(
              text = "Tap to begin real-time session",
              style = MaterialTheme.typography.labelSmall.copy(
                color = SukhamColors.BodyGray,
                fontSize = 10.sp
              )
            )
          }

          Box(
            modifier = Modifier
              .size(34.dp)
              .graphicsLayer {
                scaleX = playPulse
                scaleY = playPulse
              }
              .clip(CircleShape)
              .background(SukhamColors.SagePrimary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.PlayCircleOutline,
              contentDescription = "Start",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun VideoAnalysisHeroCard(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamColors.CardBg),
    border = BorderStroke(1.dp, SukhamColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .padding(13.dp)
    ) {
      // Image resizes dynamically based on device screen width
      val imageSize = (maxWidth * 0.28f).coerceIn(76.dp, 106.dp)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            color = SukhamColors.LavenderSoft,
            shape = RoundedCornerShape(6.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.Videocam,
                contentDescription = null,
                tint = SukhamColors.LavenderPrimary,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "ASYNC AUDIT",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = SukhamColors.LavenderPrimary,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 10.sp
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(5.dp))

          Text(
            text = "Upload & Analyze Video",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamColors.TitleBlack,
              fontSize = 17.sp
            )
          )

          Text(
            text = "Frame-by-frame joint angles & form critique",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamColors.BodyGray,
              fontSize = 11.5.sp
            ),
            maxLines = 1,
            modifier = Modifier.padding(top = 1.dp, bottom = 8.dp)
          )

          Button(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SukhamColors.LavenderPrimary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.Videocam,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "Upload & Audit Video",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 11.5.sp
              )
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Image(
          painter = painterResource(R.drawable.video_motion_audit_hero),
          contentDescription = "Video Motion Audit",
          modifier = Modifier
            .size(imageSize)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SukhamColors.CardBorder, RoundedCornerShape(16.dp)),
          contentScale = ContentScale.Crop
        )
      }
    }
  }
}

@Composable
private fun DisciplineChip(icon: ImageVector, label: String) {
  Surface(
    color = SukhamColors.ChipBg,
    shape = RoundedCornerShape(6.dp),
    border = BorderStroke(1.dp, SukhamColors.ChipBorder)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = SukhamColors.SagePrimary,
        modifier = Modifier.size(11.dp)
      )
      Spacer(modifier = Modifier.width(3.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          color = SukhamColors.TitleBlack,
          fontWeight = FontWeight.SemiBold,
          fontSize = 10.sp
        )
      )
    }
  }
}

@Composable
fun SukhamMainChatCard(onClick: () -> Unit) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    color = SukhamColors.LavenderSoft,
    border = BorderStroke(1.dp, SukhamColors.LavenderBorder),
    shadowElevation = 0.5.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: 3D AI Lotus Orb thumbnail + Prompt Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .border(1.5.dp, SukhamColors.LavenderBorder, CircleShape)
        ) {
          Image(
            painter = painterResource(R.drawable.sukham_ai_chat_orb),
            contentDescription = "Sukham AI Assistant",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }

        Spacer(modifier = Modifier.width(11.dp))

        Column(modifier = Modifier.padding(end = 4.dp)) {
          Text(
            text = "Ask Sukham AI ✨",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamColors.TitleBlack,
              fontSize = 15.sp
            ),
            maxLines = 1,
            softWrap = false
          )
          Text(
            text = "Form critique, nutrition & lab reports",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamColors.BodyGray,
              fontSize = 11.5.sp
            ),
            maxLines = 1
          )
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Right: Slim Prompt Action Pill
      Surface(
        color = SukhamColors.LavenderPrimary,
        shape = RoundedCornerShape(percent = 50),
        modifier = Modifier.clickable(onClick = onClick)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "Chat",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 11.5.sp
            )
          )
        }
      }
    }
  }
}

@Composable
fun SukhamGridCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = Color.Transparent,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    actionText: String,
    actionColor: Color = Color.Unspecified,
    actionTextColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val actualActionColor = if (actionColor != Color.Unspecified) actionColor else SukhamColors.SageContainer
    val actualActionTextColor = if (actionTextColor != Color.Unspecified) actionTextColor else SukhamColors.SageDark

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SukhamColors.CardBg),
        border = BorderStroke(1.dp, SukhamColors.CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val cardWidth = maxWidth
            val isNarrow = cardWidth < 165.dp
            val imgHeight = (cardWidth * 0.58f).coerceIn(68.dp, 94.dp)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top row: Action tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = actualActionColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = actionText.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp,
                                color = actualActionTextColor
                            ),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }

                // Title & subtitle
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SukhamColors.TitleBlack,
                            fontSize = if (isNarrow) 13.5.sp else 15.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SukhamColors.BodyGray,
                            fontSize = if (isNarrow) 10.5.sp else 11.5.sp
                        ),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Screen-responsive image preview
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imgHeight)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, SukhamColors.CardBorder, RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun SukhamFooterCard(
  onClick: () -> Unit,
  name: String = "Alex",
  formScore: Int = 84,
  streakDays: Int = 6,
  spineImprovement: Int = 6,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          brush = Brush.linearGradient(
            colors = listOf(
              Color(0xFF1B4332), // Deep Forest Emerald
              Color(0xFF2D6A4F), // Sage Emerald
              Color(0xFF1F3A2B)  // Dark Zen Green
            ),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
          )
        )
    ) {
      // Subtle background decoration glow circle
      Canvas(modifier = Modifier.matchParentSize()) {
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(Color(0x3352B788), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.2f),
            radius = size.width * 0.45f
          )
        )
      }

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // Top Header: Milestone Badge & Share Icon
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = Color(0x33FFFFFF),
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(1.dp, Color(0x44FFFFFF))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "🔥 $streakDays-DAY STREAK UNLOCKED",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 10.sp,
                  color = Color(0xFFFFD166), // Warm Gold
                  letterSpacing = 0.5.sp
                )
              )
            }
          }

          Surface(
            color = Color(0x22FFFFFF),
            shape = CircleShape,
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = "Share",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title and Pitch
        Text(
          text = "Share Your Weekly Report Card",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 18.sp
          )
        )
        Text(
          text = "Inspire your fitness circle. Top 5% form accuracy this week!",
          style = MaterialTheme.typography.bodySmall.copy(
            color = Color(0xFFD8F3DC),
            fontSize = 12.sp
          ),
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Mini preview card of their shareable story
        Surface(
          color = Color(0x1AFFFFFF),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, Color(0x22FFFFFF)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // User Avatar & Name
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                painter = painterResource(R.drawable.user_avatar_alex),
                contentDescription = null,
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .border(1.dp, Color(0xFF52B788), CircleShape),
                contentScale = ContentScale.Crop
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "$name's Summary",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp
                  )
                )
                Text(
                  text = "Sukham AI Verified",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFB7E4C7),
                    fontSize = 10.sp
                  )
                )
              }
            }

            // Quick Stats Preview
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "$formScore%",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF74C69D),
                    fontSize = 14.sp
                  )
                )
                Text(
                  text = "Form",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xAAFFFFFF),
                    fontSize = 9.sp
                  )
                )
              }

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "+$spineImprovement%",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD166),
                    fontSize = 14.sp
                  )
                )
                Text(
                  text = "Spine",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xAAFFFFFF),
                    fontSize = 9.sp
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CTA Share Button
        Button(
          onClick = onClick,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color.White),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = SukhamColors.SageDark
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Share Story Card",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamColors.SageDark,
              fontSize = 13.sp
            ),
            maxLines = 1
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = SukhamColors.SageContainer,
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = "INSTA • WHATSAPP",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = SukhamColors.SagePrimary,
                fontSize = 9.sp
              ),
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }
      }
    }
  }
}

enum class SukhamNavTab {
  HOME,
  INSIGHTS,
  PROGRAMS,
  PROFILE,
}

@Composable
fun SukhamBottomNav(
  currentTab: SukhamNavTab = SukhamNavTab.HOME,
  onHomeClick: () -> Unit = {},
  onInsightsClick: () -> Unit = {},
  onProgramsClick: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onCentralClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(66.dp),
      shadowElevation = 14.dp,
      color = SukhamColors.NavDockBg,
      shape = RoundedCornerShape(33.dp),
      border = BorderStroke(1.2.dp, SukhamColors.NavDockBorder),
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SukhamNavItem(
          icon = Icons.Outlined.Home,
          label = "Home",
          selected = currentTab == SukhamNavTab.HOME,
          onClick = if (currentTab != SukhamNavTab.HOME) onHomeClick else null,
        )
        SukhamNavItem(
          icon = Icons.Outlined.Insights,
          label = "Insights",
          selected = currentTab == SukhamNavTab.INSIGHTS,
          onClick = if (currentTab != SukhamNavTab.INSIGHTS) onInsightsClick else null,
        )
        // Central Lotus Coach Button in elevated glowing circle with subtle breath animation
        val coachTransition = rememberInfiniteTransition(label = "coachBreath")
        val coachScale by coachTransition.animateFloat(
          initialValue = 1f,
          targetValue = 1.06f,
          animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
          ),
          label = "coachScale"
        )

        Box(
          modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
              scaleX = coachScale
              scaleY = coachScale
            }
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                listOf(SukhamColors.SagePrimary, SukhamColors.TealAccent)
              )
            )
            .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onCentralClick),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(R.drawable.lotus_logo),
            contentDescription = "AI Coach",
            modifier = Modifier.size(26.dp),
          )
        }
        SukhamNavItem(
          icon = Icons.Outlined.PlayCircleOutline,
          label = "Programs",
          selected = currentTab == SukhamNavTab.PROGRAMS,
          onClick = if (currentTab != SukhamNavTab.PROGRAMS) onProgramsClick else null,
        )
        SukhamNavItem(
          icon = Icons.Outlined.Person,
          label = "Profile",
          selected = currentTab == SukhamNavTab.PROFILE,
          onClick = if (currentTab != SukhamNavTab.PROFILE) onProfileClick else null,
        )
      }
    }
  }
}

@Composable
fun SukhamNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val baseModifier =
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    val itemColor by animateColorAsState(
        targetValue = if (selected) SukhamColors.SagePrimary else SukhamColors.MutedGray,
        animationSpec = tween(durationMillis = 220),
        label = "navItemColor"
    )
    val pillBg by animateColorAsState(
        targetValue = if (selected) SukhamColors.SageContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "navPillBg"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navIconScale"
    )

    Box(
        modifier = baseModifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillBg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = itemColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = itemColor,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 9.5.sp
                )
            )
        }
    }
}

@Composable
fun SquareDrawerItem(
  label: String,
  description: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  iconBrush: Brush,
) {
  Card(
    modifier = modifier.height(140.dp).clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBrush),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White)
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(text = label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
      )
    }
  }
}

// -----------------------------------------------------------------------------
// MARK: - Smart Recommendations Carousel
// -----------------------------------------------------------------------------
data class HomeRecommendation(
  val id: String,
  val tag: String,
  val title: String,
  val description: String,
  val metricBenefit: String,
  val duration: String,
  val imageRes: Int,
  val tagBg: Color,
  val tagColor: Color,
  val buttonText: String,
  val onClick: () -> Unit,
)

@Composable
fun SmartRecommendationsCarousel(
  onViewAllInsights: () -> Unit,
  onStartSpineReset: () -> Unit,
  onStartBalanceAudit: () -> Unit,
  onStartSleepWinddown: () -> Unit,
  onStartCoreMobility: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val recommendations = listOf(
    HomeRecommendation(
      id = "spine_reset",
      tag = "POSTURE AI",
      title = "3-Min Cervical Spine Reset",
      description = "Decompress forward neck tilt with Cat-Cow & Sphinx holds before afternoon desk work.",
      metricBenefit = "Spine Align +8%",
      duration = "3 min",
      imageRes = R.drawable.pose_cat_cow,
      tagBg = SukhamColors.SageContainer,
      tagColor = SukhamColors.SageDark,
      buttonText = "Start Routine",
      onClick = onStartSpineReset,
    ),
    HomeRecommendation(
      id = "balance_radar",
      tag = "BALANCE RADAR",
      title = "Left Hip Symmetry Boost",
      description = "Correct 6% right-side bias with Tree & Warrior II unilateral stability holds.",
      metricBenefit = "Symmetry 50:50",
      duration = "10s Hold",
      imageRes = R.drawable.pose_tree,
      tagBg = SukhamColors.StreakGlow,
      tagColor = SukhamColors.StreakAmber,
      buttonText = "Test Balance",
      onClick = onStartBalanceAudit,
    ),
    HomeRecommendation(
      id = "sleep_recovery",
      tag = "RECOVERY",
      title = "Restorative Winddown",
      description = "Calm evening HRV stress with Corpse & Child's pose to enhance deep sleep cycles.",
      metricBenefit = "HRV Recovery +14%",
      duration = "5 min",
      imageRes = R.drawable.pose_childs,
      tagBg = SukhamColors.LavenderSoft,
      tagColor = SukhamColors.LavenderPrimary,
      buttonText = "Begin Winddown",
      onClick = onStartSleepWinddown,
    ),
    HomeRecommendation(
      id = "core_stabilizer",
      tag = "CORE STRENGTH",
      title = "Boat & Plank Kinetic Hold",
      description = "Reinforce abdominal wall & lumbar support with 10-second posture audits.",
      metricBenefit = "Core Stability 92%",
      duration = "2 min",
      imageRes = R.drawable.pose_boat,
      tagBg = Color(0xFFE0F2F1),
      tagColor = Color(0xFF00796B),
      buttonText = "Audit Form",
      onClick = onStartCoreMobility,
    ),
  )

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Header Row with "See Insights" link
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f, fill = false),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = SukhamColors.SageContainer,
          modifier = Modifier.size(24.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Outlined.Eco,
              contentDescription = null,
              tint = SukhamColors.SagePrimary,
              modifier = Modifier.size(13.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "Smart Recommendations",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamColors.TitleBlack,
              fontSize = 13.5.sp,
            ),
            maxLines = 1,
          )
          Text(
            text = "Personalized mobility & recovery plan",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamColors.BodyGray,
              fontSize = 10.5.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = Modifier.clickable(onClick = onViewAllInsights),
      ) {
        Row(
          modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "See Insights",
            softWrap = false,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamColors.SagePrimary,
              fontSize = 11.5.sp,
            ),
          )
          Icon(
            Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = SukhamColors.SagePrimary,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }

    // Horizontally Scrollable Carousel of Cards
    LazyRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
    ) {
      items(recommendations.size) { index ->
        val item = recommendations[index]
        RecommendationCard(item)
      }
    }
  }
}

@Composable
private fun RecommendationCard(item: HomeRecommendation) {
  Card(
    modifier = Modifier
      .width(295.dp)
      .clickable(onClick = item.onClick),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamColors.CardBg),
    border = BorderStroke(1.dp, SukhamColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top Tag & Duration Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = item.tagBg,
        ) {
          Text(
            text = item.tag,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = item.tagColor,
              fontSize = 9.5.sp,
              letterSpacing = 0.5.sp,
            ),
          )
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = SukhamColors.ChipBg,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              Icons.Outlined.Timer,
              contentDescription = null,
              tint = SukhamColors.BodyGray,
              modifier = Modifier.size(11.dp),
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = item.duration,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = SukhamColors.BodyGray,
                fontSize = 10.sp,
              ),
            )
          }
        }
      }

      // Title & Illustration Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamColors.TitleBlack,
              fontSize = 14.sp,
              lineHeight = 18.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(3.dp))

          Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamColors.BodyGray,
              fontSize = 11.5.sp,
              lineHeight = 15.sp,
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Visual Illustration Box with actual Pocket Yoga PDF illustration
        Surface(
          modifier = Modifier.size(56.dp),
          shape = RoundedCornerShape(14.dp),
          color = Color.White,
          border = BorderStroke(1.dp, SukhamColors.CardBorder),
          shadowElevation = 0.5.dp,
        ) {
          Box(
            modifier = Modifier.fillMaxSize().padding(3.dp),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(item.imageRes),
              contentDescription = item.title,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Fit,
            )
          }
        }
      }

      // Metric Benefit pill & Action Button Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = item.tagBg,
        ) {
          Text(
            text = item.metricBenefit,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = item.tagColor,
              fontSize = 10.sp,
            ),
          )
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = SukhamColors.SagePrimary,
          modifier = Modifier.clickable(onClick = item.onClick),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = item.buttonText,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 11.sp,
              ),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              Icons.Outlined.KeyboardArrowRight,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(13.dp),
            )
          }
        }
      }
    }
  }
}

