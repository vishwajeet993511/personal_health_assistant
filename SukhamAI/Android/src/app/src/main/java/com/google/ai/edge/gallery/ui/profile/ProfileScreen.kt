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

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.google.ai.edge.gallery.ui.home.SukhamBottomNav
import com.google.ai.edge.gallery.ui.home.SukhamNavTab
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatus
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.ui.common.humanReadableSize
import com.google.ai.edge.gallery.ui.common.tos.AppTosDialog
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

import com.google.ai.edge.gallery.ui.theme.SukhamColors
import com.google.ai.edge.gallery.ui.theme.isSukhamDarkTheme

/** Sukham dynamic colors connected with SukhamTheme. */
private object ProfileColors {
  val ScreenBg: Color @Composable get() = SukhamColors.ScreenBg
  val CardBg: Color @Composable get() = SukhamColors.CardBg
  val CardBorder: Color @Composable get() = SukhamColors.CardBorder
  val SagePrimary: Color @Composable get() = SukhamColors.SagePrimary
  val SageContainer: Color @Composable get() = SukhamColors.SageContainer
  val SageDark: Color @Composable get() = SukhamColors.SageDark
  val LavenderPrimary: Color @Composable get() = SukhamColors.LavenderPrimary
  val LavenderSoft: Color @Composable get() = SukhamColors.LavenderSoft
  val StreakAmber: Color @Composable get() = SukhamColors.StreakAmber
  val StreakGlow: Color @Composable get() = SukhamColors.StreakGlow
  val TitleBlack: Color @Composable get() = SukhamColors.TitleBlack
  val BodyGray: Color @Composable get() = SukhamColors.BodyGray
  val ChipBg: Color @Composable get() = SukhamColors.ChipBg
  val ChipBorder: Color @Composable get() = SukhamColors.ChipBorder
}

@Composable
fun ProfileScreen(
  onNavigateUp: () -> Unit,
  onHomeClicked: () -> Unit = onNavigateUp,
  onInsightsClicked: () -> Unit = {},
  onProgramsClicked: () -> Unit = {},
  onCentralClick: () -> Unit = {},
  viewModel: ProfileViewModel = hiltViewModel(),
  tosViewModel: TosViewModel = hiltViewModel(),
  modelManagerViewModel: ModelManagerViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  var showTosDialog by remember { mutableStateOf(false) }
  var showModelSelectionSheet by remember { mutableStateOf(false) }

  // Local LLM models state
  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()

  val allModels = remember(modelManagerUiState.tasks, modelManagerViewModel.allowlistModels) {
    val combined = (modelManagerViewModel.getAllModels() + modelManagerViewModel.allowlistModels)
      .distinctBy { it.name }
    val llms = combined.filter { it.isLlm || it.llmSupportImage }
    if (llms.isNotEmpty()) llms else combined
  }

  val downloadedModels = remember(allModels, modelManagerUiState.modelDownloadStatus) {
    allModels.filter { model ->
      modelManagerViewModel.isModelDownloaded(model) ||
        modelManagerUiState.modelDownloadStatus[model.name]?.status == ModelDownloadStatusType.SUCCEEDED
    }
  }

  val hasDownloadedModel = downloadedModels.isNotEmpty()

  val activeModel: Model? = remember(downloadedModels, modelManagerUiState.selectedModel) {
    downloadedModels.firstOrNull { it.name == modelManagerUiState.selectedModel.name }
      ?: downloadedModels.firstOrNull()
  }

  val currentlyDownloadingModel = remember(allModels, modelManagerUiState.modelDownloadStatus) {
    allModels.firstOrNull { model ->
      modelManagerUiState.modelDownloadStatus[model.name]?.status == ModelDownloadStatusType.IN_PROGRESS
    }
  }
  val downloadingStatus = currentlyDownloadingModel?.let { modelManagerUiState.modelDownloadStatus[it.name] }

  // Editable local state buffer
  var editName by remember(uiState.profile.name) { mutableStateOf(uiState.profile.name) }
  var editTagline by remember(uiState.profile.tagline) { mutableStateOf(uiState.profile.tagline) }
  var editHeightText by remember(uiState.profile.heightCm) { mutableStateOf(uiState.profile.heightCm.toString()) }
  var editWeightText by remember(uiState.profile.weightKg) { mutableStateOf(uiState.profile.weightKg.toString()) }
  var editGender by remember(uiState.profile.gender) { mutableStateOf(uiState.profile.gender) }
  var editTargetFocus by remember(uiState.profile.targetFocus) { mutableStateOf(uiState.profile.targetFocus) }

  LaunchedEffect(uiState.userFeedbackMessage) {
    uiState.userFeedbackMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearFeedbackMessage()
    }
  }

  Scaffold(
    containerColor = ProfileColors.ScreenBg,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      ProfileTopBar(
        isEditing = uiState.isEditing,
        onBackClick = onNavigateUp,
        onToggleEdit = {
          if (uiState.isEditing) {
            val finalHeight = editHeightText.toIntOrNull() ?: uiState.profile.heightCm
            val finalWeight = editWeightText.toIntOrNull() ?: uiState.profile.weightKg
            viewModel.saveProfile(
              name = editName,
              tagline = editTagline,
              heightCm = finalHeight,
              weightKg = finalWeight,
              gender = editGender,
              targetFocus = editTargetFocus,
            )
          } else {
            editHeightText = uiState.profile.heightCm.toString()
            editWeightText = uiState.profile.weightKg.toString()
            editGender = uiState.profile.gender
            viewModel.toggleEditMode()
          }
        }
      )
    },
    bottomBar = {
      SukhamBottomNav(
        currentTab = SukhamNavTab.PROFILE,
        onHomeClick = onHomeClicked,
        onInsightsClick = onInsightsClicked,
        onProgramsClick = onProgramsClicked,
        onCentralClick = onCentralClick,
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. User Identity Hero Card
      ProfileHeroCard(
        isEditing = uiState.isEditing,
        name = editName,
        tagline = editTagline,
        streakDays = uiState.profile.streakDays,
        formScore = uiState.profile.formScore,
        onNameChange = { editName = it },
        onTaglineChange = { editTagline = it },
        onAvatarClick = {
          Toast.makeText(context, "Avatar updated", Toast.LENGTH_SHORT).show()
        }
      )

      // 2. Biometrics & Physical Dimensions Card
      BiometricsSection(
        isEditing = uiState.isEditing,
        heightText = editHeightText,
        weightText = editWeightText,
        gender = editGender,
        targetFocus = editTargetFocus,
        onHeightTextChange = { editHeightText = it },
        onWeightTextChange = { editWeightText = it },
        onGenderChange = { editGender = it },
        onFocusChange = { editTargetFocus = it }
      )

      // 3. Connected Devices Card
      ConnectedDeviceCard(
        deviceName = uiState.profile.connectedDevice,
        isConnected = uiState.profile.isDeviceConnected
      )

      // 4. Settings & Preferences Section (Theme, Audio TTS, Local LLM Model, Privacy, TOS, Export)
      SettingsSection(
        currentTheme = uiState.currentTheme,
        onThemeSelected = { newTheme ->
          viewModel.updateTheme(newTheme)
        },
        isReadAloudEnabled = uiState.profile.isReadAloudEnabled,
        onToggleReadAloud = { enabled ->
          viewModel.toggleReadAloud(enabled)
        },
        activeModel = activeModel,
        hasDownloadedModel = hasDownloadedModel,
        downloadingModel = currentlyDownloadingModel,
        downloadingStatus = downloadingStatus,
        onOpenModelSelection = { showModelSelectionSheet = true },
        onTosClick = { showTosDialog = true },
        onExportClick = {
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
              Intent.EXTRA_SUBJECT,
              "Sukham AI Health Summary - ${uiState.profile.name}"
            )
            putExtra(
              Intent.EXTRA_TEXT,
              """
              Sukham AI Health Profile:
              User: ${uiState.profile.name}
              Gender: ${uiState.profile.gender}
              Focus: ${uiState.profile.targetFocus}
              Current Streak: ${uiState.profile.streakDays} Days
              Form Accuracy: ${uiState.profile.formScore}%
              Height: ${uiState.profile.heightCm} cm | Weight: ${uiState.profile.weightKg} kg
              Generated by Sukham AI (On-Device Intelligence)
              """.trimIndent()
            )
          }
          context.startActivity(Intent.createChooser(shareIntent, "Export Health Profile"))
        }
      )

      Spacer(modifier = Modifier.height(32.dp))
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

  if (showModelSelectionSheet) {
    ModelSelectionBottomSheet(
      models = allModels,
      modelManagerViewModel = modelManagerViewModel,
      onDismiss = { showModelSelectionSheet = false },
    )
  }
}

@Composable
private fun ProfileTopBar(
  isEditing: Boolean,
  onBackClick: () -> Unit,
  onToggleEdit: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Back navigation chip button
    Box(
      modifier = Modifier
        .size(38.dp)
        .background(ProfileColors.ChipBg, CircleShape)
        .border(1.dp, ProfileColors.CardBorder, CircleShape)
        .clickable(onClick = onBackClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = "Back",
        tint = ProfileColors.TitleBlack,
        modifier = Modifier.size(20.dp)
      )
    }

    Text(
      text = if (isEditing) "Edit Profile" else "My Profile",
      style = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        color = ProfileColors.TitleBlack,
        fontSize = 18.sp
      )
    )

    // Edit / Save toggle button
    Surface(
      color = if (isEditing) ProfileColors.SagePrimary else ProfileColors.SageContainer,
      shape = RoundedCornerShape(percent = 50),
      modifier = Modifier.clickable(onClick = onToggleEdit)
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (isEditing) Icons.Outlined.Check else Icons.Outlined.Edit,
          contentDescription = if (isEditing) "Save" else "Edit",
          tint = if (isEditing) Color.White else ProfileColors.SageDark,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isEditing) "Save" else "Edit",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = if (isEditing) Color.White else ProfileColors.SageDark,
            fontSize = 11.5.sp
          )
        )
      }
    }
  }
}

@Composable
private fun ProfileHeroCard(
  isEditing: Boolean,
  name: String,
  tagline: String,
  streakDays: Int,
  formScore: Int,
  onNameChange: (String) -> Unit,
  onTaglineChange: (String) -> Unit,
  onAvatarClick: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = ProfileColors.CardBg),
    border = BorderStroke(1.dp, ProfileColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Circular Avatar with Camera edit badge
      Box(
        modifier = Modifier.size(92.dp),
        contentAlignment = Alignment.BottomEnd
      ) {
        Image(
          painter = painterResource(R.drawable.user_avatar_alex),
          contentDescription = "User Avatar",
          modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .border(2.dp, ProfileColors.CardBorder, CircleShape)
            .clickable(onClick = onAvatarClick),
          contentScale = ContentScale.Crop
        )

        Box(
          modifier = Modifier
            .size(28.dp)
            .background(ProfileColors.LavenderSoft, CircleShape)
            .border(1.5.dp, ProfileColors.LavenderPrimary, CircleShape)
            .clickable(onClick = onAvatarClick),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = "Change Photo",
            tint = ProfileColors.LavenderPrimary,
            modifier = Modifier.size(15.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (isEditing) {
        OutlinedTextField(
          value = name,
          onValueChange = onNameChange,
          label = { Text("Full Name", fontSize = 12.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(0.92f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ProfileColors.TitleBlack,
            unfocusedTextColor = ProfileColors.TitleBlack,
            focusedBorderColor = ProfileColors.SagePrimary,
            unfocusedBorderColor = ProfileColors.CardBorder
          )
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = tagline,
          onValueChange = onTaglineChange,
          label = { Text("Wellness Focus / Bio", fontSize = 12.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(0.92f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ProfileColors.TitleBlack,
            unfocusedTextColor = ProfileColors.TitleBlack,
            focusedBorderColor = ProfileColors.SagePrimary,
            unfocusedBorderColor = ProfileColors.CardBorder
          )
        )
      } else {
        Text(
          text = name,
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = ProfileColors.TitleBlack,
            fontSize = 20.sp
          )
        )
        Text(
          text = tagline,
          style = MaterialTheme.typography.bodySmall.copy(
            color = ProfileColors.BodyGray,
            fontSize = 12.5.sp
          ),
          modifier = Modifier.padding(top = 2.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Gamification Pills
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = ProfileColors.StreakGlow,
          shape = RoundedCornerShape(percent = 50)
        ) {
          Text(
            text = "$streakDays-Day Streak 🔥",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = ProfileColors.TitleBlack,
              fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp)
          )
        }

        Surface(
          color = ProfileColors.LavenderSoft,
          shape = RoundedCornerShape(percent = 50)
        ) {
          Text(
            text = "$formScore% Form Score",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = ProfileColors.LavenderPrimary,
              fontSize = 11.sp
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp)
          )
        }
      }
    }
  }
}

private data class TargetFocusOption(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
)

private val TARGET_FOCUS_OPTIONS = listOf(
  TargetFocusOption(
    title = "Posture & Alignment",
    subtitle = "Spine alignment, posture audit & ergonomic balance",
    icon = Icons.Outlined.Straighten,
  ),
  TargetFocusOption(
    title = "Mobility & Flexibility",
    subtitle = "Full joint range, hamstring release & deep yoga flow",
    icon = Icons.Outlined.SelfImprovement,
  ),
  TargetFocusOption(
    title = "Strength & Conditioning",
    subtitle = "Functional resistance, muscle endurance & gym form",
    icon = Icons.Outlined.FitnessCenter,
  ),
  TargetFocusOption(
    title = "Core Stability & Balance",
    subtitle = "Pelvic floor, abdominal brace & isometric hold",
    icon = Icons.Outlined.Spa,
  ),
  TargetFocusOption(
    title = "Cardio & Calorie Burn",
    subtitle = "High tempo agility, dance cardio & aerobic fitness",
    icon = Icons.Outlined.DirectionsRun,
  ),
  TargetFocusOption(
    title = "Stress Relief & Breathing",
    subtitle = "Pranayama, nervous system reset & mindful flow",
    icon = Icons.Outlined.Spa,
  ),
)

@Composable
private fun BiometricsSection(
  isEditing: Boolean,
  heightText: String,
  weightText: String,
  gender: String,
  targetFocus: String,
  onHeightTextChange: (String) -> Unit,
  onWeightTextChange: (String) -> Unit,
  onGenderChange: (String) -> Unit,
  onFocusChange: (String) -> Unit,
) {
  var showFocusDialog by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    // Row of Height and Weight
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      BiometricEditableCard(
        label = "Height",
        unit = "cm",
        valueText = heightText,
        isEditing = isEditing,
        onValueTextChange = onHeightTextChange,
        onIncrement = {
          val cur = heightText.toIntOrNull() ?: 178
          onHeightTextChange((cur + 1).coerceIn(100, 250).toString())
        },
        onDecrement = {
          val cur = heightText.toIntOrNull() ?: 178
          onHeightTextChange((cur - 1).coerceIn(100, 250).toString())
        },
        modifier = Modifier.weight(1f)
      )

      BiometricEditableCard(
        label = "Weight",
        unit = "kg",
        valueText = weightText,
        isEditing = isEditing,
        onValueTextChange = onWeightTextChange,
        onIncrement = {
          val cur = weightText.toIntOrNull() ?: 72
          onWeightTextChange((cur + 1).coerceIn(30, 300).toString())
        },
        onDecrement = {
          val cur = weightText.toIntOrNull() ?: 72
          onWeightTextChange((cur - 1).coerceIn(30, 300).toString())
        },
        modifier = Modifier.weight(1f)
      )
    }

    // Gender Selection Card (Male / Female / Other)
    GenderSelectionCard(
      isEditing = isEditing,
      selectedGender = gender,
      onGenderChange = onGenderChange
    )

    // Target Focus Full Card with Clickable Dialog Picker
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(enabled = isEditing, onClick = { showFocusDialog = true }),
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = ProfileColors.CardBg),
      border = BorderStroke(1.dp, ProfileColors.CardBorder),
      elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = if (isEditing) "Target Focus (Tap to Choose)" else "Target Focus",
              style = MaterialTheme.typography.labelSmall.copy(
                color = ProfileColors.BodyGray,
                fontSize = 11.sp
              )
            )
            Text(
              text = targetFocus,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ProfileColors.TitleBlack,
                fontSize = 15.sp
              ),
              modifier = Modifier.padding(top = 2.dp)
            )
          }

          if (isEditing) {
            Surface(
              color = ProfileColors.SageContainer,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, ProfileColors.CardBorder),
              modifier = Modifier.clickable { showFocusDialog = true }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Choose Goal",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ProfileColors.SageDark,
                    fontSize = 11.sp
                  )
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                  imageVector = Icons.Outlined.ArrowDropDown,
                  contentDescription = "Open chooser",
                  tint = ProfileColors.SageDark,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  // Interactive Target Focus Selection Dialog
  if (showFocusDialog) {
    AlertDialog(
      onDismissRequest = { showFocusDialog = false },
      containerColor = ProfileColors.CardBg,
      shape = RoundedCornerShape(22.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Outlined.Straighten,
            contentDescription = null,
            tint = ProfileColors.SagePrimary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Select Health Focus",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = ProfileColors.TitleBlack,
              fontSize = 17.sp
            )
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TARGET_FOCUS_OPTIONS.forEach { option ->
            val isSelected = option.title.equals(targetFocus, ignoreCase = true)
            Surface(
              color = if (isSelected) ProfileColors.SageContainer else ProfileColors.ChipBg,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(
                1.dp,
                if (isSelected) ProfileColors.SagePrimary else ProfileColors.ChipBorder
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onFocusChange(option.title)
                  showFocusDialog = false
                }
            ) {
              Row(
                modifier = Modifier.padding(11.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .background(
                      if (isSelected) Color.White else Color(0xFFE2E0D8),
                      CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = if (isSelected) ProfileColors.SagePrimary else ProfileColors.TitleBlack,
                    modifier = Modifier.size(16.dp)
                  )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) ProfileColors.SageDark else ProfileColors.TitleBlack,
                      fontSize = 13.5.sp
                    )
                  )
                  Text(
                    text = option.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = ProfileColors.BodyGray,
                      fontSize = 10.5.sp
                    ),
                    modifier = Modifier.padding(top = 1.dp)
                  )
                }

                if (isSelected) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint = ProfileColors.SagePrimary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showFocusDialog = false }) {
          Text("Cancel", color = ProfileColors.BodyGray, fontWeight = FontWeight.SemiBold)
        }
      }
    )
  }
}

@Composable
private fun BiometricEditableCard(
  label: String,
  unit: String,
  valueText: String,
  isEditing: Boolean,
  onValueTextChange: (String) -> Unit,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = ProfileColors.CardBg),
    border = BorderStroke(1.dp, ProfileColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(
            color = ProfileColors.BodyGray,
            fontSize = 11.5.sp
          )
        )

        Surface(
          color = ProfileColors.ChipBg,
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = unit.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = ProfileColors.BodyGray,
              fontSize = 9.sp
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      if (isEditing) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Decrement button (-)
          Box(
            modifier = Modifier
              .size(30.dp)
              .background(ProfileColors.ChipBg, CircleShape)
              .border(1.dp, ProfileColors.CardBorder, CircleShape)
              .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Remove,
              contentDescription = "Decrease $label",
              tint = ProfileColors.TitleBlack,
              modifier = Modifier.size(14.dp)
            )
          }

          // Editable numeric input field with zero clipping
          Box(
            modifier = Modifier
              .weight(1f)
              .height(38.dp)
              .padding(horizontal = 4.dp)
              .background(ProfileColors.CardBg, RoundedCornerShape(8.dp))
              .border(1.dp, ProfileColors.CardBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            BasicTextField(
              value = valueText,
              onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }.take(3)
                onValueTextChange(filtered)
              },
              singleLine = true,
              textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = ProfileColors.TitleBlack,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
              ),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              cursorBrush = SolidColor(ProfileColors.SagePrimary),
              modifier = Modifier.fillMaxWidth()
            )
          }

          // Increment button (+)
          Box(
            modifier = Modifier
              .size(30.dp)
              .background(ProfileColors.SageContainer, CircleShape)
              .border(1.dp, ProfileColors.SagePrimary.copy(alpha = 0.5f), CircleShape)
              .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Add,
              contentDescription = "Increase $label",
              tint = ProfileColors.SageDark,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      } else {
        Text(
          text = "$valueText $unit",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = ProfileColors.TitleBlack,
            fontSize = 17.sp
          )
        )
      }
    }
  }
}

@Composable
private fun GenderSelectionCard(
  isEditing: Boolean,
  selectedGender: String,
  onGenderChange: (String) -> Unit,
) {
  val genderOptions = listOf("Male", "Female", "Other")

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = ProfileColors.CardBg),
    border = BorderStroke(1.dp, ProfileColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isEditing) "Gender (Select Option)" else "Gender",
          style = MaterialTheme.typography.labelSmall.copy(
            color = ProfileColors.BodyGray,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
          )
        )

        if (!isEditing) {
          Surface(
            color = ProfileColors.SageContainer,
            shape = RoundedCornerShape(percent = 50)
          ) {
            Text(
              text = selectedGender,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = ProfileColors.SageDark,
                fontSize = 11.5.sp
              ),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.5.dp)
            )
          }
        }
      }

      if (isEditing) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          genderOptions.forEach { option ->
            val isSelected = selectedGender.equals(option, ignoreCase = true)
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onGenderChange(option) },
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) ProfileColors.SagePrimary else ProfileColors.ScreenBg,
              border = BorderStroke(
                1.dp,
                if (isSelected) ProfileColors.SagePrimary else ProfileColors.CardBorder
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                      .size(14.dp)
                      .padding(end = 4.dp)
                  )
                }
                Text(
                  text = option,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else ProfileColors.TitleBlack,
                    fontSize = 13.sp
                  )
                )
              }
            }
          }
        }
      } else {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = selectedGender,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = ProfileColors.TitleBlack,
            fontSize = 16.sp
          )
        )
      }
    }
  }
}

@Composable
private fun ConnectedDeviceCard(
  deviceName: String,
  isConnected: Boolean,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = ProfileColors.CardBg),
    border = BorderStroke(1.dp, ProfileColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .background(ProfileColors.ChipBg, CircleShape)
            .border(1.dp, ProfileColors.CardBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.Watch,
            contentDescription = "Device",
            tint = ProfileColors.TitleBlack,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = "Connected Wearables",
            style = MaterialTheme.typography.labelSmall.copy(
              color = ProfileColors.BodyGray,
              fontSize = 11.sp
            )
          )
          Text(
            text = deviceName,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = ProfileColors.TitleBlack,
              fontSize = 14.sp
            )
          )
        }
      }

      Surface(
        color = if (isConnected) ProfileColors.SageContainer else ProfileColors.ChipBg,
        shape = RoundedCornerShape(6.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(5.dp)
              .background(
                if (isConnected) ProfileColors.SagePrimary else ProfileColors.BodyGray,
                CircleShape
              )
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isConnected) "LIVE SYNC" else "OFFLINE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              fontSize = 9.sp,
              color = if (isConnected) ProfileColors.SageDark else ProfileColors.BodyGray
            )
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsSection(
  currentTheme: Theme,
  onThemeSelected: (Theme) -> Unit,
  isReadAloudEnabled: Boolean,
  onToggleReadAloud: (Boolean) -> Unit,
  activeModel: Model?,
  hasDownloadedModel: Boolean,
  downloadingModel: Model?,
  downloadingStatus: ModelDownloadStatus?,
  onOpenModelSelection: () -> Unit,
  onTosClick: () -> Unit,
  onExportClick: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = ProfileColors.CardBg),
    border = BorderStroke(1.dp, ProfileColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Text(
        text = "Preferences & Privacy",
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          color = ProfileColors.TitleBlack,
          fontSize = 14.sp
        )
      )

      // Local On-Device AI Model in use / Download prompt card
      LocalLlmSettingCard(
        activeModel = activeModel,
        hasDownloadedModel = hasDownloadedModel,
        downloadingModel = downloadingModel,
        downloadingStatus = downloadingStatus,
        onSelectModelClick = onOpenModelSelection,
      )

      // Theme Mode Segmented Switch
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(ProfileColors.ChipBg, RoundedCornerShape(14.dp))
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "App Theme Mode",
          style = MaterialTheme.typography.bodySmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = ProfileColors.TitleBlack,
            fontSize = 12.5.sp
          )
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ThemePill(
            label = "System",
            icon = Icons.Outlined.SettingsBrightness,
            selected = currentTheme == Theme.THEME_AUTO,
            onClick = { onThemeSelected(Theme.THEME_AUTO) },
            modifier = Modifier.weight(1f)
          )
          ThemePill(
            label = "Light",
            icon = Icons.Outlined.LightMode,
            selected = currentTheme == Theme.THEME_LIGHT,
            onClick = { onThemeSelected(Theme.THEME_LIGHT) },
            modifier = Modifier.weight(1f)
          )
          ThemePill(
            label = "Dark",
            icon = Icons.Outlined.DarkMode,
            selected = currentTheme == Theme.THEME_DARK,
            onClick = { onThemeSelected(Theme.THEME_DARK) },
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Audio Read Aloud Setting Toggle
      Surface(
        color = ProfileColors.ChipBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ProfileColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(
                  if (isReadAloudEnabled) ProfileColors.SageContainer else ProfileColors.CardBg,
                  RoundedCornerShape(10.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = null,
                tint = if (isReadAloudEnabled) ProfileColors.SagePrimary else ProfileColors.BodyGray,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Read Aloud Sukham's Analysis",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = ProfileColors.TitleBlack,
                  fontSize = 13.5.sp
                )
              )
              Text(
                text = "Speak posture analysis and coaching guidance out loud using voice TTS",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = ProfileColors.BodyGray,
                  fontSize = 10.5.sp,
                  lineHeight = 14.sp
                )
              )
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Switch(
            checked = isReadAloudEnabled,
            onCheckedChange = onToggleReadAloud,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = ProfileColors.SagePrimary,
              uncheckedThumbColor = ProfileColors.BodyGray,
              uncheckedTrackColor = ProfileColors.CardBg
            )
          )
        }
      }

      // 100% On-Device AI Security Trust Badge
      Surface(
        color = ProfileColors.SageContainer,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ProfileColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.Security,
            contentDescription = null,
            tint = ProfileColors.SagePrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "On-Device AI Security",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = ProfileColors.SageDark,
                fontSize = 11.5.sp
              )
            )
            Text(
              text = "Camera motion tracking and biometrics stay 100% private on your phone.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = ProfileColors.BodyGray,
                fontSize = 10.sp
              )
            )
          }
        }
      }

      // Terms & Conditions row
      SettingActionRow(
        icon = Icons.Outlined.Description,
        title = "Terms & Conditions (T&C)",
        onClick = onTosClick
      )

      // Export Data row
      SettingActionRow(
        icon = Icons.Outlined.Download,
        title = "Export Health Profile",
        onClick = onExportClick
      )
    }
  }
}

@Composable
private fun ThemePill(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val animatedBg by animateColorAsState(
    targetValue = if (selected) ProfileColors.SagePrimary else ProfileColors.CardBg,
    animationSpec = tween(durationMillis = 220),
    label = "themePillBg"
  )
  val animatedContentColor by animateColorAsState(
    targetValue = if (selected) {
      if (isSukhamDarkTheme()) Color(0xFF080908) else Color.White
    } else ProfileColors.BodyGray,
    animationSpec = tween(durationMillis = 220),
    label = "themePillText"
  )
  val pillScale by animateFloatAsState(
    targetValue = if (selected) 1.04f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "themePillScale"
  )

  Surface(
    color = animatedBg,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(
      0.8.dp,
      if (selected) ProfileColors.SagePrimary else ProfileColors.ChipBorder
    ),
    modifier = modifier
      .graphicsLayer {
        scaleX = pillScale
        scaleY = pillScale
      }
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = animatedContentColor,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          color = animatedContentColor,
          fontSize = 11.sp
        )
      )
    }
  }
}

@Composable
private fun SettingActionRow(
  icon: ImageVector,
  title: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = ProfileColors.BodyGray,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.Medium,
          color = ProfileColors.TitleBlack,
          fontSize = 13.sp
        )
      )
    }

    Icon(
      imageVector = Icons.Outlined.ChevronRight,
      contentDescription = null,
      tint = ProfileColors.BodyGray,
      modifier = Modifier.size(16.dp)
    )
  }
}

/**
 * Card displaying the active on-device LLM model in use, or an amber attention banner
 * prompting the user to select and download a model if none is currently downloaded.
 */
@Composable
private fun LocalLlmSettingCard(
  activeModel: Model?,
  hasDownloadedModel: Boolean,
  downloadingModel: Model?,
  downloadingStatus: ModelDownloadStatus?,
  onSelectModelClick: () -> Unit,
) {
  val context = LocalContext.current
  val ramGb = remember { getDeviceTotalRamInGb(context) }

  if (hasDownloadedModel && activeModel != null) {
    // Model in use card
    Surface(
      color = ProfileColors.ChipBg,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, ProfileColors.CardBorder),
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onSelectModelClick)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Top header row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .background(ProfileColors.SageContainer, RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = null,
                tint = ProfileColors.SagePrimary,
                modifier = Modifier.size(19.dp)
              )
            }
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "Local LLM in Use",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ProfileColors.TitleBlack,
                    fontSize = 13.5.sp
                  )
                )
                // "IN USE" badge
                Surface(
                  color = ProfileColors.SageContainer,
                  shape = RoundedCornerShape(percent = 50),
                  border = BorderStroke(0.6.dp, ProfileColors.SagePrimary.copy(alpha = 0.4f))
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .background(ProfileColors.SagePrimary, CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                      text = "ACTIVE",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        color = ProfileColors.SageDark
                      )
                    )
                  }
                }
              }
              Text(
                text = "Hardware accelerated · ${ramGb} GB RAM detected",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = ProfileColors.BodyGray,
                  fontSize = 10.5.sp
                )
              )
            }
          }

          // Switch / Change button
          Surface(
            color = ProfileColors.CardBg,
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(0.8.dp, ProfileColors.CardBorder),
            modifier = Modifier.clickable(onClick = onSelectModelClick)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Switch",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp,
                  color = ProfileColors.SagePrimary
                )
              )
              Spacer(Modifier.width(2.dp))
              Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = null,
                tint = ProfileColors.SagePrimary,
                modifier = Modifier.size(13.dp)
              )
            }
          }
        }

        // Active Model Name & Details Box
        Surface(
          color = ProfileColors.CardBg,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(0.8.dp, ProfileColors.CardBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = activeModel.displayName.ifEmpty { activeModel.name },
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ProfileColors.TitleBlack,
                fontSize = 14.sp
              )
            )

            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (activeModel.llmSupportImage) {
                Surface(
                  color = ProfileColors.ChipBg,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Image,
                      contentDescription = null,
                      tint = ProfileColors.SagePrimary,
                      modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                      text = "Vision AI",
                      fontSize = 10.sp,
                      color = ProfileColors.TitleBlack,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }

              Surface(
                color = ProfileColors.ChipBg,
                shape = RoundedCornerShape(6.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = ProfileColors.SagePrimary,
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(Modifier.width(3.dp))
                  Text(
                    text = "100% Offline",
                    fontSize = 10.sp,
                    color = ProfileColors.TitleBlack,
                    fontWeight = FontWeight.Medium
                  )
                }
              }

              if (activeModel.sizeInBytes > 0) {
                Surface(
                  color = ProfileColors.ChipBg,
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Text(
                    text = activeModel.sizeInBytes.humanReadableSize(),
                    fontSize = 10.sp,
                    color = ProfileColors.BodyGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }

        // Active download progress bar if any other model is downloading in background
        if (downloadingModel != null && downloadingStatus != null) {
          val total = downloadingStatus.totalBytes
          val received = downloadingStatus.receivedBytes
          val percent = if (total > 0) (received.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Downloading ${downloadingModel.displayName}… ${(percent * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = ProfileColors.SagePrimary
                )
              )
              Text(
                text = "${received.humanReadableSize()} / ${total.humanReadableSize()}",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 10.5.sp,
                  color = ProfileColors.BodyGray
                )
              )
            }
            LinearProgressIndicator(
              progress = { percent },
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp)),
              color = ProfileColors.SagePrimary,
              trackColor = ProfileColors.SageContainer
            )
          }
        }
      }
    }
  } else {
    // No model downloaded yet card -> Alert / Prompt to select
    Surface(
      color = Color(0xFFFFFBEB),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, Color(0xFFFDE68A)),
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onSelectModelClick)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.SmartToy,
              contentDescription = null,
              tint = Color(0xFFD97706),
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "No Model Downloaded",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF92400E),
                  fontSize = 13.5.sp
                )
              )
              Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(percent = 50)
              ) {
                Text(
                  text = "ACTION REQUIRED",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    color = Color(0xFFB91C1C)
                  ),
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Sukham needs a local AI model for private posture coaching and health insights.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF78350F),
                fontSize = 10.5.sp,
                lineHeight = 14.sp
              )
            )
          }
        }

        // If a model is currently downloading
        if (downloadingModel != null && downloadingStatus != null) {
          val total = downloadingStatus.totalBytes
          val received = downloadingStatus.receivedBytes
          val percent = if (total > 0) (received.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
          Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Downloading ${downloadingModel.displayName}… ${(percent * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFFB45309)
                )
              )
              Text(
                text = "${received.humanReadableSize()} / ${total.humanReadableSize()}",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 10.5.sp,
                  color = Color(0xFF92400E)
                )
              )
            }
            LinearProgressIndicator(
              progress = { percent },
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp)),
              color = Color(0xFFD97706),
              trackColor = Color(0xFFFEF3C7)
            )
          }
        } else {
          Button(
            onClick = onSelectModelClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFD97706),
              contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Outlined.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Select & Download Recommended Model",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

