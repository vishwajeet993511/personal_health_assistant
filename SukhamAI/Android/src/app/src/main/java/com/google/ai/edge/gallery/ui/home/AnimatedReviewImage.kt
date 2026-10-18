package com.google.ai.edge.gallery.ui.home

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ai.edge.gallery.R

/** Plays the bundled illustration only while its host is active and the app is visible. */
@Composable
internal fun AnimatedReviewImage(active: Boolean, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val drawable = remember(context) {
    ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, R.raw.video_review_transition))
      as AnimatedImageDrawable
  }
  DisposableEffect(drawable, lifecycle, active) {
    fun updatePlayback() {
      if (active && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) drawable.start()
      else drawable.stop()
    }
    val observer = LifecycleEventObserver { _, _ -> updatePlayback() }
    lifecycle.addObserver(observer)
    updatePlayback()
    onDispose {
      lifecycle.removeObserver(observer)
      drawable.stop()
    }
  }
  AndroidView(
    modifier = modifier,
    factory = { viewContext ->
      ImageView(viewContext).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        contentDescription = "Standing to Warrior II with illustrative pose-analysis lines"
        setImageDrawable(drawable)
      }
    },
  )
}
