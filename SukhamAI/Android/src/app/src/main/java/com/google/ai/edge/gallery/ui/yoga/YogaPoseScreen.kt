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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
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

private const val TAG = "YogaPoseScreen"

// Soft cap for the one-shot photo flow. More generous than the session cap in YogaCoachEngine
// because a stalled single photo only keeps the user waiting; it doesn't block later frames.
private const val MAX_RESPONSE_MS = 90_000L

/** Which frame source is feeding the coach. */
private enum class YogaMode(val label: String) {
  PHOTO("Photo"),
  LIVE("Live session"),
  VIDEO("Upload video"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YogaPoseScreen(modelManagerViewModel: ModelManagerViewModel, onNavigateUp: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uiState by modelManagerViewModel.uiState.collectAsState()

  val imageChatTask: Task? = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_ASK_IMAGE)
  val imageModel: Model? = imageChatTask?.let { modelManagerViewModel.resolveModelForTask(it) }

  val poseHelper = remember { PoseLandmarkerHelper(context.applicationContext) }
  val engine = remember(poseHelper) { YogaCoachEngine(poseHelper, scope) }
  DisposableEffect(Unit) { onDispose { poseHelper.close() } }

  var mode by remember { mutableStateOf(YogaMode.PHOTO) }

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
          "Multimodal Gemma not available. Open Ask Image once to install it.",
          Toast.LENGTH_LONG,
        )
        .show()
      return false
    }
    if (!modelDownloaded) {
      Toast.makeText(
          context,
          "Download the multimodal Gemma from \"Ask SUKHAM about an Image\" first.",
          Toast.LENGTH_LONG,
        )
        .show()
      return false
    }
    modelManagerViewModel.initializeModel(context = context, task = task, model = model)
    return true
  }

  fun stopSession() {
    sessionActive = false
    pendingSessionStart = false
    videoJob?.cancel()
    videoJob = null
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
    topBar = {
      TopAppBar(
        title = { Text("Yoga Coach") },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
      )
    }
  ) { padding ->
    // A LazyColumn rather than verticalScroll: the live/video timelines grow unbounded and a
    // scrollable list can't be nested inside a scrolling parent.
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(vertical = 16.dp),
    ) {
      item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          YogaMode.entries.forEach { entry ->
            FilterChip(
              selected = mode == entry,
              onClick = {
                if (mode != entry) {
                  stopSession()
                  mode = entry
                }
              },
              label = { Text(entry.label) },
            )
          }
        }
      }

      item {
        Text(
          when (mode) {
            YogaMode.PHOTO ->
              "Capture or upload a photo of yourself in a yoga pose. SUKHAM will draw the body " +
                "skeleton and have on-device Gemma suggest the closest asana and corrections."
            YogaMode.LIVE ->
              "Start a session and hold a standing pose. Once you settle, SUKHAM reads your " +
                "skeleton and has on-device Gemma coach that pose — all offline. Move to a new " +
                "pose for the next tip."
            YogaMode.VIDEO ->
              "Upload a yoga video. SUKHAM samples evenly spaced frames across the clip and " +
                "builds a timeline of corrections, entirely on-device."
          },
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      when (mode) {
        YogaMode.PHOTO -> {
          item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedButton(
                modifier = Modifier.weight(1f).height(56.dp),
                onClick = {
                  galleryLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                      ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                  )
                },
              ) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Gallery")
              }
              OutlinedButton(
                modifier = Modifier.weight(1f).height(56.dp),
                onClick = {
                  val uri = createCameraOutputUri(context)
                  pendingCameraUri = uri
                  cameraLauncher.launch(uri)
                },
              ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Camera")
              }
            }
          }

          // Image preview area. The AI yoga recommendation is overlaid on top of the photo so
          // the user sees the cue right next to the skeleton it's referring to.
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F4FB)),
            ) {
              Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                contentAlignment = Alignment.Center,
              ) {
                val display = annotatedBitmap ?: originalBitmap
                when {
                  isDetecting -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      CircularProgressIndicator()
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
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                      ) {
                        Text(
                          "SUKHAM's analysis",
                          style =
                            MaterialTheme.typography.labelSmall.copy(
                              color = Color(0xFFE1BEE7),
                              fontWeight = FontWeight.SemiBold,
                            ),
                        )
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
                  else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Icon(
                        Icons.Outlined.SelfImprovement,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                      )
                      Spacer(Modifier.size(8.dp))
                      Text(
                        "Pick a photo to begin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
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
                  modifier = Modifier.weight(1f).height(52.dp),
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
                  Icon(Icons.Outlined.Save, contentDescription = null)
                  Spacer(Modifier.size(8.dp))
                  Text(if (savedUri == null) "Save" else "Saved")
                }
                OutlinedButton(
                  modifier = Modifier.weight(1f).height(52.dp),
                  onClick = {
                    originalBitmap = null
                    annotatedBitmap = null
                    poseResult = null
                    aiResponse = ""
                    savedUri = null
                  },
                ) {
                  Icon(Icons.Outlined.Refresh, contentDescription = null)
                  Spacer(Modifier.size(8.dp))
                  Text("New photo")
                }
              }
            }

            item {
              Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !aiInFlight && !pendingInference && annotatedBitmap != null,
                colors =
                  ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                  Text(label)
                } else {
                  Icon(Icons.Outlined.SelfImprovement, contentDescription = null)
                  Spacer(Modifier.size(8.dp))
                  Text("Get Yoga Coaching")
                }
              }
            }
          }
        }

        YogaMode.LIVE -> {
          item {
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F4FB)),
            ) {
              Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                contentAlignment = Alignment.Center,
              ) {
                if (sessionActive) {
                  val selector =
                    if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                    else CameraSelector.DEFAULT_BACK_CAMERA
                  // key() forces a fresh preview when the lens flips — the camera is bound in a
                  // LaunchedEffect(Unit) underneath, so it won't rebind on its own.
                  key(selector) {
                    LivePosePreview(
                      cameraSelector = selector,
                      // Held in an AtomicReference, not Compose state: frames arrive at ~30fps
                      // and writing state here would recompose the whole screen every frame.
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
                } else {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                      Icons.Outlined.SelfImprovement,
                      contentDescription = null,
                      modifier = Modifier.size(48.dp),
                      tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                      "Start a session to begin",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }

          item {
            Button(
              modifier = Modifier.fillMaxWidth().height(56.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor =
                    if (sessionActive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                ),
              onClick = {
                if (sessionActive) {
                  stopSession()
                } else {
                  if (!ensureModelOrToast()) return@Button
                  engine.reset()
                  holdDetector.reset()
                  latestFrame.set(null)
                  pendingSessionStart = true
                }
              },
            ) {
              if (pendingSessionStart && !sessionActive) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = Color.White,
                  strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(10.dp))
                Text("Warming up Gemma…")
              } else {
                Icon(
                  if (sessionActive) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                  contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(if (sessionActive) "Stop session" else "Start live session")
              }
            }
          }

          timelineItems(cards = cards, skippedTicks = skippedTicks, newestFirst = true)
        }

        YogaMode.VIDEO -> {
          item {
            OutlinedButton(
              modifier = Modifier.fillMaxWidth().height(56.dp),
              onClick = {
                videoPickerLauncher.launch(
                  androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.VideoOnly
                  )
                )
              },
            ) {
              Icon(Icons.Outlined.Videocam, contentDescription = null)
              Spacer(Modifier.size(8.dp))
              Text(if (videoUri == null) "Choose a video" else "Choose a different video")
            }
          }

          if (videoUri != null) {
            item {
              val analyzed = cards.size
              val total = samplePositions.size
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                  "Clip ${formatTimestamp(videoDurationMs)} · $total frame" +
                    (if (total == 1) "" else "s") +
                    " (one every ${videoIntervalMs / 1000}s)" +
                    (if (sessionActive) " · $analyzed/$total done" else ""),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (sessionActive && total > 0) {
                  LinearProgressIndicator(
                    progress = { analyzed.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }
            }

            item {
              Button(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = samplePositions.isNotEmpty(),
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor =
                      if (sessionActive) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.primary
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
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                  )
                  Spacer(Modifier.size(10.dp))
                  Text("Warming up Gemma…")
                } else {
                  Icon(
                    if (sessionActive) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                  )
                  Spacer(Modifier.size(8.dp))
                  Text(if (sessionActive) "Stop" else "Analyze video")
                }
              }
            }
          }

          timelineItems(cards = cards, skippedTicks = 0)
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
          }
          if (done) aiInFlight = false
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
  modifier: Modifier = Modifier,
) {
  val status =
    when {
      analyzing -> "analysing\u2026"
      holdState == PoseHoldDetector.State.NO_PERSON -> "step into frame"
      holdState == PoseHoldDetector.State.NOT_STANDING -> "stand up to be coached"
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
      horizontalArrangement = Arrangement.spacedBy(10.dp),
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
  items(items = ordered, key = { it.id }) { card -> CoachingCardRow(card) }
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
