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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * One row of the coaching timeline: the sampled frame with its skeleton on the left, the
 * timestamp and Gemma's coaching on the right.
 *
 * Kept deliberately compact (96 dp thumbnail) so several cards are visible at once — the value
 * of the session view is seeing form change *across* the sampled poses, not studying one frame.
 */
@Composable
fun CoachingCardRow(card: CoachingCard, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F4FB)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
      ) {
        val thumb = card.thumbnail
        if (thumb != null) {
          Image(
            bitmap = thumb.asImageBitmap(),
            contentDescription = "Pose at ${formatTimestamp(card.timestampMs)}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(96.dp),
          )
        } else {
          Text("—", style = MaterialTheme.typography.bodyMedium)
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            formatTimestamp(card.timestampMs),
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A148C),
              ),
          )
          if (card.status == CoachingStatus.ANALYZING) {
            Spacer(Modifier.size(8.dp))
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
          }
        }
        Spacer(Modifier.size(4.dp))
        val body =
          when {
            card.text.isNotBlank() -> card.text
            card.status == CoachingStatus.ANALYZING -> "Looking at the pose…"
            else -> "—"
          }
        // Explicit near-black rather than a theme color: the card background above is a
        // hardcoded light lavender, so on a dark-theme device `onSurface` resolves to white and
        // the coaching text disappears into the card. Fixed colors on a fixed background.
        Text(
          body,
          style =
            MaterialTheme.typography.bodySmall.copy(
              color =
                when (card.status) {
                  CoachingStatus.ERROR -> Color(0xFFB3261E)
                  CoachingStatus.NO_POSE -> Color(0xFF49454F)
                  else -> Color.Black
                },
              fontWeight = FontWeight.Medium,
            ),
        )
      }
    }
  }
}
