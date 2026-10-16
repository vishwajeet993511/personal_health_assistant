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

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.google.ai.edge.gallery.data.Model
import kotlin.math.roundToInt

private const val BYTES_IN_GB = 1024.0 * 1024.0 * 1024.0

/** Information regarding model recommendation tailored to device RAM hardware. */
data class ModelRecommendationInfo(
  val recommendedModel: Model?,
  val reason: String,
  val ramGb: Int,
  val tierName: String,
)

/** Returns the total physical RAM of the device in GB rounded to the nearest integer. */
fun getDeviceTotalRamInGb(context: Context): Int {
  val activityManager =
    context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 8
  val memoryInfo = ActivityManager.MemoryInfo()
  activityManager.getMemoryInfo(memoryInfo)
  val deviceMemInGb =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      memoryInfo.advertisedMem / BYTES_IN_GB
    } else {
      memoryInfo.totalMem / BYTES_IN_GB
    }
  return deviceMemInGb.roundToInt().coerceAtLeast(2)
}

/**
 * Resolves the optimal on-device LLM model based on physical hardware memory constraints
 * and provides clear architectural reasoning for the recommendation.
 */
fun resolveModelRecommendation(context: Context, models: List<Model>): ModelRecommendationInfo {
  val ramGb = getDeviceTotalRamInGb(context)
  val llmModels = models.filter { it.isLlm }

  return when {
    ramGb < 8 -> {
      // Devices with < 8GB RAM: still Gemma 4 E2B. It is the smallest model Sukham ships (the
      // gated 1B was dropped), and the one downloaded automatically on first launch.
      val candidate =
        llmModels.firstOrNull { it.name.contains("e2b", ignoreCase = true) }
          ?: llmModels.minByOrNull { it.sizeInBytes.takeIf { s -> s > 0 } ?: Long.MAX_VALUE }
          ?: llmModels.firstOrNull()

      ModelRecommendationInfo(
        recommendedModel = candidate,
        reason =
          "Your device has ~${ramGb} GB RAM. Gemma 4 E2B is the most compact model available and keeps every feature, including image and pose analysis, working on-device.",
        ramGb = ramGb,
        tierName = "Efficiency Tier (< 8 GB)",
      )
    }

    ramGb in 8..11 -> {
      // Standard / flagship tier (8-11 GB RAM): balanced 2B multimodal model
      val candidate =
        llmModels.firstOrNull {
          it.llmSupportImage &&
            (it.name.contains("2b", ignoreCase = true) || it.name.contains("e2b", ignoreCase = true))
        }
          ?: llmModels.firstOrNull { it.llmSupportImage }
          ?: llmModels.firstOrNull()

      ModelRecommendationInfo(
        recommendedModel = candidate,
        reason =
          "Your device has ${ramGb} GB RAM. A 2B-parameter multimodal model is the sweet spot — delivering full biomechanical yoga critique and report analysis with fast tokens and safe memory headroom.",
        ramGb = ramGb,
        tierName = "Balanced Performance Tier (${ramGb} GB)",
      )
    }

    else -> {
      // High-performance tier (>= 12 GB RAM, e.g. Fold5 / S24 Ultra): full multimodal Gemma
      val candidate =
        llmModels.firstOrNull {
          it.llmSupportImage &&
            (it.name.contains("2b", ignoreCase = true) || it.name.contains("e2b", ignoreCase = true))
        }
          ?: llmModels.firstOrNull { it.llmSupportImage }
          ?: llmModels.maxByOrNull { it.sizeInBytes }
          ?: llmModels.firstOrNull()

      ModelRecommendationInfo(
        recommendedModel = candidate,
        reason =
          "Your device features a high-capacity ${ramGb} GB RAM configuration. High-fidelity multimodal Gemma is recommended for maximum accuracy in skeletal pose critique and document reasoning with zero memory bottlenecks.",
        ramGb = ramGb,
        tierName = "High-Performance Tier (${ramGb} GB)",
      )
    }
  }
}
