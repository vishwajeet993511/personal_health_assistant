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

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.ui.common.humanReadableSize
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

private object SheetColors {
  val SagePrimary = Color(0xFF4A6B53)
  val SageContainer = Color(0xFFE8EFEA)
  val SageDark = Color(0xFF2C4332)
  val CardBorder = Color(0xFFE0DDD5)
  val AmberBg = Color(0xFFFFF8E1)
  val AmberText = Color(0xFFE65100)
  val BodyGray = Color(0xFF6B7280)
  val TitleBlack = Color(0xFF1E242B)
}

/**
 * Bottom sheet displaying all available on-device LLM models with device RAM detection,
 * clear recommendation badge, and hardware reasoning.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionBottomSheet(
  models: List<Model>,
  modelManagerViewModel: ModelManagerViewModel,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val recommendation = remember(models) { resolveModelRecommendation(context, models) }

  // Sort list to place recommended model at the top
  val sortedModels = remember(models, recommendation) {
    val rec = recommendation.recommendedModel
    if (rec == null) models
    else listOf(rec) + models.filter { it.name != rec.name }
  }

  val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color.White,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp)
        .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "On-Device LLM Models",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = SheetColors.TitleBlack
            )
          )
          Text(
            text = "Select or download models running 100% privately on your device",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SheetColors.BodyGray,
              fontSize = 11.5.sp
            )
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Outlined.Close, contentDescription = "Close", tint = SheetColors.BodyGray)
        }
      }

      // Hardware RAM Info Banner
      Surface(
        color = SheetColors.SageContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SheetColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(Color.White, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Memory,
              contentDescription = null,
              tint = SheetColors.SagePrimary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(Modifier.width(10.dp))
          Column {
            Text(
              text = "${Build.MODEL} · ${recommendation.ramGb} GB RAM",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SheetColors.SageDark,
                fontSize = 12.5.sp
              )
            )
            Text(
              text = "Detected ${recommendation.tierName}. On-device models run entirely on your phone processor without cloud servers.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = SheetColors.BodyGray,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
              )
            )
          }
        }
      }

      // Models List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(440.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(sortedModels, key = { it.name }) { model ->
          val isRecommended = model.name == recommendation.recommendedModel?.name
          val isDownloaded = modelManagerViewModel.isModelDownloaded(model) ||
            modelManagerUiState.modelDownloadStatus[model.name]?.status == ModelDownloadStatusType.SUCCEEDED
          val downloadStatus = modelManagerUiState.modelDownloadStatus[model.name]
          val isDownloading = downloadStatus?.status == ModelDownloadStatusType.IN_PROGRESS
          val isSelected = model.name == modelManagerUiState.selectedModel.name && isDownloaded

          ModelCardItem(
            model = model,
            isRecommended = isRecommended,
            recommendationReason = recommendation.reason,
            ramGb = recommendation.ramGb,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            isSelected = isSelected,
            downloadStatus = downloadStatus,
            onSelect = {
              modelManagerViewModel.selectModel(model)
              onDismiss()
            },
            onDownload = {
              val task = modelManagerUiState.tasks.firstOrNull { t -> t.models.any { m -> m.name == model.name } }
                ?: modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_ASK_IMAGE)
                ?: modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT)
              modelManagerViewModel.downloadModel(task, model)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun ModelCardItem(
  model: Model,
  isRecommended: Boolean,
  recommendationReason: String,
  ramGb: Int,
  isDownloaded: Boolean,
  isDownloading: Boolean,
  isSelected: Boolean,
  downloadStatus: com.google.ai.edge.gallery.data.ModelDownloadStatus?,
  onSelect: () -> Unit,
  onDownload: () -> Unit,
) {
  val displayName = model.displayName.ifEmpty { model.name }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) SheetColors.SageContainer.copy(alpha = 0.5f) else Color.White
    ),
    border = BorderStroke(
      if (isSelected || isRecommended) 1.5.dp else 1.dp,
      if (isSelected) SheetColors.SagePrimary
      else if (isRecommended) Color(0xFFF59E0B)
      else SheetColors.CardBorder
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Top row: Model title & Recommended badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Outlined.SmartToy,
            contentDescription = null,
            tint = if (isSelected) SheetColors.SagePrimary else SheetColors.BodyGray,
            modifier = Modifier.size(18.dp)
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = SheetColors.TitleBlack
            )
          )
        }

        if (isRecommended) {
          Surface(
            color = SheetColors.AmberBg,
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(0.8.dp, Color(0xFFF59E0B))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = SheetColors.AmberText,
                modifier = Modifier.size(11.dp)
              )
              Spacer(Modifier.width(3.dp))
              Text(
                text = "RECOMMENDED",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.5.sp,
                  color = SheetColors.AmberText
                )
              )
            }
          }
        }
      }

      // Feature tags row (Vision, Voice, Size)
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (model.llmSupportImage) {
          FeatureTag(text = "Vision", icon = Icons.Outlined.Image)
        }
        if (model.llmSupportAudio) {
          FeatureTag(text = "Voice", icon = Icons.Outlined.Mic)
        }
        FeatureTag(
          text = if (model.sizeInBytes > 0) model.sizeInBytes.humanReadableSize() else "On-Device",
          icon = null
        )
      }

      // Recommended explanation banner
      if (isRecommended) {
        Surface(
          color = Color(0xFFFFFBEB),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(0.6.dp, Color(0xFFFDE68A)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null,
              tint = Color(0xFFB45309),
              modifier = Modifier.size(14.dp).padding(top = 1.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column {
              Text(
                text = "Why recommended for your ${ramGb} GB RAM:",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  color = Color(0xFF92400E)
                )
              )
              Text(
                text = recommendationReason,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 10.5.sp,
                  color = Color(0xFF78350F),
                  lineHeight = 14.sp
                )
              )
            }
          }
        }
      }

      // Download Progress Bar if downloading
      if (isDownloading && downloadStatus != null) {
        val total = downloadStatus.totalBytes
        val received = downloadStatus.receivedBytes
        val percent = if (total > 0) (received.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Downloading… ${(percent * 100).toInt()}%",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.5.sp,
                color = SheetColors.SagePrimary
              )
            )
            Text(
              text = "${received.humanReadableSize()} / ${total.humanReadableSize()}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                color = SheetColors.BodyGray
              )
            )
          }
          Spacer(Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)),
            color = SheetColors.SagePrimary,
            trackColor = SheetColors.SageContainer
          )
        }
      }

      // Action Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        when {
          isSelected -> {
            Surface(
              color = SheetColors.SageContainer,
              shape = RoundedCornerShape(percent = 50)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Outlined.CheckCircle,
                  contentDescription = null,
                  tint = SheetColors.SagePrimary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                  text = "Active Model",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = SheetColors.SageDark,
                    fontSize = 11.sp
                  )
                )
              }
            }
          }

          isDownloaded -> {
            Button(
              onClick = onSelect,
              shape = RoundedCornerShape(percent = 50),
              colors = ButtonDefaults.buttonColors(containerColor = SheetColors.SagePrimary),
              modifier = Modifier.height(36.dp)
            ) {
              Text("Select & Use", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }

          isDownloading -> {
            OutlinedButton(
              onClick = {},
              enabled = false,
              shape = RoundedCornerShape(percent = 50),
              modifier = Modifier.height(36.dp)
            ) {
              Text("Downloading…", fontSize = 11.sp)
            }
          }

          else -> {
            Button(
              onClick = onDownload,
              shape = RoundedCornerShape(percent = 50),
              colors = ButtonDefaults.buttonColors(containerColor = SheetColors.SagePrimary),
              modifier = Modifier.height(36.dp)
            ) {
              Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(Modifier.width(4.dp))
              val sizeText = if (model.sizeInBytes > 0) " (${model.sizeInBytes.humanReadableSize()})" else ""
              Text("Download$sizeText", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeatureTag(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
  Surface(
    color = Color(0xFFF3F4F6),
    shape = RoundedCornerShape(6.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = SheetColors.BodyGray,
          modifier = Modifier.size(10.dp)
        )
        Spacer(Modifier.width(3.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
          color = SheetColors.BodyGray,
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium
        )
      )
    }
  }
}
