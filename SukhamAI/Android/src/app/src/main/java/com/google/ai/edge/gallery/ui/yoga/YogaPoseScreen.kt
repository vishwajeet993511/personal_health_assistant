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

package com.google.ai.edge.gallery.ui.yoga

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ai.edge.gallery.ui.common.tts.rememberSukhamTtsHelper
import com.google.ai.edge.gallery.ui.profile.ProfileViewModel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.runtime.runtimeHelper
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.ai.edge.gallery.ui.theme.SukhamColors

private const val TAG = "YogaPoseScreen"

// Soft cap for the one-shot photo flow. More generous than the session cap in YogaCoachEngine
// because a stalled single photo only keeps the user waiting; it doesn't block later frames.
private const val MAX_RESPONSE_MS = 90_000L

/** Sukham brand colors aligned with the Zen-Tech dynamic theme. */
private object YogaThemeColors {
  val ScreenBg: Color @Composable get() = SukhamColors.ScreenBg
  val CardBg: Color @Composable get() = SukhamColors.CardBg
  val CardBorder: Color @Composable get() = SukhamColors.CardBorder
  val SagePrimary: Color @Composable get() = SukhamColors.SagePrimary
  val SageContainer: Color @Composable get() = SukhamColors.SageContainer
  val SageDark: Color @Composable get() = SukhamColors.SageDark
  val LavenderPrimary: Color @Composable get() = SukhamColors.LavenderPrimary
  val LavenderSoft: Color @Composable get() = SukhamColors.LavenderSoft
  val TitleBlack: Color @Composable get() = SukhamColors.TitleBlack
  val BodyGray: Color @Composable get() = SukhamColors.BodyGray
  val ChipBg: Color @Composable get() = SukhamColors.ChipBg
}

@Composable
private fun AuditFeatureRow(text: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Surface(
      color = YogaThemeColors.SageContainer,
      shape = CircleShape,
      modifier = Modifier.size(18.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Outlined.Check,
          contentDescription = null,
          tint = YogaThemeColors.SageDark,
          modifier = Modifier.size(11.dp)
        )
      }
    }
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall.copy(
        color = YogaThemeColors.BodyGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
      )
    )
  }
}

/** Which frame source is feeding the coach. */
private enum class YogaMode(val label: String) {
  PHOTO("Photo"),
  LIVE("Live session"),
  VIDEO("Upload video"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YogaPoseScreen(
  modelManagerViewModel: ModelManagerViewModel,
  initialMode: String = "photo",
  onNavigateUp: () -> Unit,
  profileViewModel: ProfileViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uiState by modelManagerViewModel.uiState.collectAsState()
  val profileUiState by profileViewModel.uiState.collectAsState()
  val isReadAloudEnabled = profileUiState.profile.isReadAloudEnabled
  val ttsHelper = rememberSukhamTtsHelper()

  val imageChatTask: Task? = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_ASK_IMAGE)
  val imageModel: Model? =
    imageChatTask?.let { modelManagerViewModel.resolveModelForTask(it) }
      ?: modelManagerViewModel.getAllModels().firstOrNull {
        it.llmSupportImage &&
          (modelManagerViewModel.isModelDownloaded(it) ||
            uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED)
      }
      ?: modelManagerViewModel.allowlistModels.firstOrNull { it.llmSupportImage }

  val poseHelper = remember { PoseLandmarkerHelper(context.applicationContext) }
  val engine = remember(poseHelper) { YogaCoachEngine(poseHelper, scope) }
  DisposableEffect(Unit) { onDispose { poseHelper.close() } }

  val initialYogaMode = remember(initialMode) {
    when {
      initialMode.equals("live", ignoreCase = true) -> YogaMode.LIVE
      initialMode.equals("photo", ignoreCase = true) -> YogaMode.PHOTO
      else -> YogaMode.VIDEO
    }
  }
  var mode by remember(initialYogaMode) { mutableStateOf(initialYogaMode) }

  val isLiveScreen = remember(initialYogaMode) { initialYogaMode == YogaMode.LIVE }

  // Exclude Live Session: Show only Photo and Video Upload options
  val uploadModes = remember { listOf(YogaMode.VIDEO, YogaMode.PHOTO) }

  // ---- Photo mode state (unchanged one-shot flow) ----
  var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var annotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var poseResult by remember { mutableStateOf<PoseLandmarkerResult?>(null) }
  var isDetecting by remember { mutableStateOf(false) }
  var detectionError by remember { mutableStateOf<String?>(null) }
  var aiResponse by remember { mutableStateOf("") }
  // Unsanitised accumulator; aiResponse holds the cleaned text that is actually rendered.
  var rawResponse by remember { mutableStateOf("") }
  var aiInFlight by remember { mutableStateOf(false) }
  var pendingInference by remember { mutableStateOf(false) }
  var savedUri by remember { mutableStateOf<Uri?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var lastInferenceStart by remember { mutableStateOf(0L) }
  var earlyStopFired by remember { mutableStateOf(false) }

  // ---- Session state shared by live + video modes ----
  val cards by engine.cards.collectAsState()
  val skippedTicks by engine.skippedTicks.collectAsState()
  val engineBusy by engine.busy.collectAsState()
  var sessionActive by remember { mutableStateOf(false) }
  // Set when the user hits Start but Gemma isn't INITIALIZED yet; a LaunchedEffect below picks
  // the session up as soon as it is. Same trick the photo flow uses, for the same reason:
  // initializeModel's onDone is silently skipped when init is already done or mid-flight.
  var pendingSessionStart by remember { mutableStateOf(false) }

  // ---- Live mode state ----
  val latestFrame = remember { AtomicReference<Bitmap?>(null) }
  var sessionStartMs by remember { mutableStateOf(0L) }
  var elapsedMs by remember { mutableStateOf(0L) }
  var useFrontCamera by remember { mutableStateOf(true) }
  val holdDetector = remember { PoseHoldDetector() }
  var holdState by remember { mutableStateOf(PoseHoldDetector.State.NO_PERSON) }
  var holdProgress by remember { mutableStateOf(0f) }
  var detectedPose by remember { mutableStateOf<String?>(null) }
  var lastSpokenLiveCardId by remember { mutableLongStateOf(-1L) }

  // ---- Video mode state ----
  var videoUri by remember { mutableStateOf<Uri?>(null) }
  var videoDurationMs by remember { mutableStateOf(0L) }
  var samplePositions by remember { mutableStateOf<List<Long>>(emptyList()) }
  var videoIntervalMs by remember { mutableStateOf(0L) }
  var videoJob by remember { mutableStateOf<Job?>(null) }

  val initStatus = imageModel?.let { uiState.modelInitializationStatus[it.name]?.status }
  val modelDownloaded =
    imageModel?.let { uiState.modelDownloadStatus[it.name]?.status } ==
      ModelDownloadStatusType.SUCCEEDED

  /**
   * Shared gate for every mode: the multimodal Gemma must already be downloaded (that flow lives
   * in "Ask Image", keeping this screen lean). Kicks initialization off and returns whether the
   * caller may proceed.
   */
  fun ensureModelOrToast(): Boolean {
    val task = imageChatTask
    val model = imageModel
    if (task == null || model == null) {
      Toast.makeText(
          context,
          "AI model not available. Download it once from your Profile.",
          Toast.LENGTH_LONG,
        )
        .show()
      return false
    }
    if (!modelDownloaded) {
      Toast.makeText(
          context,
          "Download the AI model from your Profile first.",
          Toast.LENGTH_LONG,
        )
        .show()
      return false
    }
    modelManagerViewModel.selectModel(model)
    modelManagerViewModel.initializeModel(context = context, task = task, model = model)
    return true
  }

  fun stopSession() {
    sessionActive = false
    pendingSessionStart = false
    videoJob?.cancel()
    videoJob = null
    ttsHelper.stop()
    lastSpokenLiveCardId = -1L
  }

  val galleryLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri ?: return@rememberLauncherForActivityResult
      handleNewImage(
        context = context,
        uri = uri,
        poseHelper = poseHelper,
        onStart = {
          isDetecting = true
          detectionError = null
          aiResponse = ""
        },
        onSuccess = { src, annotated, result ->
          originalBitmap = src
          annotatedBitmap = annotated
          poseResult = result
          isDetecting = false
        },
        onError = { msg ->
          detectionError = msg
          isDetecting = false
        },
        scope = scope,
      )
    }

  val cameraLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
      val uri = pendingCameraUri
      if (success && uri != null) {
        handleNewImage(
          context = context,
          uri = uri,
          poseHelper = poseHelper,
          onStart = {
            isDetecting = true
            detectionError = null
            aiResponse = ""
          },
          onSuccess = { src, annotated, result ->
            originalBitmap = src
            annotatedBitmap = annotated
            poseResult = result
            isDetecting = false
          },
          onError = { msg ->
            detectionError = msg
            isDetecting = false
          },
          scope = scope,
        )
      }
    }

  val videoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri ?: return@rememberLauncherForActivityResult
      stopSession()
      engine.reset()
      videoUri = uri
      scope.launch {
        val duration = withContext(Dispatchers.IO) { VideoFrameSampler.durationMs(context, uri) }
        videoDurationMs = duration
        val interval = VideoFrameSampler.intervalFor(duration)
        videoIntervalMs = interval
        samplePositions = VideoFrameSampler.samplePositions(duration, interval)
      }
    }

  Scaffold(
    containerColor = YogaThemeColors.ScreenBg,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = if (isLiveScreen) "Live AI Coach" else "Upload & Analyse",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = YogaThemeColors.TitleBlack,
              fontSize = 18.sp
            )
          )
        },
        navigationIcon = {
          IconButton(onClick = {
            ttsHelper.stop()
            onNavigateUp()
          }) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
              tint = YogaThemeColors.TitleBlack
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = YogaThemeColors.ScreenBg,
          titleContentColor = YogaThemeColors.TitleBlack,
        ),
      )
    }
  ) { padding ->
    // A LazyColumn rather than verticalScroll: the video timeline grows unbounded and a
    // scrollable list can't be nested inside a scrolling parent.
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
    ) {
      // 1. Header block
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (isLiveScreen) {
            Surface(
              color = YogaThemeColors.SageContainer,
              shape = RoundedCornerShape(percent = 50),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .background(YogaThemeColors.SagePrimary, CircleShape)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = "((•)) REAL-TIME MOTION COACH",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = YogaThemeColors.SageDark,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp
                  )
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Live Form & Posture Coach",
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = YogaThemeColors.TitleBlack,
                fontSize = 22.sp
              )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Position your phone with full body in view. Sukham AI tracks joints in real time and delivers instant audio-visual guidance as you exercise.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = YogaThemeColors.BodyGray,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
              )
            )
          } else {
            Surface(
              color = YogaThemeColors.LavenderSoft,
              shape = RoundedCornerShape(percent = 50),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.Videocam,
                  contentDescription = null,
                  tint = YogaThemeColors.LavenderPrimary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "ASYNC BIOMECHANICAL AUDIT",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = YogaThemeColors.LavenderPrimary,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp
                  )
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Upload & Analyse Form",
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = YogaThemeColors.TitleBlack,
                fontSize = 22.sp
              )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Select a recorded video or pose photo for on-device AI movement analysis, spine angle tracking, and joint feedback.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = YogaThemeColors.BodyGray,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
              )
            )
          }
        }
      }

      // 2. Mode Selector: Only shown for Upload screen (Video & Photo)
      if (!isLiveScreen) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = YogaThemeColors.ChipBg,
            border = BorderStroke(1.dp, YogaThemeColors.CardBorder)
          ) {
            Row(
              modifier = Modifier.padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              uploadModes.forEach { entry ->
                val isSelected = mode == entry
                Surface(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      if (mode != entry) {
                        ttsHelper.stop()
                        stopSession()
                        mode = entry
                      }
                    },
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) YogaThemeColors.SagePrimary else Color.Transparent,
                  shadowElevation = if (isSelected) 1.dp else 0.dp
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = if (entry == YogaMode.VIDEO) Icons.Outlined.Videocam else Icons.Outlined.PhotoCamera,
                      contentDescription = null,
                      tint = if (isSelected) Color.White else YogaThemeColors.BodyGray,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (entry == YogaMode.VIDEO) "Video Upload" else "Photo Upload",
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else YogaThemeColors.TitleBlack,
                        fontSize = 13.sp
                      )
                    )
                  }
                }
              }
            }
          }
        }
      }

      when (mode) {
        YogaMode.PHOTO -> {
          val hasPhoto = (annotatedBitmap != null || originalBitmap != null)
          if (!hasPhoto) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = YogaThemeColors.CardBg),
                border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Image(
                    painter = painterResource(R.drawable.live_ai_coach_hero),
                    contentDescription = "Photo Pose Audit",
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(180.dp)
                      .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  Text(
                    text = "Pose Form & Alignment Critique",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = YogaThemeColors.TitleBlack,
                      fontSize = 16.5.sp
                    )
                  )

                  Spacer(modifier = Modifier.height(6.dp))

                  Text(
                    text = "Capture or select a photo in any posture. Sukham AI maps your skeleton, identifies the closest asana, and provides immediate biomechanical coaching.",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = YogaThemeColors.BodyGray,
                      fontSize = 12.sp,
                      textAlign = TextAlign.Center,
                      lineHeight = 16.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    AuditFeatureRow("Real-time landmark skeleton overlay")
                    AuditFeatureRow("Asana match & form accuracy score")
                    AuditFeatureRow("Personalized coaching cues from Gemma")
                  }

                  Spacer(modifier = Modifier.height(16.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    OutlinedButton(
                      modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                      shape = RoundedCornerShape(percent = 50),
                      border = BorderStroke(1.dp, YogaThemeColors.SagePrimary),
                      onClick = {
                        galleryLauncher.launch(
                          androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                          )
                        )
                      }
                    ) {
                      Icon(Icons.Outlined.Image, contentDescription = null, tint = YogaThemeColors.SagePrimary)
                      Spacer(Modifier.width(6.dp))
                      Text("Gallery", color = YogaThemeColors.SageDark, fontWeight = FontWeight.Bold)
                    }

                    Button(
                      modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                      shape = RoundedCornerShape(percent = 50),
                      colors = ButtonDefaults.buttonColors(containerColor = YogaThemeColors.SagePrimary),
                      onClick = {
                        val uri = createCameraOutputUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                      }
                    ) {
                      Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White)
                      Spacer(Modifier.width(6.dp))
                      Text("Camera", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          } else {
            // Image preview area when photo is selected
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = YogaThemeColors.CardBg),
                border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
              ) {
                Box(
                  modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                  contentAlignment = Alignment.Center,
                ) {
                  val display = annotatedBitmap ?: originalBitmap
                  when {
                    isDetecting -> {
                      Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = YogaThemeColors.SagePrimary)
                        Spacer(Modifier.size(8.dp))
                        Text("Running pose detection…", style = MaterialTheme.typography.bodySmall)
                      }
                    }
                    display != null -> {
                      Image(
                        bitmap = display.asImageBitmap(),
                        contentDescription = "Annotated yoga pose",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                      )
                      if (aiResponse.isNotBlank()) {
                        Column(
                          modifier =
                            Modifier.fillMaxWidth()
                              .align(Alignment.TopCenter)
                              .padding(12.dp)
                              .clip(RoundedCornerShape(16.dp))
                              .background(Color.Black.copy(alpha = 0.70f))
                              .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            Text(
                              "SUKHAM's analysis",
                              style =
                                MaterialTheme.typography.labelSmall.copy(
                                  color = Color(0xFFE1BEE7),
                                  fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            Surface(
                              shape = RoundedCornerShape(percent = 50),
                              color = if (ttsHelper.isSpeaking.value) Color(0xFFCE93D8) else Color(0x33FFFFFF),
                              modifier = Modifier.clickable {
                                if (ttsHelper.isSpeaking.value) {
                                  ttsHelper.stop()
                                } else {
                                  ttsHelper.speak(aiResponse)
                                }
                              }
                            ) {
                              Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                              ) {
                                Icon(
                                  imageVector = if (ttsHelper.isSpeaking.value) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                  contentDescription = if (ttsHelper.isSpeaking.value) "Stop voice" else "Read aloud",
                                  tint = if (ttsHelper.isSpeaking.value) Color.Black else Color.White,
                                  modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                  text = if (ttsHelper.isSpeaking.value) "Stop" else "Read Aloud",
                                  style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (ttsHelper.isSpeaking.value) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                  )
                                )
                              }
                            }
                          }
                          Spacer(Modifier.size(4.dp))
                          Text(
                            aiResponse,
                            style =
                              MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                              ),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            detectionError?.let { err -> item { Text(err, color = MaterialTheme.colorScheme.error) } }

            if (annotatedBitmap != null) {
              item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  OutlinedButton(
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(percent = 50),
                    border = BorderStroke(1.dp, YogaThemeColors.SagePrimary),
                    onClick = {
                      val bmp = annotatedBitmap ?: return@OutlinedButton
                      scope.launch(Dispatchers.IO) {
                        val uri = saveAnnotatedBitmap(context, bmp)
                        withContext(Dispatchers.Main) {
                          savedUri = uri
                          Toast.makeText(
                              context,
                              if (uri != null) "Saved to Pictures/SukhamAI" else "Save failed",
                              Toast.LENGTH_SHORT,
                            )
                            .show()
                        }
                      }
                    },
                  ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, tint = YogaThemeColors.SagePrimary)
                    Spacer(Modifier.size(8.dp))
                    Text(if (savedUri == null) "Save" else "Saved", color = YogaThemeColors.SageDark, fontWeight = FontWeight.Bold)
                  }
                  OutlinedButton(
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(percent = 50),
                    border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
                    onClick = {
                      ttsHelper.stop()
                      originalBitmap = null
                      annotatedBitmap = null
                      poseResult = null
                      aiResponse = ""
                      savedUri = null
                    },
                  ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = YogaThemeColors.BodyGray)
                    Spacer(Modifier.size(8.dp))
                    Text("New photo", color = YogaThemeColors.BodyGray, fontWeight = FontWeight.Bold)
                  }
                }
              }

              item {
                Button(
                  modifier = Modifier.fillMaxWidth().height(52.dp),
                  shape = RoundedCornerShape(percent = 50),
                  enabled = !aiInFlight && !pendingInference && annotatedBitmap != null,
                  colors =
                    ButtonDefaults.buttonColors(containerColor = YogaThemeColors.SagePrimary),
                  onClick = {
                    if (!ensureModelOrToast()) return@Button
                    aiResponse = ""
                    rawResponse = ""
                    pendingInference = true
                  },
                ) {
                  if (pendingInference || aiInFlight) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(20.dp),
                      color = Color.White,
                      strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(10.dp))
                    val label =
                      when {
                        pendingInference && initStatus != ModelInitializationStatusType.INITIALIZED ->
                          "Warming up Gemma…"
                        aiResponse.isBlank() -> "Looking at the pose…"
                        else -> "Coaching…"
                      }
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                  } else {
                    Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.size(8.dp))
                    Text("Get Yoga Coaching", color = Color.White, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        YogaMode.LIVE -> {
          if (!sessionActive) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = YogaThemeColors.CardBg),
                border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Image(
                    painter = painterResource(R.drawable.live_ai_coach_hero),
                    contentDescription = "Live AI Coach",
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(185.dp)
                      .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  Text(
                    text = "Real-Time Movement Guidance",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = YogaThemeColors.TitleBlack,
                      fontSize = 17.sp
                    )
                  )

                  Spacer(modifier = Modifier.height(6.dp))

                  Text(
                    text = "Position your phone 5–8 ft away on a stable surface. Sukham AI tracks 33 skeletal landmarks in real-time, detecting repetitions, hold stability, and postural deviations with instant audio feedback.",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = YogaThemeColors.BodyGray,
                      fontSize = 12.sp,
                      textAlign = TextAlign.Center,
                      lineHeight = 16.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    AuditFeatureRow("33-Point skeletal landmark tracking at 30+ FPS")
                    AuditFeatureRow("Instant audio & visual posture correction cues")
                    AuditFeatureRow("Supports Gym reps, Yoga poses, and Dance flow")
                    AuditFeatureRow("100% Private — processed on-device with Gemma")
                  }

                  Spacer(modifier = Modifier.height(16.dp))

                  // Camera Choice Toggle (Front / Back camera)
                  Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = YogaThemeColors.ChipBg,
                    border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(3.dp),
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Surface(
                        modifier = Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(percent = 50))
                          .clickable { useFrontCamera = true },
                        shape = RoundedCornerShape(percent = 50),
                        color = if (useFrontCamera) YogaThemeColors.SagePrimary else Color.Transparent
                      ) {
                        Text(
                          text = "Front Camera (Selfie)",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (useFrontCamera) FontWeight.Bold else FontWeight.Medium,
                            color = if (useFrontCamera) Color.White else YogaThemeColors.BodyGray,
                            fontSize = 11.5.sp
                          ),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.padding(vertical = 8.dp)
                        )
                      }

                      Surface(
                        modifier = Modifier
                          .weight(1f)
                          .clip(RoundedCornerShape(percent = 50))
                          .clickable { useFrontCamera = false },
                        shape = RoundedCornerShape(percent = 50),
                        color = if (!useFrontCamera) YogaThemeColors.SagePrimary else Color.Transparent
                      ) {
                        Text(
                          text = "Rear Camera (Main)",
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (!useFrontCamera) FontWeight.Bold else FontWeight.Medium,
                            color = if (!useFrontCamera) Color.White else YogaThemeColors.BodyGray,
                            fontSize = 11.5.sp
                          ),
                          textAlign = TextAlign.Center,
                          modifier = Modifier.padding(vertical = 8.dp)
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(16.dp))

                  Button(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(50.dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(containerColor = YogaThemeColors.SagePrimary),
                    onClick = {
                      if (!ensureModelOrToast()) return@Button
                      engine.reset()
                      holdDetector.reset()
                      latestFrame.set(null)
                      pendingSessionStart = true
                    }
                  ) {
                    if (pendingSessionStart) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                      )
                      Spacer(Modifier.size(10.dp))
                      Text("Warming up Gemma…", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                      Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                      )
                      Spacer(Modifier.width(8.dp))
                      Text(
                        text = "Start Live Session",
                        style = MaterialTheme.typography.labelLarge.copy(
                          fontWeight = FontWeight.Bold,
                          color = Color.White,
                          fontSize = 14.5.sp
                        )
                      )
                    }
                  }
                }
              }
            }
          } else {
            // Live Session is active! Full camera view
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
              ) {
                Box(
                  modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                  contentAlignment = Alignment.Center,
                ) {
                  val selector =
                    if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA
                  key(selector) {
                    LivePosePreview(
                      cameraSelector = selector,
                      onFrame = { bitmap -> latestFrame.set(bitmap) },
                      modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                      onPose = { result ->
                        val decision =
                          holdDetector.onFrame(
                            result = result,
                            nowMs = System.currentTimeMillis(),
                            engineBusy = engineBusy,
                          )
                        holdState = decision.state
                        holdProgress = decision.holdProgress
                        detectedPose = decision.topPoseName
                        val candidates = decision.trigger
                        val model = imageModel
                        val frame = latestFrame.get()
                        if (candidates != null && model != null && frame != null) {
                          val stamp = System.currentTimeMillis() - sessionStartMs
                          scope.launch { engine.offerFrame(model, stamp, frame, candidates) }
                        }
                      },
                    )
                  }
                  SessionOverlay(
                    elapsedMs = elapsedMs,
                    analyzing = engineBusy,
                    holdState = holdState,
                    holdProgress = holdProgress,
                    detectedPose = detectedPose,
                    isVoiceCoachEnabled = isReadAloudEnabled,
                    isSpeaking = ttsHelper.isSpeaking.value,
                    modifier = Modifier.align(Alignment.TopCenter),
                  )
                  IconButton(
                    onClick = { useFrontCamera = !useFrontCamera },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                  ) {
                    Icon(
                      Icons.Outlined.Cameraswitch,
                      contentDescription = "Switch camera",
                      tint = Color.White,
                    )
                  }
                }
              }
            }

            item {
              Button(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = { stopSession() }
              ) {
                Icon(
                  imageVector = Icons.Outlined.Stop,
                  contentDescription = null,
                  tint = Color.White
                )
                Spacer(Modifier.size(8.dp))
                Text(
                  text = "Stop Session",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }
            }

            timelineItems(
              cards = cards,
              skippedTicks = skippedTicks,
              newestFirst = true,
              onSpeak = { text ->
                if (ttsHelper.isSpeaking.value) {
                  ttsHelper.stop()
                } else {
                  ttsHelper.speak(text)
                }
              },
              isSpeaking = ttsHelper.isSpeaking.value,
            )
          }
        }

        YogaMode.VIDEO -> {
          if (videoUri == null) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = YogaThemeColors.CardBg),
                border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Image(
                    painter = painterResource(R.drawable.video_motion_audit_hero),
                    contentDescription = "Video Motion Audit",
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(180.dp)
                      .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  Text(
                    text = "Frame-by-Frame Motion Audit",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = YogaThemeColors.TitleBlack,
                      fontSize = 16.5.sp
                    )
                  )

                  Spacer(modifier = Modifier.height(6.dp))

                  Text(
                    text = "Select any recorded workout, yoga flow, or gym set. Sukham AI breaks the video down into kinetic keyframes, detects deviations, and builds an on-device posture timeline.",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = YogaThemeColors.BodyGray,
                      fontSize = 12.sp,
                      textAlign = TextAlign.Center,
                      lineHeight = 16.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                  )

                  Spacer(modifier = Modifier.height(14.dp))

                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    AuditFeatureRow("33-Point skeletal joint angle tracking")
                    AuditFeatureRow("Spine alignment & kinetic deviation scoring")
                    AuditFeatureRow("100% Private — processed on-device with Gemma")
                  }

                  Spacer(modifier = Modifier.height(16.dp))

                  Button(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(50.dp),
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(containerColor = YogaThemeColors.LavenderPrimary),
                    onClick = {
                      videoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                          ActivityResultContracts.PickVisualMedia.VideoOnly
                        )
                      )
                    }
                  ) {
                    Icon(Icons.Outlined.Videocam, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                      "Choose Video from Gallery",
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      fontSize = 14.sp
                    )
                  }
                }
              }
            }
          } else {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = YogaThemeColors.CardBg),
                border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  val analyzed = cards.size
                  val total = samplePositions.size
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = YogaThemeColors.LavenderPrimary,
                        modifier = Modifier.size(18.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        "Clip Selected",
                        style = MaterialTheme.typography.titleSmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = YogaThemeColors.TitleBlack
                        )
                      )
                    }
                    Surface(
                      color = YogaThemeColors.LavenderSoft,
                      shape = RoundedCornerShape(percent = 50)
                    ) {
                      Text(
                        text = formatTimestamp(videoDurationMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = YogaThemeColors.LavenderPrimary,
                          fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  }

                  Text(
                    text = "$total keyframes sampled (every ${videoIntervalMs / 1000}s)" +
                      (if (sessionActive) " · $analyzed/$total analyzed" else ""),
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = YogaThemeColors.BodyGray,
                      fontSize = 12.sp
                    )
                  )

                  if (sessionActive && total > 0) {
                    LinearProgressIndicator(
                      progress = { analyzed.toFloat() / total.toFloat() },
                      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                      color = YogaThemeColors.SagePrimary,
                      trackColor = YogaThemeColors.ChipBg,
                    )
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedButton(
                      modifier = Modifier.weight(1f).height(46.dp),
                      shape = RoundedCornerShape(percent = 50),
                      border = BorderStroke(1.dp, YogaThemeColors.CardBorder),
                      onClick = {
                        videoPickerLauncher.launch(
                          androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.VideoOnly
                          )
                        )
                      }
                    ) {
                      Text("Change Video", color = YogaThemeColors.BodyGray, fontSize = 12.sp)
                    }

                    Button(
                      modifier = Modifier.weight(1.3f).height(46.dp),
                      shape = RoundedCornerShape(percent = 50),
                      enabled = samplePositions.isNotEmpty(),
                      colors =
                        ButtonDefaults.buttonColors(
                          containerColor =
                            if (sessionActive) MaterialTheme.colorScheme.error
                            else YogaThemeColors.SagePrimary
                        ),
                      onClick = {
                        if (sessionActive) {
                          stopSession()
                        } else {
                          if (!ensureModelOrToast()) return@Button
                          engine.reset()
                          pendingSessionStart = true
                        }
                      },
                    ) {
                      if (pendingSessionStart && !sessionActive) {
                        CircularProgressIndicator(
                          modifier = Modifier.size(16.dp),
                          color = Color.White,
                          strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Starting…", color = Color.White, fontSize = 12.5.sp)
                      } else {
                        Icon(
                          if (sessionActive) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                          contentDescription = null,
                          tint = Color.White,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                          if (sessionActive) "Stop Audit" else "Analyze Video",
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          fontSize = 13.sp
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          timelineItems(
            cards = cards,
            skippedTicks = 0,
            onSpeak = { text ->
              if (ttsHelper.isSpeaking.value) {
                ttsHelper.stop()
              } else {
                ttsHelper.speak(text)
              }
            },
            isSpeaking = ttsHelper.isSpeaking.value,
          )
        }
      }
    }
  }

  // Pre-warm the multimodal Gemma in the background as soon as the screen opens so the first
  // "Get Yoga Coaching" tap doesn't pay the ~30 s init cost. Safe to call multiple times —
  // MMVM short-circuits when the model is already initialized or initializing.
  LaunchedEffect(imageModel?.name, uiState.loadingModelAllowlist) {
    val task = imageChatTask
    val model = imageModel
    if (
      task != null &&
        model != null &&
        !uiState.loadingModelAllowlist &&
        uiState.modelDownloadStatus[model.name]?.status == ModelDownloadStatusType.SUCCEEDED &&
        uiState.modelInitializationStatus[model.name]?.status !=
          ModelInitializationStatusType.INITIALIZED
    ) {
      Log.d(TAG, "Pre-warming image Gemma in background.")
      modelManagerViewModel.initializeModel(context = context, task = task, model = model)
    }
  }

  // Reactive inference runner for the photo flow. Watches the model init status; the moment the
  // user has requested inference (pendingInference == true) and the model reports INITIALIZED,
  // we kick off runInference once. This avoids relying on initializeModel's onDone callback,
  // which silently skips when the model is already initialized or initialization is mid-flight.
  LaunchedEffect(pendingInference, initStatus) {
    if (
      pendingInference &&
        initStatus == ModelInitializationStatusType.INITIALIZED &&
        imageModel != null &&
        !aiInFlight
    ) {
      val bmp = annotatedBitmap
      if (bmp == null) {
        pendingInference = false
        return@LaunchedEffect
      }
      val model = imageModel
      val gemmaInput = downscaleForInference(bmp, GEMMA_INPUT_MAX_EDGE)
      pendingInference = false
      aiInFlight = true
      earlyStopFired = false
      lastInferenceStart = System.currentTimeMillis()
      // NOTE: deliberately no resetConversation here. Tearing down and rebuilding the
      // conversation re-initializes the vision stack on every photo, which cost far more than
      // the stale-context it was meant to avoid. The original one-shot flow reused the
      // pre-warmed conversation and was fast; keep it that way.
      // poseResult was previously computed and discarded; it now drives the geometric ranking
      // that the prompt is built around.
      val candidates = poseResult?.let { PoseClassifier.classify(it) } ?: emptyList()
      val geometry = poseResult?.let { PoseClassifier.describeGeometry(it) } ?: emptyList()
      val deviations =
        poseResult?.let { r ->
          candidates.firstOrNull()?.let { PoseClassifier.formDeviations(r, it.name) }
        } ?: emptyList()
      Log.d(
        TAG,
        "Pose candidates: " + candidates.joinToString { "${it.name}=%.2f".format(it.score) },
      )
      Log.d(TAG, "Running inference on ${gemmaInput.width}x${gemmaInput.height} bitmap")
      var firstTokenLogged = false
      model.runtimeHelper.runInference(
        model = model,
        input = buildYogaPrompt(candidates, geometry, deviations),
        images = listOf(gemmaInput),
        resultListener = { partial, done, _ ->
          if (!firstTokenLogged) {
            firstTokenLogged = true
            Log.d(
              TAG,
              "TIMING first token after " +
                "${System.currentTimeMillis() - lastInferenceStart}ms (prefill)",
            )
          }
          if (done) {
            Log.d(
              TAG,
              "TIMING photo inference total " +
                "${System.currentTimeMillis() - lastInferenceStart}ms",
            )
          }
          rawResponse += partial
          aiResponse = cleanCoachingText(rawResponse)
          val tooLong = rawResponse.length > MAX_RESPONSE_CHARS
          val tookTooLong = System.currentTimeMillis() - lastInferenceStart > MAX_RESPONSE_MS
          if (!earlyStopFired && (tooLong || tookTooLong)) {
            earlyStopFired = true
            Log.d(
              TAG,
              "Early-stopping generation (chars=${aiResponse.length}," +
                " elapsedMs=${System.currentTimeMillis() - lastInferenceStart})",
            )
            model.runtimeHelper.stopResponse(model)
            if (isReadAloudEnabled && aiResponse.isNotBlank()) {
              ttsHelper.speak(aiResponse)
            }
          }
          if (done) {
            aiInFlight = false
            if (isReadAloudEnabled && aiResponse.isNotBlank()) {
              ttsHelper.speak(aiResponse)
            }
          }
        },
        cleanUpListener = { aiInFlight = false },
        onError = { msg ->
          aiInFlight = false
          aiResponse = "Error: $msg"
        },
        coroutineScope = scope,
      )
    }
  }

  // Session starter: flips pendingSessionStart into a running session once Gemma is ready.
  LaunchedEffect(pendingSessionStart, initStatus) {
    if (pendingSessionStart && initStatus == ModelInitializationStatusType.INITIALIZED) {
      pendingSessionStart = false
      sessionStartMs = System.currentTimeMillis()
      elapsedMs = 0L
      sessionActive = true
    }
  }

  // Live mode has no sampling timer any more: PoseHoldDetector decides when a frame is worth
  // analysing (settled, a different pose, confidently classified) from the preview's own
  // detections. All that remains here is the session clock for the overlay.
  LaunchedEffect(sessionActive, mode) {
    if (!sessionActive || mode != YogaMode.LIVE) return@LaunchedEffect
    while (isActive) {
      delay(1_000)
      elapsedMs = System.currentTimeMillis() - sessionStartMs
    }
  }

  // Live session voice coach: automatically speaks posture analysis aloud when a pose frame
  // finishes coaching inference, honoring the Read Aloud toggle from Profile Settings.
  LaunchedEffect(cards, isReadAloudEnabled, sessionActive, mode) {
    if (!sessionActive || mode != YogaMode.LIVE) {
      if (!sessionActive) {
        lastSpokenLiveCardId = -1L
      }
      return@LaunchedEffect
    }

    if (!isReadAloudEnabled) {
      if (ttsHelper.isSpeaking.value) {
        ttsHelper.stop()
      }
      return@LaunchedEffect
    }

    val latestCompletedCard = cards.lastOrNull { it.status == CoachingStatus.DONE && it.text.isNotBlank() }
    if (latestCompletedCard != null && latestCompletedCard.id != lastSpokenLiveCardId) {
      lastSpokenLiveCardId = latestCompletedCard.id
      ttsHelper.speak(latestCompletedCard.text)
    }
  }

  // Video pass. Frames are decoded and analyzed strictly in order; unlike the live loop nothing
  // is dropped, since there is no real-time clock to fall behind.
  LaunchedEffect(sessionActive, mode) {
    if (!sessionActive || mode != YogaMode.VIDEO) return@LaunchedEffect
    val model = imageModel ?: return@LaunchedEffect
    val uri = videoUri ?: return@LaunchedEffect
    val positions = samplePositions
    val job = launch {
      try {
        VideoFrameSampler.forEachFrame(
          context = context,
          uri = uri,
          positions = positions,
          onFrame = { sampled -> engine.analyzeFrame(model, sampled.timestampMs, sampled.bitmap) },
        )
      } catch (c: CancellationException) {
        // The user hit Stop (or switched modes). Not an error — let it unwind quietly.
        throw c
      } catch (t: Throwable) {
        Log.e(TAG, "Video analysis failed", t)
        Toast.makeText(context, t.message ?: "Video analysis failed", Toast.LENGTH_LONG).show()
      } finally {
        // NonCancellable so the button still flips back to "Analyze video" on the cancel path;
        // a plain state write here would be skipped once the coroutine is already cancelling.
        withContext(NonCancellable) { sessionActive = false }
      }
    }
    videoJob = job
  }
}

/**
 * Status readout over the live preview.
 *
 * Shows what the gate is waiting on rather than a countdown, because there is no timer any
 * more — analysis fires when the person settles into a new pose. Telling them "hold still" is
 * also the instruction that actually makes it fire, so the hint doubles as coaching.
 */
@Composable
private fun SessionOverlay(
  elapsedMs: Long,
  analyzing: Boolean,
  holdState: PoseHoldDetector.State,
  holdProgress: Float,
  detectedPose: String?,
  isVoiceCoachEnabled: Boolean = false,
  isSpeaking: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val status =
    when {
      analyzing -> "analysing\u2026"
      holdState == PoseHoldDetector.State.NO_PERSON -> "step into frame"
      holdState == PoseHoldDetector.State.MOVING -> "hold your pose"
      holdState == PoseHoldDetector.State.SETTLING ->
        "holding\u2026 ${(holdProgress * 100).toInt()}%"
      holdState == PoseHoldDetector.State.READY -> "got it"
      else -> detectedPose?.substringBefore(" (") ?: "move to a new pose"
    }
  Column(
    modifier =
      modifier
        .padding(12.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.Black.copy(alpha = 0.55f))
        .padding(horizontal = 14.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        "\u25cf ${formatTimestamp(elapsedMs)}",
        style =
          MaterialTheme.typography.labelMedium.copy(
            color = Color(0xFFFF8A80),
            fontWeight = FontWeight.Bold,
          ),
      )
      Text(status, style = MaterialTheme.typography.labelMedium.copy(color = Color.White))
      if (isVoiceCoachEnabled) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (isSpeaking) Color(0xFF2E7D32).copy(alpha = 0.85f) else Color(0x33FFFFFF))
            .padding(horizontal = 6.dp, vertical = 2.dp),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
              contentDescription = "Voice Guidance Active",
              tint = if (isSpeaking) Color.White else Color(0xFFE1BEE7),
              modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
              text = if (isSpeaking) "Speaking" else "Voice On",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSpeaking) Color.White else Color(0xFFE1BEE7),
              ),
            )
          }
        }
      }
    }
    if (holdState == PoseHoldDetector.State.SETTLING) {
      Spacer(Modifier.size(6.dp))
      LinearProgressIndicator(
        progress = { holdProgress },
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF00E5FF),
      )
    }
  }
}

/** Shared coaching timeline, used by both the live and video modes. */
private fun LazyListScope.timelineItems(
  cards: List<CoachingCard>,
  skippedTicks: Int,
  /**
   * Live sessions show the newest coaching first: the person is mid-practice and wants the cue
   * that just arrived, not to scroll past everything they already did. Video keeps chronological
   * order, where the point is reading the clip start to finish.
   */
  newestFirst: Boolean = false,
  onSpeak: ((String) -> Unit)? = null,
  isSpeaking: Boolean = false,
) {
  if (cards.isEmpty()) return
  item {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        "Coaching timeline",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
      if (skippedTicks > 0) {
        Spacer(Modifier.size(8.dp))
        Text(
          "· $skippedTicks skipped while busy",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
  val ordered = if (newestFirst) cards.asReversed() else cards
  items(items = ordered, key = { it.id }) { card ->
    CoachingCardRow(
      card = card,
      onSpeak = onSpeak,
      isSpeaking = isSpeaking,
    )
  }
}

private fun handleNewImage(
  context: Context,
  uri: Uri,
  poseHelper: PoseLandmarkerHelper,
  onStart: () -> Unit,
  onSuccess: (Bitmap, Bitmap, PoseLandmarkerResult) -> Unit,
  onError: (String) -> Unit,
  scope: kotlinx.coroutines.CoroutineScope,
) {
  onStart()
  scope.launch(Dispatchers.IO) {
    try {
      val source = loadBitmap(context, uri) ?: throw IllegalStateException("Unable to read image")
      val scaled = downscaleForInference(source, maxEdge = LANDMARK_INPUT_MAX_EDGE)
      poseHelper.setup()
      val result = poseHelper.detect(scaled) ?: throw IllegalStateException("Pose detection failed")
      if (result.landmarks().isEmpty()) {
        withContext(Dispatchers.Main) {
          onError("No person detected in the image. Try a clearer full-body shot.")
        }
        return@launch
      }
      val annotated = PoseOverlay.drawOverlay(scaled, result)
      withContext(Dispatchers.Main) { onSuccess(scaled, annotated, result) }
    } catch (t: Throwable) {
      Log.e(TAG, "handleNewImage failed", t)
      withContext(Dispatchers.Main) { onError(t.message ?: "Unknown error") }
    }
  }
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? =
  try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val src = ImageDecoder.createSource(context.contentResolver, uri)
      ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = true
      }
    } else {
      context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
  } catch (t: Throwable) {
    Log.e(TAG, "loadBitmap failed for $uri", t)
    null
  }

private fun createCameraOutputUri(context: Context): Uri {
  val dir = File(context.cacheDir, "yoga").apply { mkdirs() }
  val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
  return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

private fun saveAnnotatedBitmap(context: Context, bitmap: Bitmap): Uri? {
  val filename = "SukhamAI_Yoga_${System.currentTimeMillis()}.jpg"
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val values =
      ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SukhamAI")
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    val out: OutputStream = resolver.openOutputStream(uri) ?: return null
    out.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(uri, values, null, null)
    uri
  } else {
    @Suppress("DEPRECATION")
    val dir =
      File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SukhamAI")
        .apply { mkdirs() }
    val file = File(dir, filename)
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    Uri.fromFile(file)
  }
}
