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

package com.google.ai.edge.gallery.ui.common.tts

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.Voice
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import java.util.UUID

private const val TAG = "SukhamTtsHelper"

private const val NORMAL_RATE = 0.95f
private const val NORMAL_PITCH = 1.0f
private const val CALM_RATE = 0.80f
private const val CALM_PITCH = 0.92f
private const val SENTENCE_PAUSE_MS = 450L

/**
 * Helper class that encapsulates Android [TextToSpeech] engine operations,
 * cleans Markdown formatting for natural sounding speech, and exposes reactive
 * Compose state for UI playback feedback.
 */
class SukhamTtsHelper(private val context: Context) {
  private var tts: TextToSpeech? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  @Volatile private var lastUtteranceId: String? = null

  private val _isSpeaking = mutableStateOf(false)
  val isSpeaking: State<Boolean> = _isSpeaking

  private val _isReady = mutableStateOf(false)
  val isReady: State<Boolean> = _isReady

  init {
    initializeTts()
  }

  private fun initializeTts() {
    try {
      tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          val currentTts = tts
          if (currentTts != null) {
            val result = currentTts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
              currentTts.setLanguage(Locale.US)
            }
            currentTts.setSpeechRate(NORMAL_RATE)
            currentTts.setPitch(NORMAL_PITCH)
            pickCalmVoice(currentTts)
            setupProgressListener(currentTts)
            mainHandler.post { _isReady.value = true }
            Log.d(TAG, "TextToSpeech successfully initialized.")
          }
        } else {
          Log.w(TAG, "Failed to initialize TextToSpeech (status: $status)")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Exception during TextToSpeech initialization", e)
    }
  }

  /**
   * Picks the most natural installed voice for the current language.
   *
   * The default voice is usually the compact, robotic one. Offline voices with a higher quality
   * rating (the neural ones, when installed) sound far less synthetic, so prefer those and fall
   * back to the engine default if none qualify.
   */
  private fun pickCalmVoice(engine: TextToSpeech) {
    try {
      val lang = engine.voice?.locale?.language ?: Locale.getDefault().language
      val best =
        engine.voices
          ?.filter { it.locale.language == lang && !it.isNetworkConnectionRequired }
          ?.filter { Voice.QUALITY_HIGH <= it.quality }
          ?.maxByOrNull { it.quality }
      if (best != null) {
        engine.voice = best
        Log.d(TAG, "Using voice ${best.name} (quality=${best.quality})")
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not select a higher-quality voice", e)
    }
  }

  private fun setupProgressListener(engine: TextToSpeech) {
    engine.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          mainHandler.post { _isSpeaking.value = true }
        }

        override fun onDone(utteranceId: String?) {
          // A calm reading is queued as several sentences; only the last one ends the speech.
          if (utteranceId == lastUtteranceId) mainHandler.post { _isSpeaking.value = false }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          mainHandler.post { _isSpeaking.value = false }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          Log.w(TAG, "TTS utterance $utteranceId failed with code $errorCode")
          mainHandler.post { _isSpeaking.value = false }
        }
      }
    )
  }

  /**
   * Cleans Markdown characters (asterisks, bullet points, hashtags, brackets)
   * so speech reads smoothly and naturally.
   */
  fun cleanMarkdownForSpeech(text: String): String {
    return text
      // Remove headers like ### or ##
      .replace(Regex("#{1,6}\\s*"), "")
      // Remove bold and italics formatting (**bold**, *italic*, __bold__, _italic_)
      .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
      .replace(Regex("\\*([^*]+)\\*"), "$1")
      .replace(Regex("__([^_]+)__"), "$1")
      .replace(Regex("_([^_]+)_"), "$1")
      // Remove inline code ticks
      .replace(Regex("`([^`]+)`"), "$1")
      // Replace degree signs with words
      .replace("°", " degrees ")
      // Replace bullet points and dashes at start of line
      .replace(Regex("^[\\s]*[-•*]\\s+", RegexOption.MULTILINE), "")
      // Clean multiple spaces and empty lines
      .replace(Regex("[ \\t]+"), " ")
      .replace(Regex("\\n{2,}"), "\n")
      .trim()
  }

  /**
   * Speaks the provided [text], interrupting any active utterance.
   *
   * With [calm] the voice is slowed, lowered a touch, and read one sentence at a time with a
   * breath of silence between, which is what makes guidance sound unhurried rather than like a
   * single synthetic run-on.
   */
  fun speak(text: String, calm: Boolean = false) {
    val engine = tts
    if (engine == null) {
      Log.w(TAG, "speak called before TTS instance created")
      return
    }

    val cleanText = cleanMarkdownForSpeech(text)
    if (cleanText.isBlank()) return

    engine.setSpeechRate(if (calm) CALM_RATE else NORMAL_RATE)
    engine.setPitch(if (calm) CALM_PITCH else NORMAL_PITCH)

    val params = Bundle().apply {
      putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
    }
    val sentences =
      if (calm) cleanText.split(Regex("(?<=[.!?])\\s+|\\n+")).filter { it.isNotBlank() }
      else listOf(cleanText)

    _isSpeaking.value = true
    var result = TextToSpeech.SUCCESS
    sentences.forEachIndexed { i, sentence ->
      val id = UUID.randomUUID().toString()
      lastUtteranceId = id
      val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
      if (i > 0 && calm) engine.playSilentUtterance(SENTENCE_PAUSE_MS, TextToSpeech.QUEUE_ADD, null)
      if (engine.speak(sentence, mode, params, id) != TextToSpeech.SUCCESS) result = TextToSpeech.ERROR
    }
    if (result != TextToSpeech.SUCCESS) {
      Log.w(TAG, "TTS speak failed")
      _isSpeaking.value = false
    }
  }

  /** Stops any currently ongoing speech. */
  fun stop() {
    try {
      tts?.stop()
    } catch (e: Exception) {
      Log.w(TAG, "Error stopping TTS", e)
    } finally {
      _isSpeaking.value = false
    }
  }

  /** Shuts down the TTS engine and cleans up resources. */
  fun shutdown() {
    stop()
    try {
      tts?.shutdown()
    } catch (e: Exception) {
      Log.w(TAG, "Error shutting down TTS", e)
    } finally {
      tts = null
      _isReady.value = false
    }
  }
}

/**
 * Remember and lifecycle-bind a [SukhamTtsHelper] instance for a Composable screen.
 */
@Composable
fun rememberSukhamTtsHelper(): SukhamTtsHelper {
  val context = LocalContext.current
  val helper = remember(context) { SukhamTtsHelper(context) }
  DisposableEffect(helper) {
    onDispose {
      helper.shutdown()
    }
  }
  return helper
}
