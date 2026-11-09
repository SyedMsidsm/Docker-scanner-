package com.doclens.scanner.engine.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.doclens.scanner.core.model.CropQuad
import com.doclens.scanner.core.model.EditRecipe
import com.doclens.scanner.core.model.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max

interface ImageEngine {
    suspend fun renderProcessedBitmap(
        sourceFile: File,
        recipe: EditRecipe,
        maxDimension: Int? = null
    ): Bitmap

    suspend fun renderToFile(
        sourceFile: File,
        recipe: EditRecipe,
        outputFile: File,
        maxDimension: Int? = null,
        quality: Int = 90
    ): File
}

class AndroidImageEngine : ImageEngine {

    override suspend fun renderProcessedBitmap(
        sourceFile: File,
        recipe: EditRecipe,
        maxDimension: Int?
    ): Bitmap = withContext(Dispatchers.Default) {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)
        val origW = boundsOptions.outWidth
        val origH = boundsOptions.outHeight

        val sampleSize = if (maxDimension != null) {
            val maxSide = max(origW, origH)
            var sample = 1
            while (maxSide / (sample * 2) >= maxDimension) {
                sample *= 2
            }
            sample
        } else {
            1
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sourceBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Failed to decode source image: ${sourceFile.absolutePath}")

        val processed = applyRecipe(sourceBitmap, recipe)
        if (processed != sourceBitmap) {
            sourceBitmap.recycle()
        }
        processed
    }

    override suspend fun renderToFile(
        sourceFile: File,
        recipe: EditRecipe,
        outputFile: File,
        maxDimension: Int?,
        quality: Int
    ): File = withContext(Dispatchers.IO) {
        val bitmap = renderProcessedBitmap(sourceFile, recipe, maxDimension)
        try {
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.flush()
            }
            outputFile
        } finally {
            bitmap.recycle()
        }
    }

    private fun applyRecipe(source: Bitmap, recipe: EditRecipe): Bitmap {
        val w = source.width.toFloat()
        val h = source.height.toFloat()

        // 1. Perspective Transform (CropQuad)
        val transformed = if (!recipe.cropQuad.isDefault()) {
            val quad = recipe.cropQuad
            val srcPoints = floatArrayOf(
                quad.topLeft.x * w, quad.topLeft.y * h,
                quad.topRight.x * w, quad.topRight.y * h,
                quad.bottomRight.x * w, quad.bottomRight.y * h,
                quad.bottomLeft.x * w, quad.bottomLeft.y * h
            )

            // Calculate output width and height from quad edges
            val topWidth = hypot(srcPoints[2] - srcPoints[0], srcPoints[3] - srcPoints[1])
            val bottomWidth = hypot(srcPoints[4] - srcPoints[6], srcPoints[5] - srcPoints[7])
            val outW = max(topWidth, bottomWidth).toInt().coerceAtLeast(100)

            val leftHeight = hypot(srcPoints[6] - srcPoints[0], srcPoints[7] - srcPoints[1])
            val rightHeight = hypot(srcPoints[4] - srcPoints[2], srcPoints[5] - srcPoints[3])
            val outH = max(leftHeight, rightHeight).toInt().coerceAtLeast(100)

            val dstPoints = floatArrayOf(
                0f, 0f,
                outW.toFloat(), 0f,
                outW.toFloat(), outH.toFloat(),
                0f, outH.toFloat()
            )

            val matrix = Matrix()
            matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

            val warped = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(warped)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(source, matrix, paint)
            warped
        } else {
            source
        }

        // 2. Rotation
        val rotated = if (recipe.rotationDegrees != 0) {
            val rotMatrix = Matrix().apply {
                postRotate(recipe.rotationDegrees.toFloat())
            }
            val rotBitmap = Bitmap.createBitmap(
                transformed, 0, 0, transformed.width, transformed.height, rotMatrix, true
            )
            if (transformed != source && transformed != rotBitmap) {
                transformed.recycle()
            }
            rotBitmap
        } else {
            transformed
        }

        // 3. Color Filter, Brightness and Contrast
        val hasFilter = recipe.filter != ImageFilter.ORIGINAL
        val hasBrightness = recipe.brightness != 0f
        val hasContrast = recipe.contrast != 1f

        if (!hasFilter && !hasBrightness && !hasContrast) {
            return rotated
        }

        val filtered = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(filtered)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val combinedMatrix = ColorMatrix()

        when (recipe.filter) {
            ImageFilter.ORIGINAL -> { /* no-op */ }
            ImageFilter.COLOR_BOOST -> {
                combinedMatrix.setSaturation(1.35f)
            }
            ImageFilter.GRAYSCALE -> {
                combinedMatrix.setSaturation(0.0f)
            }
            ImageFilter.BW_CLEAN -> {
                // High contrast black & white
                val bwMatrix = ColorMatrix().apply {
                    setSaturation(0.0f)
                }
                val scale = 1.6f
                val translate = (-0.3f * 255f)
                val thresholdMatrix = ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                bwMatrix.postConcat(thresholdMatrix)
                combinedMatrix.postConcat(bwMatrix)
            }
        }

        if (hasBrightness || hasContrast) {
            val c = recipe.contrast
            val b = recipe.brightness * 255f
            val bcMatrix = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, b,
                    0f, c, 0f, 0f, b,
                    0f, 0f, c, 0f, b,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            combinedMatrix.postConcat(bcMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(rotated, 0f, 0f, paint)

        if (rotated != source) {
            rotated.recycle()
        }

        return filtered
    }
}
