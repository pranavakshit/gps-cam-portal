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
            // 1. Scale down the image if it's too large (e.g. max 1920px) to save disk/bandwidth
            val maxDimension = 1920
            val scale = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                Math.min(
                    maxDimension.toFloat() / originalBitmap.width,
                    maxDimension.toFloat() / originalBitmap.height
                )
            } else {
                1f
            }

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            // 2. Draw watermark on the scaled image
            val resultBitmap = Bitmap.createBitmap(
                scaledBitmap.width,
                scaledBitmap.height,
                scaledBitmap.config ?: Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (scaledBitmap.height * 0.03f).coerceAtLeast(30f) // Dynamic text size
                setShadowLayer(5f, 2f, 2f, Color.BLACK)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            val dateString = dateFormat.format(Date(timestamp))

            val margin = scaledBitmap.width * 0.05f
            var startY = scaledBitmap.height - margin

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

            // 3. Compress using lower JPEG quality (70 instead of 90)
            FileOutputStream(outputFile).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            
            resultBitmap.recycle()
            if (scale < 1f) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
