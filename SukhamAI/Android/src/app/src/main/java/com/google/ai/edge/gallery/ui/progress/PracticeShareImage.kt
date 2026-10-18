package com.google.ai.edge.gallery.ui.progress

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

internal enum class PracticeCardStyle(val label: String, val background: Int, val ink: Int, val accent: Int) {
  CREAM("Cream", Color.rgb(247,245,236), Color.rgb(40,65,49), Color.rgb(216,224,201)),
  SAGE("Sage", Color.rgb(220,229,212), Color.rgb(39,65,46), Color.rgb(188,207,172)),
  FOREST("Forest", Color.rgb(30,54,43), Color.rgb(244,242,226), Color.rgb(71,100,73)),
}

/** The same bitmap is previewed, shared and saved; exports never capture app controls. */
internal fun renderPracticeCard(data: PracticeShareData, style: PracticeCardStyle, story: Boolean, caption: String, name: String): Bitmap {
  val height = if (story) 1920 else 1350
  val bitmap = Bitmap.createBitmap(1080, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)
  val paint = Paint(Paint.ANTI_ALIAS_FLAG)
  canvas.drawColor(style.background)
  val top = if (story) 250f else 100f
  fun text(value: String, y: Float, size: Float, serif: Boolean = false, alpha: Int = 255) {
    paint.apply { color = style.ink; this.alpha = alpha; textSize = size; textAlign = Paint.Align.CENTER
      typeface = Typeface.create(if (serif) "serif" else "sans-serif", Typeface.NORMAL) }
    canvas.drawText(value, 540f, y, paint)
  }
  text("S U K H A M", top, 26f)
  text("A little practice.", top + 108, 64f, true)
  text("A little more peace.", top + 185, 64f, true)

  // Abstract sunrise and botanical petals: understated artwork that remains crisp on export.
  val centerY = top + 375
  paint.apply { color = style.accent; alpha = 255; this.style = Paint.Style.FILL }
  canvas.drawCircle(540f, centerY, 125f, paint)
  paint.apply { color = style.ink; alpha = 110; this.style = Paint.Style.STROKE; strokeWidth = 3f }
  for (rotation in listOf(-48f, -24f, 0f, 24f, 48f)) {
    canvas.save()
    canvas.rotate(rotation, 540f, centerY + 110)
    canvas.drawOval(RectF(502f, centerY - 80, 578f, centerY + 110), paint)
    canvas.restore()
  }
  canvas.drawLine(375f, centerY + 125, 705f, centerY + 125, paint)
  paint.style = Paint.Style.FILL
  text(data.heading, top + 615, 35f)
  text(data.dateLabel, top + 665, 25f, alpha = 165)
  fun metric(value: String, label: String, x: Float) {
    paint.apply { color = style.ink; alpha = 255; textAlign = Paint.Align.CENTER; textSize = 92f; typeface = Typeface.create("serif", Typeface.NORMAL) }
    canvas.drawText(value, x, top + 800, paint)
    paint.apply { textSize = 25f; alpha = 180; typeface = Typeface.create("sans-serif", Typeface.NORMAL) }
    canvas.drawText(label, x, top + 852, paint)
  }
  metric(data.firstValue, data.firstLabel, 320f)
  metric(data.secondValue, data.secondLabel, 760f)
  paint.apply { color = style.ink; alpha = 50; strokeWidth = 2f }
  canvas.drawLine(540f, top + 720, 540f, top + 860, paint)
  data.activeDays.forEachIndexed { i, active ->
    val x = 330f + i * 70f
    paint.apply { color = style.ink; alpha = if (active) 230 else 45 }
    canvas.drawCircle(x, top + 925, 10f, paint)
    paint.apply { textSize = 18f; textAlign = Paint.Align.CENTER; alpha = 160 }
    canvas.drawText(data.dayLabels[i], x, top + 964, paint)
  }
  val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = style.ink; textSize = 34f; typeface = Typeface.create("serif", Typeface.ITALIC)
  }
  val captionLayout = StaticLayout.Builder.obtain(caption.take(80), 0, caption.take(80).length, captionPaint, 840)
    .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setMaxLines(2)
    .setEllipsize(TextUtils.TruncateAt.END).build()
  canvas.save()
  canvas.translate(120f, top + 1010)
  captionLayout.draw(canvas)
  canvas.restore()
  if (name.isNotBlank()) text(name.take(32), top + 1140, 24f, alpha = 180)
  text("A little practice, every day.  ·  Sukham", height - if (story) 180f else 46f, 21f, alpha = 150)
  return bitmap
}

internal fun savePracticeCard(context: Context, bitmap: Bitmap): Uri {
  val resolver = context.contentResolver
  val values = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, "Sukham_Practice_${UUID.randomUUID()}.png")
    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Sukham")
    put(MediaStore.Images.Media.IS_PENDING, 1)
  }
  val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Unable to create gallery image")
  try {
    checkNotNull(resolver.openOutputStream(uri)).use { stream ->
      check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Unable to encode card" }
    }
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    check(resolver.update(uri, values, null, null) > 0) { "Unable to publish gallery image" }
    return uri
  } catch (e: Exception) {
    resolver.delete(uri, null, null)
    throw e
  }
}

internal fun practiceCardShareIntent(context: Context, bitmap: Bitmap): Intent {
  val directory = File(context.cacheDir, "images/practice_cards").apply { check(mkdirs() || isDirectory) }
  // Keep the newest exports long enough for receiving apps to read them.
  directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 7 * 86_400_000L }?.forEach { it.delete() }
  val file = File(directory, "Sukham_Practice_${UUID.randomUUID()}.png")
  file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
  val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
  return Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri)
    clipData = ClipData.newRawUri("Sukham practice card", uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
}
