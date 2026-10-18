package com.google.ai.edge.gallery.ui.progress

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.ai.edge.gallery.ui.theme.SukhamColors
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SharePracticeEntry(enabled: Boolean, onClick: () -> Unit) {
  Surface(color = SukhamColors.SageContainer, shape = RoundedCornerShape(22.dp)) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("A little practice. Worth sharing.", color = SukhamColors.TitleBlack, style = MaterialTheme.typography.titleMedium)
      Text("Turn your week or today's activity into a beautiful card.", color = SukhamColors.BodyGray, style = MaterialTheme.typography.bodySmall)
      TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(0.dp)) {
        Icon(Icons.Outlined.Share, null)
        Spacer(Modifier.width(8.dp))
        Text("Create my card")
      }
    }
  }
}

@Composable
internal fun PracticeShareSheet(sessions: List<ActivitySession>, weekStart: LocalDate, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var weekly by rememberSaveable { mutableStateOf(true) }
  var story by rememberSaveable { mutableStateOf(true) }
  var styleIndex by rememberSaveable { mutableIntStateOf(0) }
  val data = remember(sessions, weekly, weekStart) { practiceShareData(sessions, weekly, weekStart, LocalDate.now()) }
  var caption by rememberSaveable(weekly) { mutableStateOf(data.defaultCaption) }
  var showName by rememberSaveable { mutableStateOf(false) }
  var name by rememberSaveable { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var status by remember { mutableStateOf<String?>(null) }
  val style = PracticeCardStyle.entries[styleIndex]
  val bitmap by produceState<Bitmap?>(null, data, style, story, caption, showName, name) {
    value = null
    value = withContext(Dispatchers.Default) { renderPracticeCard(data, style, story, caption, if (showName) name else "") }
  }
  Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(color = SukhamColors.ScreenBg, modifier = Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          Text("Share your practice", Modifier.weight(1f), color = SukhamColors.TitleBlack, style = MaterialTheme.typography.titleLarge)
          IconButton(onClick = onDismiss, enabled = !busy) { Icon(Icons.Outlined.Close, "Close") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(weekly, { if (!busy) weekly = true }, label = { Text("My week") })
            FilterChip(!weekly, { if (!busy) weekly = false }, label = { Text("Today") })
          }
          Text("Your saved activity · ${data.dateLabel}", color = SukhamColors.BodyGray, style = MaterialTheme.typography.bodySmall)
          Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PracticeCardStyle.entries.forEachIndexed { index, option ->
              FilterChip(styleIndex == index, { if (!busy) styleIndex = index }, label = { Text(option.label) })
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(story, { if (!busy) story = true }, label = { Text("Story · 9:16") })
            FilterChip(!story, { if (!busy) story = false }, label = { Text("Post · 4:5") })
          }
          Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(if (story) 9f / 16 else 4f / 5)) {
              bitmap?.let { Image(it.asImageBitmap(), "Preview: ${data.heading}, ${data.firstValue} ${data.firstLabel}, ${data.secondValue} ${data.secondLabel}", Modifier.fillMaxSize()) }
                ?: Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
          }
          OutlinedTextField(value = caption, onValueChange = { caption = it.take(80) }, label = { Text("Your caption") },
            enabled = !busy, modifier = Modifier.fillMaxWidth(), maxLines = 3, supportingText = { Text("${caption.length}/80") })
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Include my name", Modifier.weight(1f), color = SukhamColors.TitleBlack)
            Switch(showName, { if (!busy) showName = it }, enabled = !busy)
          }
          if (showName) OutlinedTextField(name, { name = it.take(32) }, label = { Text("Name on card") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
          Text("Only the activity shown in this preview is exported. Profile details, photos and coaching notes stay private.", color = SukhamColors.BodyGray, style = MaterialTheme.typography.bodySmall)
          if (!data.hasActivity) Text("No saved activity for these dates yet. Complete a practice or pose review to create your card.", color = SukhamColors.BodyGray)
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          status?.let { Text(it, color = SukhamColors.BodyGray, style = MaterialTheme.typography.bodySmall) }
          if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
          Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
              val image = bitmap ?: return@OutlinedButton
              scope.launch {
                busy = true; status = null
                try {
                  withContext(Dispatchers.IO) { savePracticeCard(context.applicationContext, image) }
                  status = "Saved to your gallery · Pictures/Sukham"
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { status = "Couldn't save the card. Please try again." }
                finally { busy = false }
              }
            }, enabled = bitmap != null && data.hasActivity && !busy, modifier = Modifier.weight(1f)) {
              Icon(Icons.Outlined.Download, null); Spacer(Modifier.width(6.dp)); Text("Download")
            }
            Button(onClick = {
              val image = bitmap ?: return@Button
              scope.launch {
                busy = true; status = null
                try {
                  val intent = withContext(Dispatchers.IO) { practiceCardShareIntent(context.applicationContext, image) }
                  context.startActivity(Intent.createChooser(intent, "Share your practice"))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { status = "Couldn't share the card. Try downloading it instead." }
                finally { busy = false }
              }
            }, enabled = bitmap != null && data.hasActivity && !busy, modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = SukhamColors.SagePrimary, contentColor = SukhamColors.ScreenBg)) {
              Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(6.dp)); Text("Share")
            }
          }
        }
      }
    }
  }
}
