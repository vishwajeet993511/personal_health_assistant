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

package com.google.ai.edge.gallery.customtasks.scandocs

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.common.decodeSampledBitmapFromUri
import com.google.ai.edge.gallery.common.rotateBitmap
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.runtime.runtimeHelper
import com.google.ai.edge.gallery.ui.common.MarkdownText
import com.google.ai.edge.gallery.ui.modelmanager.ModelInitializationStatusType
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.litertlm.Contents
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ScanDocsScreen"
private const val MAX_SCAN_DOCS_IMAGE_COUNT = 20
private const val MAX_PDF_PAGES_TO_RENDER = 10
private const val WARMUP_FORCE_RETRY_MS = 20_000L
private const val WARMUP_TIMEOUT_MS = 30_000L
private const val INFERENCE_TIMEOUT_MS = 120_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDocsScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
) {
  val context = LocalContext.current
  val screenScope = rememberCoroutineScope()
  val uiScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
  val inferenceScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
  val uiState by modelManagerViewModel.uiState.collectAsState()
  val downloadedVisionModel =
    task.models.firstOrNull {
      it.llmSupportImage &&
        uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
    }
  val model = downloadedVisionModel ?: modelManagerViewModel.resolveModelForTask(task)
  val initStatus = model?.let { uiState.modelInitializationStatus[it.name]?.status }
  val downloadStatus = model?.let { uiState.modelDownloadStatus[it.name]?.status }
  val inferencePrompt = stringResource(R.string.scan_docs_inference_prompt)

  var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
  var isPreparingPages by remember { mutableStateOf(false) }
  var pendingInference by remember { mutableStateOf(false) }
  var inferenceInFlight by remember { mutableStateOf(false) }
  var analysis by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }
  var statusText by remember { mutableStateOf<String?>(null) }
  var forceRetryTriggered by remember { mutableStateOf(false) }
  var inferenceStartedAtMs by remember { mutableStateOf(0L) }
  var hasAttemptedAnalysis by remember { mutableStateOf(false) }
  var selectedModelHint by remember { mutableStateOf("") }
  var lastPdfPageCount by remember { mutableIntStateOf(0) }
  var isScreenActive by remember { mutableStateOf(true) }

  DisposableEffect(Unit) {
    isScreenActive = true
    onDispose {
      isScreenActive = false
      uiScope.cancel()
      inferenceScope.cancel()
    }
  }

  val imagePickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
      if (uris.isEmpty()) return@rememberLauncherForActivityResult
      isPreparingPages = true
      errorText = null
      screenScope.launch(Dispatchers.IO) {
        val loaded = loadImageBitmaps(context = context, uris = uris)
        withContext(Dispatchers.Main) {
          pages = (pages + loaded).take(MAX_SCAN_DOCS_IMAGE_COUNT)
          isPreparingPages = false
        }
      }
    }

  val pdfPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode != android.app.Activity.RESULT_OK) return@rememberLauncherForActivityResult
      val uri = result.data?.data ?: return@rememberLauncherForActivityResult
      isPreparingPages = true
      errorText = null
      screenScope.launch(Dispatchers.IO) {
        val rendered = renderPdfToBitmaps(context = context, uri = uri)
        withContext(Dispatchers.Main) {
          lastPdfPageCount = rendered.size
          pages = (pages + rendered).take(MAX_SCAN_DOCS_IMAGE_COUNT)
          isPreparingPages = false
        }
      }
    }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.scan_docs_label)) },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(horizontal = 20.dp, vertical = 16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = stringResource(R.string.scan_docs_description),
        style = MaterialTheme.typography.bodyMedium,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          modifier = Modifier.weight(1f).height(54.dp),
          enabled = !isPreparingPages && !pendingInference && !inferenceInFlight,
          onClick = {
            imagePickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
        ) {
          Icon(Icons.Outlined.Image, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("Gallery")
        }
        OutlinedButton(
          modifier = Modifier.weight(1f).height(54.dp),
          enabled = !isPreparingPages && !pendingInference && !inferenceInFlight,
          onClick = {
            pdfPickerLauncher.launch(
              Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
              }
            )
          },
        ) {
          Icon(Icons.Outlined.Description, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("PDF")
        }
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F4FB)),
      ) {
        Box(
          modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
          contentAlignment = Alignment.Center,
        ) {
          when {
            isPreparingPages -> {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.size(10.dp))
                Text("Preparing document pages...")
              }
            }
            pages.isEmpty() -> {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  Icons.Outlined.DocumentScanner,
                  contentDescription = null,
                  modifier = Modifier.size(52.dp),
                  tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                  stringResource(R.string.scan_docs_emptystate_title),
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                  stringResource(R.string.scan_docs_emptystate_content),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 18.dp),
                )
              }
            }
            else -> {
              Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = "Attached pages: ${pages.size}",
                  style = MaterialTheme.typography.titleSmall,
                  modifier = Modifier.padding(horizontal = 12.dp),
                )
                LazyRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                  itemsIndexed(pages) { index, bitmap ->
                    Image(
                      bitmap = bitmap.asImageBitmap(),
                      contentDescription = "Document page ${index + 1}",
                      modifier =
                        Modifier.size(width = 120.dp, height = 160.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(MaterialTheme.colorScheme.surface),
                    )
                  }
                }
              }
            }
          }
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          modifier = Modifier.weight(1f).height(52.dp),
          enabled = pages.isNotEmpty() && !pendingInference && !inferenceInFlight,
          onClick = {
            pages = emptyList()
            analysis = ""
            errorText = null
            statusText = null
            hasAttemptedAnalysis = false
            lastPdfPageCount = 0
          },
        ) {
          Icon(Icons.Outlined.Refresh, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("Clear")
        }
        Button(
          modifier = Modifier.weight(1f).height(52.dp),
          enabled = pages.isNotEmpty() && !isPreparingPages && !pendingInference && !inferenceInFlight,
          onClick = {
            statusText = "Analyze tapped..."
            if (model == null) {
              errorText = "No compatible model is available for this task."
              statusText = errorText
              Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
              return@Button
            }
            if (downloadStatus != ModelDownloadStatusType.SUCCEEDED) {
              selectedModelHint = model.name
              errorText = "Download $selectedModelHint first, then analyze."
              statusText = errorText
              Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
              return@Button
            }
            errorText = null
            analysis = ""
            forceRetryTriggered = false
            hasAttemptedAnalysis = true
            pendingInference = true
            statusText = "Warming up model..."
            Toast.makeText(context, "Started analysis", Toast.LENGTH_SHORT).show()
            modelManagerViewModel.initializeModel(context = context, task = task, model = model)
          },
        ) {
          if (pendingInference || inferenceInFlight) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = Color.White,
              strokeWidth = 2.dp,
            )
            Spacer(Modifier.size(8.dp))
            Text(
              when {
                pendingInference && initStatus != ModelInitializationStatusType.INITIALIZED ->
                  "Warming up..."
                else -> "Analyzing..."
              }
            )
          } else {
            Icon(Icons.Outlined.Summarize, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Analyze")
          }
        }
      }

      statusText?.let {
        Text(
          text = it,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      if (lastPdfPageCount > 0) {
        Text(
          text = "Loaded $lastPdfPageCount pages from last PDF.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      model?.let {
        Text(
          text = "Model: ${it.name}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      errorText?.let {
        Text(
          text = it,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }

      if (hasAttemptedAnalysis) {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              "SUKHAM analysis",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            val displayText =
              when {
                inferenceInFlight || pendingInference ->
                  "Running analysis... this can take a little while on-device."
                analysis.isNotBlank() -> formatAnalysisForDisplay(analysis)
                else ->
                  "Analysis finished but no text was produced.\n\nTry again with fewer or clearer pages."
              }
            MarkdownText(
              text = displayText,
              smallFontSize = true,
              textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    }
  }

  LaunchedEffect(model?.name, downloadStatus, uiState.loadingModelAllowlist) {
    if (
      model != null &&
        !uiState.loadingModelAllowlist &&
        downloadStatus == ModelDownloadStatusType.SUCCEEDED &&
        initStatus != ModelInitializationStatusType.INITIALIZED
    ) {
      modelManagerViewModel.initializeModel(context = context, task = task, model = model)
    }
  }

  LaunchedEffect(pendingInference, initStatus, model?.name, pages.size) {
    if (
      pendingInference &&
        model != null &&
        initStatus == ModelInitializationStatusType.INITIALIZED &&
        pages.isNotEmpty() &&
        !inferenceInFlight
    ) {
      pendingInference = false
      forceRetryTriggered = false
      inferenceInFlight = true
      statusText = "Analyzing document..."
      inferenceStartedAtMs = System.currentTimeMillis()
      var receivedAnyOutput = false
      try {
        withContext(Dispatchers.Default) {
          model.runtimeHelper.resetConversation(
            model = model,
            supportImage = true,
            supportAudio = false,
            systemInstruction = Contents.of(task.defaultSystemPrompt),
          )
        }
      } catch (e: Exception) {
        inferenceInFlight = false
        errorText = e.message ?: "Failed to prepare model session."
        statusText = errorText
        Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
        return@LaunchedEffect
      }
      model.runtimeHelper.runInference(
        model = model,
        input = inferencePrompt,
        images = pages.take(MAX_SCAN_DOCS_IMAGE_COUNT),
        resultListener = { partial, done, _ ->
          if (!isScreenActive || !uiScope.isActive) return@runInference
          uiScope.launch {
            if (partial.isNotBlank()) {
              receivedAnyOutput = true
              analysis += partial
            }
            if (done) {
              inferenceInFlight = false
              if (!receivedAnyOutput || analysis.isBlank()) {
                // Keep result area visible even when model returns empty output.
                analysis =
                  "Analysis completed, but no readable output was produced.\n\n" +
                    "Please retry with fewer pages or clearer document photos."
                statusText = "Analysis complete (empty output)."
              } else {
                statusText = "Analysis complete."
              }
            }
          }
        },
        cleanUpListener = {
          if (!isScreenActive || !uiScope.isActive) return@runInference
          uiScope.launch {
            inferenceInFlight = false
            if (!receivedAnyOutput && analysis.isBlank() && errorText.isNullOrBlank()) {
              analysis =
                "Analysis ended unexpectedly before any output was generated.\n\n" +
                  "Please tap Analyze again."
              statusText = "Analysis stopped unexpectedly."
            }
          }
        },
        onError = { message ->
          if (!isScreenActive || !uiScope.isActive) return@runInference
          uiScope.launch {
            inferenceInFlight = false
            errorText = message
            statusText = message
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
          }
        },
        coroutineScope = inferenceScope,
      )
    }
  }

  LaunchedEffect(pendingInference, initStatus, model?.name, forceRetryTriggered) {
    if (!pendingInference || model == null || initStatus == ModelInitializationStatusType.INITIALIZED) {
      return@LaunchedEffect
    }

    if (!forceRetryTriggered) {
      delay(WARMUP_FORCE_RETRY_MS)
      if (pendingInference && initStatus != ModelInitializationStatusType.INITIALIZED) {
        forceRetryTriggered = true
        modelManagerViewModel.initializeModel(
          context = context,
          task = task,
          model = model,
          force = true,
        )
      }
      return@LaunchedEffect
    }

    delay(WARMUP_TIMEOUT_MS)
    if (pendingInference && initStatus != ModelInitializationStatusType.INITIALIZED) {
      pendingInference = false
      forceRetryTriggered = false
      errorText = "Model warm-up is taking too long. Please tap Analyze again."
      statusText = errorText
      Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(inferenceInFlight, inferenceStartedAtMs) {
    if (!inferenceInFlight || inferenceStartedAtMs <= 0L) return@LaunchedEffect
    delay(INFERENCE_TIMEOUT_MS)
    if (inferenceInFlight && (System.currentTimeMillis() - inferenceStartedAtMs) >= INFERENCE_TIMEOUT_MS) {
      inferenceInFlight = false
      errorText = "Analysis timed out. Try fewer pages and tap Analyze again."
      statusText = errorText
      Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
    }
  }
}

private fun loadImageBitmaps(context: Context, uris: List<Uri>): List<Bitmap> {
  val bitmaps = mutableListOf<Bitmap>()
  for (uri in uris) {
    try {
      val input =
        if (uri.scheme == null || uri.scheme == "file") FileInputStream(uri.path ?: "")
        else context.contentResolver.openInputStream(uri)
      val orientation =
        input?.use {
          ExifInterface(it).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
          )
        } ?: ExifInterface.ORIENTATION_NORMAL
      val bitmap = decodeSampledBitmapFromUri(context, uri, 1400, 1400)
      if (bitmap != null) {
        bitmaps.add(rotateBitmap(bitmap = bitmap, orientation = orientation))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed loading image: $uri", e)
    }
  }
  return bitmaps
}

private fun formatAnalysisForDisplay(raw: String): String {
  var text = raw.trim()
  if (text.isBlank()) return text

  // If the model already produced structured text, keep it.
  if (text.contains("\n") && (text.contains("**") || text.contains("- ") || text.contains("• "))) {
    return text
  }

  val sectionHeaders =
    listOf(
      "Document type",
      "What looks healthy",
      "What looks good",
      "What needs attention",
      "Recommendations",
      "Disclaimer",
    )
  for (header in sectionHeaders) {
    val regex = Regex("(?i)\\b${Regex.escape(header)}\\b\\s*[:\\-]?")
    text = text.replace(regex, "\n\n### $header\n")
  }

  // If still mostly one block, split into sentence bullets for readability.
  if (!text.contains("\n")) {
    val sentences =
      text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
    if (sentences.size > 1) {
      text = "### Analysis\n" + sentences.joinToString("\n") { "- $it" }
    }
  }

  return text.replace(Regex("\n{3,}"), "\n\n").trim()
}

private fun renderPdfToBitmaps(context: Context, uri: Uri): List<Bitmap> {
  val bitmaps = mutableListOf<Bitmap>()
  try {
    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
      PdfRenderer(pfd).use { renderer ->
        val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES_TO_RENDER)
        for (pageIndex in 0 until pageCount) {
          renderer.openPage(pageIndex).use { page ->
            val scale = 2
            val width = page.width * scale
            val height = page.height * scale
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
          }
        }
      }
    }
  } catch (e: Exception) {
    Log.e(TAG, "Failed rendering PDF", e)
  }
  return bitmaps
}
