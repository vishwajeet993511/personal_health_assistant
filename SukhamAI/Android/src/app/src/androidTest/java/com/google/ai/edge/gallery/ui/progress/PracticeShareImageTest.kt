package com.google.ai.edge.gallery.ui.progress

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PracticeShareImageTest {
  private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
  private val data = PracticeShareData("My week in motion", "12–18 Oct 2026", "48", "minutes practiced", "5", "active days", listOf(true,true,false,true,false,true,true), listOf("M","T","W","T","F","S","S"), "Made time for myself.", true)

  @Test fun exportsStoryAndPostAndPublishesGalleryImage() {
    val story = renderPracticeCard(data, PracticeCardStyle.CREAM, true, data.defaultCaption, "")
    val post = renderPracticeCard(data, PracticeCardStyle.FOREST, false, "Small moments, a little more peace. Made time for myself every day this week.", "A name I chose")
    assertEquals(1080, story.width); assertEquals(1920, story.height)
    assertEquals(1080, post.width); assertEquals(1350, post.height)
    context.openFileOutput("practice-story-preview.png", 0).use { story.compress(Bitmap.CompressFormat.PNG, 100, it) }
    context.openFileOutput("practice-post-preview.png", 0).use { post.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = savePracticeCard(context, story)
    try {
      context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.IS_PENDING, MediaStore.Images.Media.RELATIVE_PATH), null, null, null)!!.use {
        assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)); assertTrue(it.getString(1).contains("Pictures/Sukham"))
      }
      context.contentResolver.openInputStream(uri).use { assertEquals(1920, BitmapFactory.decodeStream(it).height) }
    } finally { context.contentResolver.delete(uri, null, null); story.recycle(); post.recycle() }
  }

  @Test fun shareUsesReadableFileProviderUriWithoutSavingToGallery() {
    val bitmap = renderPracticeCard(data, PracticeCardStyle.SAGE, false, data.defaultCaption, "")
    val intent = practiceCardShareIntent(context, bitmap)
    assertEquals(Intent.ACTION_SEND, intent.action); assertEquals("image/png", intent.type)
    assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    val uri = intent.clipData!!.getItemAt(0).uri
    assertEquals("content", uri.scheme)
    context.contentResolver.openInputStream(uri).use { assertEquals(1080, BitmapFactory.decodeStream(it).width) }
    bitmap.recycle()
  }
}
