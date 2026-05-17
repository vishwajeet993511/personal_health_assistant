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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.google.ai.edge.gallery.ui.theme.customColors
import com.google.ai.edge.gallery.ui.theme.homePageTitleStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "AGHomeScreen"

/** Sukham brand colors aligned with the UI mockup. */
private object SukhamColors {
  val TitleBlack = Color(0xFF1A1A1A)
  val BodyGray = Color(0xFF616161)
  val ScreenBg = Color(0xFFF5F4EF)
  val Purple = Color(0xFF7E57C2)
  val PurpleDark = Color(0xFF673AB7)
  val LavenderCard = Color(0xFFF3E8FF)
  val Teal = Color(0xFF26A69A)
  val Peach = Color(0xFFFFAB91)
  val LightBlue = Color(0xFF81D4FA)
  val LavenderBtn = Color(0xFFE1BEE7)
  val LiveGreen = Color(0xFF43A047)
}
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
  enableAnimation: Boolean,
  modifier: Modifier = Modifier,
  gm4: Boolean = false,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  fun openChat(task: Task?) {
    if (task == null || uiState.loadingModelAllowlist) return
    val targetModel = modelManagerViewModel.resolveModelForTask(task)
    if (targetModel != null) {
      navigateToModelScreen(task, targetModel)
    } else {
      navigateToTaskScreen(task)
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
            SukhamHeader(onMenuClick = { scope.launch { drawerState.open() } })
          },
          bottomBar = { 
              SukhamBottomNav(
                  onCentralClick = { 
                      openChat(modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT))
                  }
              ) 
          }
        ) { innerPadding ->
          Column(
            modifier =
              Modifier.fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            // App Title and Subtitle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "SUKHAM AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 4.sp,
                  color = SukhamColors.TitleBlack,
                ),
                textAlign = TextAlign.Center
              )
              Text(
                text = "Your AI Partner in Holistic Wellbeing",
                style = MaterialTheme.typography.bodyMedium.copy(color = SukhamColors.BodyGray),
                textAlign = TextAlign.Center
              )
            }

            val chatTask = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT)
            val imageChatTask = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_ASK_IMAGE)

            if (chatTask != null) {
                SukhamMainChatCard(onClick = { openChat(chatTask) })
            }

            // Grid Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SukhamGridCard(
                    title = "Live Exercise",
                    subtitle = "Join expert-led sessions in real time.",
                    imageRes = R.drawable.live_exercise,
                    modifier = Modifier.weight(1f),
                    badge = "LIVE",
                    badgeColor = SukhamColors.LiveGreen,
                    actionText = "Join Live",
                    actionColor = SukhamColors.Teal,
                    onClick = { openChat(chatTask) }
                )
                SukhamGridCard(
                    title = "Yoga Tips",
                    subtitle = "Daily guidance for body, mind & soul.",
                    imageRes = R.drawable.yoga_tips,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Eco,
                    iconTint = Color(0xFF8D6E63),
                    actionText = "View Tips",
                    actionColor = SukhamColors.Peach,
                    onClick = { openChat(chatTask) }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SukhamGridCard(
                    title = "Wearables Data Analysis",
                    subtitle = "Understand your body. Improve every day.",
                    imageRes = R.drawable.wearables_analysis,
                    modifier = Modifier.weight(1f),
                    actionText = "View Insights",
                    actionColor = SukhamColors.LightBlue,
                    onClick = { openChat(imageChatTask) }
                )
                // Personal Files Analysis
                SukhamGridCard(
                    title = "Personal Files Analysis",
                    subtitle = "Upload your reports. Get AI insights.",
                    imageRes = R.drawable.personal_files,
                    modifier = Modifier.weight(1f),
                    actionText = "Upload & Analyze",
                    actionColor = SukhamColors.LavenderBtn,
                    onClick = { openChat(imageChatTask) }
                )
            }

            // Share My Progress Footer
            SukhamFooterCard(onClick = {})

            Spacer(modifier = Modifier.height(20.dp))
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
fun SukhamHeader(onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick, modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape)) {
            Icon(Icons.Outlined.Menu, contentDescription = "Menu")
        }
        Image(
            painter = painterResource(R.drawable.lotus_logo),
            contentDescription = "Sukham Logo",
            modifier = Modifier.size(40.dp)
        )
        IconButton(onClick = {}, modifier = Modifier.background(Color(0xFFF5F5F5), CircleShape)) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
        }
    }
}

@Composable
fun SukhamMainChatCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(160.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = SukhamColors.LavenderCard)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp).background(
                    brush = Brush.radialGradient(listOf(Color(0xFFD1C4E9), SukhamColors.Purple)),
                    shape = CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.lotus_logo),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Chat with SUKHAM AI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SukhamColors.TitleBlack,
                    ),
                )
                Text(
                    "Your personal wellness guide. Ask anything, anytime.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SukhamColors.BodyGray),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(56.dp).background(SukhamColors.PurpleDark, CircleShape)
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = Color.White)
                }
                Text(
                    "Start Chat",
                    style = MaterialTheme.typography.labelSmall.copy(color = SukhamColors.TitleBlack),
                )
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
    iconTint: Color = Color(0xFF8D6E63),
    actionText: String,
    actionColor: Color = Color(0xFFEEEEEE),
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(280.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (badge != null) {
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                        )
                    }
                } else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(bottom = 8.dp))
                }

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = SukhamColors.TitleBlack,
                    ),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = SukhamColors.BodyGray),
                    maxLines = 2,
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        actionText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SukhamColors.TitleBlack,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd).size(140.dp).offset(x = 10.dp, y = 10.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun SukhamFooterCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.gemma_promo_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp).align(Alignment.BottomStart)) {
                Text(
                    "Share My Progress",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = SukhamColors.TitleBlack,
                    ),
                )
                Text(
                    "Celebrate milestones. Inspire others. Stay motivated together.",
                    style = MaterialTheme.typography.bodySmall.copy(color = SukhamColors.BodyGray),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Progress", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SukhamBottomNav(onCentralClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shadowElevation = 8.dp,
        color = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SukhamNavItem(icon = Icons.Outlined.Home, label = "Home", selected = true)
            SukhamNavItem(icon = Icons.Outlined.Insights, label = "Insights")
            IconButton(
                onClick = onCentralClick,
                modifier = Modifier.size(56.dp).offset(y = (-10).dp).background(SukhamColors.LavenderCard, CircleShape)
            ) {
                Image(painter = painterResource(R.drawable.lotus_logo), contentDescription = null, modifier = Modifier.size(32.dp))
            }
            SukhamNavItem(icon = Icons.Outlined.PlayCircleOutline, label = "Programs")
            SukhamNavItem(icon = Icons.Outlined.Person, label = "Profile")
        }
    }
}

@Composable
fun SukhamNavItem(icon: ImageVector, label: String, selected: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = if (selected) SukhamColors.PurpleDark else Color.LightGray)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = if (selected) SukhamColors.PurpleDark else Color.LightGray))
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
