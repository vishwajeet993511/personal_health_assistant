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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "YogaPoseScreen"

// Image edge cap for the multimodal Gemma input. Pose detection still runs on the larger
// LANDMARK_INPUT_MAX_EDGE bitmap so accuracy isn't sacrificed.
private const val GEMMA_INPUT_MAX_EDGE = 512
private const val LANDMARK_INPUT_MAX_EDGE = 1024

// Soft caps so a runaway generation doesn't keep the user staring at "Thinking…".
private const val MAX_RESPONSE_CHARS = 700
private const val MAX_RESPONSE_MS = 90_000L

// Compact prompt — coaching first, so even if the user only reads the first line they get the
// actionable cue. Still 3 short bullets to keep generation under ~60 words so the model emits
// an EOS quickly instead of filling the context window.
private const val YOGA_PROMPT =
  "Skeleton overlay shows a yoga pose. Reply in under 60 words as 3 bullets, in this order:\n" +
    "- Coaching: one specific cue to improve form right now\n" +
    "- Pose: <Sanskrit / English name>\n" +
    "- Form: <score>/10"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YogaPoseScreen(
  modelManagerViewModel: ModelManagerViewModel,
  onNavigateUp: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val uiState by modelManagerViewModel.uiState.collectAsState()

  val imageChatTask: Task? = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_ASK_IMAGE)
  val imageModel: Model? = imageChatTask?.let { modelManagerViewModel.resolveModelForTask(it) }

  var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var annotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var poseResult by remember { mutableStateOf<PoseLandmarkerResult?>(null) }
  var isDetecting by remember { mutableStateOf(false) }
  var detectionError by remember { mutableStateOf<String?>(null) }
  var aiResponse by remember { mutableStateOf("") }
  var aiInFlight by remember { mutableStateOf(false) }
  // True when the user has clicked "Get Yoga Coaching" and we are waiting for the multimodal
  // Gemma to finish initializing before kicking off inference. Inference is started reactively
  // by a LaunchedEffect that watches modelInitializationStatus, so we don't depend on
  // initializeModel's onDone callback (which it silently skips when the model is already
  // initialized or initialization is mid-flight — the source of the original stuck spinner).
  var pendingInference by remember { mutableStateOf(false) }
  var savedUri by remember { mutableStateOf<Uri?>(null) }
  var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
  var lastInferenceStart by remember { mutableStateOf(0L) }
  var earlyStopFired by remember { mutableStateOf(false) }

  val poseHelper = remember { PoseLandmarkerHelper(context.applicationContext) }
  DisposableEffect(Unit) { onDispose { poseHelper.close() } }

  val initStatus = imageModel?.let { uiState.modelInitializationStatus[it.name]?.status }

  val galleryLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri ?: return@rememberLauncherForActivityResult
      handleNewImage(
        context = context,
        uri = uri,
        poseHelper = poseHelper,
        onStart = { isDetecting = true; detectionError = null; aiResponse = "" },
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
          onStart = { isDetecting = true; detectionError = null; aiResponse = "" },
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
    },
  ) { padding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        "Capture or upload a photo of yourself in a yoga pose. SUKHAM will draw the body " +
          "skeleton and have on-device Gemma suggest the closest asana and corrections.",
        style = MaterialTheme.typography.bodyMedium,
      )

      // Action buttons.
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

      // Image preview area. The AI yoga recommendation is overlaid on top of the photo so
      // the user sees the cue right next to the skeleton it's referring to.
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
                      .padding(horizontal = 14.dp, vertical = 10.dp),
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

      detectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

      // Save + Re-run row (only when we have an annotated image).
      if (annotatedBitmap != null) {
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

        Button(
          modifier = Modifier.fillMaxWidth().height(56.dp),
          enabled = !aiInFlight && !pendingInference && annotatedBitmap != null,
          colors =
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          onClick = {
            val task = imageChatTask
            val model = imageModel
            if (task == null || model == null || annotatedBitmap == null) {
              Toast.makeText(
                  context,
                  "Multimodal Gemma not available. Open Ask Image once to install it.",
                  Toast.LENGTH_LONG,
                )
                .show()
              return@Button
            }
            // Require the model to already be on disk; sending the user to the Ask Image
            // download flow keeps this screen lean.
            val downloadStatus = uiState.modelDownloadStatus[model.name]?.status
            if (downloadStatus != ModelDownloadStatusType.SUCCEEDED) {
              Toast.makeText(
                  context,
                  "Download the multimodal Gemma from \"Ask SUKHAM about an Image\" first.",
                  Toast.LENGTH_LONG,
                )
                .show()
              return@Button
            }

            aiResponse = ""
            pendingInference = true
            // Trigger initialize even if it's already running / done — the MMVM will short
            // circuit safely. We don't depend on its onDone; the LaunchedEffect below picks
            // up the INITIALIZED status and runs inference then.
            modelManagerViewModel.initializeModel(
              context = context,
              task = task,
              model = model,
            )
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
                pendingInference &&
                  initStatus != ModelInitializationStatusType.INITIALIZED ->
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

  // Reactive inference runner. Watches the model init status; the moment the user has
  // requested inference (pendingInference == true) and the model reports INITIALIZED, we kick
  // off runInference once. This avoids relying on initializeModel's onDone callback, which
  // silently skips when the model is already initialized or initialization is mid-flight.
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
      Log.d(
        TAG,
        "Running inference on ${gemmaInput.width}x${gemmaInput.height} bitmap",
      )
      model.runtimeHelper.runInference(
        model = model,
        input = YOGA_PROMPT,
        images = listOf(gemmaInput),
        resultListener = { partial, done, _ ->
          aiResponse += partial
          val tooLong = aiResponse.length > MAX_RESPONSE_CHARS
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
        withContext(Dispatchers.Main) { onError("No person detected in the image. Try a clearer full-body shot.") }
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

private fun downscaleForInference(source: Bitmap, maxEdge: Int): Bitmap {
  val biggest = maxOf(source.width, source.height)
  if (biggest <= maxEdge) return source.copy(Bitmap.Config.ARGB_8888, false)
  val scale = maxEdge.toFloat() / biggest
  val w = (source.width * scale).toInt()
  val h = (source.height * scale).toInt()
  return Bitmap.createScaledBitmap(source, w, h, true)
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
    val uri =
      resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
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
