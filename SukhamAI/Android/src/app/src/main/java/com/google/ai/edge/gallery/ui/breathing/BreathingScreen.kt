package com.google.ai.edge.gallery.ui.breathing

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ai.edge.gallery.ui.progress.ActivitySession
import com.google.ai.edge.gallery.ui.progress.ActivityStatus
import com.google.ai.edge.gallery.ui.progress.ActivityType
import com.google.ai.edge.gallery.ui.progress.ProgressRepository
import java.util.UUID
import java.time.ZoneId
import com.google.ai.edge.gallery.ui.theme.SukhamColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreathingScreen(onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val activity = LocalActivity.current
  val preferences = remember { context.getSharedPreferences("breathing", Context.MODE_PRIVATE) }
  var minutes by rememberSaveable { mutableIntStateOf(preferences.getInt("last_duration_minutes", 2).takeIf { it in BreathingTiming.durations } ?: 2) }
  var elapsedMs by rememberSaveable { mutableLongStateOf(0L) }
  // Returning from another screen or recreating the activity never restarts audio automatically.
  var running by remember { mutableStateOf(false) }
  var sound by rememberSaveable { mutableStateOf(preferences.getBoolean("sound", true)) }
  var audioUnavailable by remember { mutableStateOf(false) }
  val onFocusLost by rememberUpdatedState { running = false }
  val audio = remember(context) { BreathingAudio(context) { onFocusLost() } }
  val repository = remember { ProgressRepository.get(context) }
  var historyId by rememberSaveable { mutableStateOf("") }
  var historyStartedAt by rememberSaveable { mutableLongStateOf(0L) }
  var historyZone by rememberSaveable { mutableStateOf(ZoneId.systemDefault().id) }
  val durationMs = minutes * 60_000L
  val complete = elapsedMs >= durationMs
  val started = elapsedMs > 0L || running
  val phase = BreathingTiming.phaseAt(elapsedMs)
  val remaining = BreathingTiming.remainingSeconds(durationMs, elapsedMs)
  val accent = SukhamColors.SagePrimary
  val titleColor = SukhamColors.TitleBlack
  val bodyColor = SukhamColors.BodyGray
  val cardColor = SukhamColors.CardBg
  val borderColor = SukhamColors.CardBorder

  fun persistBreathing(status: ActivityStatus) {
    if (historyId.isNotEmpty()) repository.save(ActivitySession(
      id = historyId, type = ActivityType.BREATHING, title = "Gentle Breathing",
      startedAt = historyStartedAt, updatedAt = System.currentTimeMillis(), timezone = historyZone,
      status = if (elapsedMs >= durationMs) ActivityStatus.COMPLETED else status,
      activeMs = elapsedMs, plannedMs = durationMs,
    ))
  }
  LaunchedEffect(minutes) { preferences.edit().putInt("last_duration_minutes", minutes).apply() }

  DisposableEffect(lifecycleOwner, audio) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
        running = false
        audio.pause()
        persistBreathing(ActivityStatus.PAUSED)
      }
    }
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
          running = false
          audio.pause()
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    ContextCompat.registerReceiver(context, receiver,
      IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      context.unregisterReceiver(receiver)
      audio.release()
      persistBreathing(if (activity?.isChangingConfigurations == true) ActivityStatus.PAUSED else ActivityStatus.ENDED_EARLY)
    }
  }

  DisposableEffect(running, activity) {
    val window = activity?.window
    val alreadyAwake = ((window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    if (running) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    onDispose {
      if (running && !alreadyAwake) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }

  LaunchedEffect(running, sound) {
    if (running && sound) {
      if (!audio.play(elapsedMs)) {
        sound = false
        audioUnavailable = true
      }
    } else audio.pause()
  }

  LaunchedEffect(running) {
    if (running) {
      var lastSavedBucket = elapsedMs / 5_000
      val startTime = SystemClock.elapsedRealtime()
      val previousElapsed = elapsedMs
      while (true) {
        elapsedMs = (previousElapsed + SystemClock.elapsedRealtime() - startTime).coerceAtMost(durationMs)
        if (elapsedMs / 5_000 > lastSavedBucket) {
          persistBreathing(ActivityStatus.IN_PROGRESS)
          lastSavedBucket = elapsedMs / 5_000
        }
        if (elapsedMs >= durationMs) {
          persistBreathing(ActivityStatus.COMPLETED)
          running = false
          audio.pause()
          break
        }
        delay(32)
      }
    } else persistBreathing(ActivityStatus.PAUSED)
  }

  val leave = { running = false; audio.pause(); onNavigateUp() }
  BackHandler(onBack = leave)

  Scaffold(
    modifier = modifier.fillMaxSize(), containerColor = SukhamColors.ScreenBg,
    topBar = {
      TopAppBar(
        title = { Text("Breathing", fontSize = 18.sp, color = titleColor) },
        navigationIcon = { IconButton(onClick = leave) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = titleColor) } },
        actions = {
          IconToggleButton(checked = sound, onCheckedChange = {
            sound = it
            audioUnavailable = false
            preferences.edit().putBoolean("sound", it).apply()
          }) {
            Icon(if (sound) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
              if (sound) "Sound on" else "Sound off", tint = accent)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SukhamColors.ScreenBg),
      )
    },
  ) { padding ->
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
      val orbSize = minOf(maxWidth - 48.dp, maxHeight * 0.40f, 310.dp).coerceAtLeast(180.dp)
      Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()
          .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("A MOMENT OF STILLNESS", color = accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Text(if (complete) "Beautifully done." else "Come back to your breath.",
          color = titleColor, fontFamily = FontFamily.Serif, fontSize = 29.sp, lineHeight = 35.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(if (complete) "Carry this quiet into the rest of your day." else "Nothing to achieve. Just a little space to breathe.",
          color = bodyColor, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))

        Box(Modifier.size(orbSize), contentAlignment = Alignment.Center) {
          val expansion = when {
            !started || complete -> 0.30f
            !ValueAnimator.areAnimatorsEnabled() -> 0.5f
            else -> BreathingTiming.expansionAt(elapsedMs)
          }
          Canvas(Modifier.fillMaxSize()) {
            val outer = size.minDimension * 0.48f
            val radius = size.minDimension * (0.27f + 0.13f * expansion)
            drawCircle(accent.copy(alpha = 0.04f), outer)
            drawCircle(accent.copy(alpha = 0.10f), outer * 0.91f, style = Stroke(1.dp.toPx()))
            drawCircle(accent.copy(alpha = 0.07f), radius * 1.17f)
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0.14f)),
              center = Offset(center.x - radius * 0.25f, center.y - radius * 0.3f), radius = radius * 1.5f), radius)
            drawCircle(accent.copy(alpha = 0.17f), radius, style = Stroke(1.dp.toPx()))
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            if (complete) Icon(Icons.Outlined.Check, "Completed", tint = accent, modifier = Modifier.size(32.dp))
            Text(when { complete -> "Well done"; !started -> "Find your calm"; !running -> "Paused"; phase == BreathingPhase.INHALE -> "Breathe in"; phase == BreathingPhase.HOLD -> "Hold gently"; else -> "Breathe out" },
              color = titleColor, fontFamily = FontFamily.Serif, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(when { complete -> "$minutes quiet minute${if (minutes == 1) "" else "s"}"; !started -> "Inhale · Hold · Exhale"; !running -> "Whenever you're ready"; phase == BreathingPhase.HOLD -> "Only as long as comfortable"; else -> "Gently, without forcing" },
              color = bodyColor, fontSize = 11.sp)
          }
        }

        Text("%d:%02d".format(remaining / 60, remaining % 60), color = titleColor, fontSize = 30.sp,
          fontWeight = FontWeight.Light, letterSpacing = 2.sp,
          modifier = Modifier.semantics { contentDescription = "$remaining seconds remaining" })
        Text(if (complete) "SESSION COMPLETE" else if (started) "TIME REMAINING" else "YOUR TIME",
          color = bodyColor, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(18.dp))

        if (!started || complete) {
          Row(Modifier.widthIn(max = 360.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BreathingTiming.durations.forEach { duration ->
              FilterChip(
                selected = minutes == duration,
                onClick = { minutes = duration; elapsedMs = 0L; historyId = "" },
                label = { Text("$duration min", fontSize = 12.sp) },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(containerColor = cardColor,
                  selectedContainerColor = accent.copy(alpha = 0.13f), selectedLabelColor = accent, labelColor = bodyColor),
                border = BorderStroke(1.dp, if (minutes == duration) accent else borderColor),
              )
            }
          }
          Spacer(Modifier.height(12.dp))
        }

        Button(
          onClick = {
            if (complete) { elapsedMs = 0L; historyId = "" }
            if (historyId.isEmpty()) {
              historyId = UUID.randomUUID().toString()
              historyStartedAt = System.currentTimeMillis()
              historyZone = ZoneId.systemDefault().id
            }
            running = !running
            persistBreathing(if (running) ActivityStatus.IN_PROGRESS else ActivityStatus.PAUSED)
          }, modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().height(56.dp),
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = SukhamColors.ScreenBg),
        ) {
          Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, modifier = Modifier.size(20.dp))
          Spacer(Modifier.width(10.dp))
          Text(when { complete -> "Breathe again"; running -> "Pause"; started -> "Continue breathing"; else -> "Begin breathing" }, fontSize = 15.sp)
        }
        if (started && !complete) {
          TextButton(onClick = { persistBreathing(ActivityStatus.ENDED_EARLY); running = false; audio.pause(); elapsedMs = 0L; historyId = "" }) {
            Text("End session", color = bodyColor, fontSize = 13.sp)
          }
        } else Spacer(Modifier.height(16.dp))
        Text(if (audioUnavailable) "Sound is unavailable. You can keep following the circle."
          else if (sound) "Gentle breathing sound · Sound on" else "Follow the circle · Sound off",
          color = bodyColor, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Text("Breathe comfortably. If you feel dizzy or uncomfortable, stop and return to your natural breath.",
          color = bodyColor, fontSize = 11.sp, lineHeight = 17.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.widthIn(max = 320.dp))
        Spacer(Modifier.height(16.dp))
      }
    }
  }
}
