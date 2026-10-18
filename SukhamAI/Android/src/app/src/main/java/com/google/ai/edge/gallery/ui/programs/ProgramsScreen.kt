package com.google.ai.edge.gallery.ui.programs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.google.ai.edge.gallery.ui.home.SukhamBottomNav
import com.google.ai.edge.gallery.ui.home.SukhamNavTab
import com.google.ai.edge.gallery.ui.theme.SukhamColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

private data class ProgramPose(
  val id: String,
  val name: String,
  val sanskrit: String,
  val category: String,
  val difficulty: String,
  @DrawableRes val image: Int,
  val sourceUrl: String,
)

private val categories = listOf("All poses", "Standing", "Seated", "Supine", "Prone", "Arm & leg support", "Balances & inversions")
private val illustrationBg = Color.White
private val forest = Color(0xFF254C3D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
  onNavigateUp: () -> Unit, modifier: Modifier = Modifier,
  onHomeClicked: () -> Unit = onNavigateUp,
  onLiveClicked: () -> Unit = {},
  onProgressClicked: () -> Unit = {},
  onProfileClicked: () -> Unit = {},
) {
  val context = LocalContext.current
  val uriHandler = LocalUriHandler.current
  val poses by produceState<List<ProgramPose>?>(null) {
    value = withContext(Dispatchers.IO) {
      val json = JSONArray(context.assets.open("program_poses.json").bufferedReader().use { it.readText() })
      List(json.length()) { index ->
        val pose = json.getJSONObject(index)
        ProgramPose(
          pose.getString("id"), pose.getString("name"), pose.getString("sanskrit"),
          pose.getString("category"), pose.getString("difficulty"),
          programPoseImages.getValue(pose.getString("image")), pose.getString("sourceUrl"),
        )
      }
    }
  }
  var query by rememberSaveable { mutableStateOf("") }
  var category by rememberSaveable { mutableStateOf("All poses") }
  var difficulty by rememberSaveable { mutableStateOf("All levels") }
  val preferences = remember { context.getSharedPreferences("program_builder", android.content.Context.MODE_PRIVATE) }
  var selectedIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
  var recentIds by remember { mutableStateOf(preferences.getString("recent", "").orEmpty().split(",").filter { it.isNotBlank() }) }
  var sort by rememberSaveable { mutableStateOf("Featured") }
  var reviewing by rememberSaveable { mutableStateOf(false) }
  val searchFocus = remember { FocusRequester() }
  val gridState = rememberLazyGridState()
  val scope = rememberCoroutineScope()
  val toggle: (ProgramPose) -> Unit = { pose ->
    selectedIds = ArrayList(if (pose.id in selectedIds) selectedIds - pose.id else selectedIds + pose.id)
    recentIds = (listOf(pose.id) + recentIds).distinct().take(10)
    preferences.edit().putString("recent", recentIds.joinToString(",")).apply()
  }
  var selectedPose by remember { mutableStateOf<ProgramPose?>(null) }
  val filtered = remember(poses, query, category, difficulty, sort) {
    val matching = poses.orEmpty().filter {
      (category == "All poses" || it.category == category) &&
        (difficulty == "All levels" || it.difficulty == difficulty) &&
        (query.isBlank() || "${it.name} ${it.sanskrit} ${it.category}".contains(query.trim(), ignoreCase = true))
    }
    when (sort) {
      "Name A–Z" -> matching.sortedBy { it.name }
      "Difficulty" -> matching.sortedBy { listOf("Beginner", "Intermediate", "Expert").indexOf(it.difficulty) }
      else -> matching.sortedBy { pose -> listOf("DownwardDog", "WarriorII", "Cobra", "Tree", "Triangle", "Child").indexOf(pose.id).let { if (it < 0) Int.MAX_VALUE else it } }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = SukhamColors.ScreenBg,
    bottomBar = {
      Column {
        Surface(color = SukhamColors.CardBg, shadowElevation = 8.dp) {
          Button(
            onClick = { reviewing = true }, enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = forest),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
          ) {
            Text("${selectedIds.size} Selected  ·  ~${selectedIds.size * 2} mins", modifier = Modifier.weight(1f))
            Text("Review Program →")
          }
        }
        SukhamBottomNav(
          currentTab = SukhamNavTab.PROGRAMS,
          onHomeClick = onHomeClicked,
          onLiveClick = onLiveClicked,
          onProgressClick = onProgressClicked,
          onProfileClick = onProfileClicked,
        )
      }
    },
    topBar = {
      TopAppBar(
        title = { Text("Programs", color = SukhamColors.TitleBlack, fontSize = 20.sp) },
        actions = { IconButton(onClick = { scope.launch { gridState.scrollToItem(0); withFrameNanos { }; searchFocus.requestFocus() } }) { Icon(Icons.Outlined.Search, "Search poses") } },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = SukhamColors.TitleBlack)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SukhamColors.ScreenBg),
      )
    },
  ) { padding ->
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      state = gridState,
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        OutlinedTextField(
          value = query, onValueChange = { query = it },
          modifier = Modifier.fillMaxWidth().focusRequester(searchFocus), singleLine = true,
          placeholder = { Text("Search poses", fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Outlined.Search, null) },
          trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") }
          },
          shape = RoundedCornerShape(18.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SukhamColors.TitleBlack, unfocusedTextColor = SukhamColors.TitleBlack,
            focusedBorderColor = SukhamColors.SagePrimary, unfocusedBorderColor = SukhamColors.CardBorder,
            focusedContainerColor = SukhamColors.CardBg, unfocusedContainerColor = SukhamColors.CardBg,
          ),
        )
      }
      item(span = { GridItemSpan(maxLineSpan) }) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item { LibraryMenu("Category", category, categories) { category = it } }
          item { LibraryMenu("Difficulty", difficulty, listOf("All levels", "Beginner", "Intermediate", "Expert")) { difficulty = it } }
          item { TextButton(onClick = { query = ""; category = "All poses"; difficulty = "All levels" }) { Text("Reset") } }
        }
      }
      if (recentIds.isNotEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("RECENTLY SELECTED", color = SukhamColors.BodyGray, fontSize = 11.sp, letterSpacing = 1.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              items(recentIds.mapNotNull { id -> poses.orEmpty().find { it.id == id } }, key = { it.id }) { pose ->
                Card(onClick = { selectedPose = pose }, modifier = Modifier.width(140.dp), colors = CardDefaults.cardColors(containerColor = SukhamColors.CardBg)) {
                  Image(painterResource(pose.image), pose.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(96.dp).background(illustrationBg))
                  Text(pose.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 13.sp)
                  TextButton(onClick = { toggle(pose) }) { Text(if (pose.id in selectedIds) "✓ Added" else "+ Add to Flow") }
                }
              }
            }
          }
        }
      }
      item(span = { GridItemSpan(maxLineSpan) }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text("ALL POSES (${filtered.size})", color = SukhamColors.TitleBlack, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
          LibraryMenu("Sort", sort, listOf("Featured", "Name A–Z", "Difficulty")) { sort = it }
        }
      }
      if (poses == null) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SukhamColors.SagePrimary) }
        }
      } else if (filtered.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
          Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No poses found", color = SukhamColors.TitleBlack, fontSize = 20.sp)
            Text("Try another name, category or level.", color = SukhamColors.BodyGray, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = { query = ""; category = "All poses"; difficulty = "All levels" }) { Text("Reset filters") }
          }
        }
      }
      items(filtered, key = { it.id }) { pose -> PoseCard(pose, pose.id in selectedIds, { toggle(pose) }) { selectedPose = pose } }
      item(span = { GridItemSpan(maxLineSpan) }) {
        TextButton(onClick = { uriHandler.openUri("https://www.pocketyoga.com/pose/") }) {
          Text("Pose reference · Pocket Yoga © Rainfrog", color = SukhamColors.MutedGray, fontSize = 10.sp)
        }
      }
    }
  }

  if (reviewing) {
    ModalBottomSheet(onDismissRequest = { reviewing = false }, containerColor = SukhamColors.CardBg) {
      Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Review Program", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = SukhamColors.TitleBlack)
        Text("${selectedIds.size} poses · ~${selectedIds.size * 2} minutes", color = SukhamColors.BodyGray)
        Text("Estimated at 2 minutes per pose, including transitions.", fontSize = 12.sp, color = SukhamColors.BodyGray)
        androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f, fill = false)) {
          items(selectedIds.toList(), key = { it }) { id ->
            poses.orEmpty().find { it.id == id }?.let { pose ->
              Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(pose.image), null, modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit)
                Text(pose.name, modifier = Modifier.weight(1f).padding(12.dp), color = SukhamColors.TitleBlack)
                IconButton(onClick = { toggle(pose) }) { Icon(Icons.Outlined.Close, "Remove ${pose.name}") }
              }
            }
          }
        }
        Button(onClick = { reviewing = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = forest)) { Text("Continue choosing poses") }
      }
    }
  }

  selectedPose?.let { pose ->
    ModalBottomSheet(onDismissRequest = { selectedPose = null }, containerColor = SukhamColors.CardBg) {
      Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(pose.category, color = SukhamColors.BodyGray, fontSize = 12.sp, modifier = Modifier.weight(1f))
          IconButton(onClick = { selectedPose = null }) { Icon(Icons.Outlined.Close, "Close pose", tint = SukhamColors.TitleBlack) }
        }
        Image(painterResource(pose.image), pose.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).aspectRatio(1.3f).clip(RoundedCornerShape(24.dp)).background(illustrationBg).padding(20.dp))
        Text(pose.name, color = SukhamColors.TitleBlack, fontFamily = FontFamily.Serif, fontSize = 28.sp)
        if (pose.sanskrit.isNotBlank()) Text(pose.sanskrit, color = SukhamColors.BodyGray, fontSize = 16.sp)
        Text(pose.difficulty, color = SukhamColors.SagePrimary, fontWeight = FontWeight.Medium)
        Button(onClick = { toggle(pose) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = forest)) { Text(if (pose.id in selectedIds) "✓ Added · Remove from Flow" else "+ Add to Flow") }
        Button(onClick = { uriHandler.openUri(pose.sourceUrl) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = forest, contentColor = Color.White)) {
          Text("Read pose guide on Pocket Yoga")
        }
      }
    }
  }
}

@Composable
private fun LibraryMenu(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, SukhamColors.CardBorder)) {
      Text("$label: $value ▾", fontSize = 11.sp, color = forest)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
    }
  }
}

@Composable
private fun PoseCard(pose: ProgramPose, selected: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
  Card(
    onClick = onClick, modifier = Modifier.fillMaxWidth().height(296.dp), shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamColors.CardBg),
    border = BorderStroke(if (selected) 2.dp else 0.7.dp, if (selected) forest else SukhamColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Box(Modifier.fillMaxWidth().height(128.dp).background(illustrationBg), contentAlignment = Alignment.Center) {
      Image(painterResource(pose.image), pose.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
    }
    Column(Modifier.fillMaxWidth().weight(1f).padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
      Text("● ${if (pose.difficulty == "Expert") "Advanced" else pose.difficulty}", color = when (pose.difficulty) { "Beginner" -> forest; "Intermediate" -> Color(0xFF957220); else -> Color(0xFFAD5149) }, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
      Text(pose.name, color = SukhamColors.TitleBlack, modifier = Modifier.weight(1f), fontSize = 16.sp, fontFamily = FontFamily.Serif, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
      Text(pose.category, color = SukhamColors.MutedGray, fontSize = 10.sp)
      FilledTonalButton(onClick = onToggle, modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (selected) forest else Color(0xFFE8EEE6), contentColor = if (selected) Color.White else forest), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) {
        Text(if (selected) "✓ Added" else "+ Add to Flow", fontSize = 12.sp)
      }
    }
  }
}
