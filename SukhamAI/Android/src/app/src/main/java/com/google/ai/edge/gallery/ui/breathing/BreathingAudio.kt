package com.google.ai.edge.gallery.ui.breathing

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.google.ai.edge.gallery.R

/** One looping pad: matching recorded inhale/exhale cues and a quiet hold, synced to the visual clock. */
internal class BreathingAudio(context: Context, onFocusLost: () -> Unit) {
  private val manager = context.getSystemService(AudioManager::class.java)
  private val attributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()
  private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
    .setAudioAttributes(attributes)
    .setOnAudioFocusChangeListener({ change ->
      if (change == AudioManager.AUDIOFOCUS_LOSS ||
        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
        if (player?.isPlaying == true) player?.pause()
        onFocusLost()
      }
    }, Handler(Looper.getMainLooper()))
    .build()
  private var player: MediaPlayer? = MediaPlayer.create(
    context, R.raw.breathing_cycle, attributes, manager.generateAudioSessionId(),
  )?.apply {
    isLooping = true
    setVolume(0.6f, 0.6f)
  }

  fun play(elapsedMs: Long): Boolean {
    val audio = player ?: return false
    if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
    audio.seekTo((elapsedMs % BreathingTiming.CYCLE_MS).toInt())
    audio.start()
    return true
  }

  fun pause() {
    if (player?.isPlaying == true) player?.pause()
    manager.abandonAudioFocusRequest(focus)
  }

  fun release() {
    pause()
    player?.release()
    player = null
  }
}
