package com.google.ai.edge.gallery.ui.llmchat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.ui.home.SukhamBottomNav
import com.google.ai.edge.gallery.ui.home.SukhamNavTab
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

/** The AI tab reuses the existing model download, initialization and chat UI. */
@Composable
fun SukhamAiScreen(
  modelManagerViewModel: ModelManagerViewModel,
  onNavigateUp: () -> Unit,
  onHomeClicked: () -> Unit,
  onProgramsClicked: () -> Unit,
  onProgressClicked: () -> Unit,
  onProfileClicked: () -> Unit,
) {
  val state by modelManagerViewModel.uiState.collectAsState()
  val task = modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT)
  val model = task?.models?.firstOrNull { it.name == state.selectedModel.name }
    ?: task?.models?.firstOrNull { state.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED }
    ?: task?.models?.firstOrNull()
  LaunchedEffect(model?.name) {
    if (model != null && model.name != state.selectedModel.name) modelManagerViewModel.selectModel(model)
  }
  Column(Modifier.fillMaxSize()) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
      when {
        task == null || model == null -> Text("No chat model is available. Check your AI model settings.", Modifier.padding(24.dp))
        state.selectedModel.name != model.name -> CircularProgressIndicator()
        else -> LlmChatScreen(
          modelManagerViewModel = modelManagerViewModel,
          navigateUp = onNavigateUp,
          useSavedWellnessContext = true,
          emptyStateComposable = {
            Text("Ask about your practice, progress, or a gentle routine.", Modifier.padding(32.dp))
          },
        )
      }
    }
    SukhamBottomNav(currentTab = SukhamNavTab.AI, onHomeClick = onHomeClicked,
      onProgramsClick = onProgramsClicked, onProgressClick = onProgressClicked,
      onProfileClick = onProfileClicked)
  }
}
