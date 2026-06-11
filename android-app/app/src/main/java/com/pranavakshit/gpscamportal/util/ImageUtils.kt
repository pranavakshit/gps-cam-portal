package com.pranavakshit.gpscamportal.util

import android.graphics.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {

    fun addWatermarkAndSave(
        originalBitmap: Bitmap,
        outputFile: File,
        area: String,
        district: String,
        state: String,
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ): Boolean {
        return try {
            val resultBitmap = Bitmap.createBitmap(
                originalBitmap.width,
                originalBitmap.height,
                originalBitmap.config ?: Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (originalBitmap.height * 0.03f).coerceAtLeast(30f) // Dynamic text size
                setShadowLayer(5f, 2f, 2f, Color.BLACK)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val dateString = dateFormat.format(Date(timestamp))

            val margin = originalBitmap.width * 0.05f
            var startY = originalBitmap.height - margin

            // Draw text from bottom to top
            val lines = listOf(
                "Date: $dateString",
                "Lat: $latitude, Lng: $longitude",
                "Loc: $area, $district, $state"
            )

            for (line in lines) {
                canvas.drawText(line, margin, startY, paint)
                startY -= (paint.textSize * 1.5f)
            }

            FileOutputStream(outputFile).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            resultBitmap.recycle()
            originalBitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
