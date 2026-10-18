package com.google.ai.edge.gallery.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.ui.home.SukhamBottomNav
import com.google.ai.edge.gallery.ui.home.SukhamNavTab
import com.google.ai.edge.gallery.ui.theme.SukhamColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(onNavigateUp: () -> Unit, onHomeClicked: () -> Unit, onProgramsClicked: () -> Unit,
  onLiveClicked: () -> Unit, onProfileClicked: () -> Unit, onBreathingClicked: () -> Unit) {
  val context = LocalContext.current
  val repository = remember { ProgressRepository.get(context) }
  val sessions by repository.sessions.collectAsState()
  val loading by repository.loading.collectAsState()
  val error by repository.error.collectAsState()
  val preferences = remember { context.getSharedPreferences("progress_preferences", android.content.Context.MODE_PRIVATE) }
  var period by remember { mutableStateOf(preferences.getString("period", "Week").takeIf { it in listOf("Week", "Month", "All time") } ?: "Week") }
  var menu by remember { mutableStateOf(false) }
  var anchor by remember { mutableStateOf(LocalDate.now()) }
  val today = LocalDate.now()
  val from = when(period) {
    "Week" -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    "Month" -> anchor.withDayOfMonth(1)
    else -> sessions.minOfOrNull { minOf(it.date, it.practiceByDay.keys.minOrNull() ?: it.date) } ?: today
  }
  val through = when(period) { "Week" -> from.plusDays(6); "Month" -> from.plusMonths(1).minusDays(1); else -> today }
  val summary = remember(sessions, from, through) { summarizeProgress(sessions, from, through) }
  var detail by remember { mutableStateOf<ActivitySession?>(null) }
  var typeDetail by remember { mutableStateOf<ActivityType?>(null) }

  Scaffold(containerColor = SukhamColors.ScreenBg,
    topBar = { TopAppBar(title = { Text("Progress", color = SukhamColors.TitleBlack, fontSize = 18.sp) },
      navigationIcon = { IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = SukhamColors.TitleBlack) } },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = SukhamColors.ScreenBg)) },
    bottomBar = { SukhamBottomNav(currentTab = SukhamNavTab.PROGRESS, onHomeClick = onHomeClicked,
      onProgramsClick = onProgramsClicked, onLiveClick = onLiveClicked, onProfileClick = onProfileClicked) },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Text("A little practice, every day.", color = SukhamColors.BodyGray, fontSize = 14.sp)
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Practice overview", color = SukhamColors.TitleBlack, fontSize = 16.sp)
        Box {
          FilledTonalButton(onClick = { menu = true }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = SukhamColors.SageContainer, contentColor = SukhamColors.SagePrimary)) { Text("$period ▾", fontSize = 12.sp) }
          DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            listOf("Week", "Month", "All time").forEach { value -> DropdownMenuItem(text = { Text(value) }, onClick = {
              period = value; anchor = today; menu = false; preferences.edit().putString("period", value).apply()
            }) }
          }
        }
      }
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        if (period != "All time") {
          IconButton(onClick = { anchor = if (period == "Week") anchor.minusWeeks(1) else anchor.minusMonths(1) }) { Icon(Icons.Outlined.ChevronLeft, "Previous $period", tint = SukhamColors.SagePrimary) }
        }
        Text("${from.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} – ${through.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}", color = SukhamColors.BodyGray, fontSize = 12.sp)
        if (period != "All time") {
          IconButton(onClick = { anchor = if (period == "Week") anchor.plusWeeks(1) else anchor.plusMonths(1) }, enabled = through < today) { Icon(Icons.Outlined.ChevronRight, "Next $period") }
        }
      }
      if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = SukhamColors.SagePrimary)
      error?.let { message ->
        Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        TextButton(onClick = repository::retry) { Text("Try again") }
      }
      Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Stat("${displayMinutes(summary.practiceMs)} min", "Practiced", Modifier.weight(1f))
        Stat(summary.activeDays.toString(), "Active days", Modifier.weight(1f))
        Stat(summary.completedSessions.toString(), "Sessions", Modifier.weight(1f))
      }
      PracticeChart(summary, from, through, period)
      val completed = summary.activities.filter { it.status == ActivityStatus.COMPLETED }
      listOf(listOf(ActivityType.PROGRAM, ActivityType.LIVE), listOf(ActivityType.PHOTO, ActivityType.VIDEO)).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          pair.forEach { type ->
            val records = completed.filter { it.type == type }
            val time = summary.daily.values.sumOf { it[type] ?: 0L }
            FeatureCard(type, records, time, Modifier.weight(1f), onClick = { typeDetail = type })
          }
        }
      }
      Surface(color = SukhamColors.CardBg, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Breathing", color = SukhamColors.TitleBlack, fontSize = 17.sp)
          val records = completed.filter { it.type == ActivityType.BREATHING }
          Text("${records.size} sessions · ${displayMinutes(summary.daily.values.sumOf { it[ActivityType.BREATHING] ?: 0L })} min practiced", color = SukhamColors.BodyGray, fontSize = 13.sp)
          Row(Modifier.fillMaxWidth()) {
            listOf(1, 2, 3, 5).forEach { minutes ->
              Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("$minutes min", color = SukhamColors.TitleBlack, fontSize = 13.sp)
                val count = records.count { it.plannedMs == minutes * 60_000L }
                Text("$count ${if (count == 1) "session" else "sessions"}", color = SukhamColors.BodyGray, fontSize = 11.sp)
              }
            }
          }
        }
      }
      Text("Recent activity", fontFamily = FontFamily.Serif, fontSize = 23.sp, color = SukhamColors.TitleBlack)
      val recent = summary.activities.filter { it.status !in listOf(ActivityStatus.IN_PROGRESS, ActivityStatus.PAUSED) }.take(3)
      if (recent.isEmpty()) Text("No saved activity in this period yet.", color = SukhamColors.BodyGray, fontSize = 13.sp)
      recent.forEach { record ->
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { detail = record }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(record.title, color = SukhamColors.TitleBlack, fontSize = 15.sp)
            Text("${record.date.format(DateTimeFormatter.ofPattern("d MMM"))} · ${record.type.title}", color = SukhamColors.BodyGray, fontSize = 12.sp)
          }
          Text(if (record.type.isPractice) "${displayMinutes(record.activeMs)} min" else statusLabel(record.status), color = SukhamColors.BodyGray, fontSize = 12.sp)
        }
        HorizontalDivider(color = SukhamColors.CardBorder.copy(alpha = 0.6f))
      }
      Text("Practice time excludes pauses. Reviews are counted separately. Partial sessions contribute time, but not completed-session counts.", color = SukhamColors.BodyGray, fontSize = 11.sp, lineHeight = 17.sp)
      Spacer(Modifier.height(12.dp))
    }
  }
  typeDetail?.let { type ->
    val records = summary.activities.filter { it.type == type && it.status == ActivityStatus.COMPLETED }
    val highlights = records.flatMap { it.poseCounts.entries }.groupBy { it.key }.mapValues { it.value.sumOf { entry -> entry.value } }.toList().sortedByDescending { it.second }
    AlertDialog(onDismissRequest = { typeDetail = null }, containerColor = SukhamColors.CardBg,
      title = { Text(type.title, fontFamily = FontFamily.Serif, color = SukhamColors.TitleBlack) },
      text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (type == ActivityType.PROGRAM) {
          val names = records.groupingBy { it.title }.eachCount().toList().sortedByDescending { it.second }
          names.take(5).forEach { (name, count) -> Text("$name · $count completed", color = SukhamColors.BodyGray) }
        } else highlights.take(5).forEach { (name, count) -> Text("$name · $count ${if (type == ActivityType.LIVE) "coaching checks" else "reviews"}", color = SukhamColors.BodyGray) }
        if (records.isEmpty()) Text(if (type == ActivityType.PROGRAM) "The program builder currently selects poses. Practice totals will appear when guided program sessions are available." else "No completed ${type.title.lowercase()} activity yet.", color = SukhamColors.BodyGray)
        records.take(5).forEach { record ->
          TextButton(onClick = { typeDetail = null; detail = record }) { Text("${record.date.format(DateTimeFormatter.ofPattern("d MMM yy"))} · ${record.title}", color = SukhamColors.SagePrimary) }
        }
      } }, confirmButton = { TextButton(onClick = { typeDetail = null }) { Text("Done") } })
  }
  detail?.let { record ->
    AlertDialog(onDismissRequest = { detail = null }, containerColor = SukhamColors.CardBg,
      title = { Text(record.title, fontFamily = FontFamily.Serif, color = SukhamColors.TitleBlack) },
      text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${record.date.format(DateTimeFormatter.ofPattern("d MMM yy"))} · ${statusLabel(record.status)}", color = SukhamColors.BodyGray)
        if (record.type.isPractice) Text("${displayMinutes(record.activeMs)} minutes practiced", color = SukhamColors.BodyGray)
        if (record.type == ActivityType.VIDEO) Text("${displayMinutes(record.analyzedMs)} minutes of footage reviewed through sampled frames", color = SukhamColors.BodyGray)
        Text(record.feedback.ifBlank { "No coaching notes for this session." }, color = SukhamColors.TitleBlack, fontSize = 14.sp, lineHeight = 15.sp)
      } }, confirmButton = { TextButton(onClick = { detail = null }) { Text("Done") } })
  }
}

internal fun displayMinutes(ms: Long): String {
  if (ms <= 0) return "0"
  if (ms < 60_000) return "<1"
  return (ms / 60_000).toString()
}
private fun statusLabel(status: ActivityStatus) = when(status) {
  ActivityStatus.COMPLETED -> "Completed"; ActivityStatus.ENDED_EARLY -> "Ended early"
  ActivityStatus.INTERRUPTED -> "Interrupted"; ActivityStatus.FAILED -> "Failed"
  ActivityStatus.PAUSED -> "Paused"; else -> "In progress"
}
@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
  Surface(modifier = modifier.fillMaxHeight(), color = SukhamColors.CardBg, shape = RoundedCornerShape(22.dp)) {
    Column(Modifier.heightIn(min = 104.dp).padding(horizontal = 8.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
      Text(value, fontSize = 24.sp, fontFamily = FontFamily.Serif, color = SukhamColors.TitleBlack)
      Text(label, fontSize = 10.sp, color = SukhamColors.BodyGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
  }
}
@Composable
private fun FeatureCard(type: ActivityType, records: List<ActivitySession>, timeMs: Long, modifier: Modifier, onClick: () -> Unit) {
  val cardHeight = 220.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
  Surface(modifier = modifier.height(cardHeight), color = SukhamColors.CardBg, shape = RoundedCornerShape(22.dp)) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(type.title, color = SukhamColors.TitleBlack, fontSize = 15.sp)
      Text("${records.size} ${if (type == ActivityType.PROGRAM) "completed" else if (type.isPractice) "sessions" else "reviews"}", fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 26.sp, minLines = 2, color = SukhamColors.TitleBlack)
      Text(when(type) {
        ActivityType.PHOTO -> "${records.flatMap { it.poseCounts.keys }.distinct().size} poses reviewed"
        ActivityType.VIDEO -> "${displayMinutes(records.sumOf { it.analyzedMs })} min footage reviewed"
        else -> "${displayMinutes(timeMs)} min practiced"
      }, color = SukhamColors.BodyGray, fontSize = 12.sp, lineHeight = 16.sp, minLines = 2)
      Spacer(Modifier.weight(1f))
      TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) {
        Text(when(type) { ActivityType.PROGRAM -> "Most practiced →"; ActivityType.LIVE -> "Pose highlights →"; else -> "Review history →" }, color = SukhamColors.SagePrimary, fontSize = 11.sp)
      }
    }
  }
}
@Composable
private fun PracticeChart(summary: ProgressSummary, from: LocalDate, through: LocalDate, period: String) {
  val types = listOf(ActivityType.PROGRAM, ActivityType.LIVE, ActivityType.BREATHING)
  val colors = listOf(SukhamColors.SagePrimary, SukhamColors.TealAccent, SukhamColors.SageDark)
  val bins: List<Pair<String, List<LocalDate>>> = when(period) {
    "Week" -> (0L..6L).map { offset -> val date = from.plusDays(offset); date.format(DateTimeFormatter.ofPattern("EEE")) to listOf(date) }
    "Month" -> generateSequence(from) { it.plusDays(7).takeIf { next -> next <= through } }.map { start ->
      "${start.dayOfMonth}" to (0L..6L).map { start.plusDays(it) }.filter { it <= through }
    }.toList()
    else -> (5L downTo 0L).map { back ->
      val first = LocalDate.now().withDayOfMonth(1).minusMonths(back)
      first.format(DateTimeFormatter.ofPattern("MMM")) to (0L until first.lengthOfMonth().toLong()).map { first.plusDays(it) }
    }
  }
  val values = bins.map { (_, dates) -> types.map { type -> dates.sumOf { summary.daily[it]?.get(type) ?: 0L } } }
  val maximum = values.maxOfOrNull { it.sum() }?.coerceAtLeast(60_000L) ?: 60_000L
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(if (period == "Week") "Your week" else if (period == "Month") "Your month" else "Last six months", fontFamily = FontFamily.Serif, fontSize = 23.sp, color = SukhamColors.TitleBlack)
    Text(if (period == "Month") "Practice minutes · grouped by seven days" else "Practice minutes", color = SukhamColors.BodyGray, fontSize = 12.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
      bins.forEachIndexed { index, (label, _) ->
        val totals = values[index]
        val total = totals.sum()
        Column(Modifier.weight(1f).semantics { contentDescription = "$label: ${displayMinutes(total)} minutes; " + types.mapIndexed { i, type -> "${type.title} ${displayMinutes(totals[i])} minutes" }.joinToString() }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(Modifier.height(130.dp), contentAlignment = Alignment.BottomCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(displayMinutes(total), color = SukhamColors.BodyGray, fontSize = 10.sp)
              Spacer(Modifier.height(5.dp))
              Column(Modifier.width(20.dp).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))) {
                totals.indices.reversed().forEach { i ->
                  if (totals[i] > 0) Box(Modifier.fillMaxWidth().height((100f * totals[i] / maximum).dp).background(colors[i]))
                }
              }
            }
          }
          Text(label, color = SukhamColors.BodyGray, fontSize = 10.sp)
        }
      }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      types.forEachIndexed { i, type ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(colors[i]))
          Text(type.title, fontSize = 10.sp, color = SukhamColors.BodyGray)
        }
      }
    }
  }
}
