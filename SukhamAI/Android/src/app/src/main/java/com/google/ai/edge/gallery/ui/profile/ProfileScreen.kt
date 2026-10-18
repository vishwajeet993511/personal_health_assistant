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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
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

@Composable
fun ProfileScreen(
  onNavigateUp: () -> Unit,
  onHomeClicked: () -> Unit = onNavigateUp,
  onLiveClicked: () -> Unit = {},
  onProgressClicked: () -> Unit = {},
  onProgramsClicked: () -> Unit = {},
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
      val status = modelManagerUiState.modelDownloadStatus[model.name]?.status
      // A download that was resumed after a restart reports PARTIALLY_DOWNLOADED until the worker
      // reports in; it is still a download in progress, not "no model".
      status == ModelDownloadStatusType.IN_PROGRESS ||
        status == ModelDownloadStatusType.PARTIALLY_DOWNLOADED
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
    containerColor = SukhamColors.ScreenBg,
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
        onProgramsClick = onProgramsClicked,
        onLiveClick = onLiveClicked,
        onProgressClick = onProgressClicked,
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      // Open identity header.
      ProfileHeroCard(
        isEditing = uiState.isEditing,
        name = editName,
        tagline = editTagline,
        onNameChange = { editName = it },
        onTaglineChange = { editTagline = it },
      )

      // Personal details and practice focus.
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

      // Preferences, on-device model, and profile actions.
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
private fun ProfileTopBar(isEditing: Boolean, onBackClick: () -> Unit, onToggleEdit: () -> Unit) {
  Row(
    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    IconButton(onClick = onBackClick) {
      Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SukhamColors.TitleBlack)
    }
    Text(if (isEditing) "Edit profile" else "Your profile", color = SukhamColors.TitleBlack, fontSize = 17.sp)
    TextButton(onClick = onToggleEdit) {
      Icon(if (isEditing) Icons.Outlined.Check else Icons.Outlined.Edit, null,
        tint = SukhamColors.SagePrimary, modifier = Modifier.size(16.dp))
      Spacer(Modifier.width(6.dp))
      Text(if (isEditing) "Save" else "Edit", color = SukhamColors.SagePrimary, fontSize = 13.sp)
    }
  }
}

@Composable
private fun ProfileHeroCard(
  isEditing: Boolean, name: String, tagline: String,
  onNameChange: (String) -> Unit, onTaglineChange: (String) -> Unit,
) {
  Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Box(Modifier.size(108.dp).background(SukhamColors.SageContainer.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
      Image(painterResource(R.drawable.user_avatar_alex), "Profile photo", contentScale = ContentScale.Crop,
        modifier = Modifier.size(88.dp).clip(CircleShape))
    }
    Spacer(Modifier.height(18.dp))
    Text("YOUR SPACE, YOUR PACE", color = SukhamColors.SagePrimary, fontSize = 10.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
    if (isEditing) {
      ProfileTextField(name, onNameChange, "Full name")
      Spacer(Modifier.height(10.dp))
      ProfileTextField(tagline, onTaglineChange, "About you")
    } else {
      Text(name, color = SukhamColors.TitleBlack, fontFamily = FontFamily.Serif, fontSize = 32.sp,
        lineHeight = 38.sp, textAlign = TextAlign.Center)
      if (tagline.isNotBlank()) Text(tagline, color = SukhamColors.BodyGray, fontSize = 14.sp,
        lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
    }
  }
}

@Composable
private fun ProfileTextField(value: String, onChange: (String) -> Unit, label: String, numeric: Boolean = false, modifier: Modifier = Modifier) {
  OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label, fontSize = 12.sp) },
    singleLine = true, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
    keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
    colors = OutlinedTextFieldDefaults.colors(
      focusedTextColor = SukhamColors.TitleBlack, unfocusedTextColor = SukhamColors.TitleBlack,
      focusedBorderColor = SukhamColors.SagePrimary, unfocusedBorderColor = SukhamColors.CardBorder,
      cursorColor = SukhamColors.SagePrimary,
    ))
}

@Composable
private fun ProfileSectionLabel(text: String) {
  Text(text.uppercase(), color = SukhamColors.BodyGray, fontSize = 10.sp,
    letterSpacing = 1.8.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun ProfileDivider() {
  HorizontalDivider(color = SukhamColors.CardBorder.copy(alpha = 0.65f), thickness = 0.5.dp)
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
    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
  ),
  TargetFocusOption(
    title = "Stress Relief & Breathing",
    subtitle = "Pranayama, nervous system reset & mindful flow",
    icon = Icons.Outlined.Spa,
  ),
)

@Composable
private fun BiometricsSection(
  isEditing: Boolean, heightText: String, weightText: String, gender: String, targetFocus: String,
  onHeightTextChange: (String) -> Unit, onWeightTextChange: (String) -> Unit,
  onGenderChange: (String) -> Unit, onFocusChange: (String) -> Unit,
) {
  var showFocusDialog by remember { mutableStateOf(false) }
  var showGenderMenu by remember { mutableStateOf(false) }
  Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
    ProfileSectionLabel("Personal details")
    if (isEditing) {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ProfileTextField(heightText, { onHeightTextChange(it.filter(Char::isDigit).take(3)) }, "Height · cm", true, Modifier.weight(1f))
        ProfileTextField(weightText, { onWeightTextChange(it.filter(Char::isDigit).take(3)) }, "Weight · kg", true, Modifier.weight(1f))
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Gender", color = SukhamColors.BodyGray, fontSize = 13.sp)
        Box {
          TextButton(onClick = { showGenderMenu = true }) { Text("$gender ▾", color = SukhamColors.SagePrimary) }
          DropdownMenu(expanded = showGenderMenu, onDismissRequest = { showGenderMenu = false }) {
            listOf("Male", "Female", "Other").forEach { option ->
              DropdownMenuItem(text = { Text(option) }, onClick = { onGenderChange(option); showGenderMenu = false })
            }
          }
        }
      }
    } else {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PersonalDetail("Height", heightText, "cm", Modifier.weight(1f))
        Box(Modifier.width(0.5.dp).height(36.dp).background(SukhamColors.CardBorder))
        PersonalDetail("Weight", weightText, "kg", Modifier.weight(1f))
        Box(Modifier.width(0.5.dp).height(36.dp).background(SukhamColors.CardBorder))
        PersonalDetail("Gender", gender, "", Modifier.weight(1f))
      }
    }
    ProfileDivider()
    Row(Modifier.fillMaxWidth().clickable(enabled = isEditing) { showFocusDialog = true }.padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Icon(Icons.Outlined.SelfImprovement, null, tint = SukhamColors.SagePrimary, modifier = Modifier.size(24.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Your practice focus", color = SukhamColors.BodyGray, fontSize = 12.sp)
        Text(targetFocus, color = SukhamColors.TitleBlack, fontFamily = FontFamily.Serif, fontSize = 19.sp, lineHeight = 24.sp)
      }
      if (isEditing) Icon(Icons.Outlined.ChevronRight, "Choose practice focus", tint = SukhamColors.SagePrimary)
    }
    ProfileDivider()
  }
  if (showFocusDialog) {
    AlertDialog(onDismissRequest = { showFocusDialog = false }, containerColor = SukhamColors.CardBg,
      title = { Text("Your practice focus", fontFamily = FontFamily.Serif, color = SukhamColors.TitleBlack) },
      text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
          TARGET_FOCUS_OPTIONS.forEach { option ->
            Row(Modifier.fillMaxWidth().clickable { onFocusChange(option.title); showFocusDialog = false }.padding(vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Icon(option.icon, null, tint = SukhamColors.SagePrimary, modifier = Modifier.size(22.dp))
              Column(Modifier.weight(1f)) {
                Text(option.title, color = SukhamColors.TitleBlack, fontSize = 14.sp)
                Text(option.subtitle, color = SukhamColors.BodyGray, fontSize = 12.sp, lineHeight = 17.sp)
              }
              if (option.title.equals(targetFocus, true)) Icon(Icons.Outlined.Check, "Selected", tint = SukhamColors.SagePrimary, modifier = Modifier.size(18.dp))
            }
            ProfileDivider()
          }
        }
      }, confirmButton = { TextButton(onClick = { showFocusDialog = false }) { Text("Done", color = SukhamColors.SagePrimary) } })
  }
}

@Composable
private fun PersonalDetail(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
  Column(modifier.padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(5.dp)) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(value, color = SukhamColors.TitleBlack, fontSize = 22.sp, fontFamily = FontFamily.Serif)
      if (unit.isNotEmpty()) Text(unit, color = SukhamColors.BodyGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
    }
    Text(label, color = SukhamColors.BodyGray, fontSize = 12.sp)
  }
}

@Composable
private fun SettingsSection(
  currentTheme: Theme, onThemeSelected: (Theme) -> Unit,
  isReadAloudEnabled: Boolean, onToggleReadAloud: (Boolean) -> Unit,
  activeModel: Model?, hasDownloadedModel: Boolean,
  downloadingModel: Model?, downloadingStatus: ModelDownloadStatus?,
  onOpenModelSelection: () -> Unit, onTosClick: () -> Unit, onExportClick: () -> Unit,
) {
  Column(Modifier.fillMaxWidth()) {
    ProfileSectionLabel("Make it yours")
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Icon(Icons.Outlined.SettingsBrightness, null, tint = SukhamColors.SagePrimary, modifier = Modifier.size(22.dp))
      Text("Appearance", color = SukhamColors.TitleBlack, fontSize = 15.sp)
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SukhamColors.CardBg).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      ThemePill("System", Icons.Outlined.SettingsBrightness, currentTheme == Theme.THEME_AUTO, { onThemeSelected(Theme.THEME_AUTO) }, Modifier.weight(1f))
      ThemePill("Light", Icons.Outlined.LightMode, currentTheme == Theme.THEME_LIGHT, { onThemeSelected(Theme.THEME_LIGHT) }, Modifier.weight(1f))
      ThemePill("Dark", Icons.Outlined.DarkMode, currentTheme == Theme.THEME_DARK, { onThemeSelected(Theme.THEME_DARK) }, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    ProfileDivider()
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Icon(Icons.AutoMirrored.Outlined.VolumeUp, null, tint = SukhamColors.SagePrimary, modifier = Modifier.size(22.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Spoken guidance", color = SukhamColors.TitleBlack, fontSize = 15.sp)
        Text("Hear your analysis and coaching", color = SukhamColors.BodyGray, fontSize = 12.sp, lineHeight = 17.sp)
      }
      Switch(checked = isReadAloudEnabled, onCheckedChange = onToggleReadAloud,
        modifier = Modifier.semantics { contentDescription = "Spoken guidance" },
        colors = SwitchDefaults.colors(checkedTrackColor = SukhamColors.SagePrimary, checkedThumbColor = SukhamColors.ScreenBg,
          uncheckedTrackColor = SukhamColors.CardBg, uncheckedThumbColor = SukhamColors.BodyGray, uncheckedBorderColor = SukhamColors.CardBorder))
    }
    ProfileDivider()
    Spacer(Modifier.height(24.dp))
    ProfileSectionLabel("Intelligence & privacy")
    Spacer(Modifier.height(8.dp))
    LocalLlmSettingCard(activeModel, hasDownloadedModel, downloadingModel, downloadingStatus, onOpenModelSelection)
    ProfileDivider()
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Icon(Icons.Outlined.Security, null, tint = SukhamColors.SagePrimary, modifier = Modifier.size(22.dp))
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("On-device intelligence", color = SukhamColors.TitleBlack, fontSize = 15.sp)
        Text("Your AI analysis runs on your device.", color = SukhamColors.BodyGray, fontSize = 12.sp, lineHeight = 18.sp)
      }
    }
    ProfileDivider()
    Spacer(Modifier.height(24.dp))
    ProfileSectionLabel("Your information")
    Spacer(Modifier.height(8.dp))
    SettingActionRow(Icons.Outlined.Description, "Terms & conditions", onTosClick)
    ProfileDivider()
    SettingActionRow(Icons.Outlined.Download, "Export health profile", onExportClick)
    ProfileDivider()
    Spacer(Modifier.height(24.dp))
    Text("SUKHAM", color = SukhamColors.SagePrimary, fontSize = 11.sp, letterSpacing = 3.sp,
      modifier = Modifier.align(Alignment.CenterHorizontally))
  }
}

@Composable
private fun ThemePill(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val background by animateColorAsState(if (selected) SukhamColors.SageContainer else SukhamColors.CardBg, tween(220), label = "appearance")
  Surface(onClick = onClick, modifier = modifier.semantics { this.selected = selected; role = Role.RadioButton }, shape = RoundedCornerShape(12.dp), color = background) {
    Row(Modifier.padding(horizontal = 6.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center) {
      Icon(icon, null, tint = if (selected) SukhamColors.SagePrimary else SukhamColors.BodyGray, modifier = Modifier.size(17.dp))
      Spacer(Modifier.width(6.dp))
      Text(label, color = if (selected) SukhamColors.SagePrimary else SukhamColors.BodyGray, fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
  }
}

@Composable
private fun SettingActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
  Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 18.dp),
    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
    Icon(icon, null, tint = SukhamColors.SagePrimary, modifier = Modifier.size(22.dp))
    Text(title, color = SukhamColors.TitleBlack, fontSize = 15.sp, modifier = Modifier.weight(1f))
    Icon(Icons.Outlined.ChevronRight, null, tint = SukhamColors.BodyGray, modifier = Modifier.size(18.dp))
  }
}

@Composable
private fun LocalLlmSettingCard(
  activeModel: Model?, hasDownloadedModel: Boolean, downloadingModel: Model?,
  downloadingStatus: ModelDownloadStatus?, onSelectModelClick: () -> Unit,
) {
  Column(Modifier.fillMaxWidth().clickable(onClick = onSelectModelClick).padding(vertical = 16.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      Icon(Icons.Outlined.SmartToy, null, tint = SukhamColors.SagePrimary, modifier = Modifier.size(22.dp))
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("AI model", color = SukhamColors.TitleBlack, fontSize = 15.sp)
        Text(if (hasDownloadedModel && activeModel != null) activeModel.displayName.ifEmpty { activeModel.name }
          else if (downloadingModel != null) "Preparing your on-device coach" else "Choose a model to enable AI coaching",
          color = SukhamColors.BodyGray, fontSize = 12.sp, lineHeight = 18.sp)
        if (hasDownloadedModel && activeModel != null) {
          Text(listOfNotNull(if (activeModel.llmSupportImage) "Vision enabled" else null,
            activeModel.sizeInBytes.takeIf { it > 0 }?.humanReadableSize()).joinToString(" · "),
            color = SukhamColors.SagePrimary, fontSize = 11.sp)
        }
      }
      Icon(Icons.Outlined.ChevronRight, "Choose AI model", tint = SukhamColors.BodyGray, modifier = Modifier.size(18.dp))
    }
    if (downloadingModel != null && downloadingStatus != null) {
      val total = downloadingStatus.totalBytes
      val received = downloadingStatus.receivedBytes
      val progress = if (total > 0) (received.toFloat() / total).coerceIn(0f, 1f) else 0f
      Column(Modifier.padding(start = 36.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Downloading ${downloadingModel.displayName.ifEmpty { downloadingModel.name }}", color = SukhamColors.BodyGray, fontSize = 12.sp)
        if (total > 0) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = SukhamColors.SagePrimary, trackColor = SukhamColors.SageContainer)
        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SukhamColors.SagePrimary, trackColor = SukhamColors.SageContainer)
        Text(if (total > 0) "${(progress * 100).toInt()}% · ${received.humanReadableSize()} of ${total.humanReadableSize()}" else "${received.humanReadableSize()} downloaded",
          color = SukhamColors.BodyGray, fontSize = 11.sp)
      }
    }
  }
}
