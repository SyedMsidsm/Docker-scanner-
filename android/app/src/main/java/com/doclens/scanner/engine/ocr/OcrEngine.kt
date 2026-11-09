package com.doclens.scanner.engine.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.doclens.scanner.core.model.NormalizedRect
import com.doclens.scanner.core.model.OcrBlock
import com.doclens.scanner.core.model.OcrLine
import com.doclens.scanner.core.model.OcrWord
import com.doclens.scanner.core.model.PageOcrResult
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface OcrEngine {
    suspend fun recognizeText(
        imageFile: File,
        pageId: String,
        revision: Int
    ): PageOcrResult
}

class MlKitOcrEngine(private val context: Context) : OcrEngine {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognizeText(
        imageFile: File,
        pageId: String,
        revision: Int
    ): PageOcrResult = withContext(Dispatchers.Default) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, options)
        val imageW = options.outWidth.toFloat().coerceAtLeast(1f)
        val imageH = options.outHeight.toFloat().coerceAtLeast(1f)

        val inputImage = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
        val visionText: Text = recognizer.process(inputImage).awaitTask()

        val blocks = visionText.textBlocks.map { textBlock ->
            val blockRect = textBlock.boundingBox?.let { toNormalized(it, imageW, imageH) }
            val lines = textBlock.lines.map { line ->
                val lineRect = line.boundingBox?.let { toNormalized(it, imageW, imageH) }
                val words = line.elements.map { element ->
                    val wordRect = element.boundingBox?.let { toNormalized(it, imageW, imageH) }
                    OcrWord(
                        text = element.text,
                        boundingBox = wordRect,
                        confidence = element.confidence
                    )
                }
                OcrLine(
                    text = line.text,
                    boundingBox = lineRect,
                    words = words
                )
            }
            OcrBlock(
                text = textBlock.text,
                boundingBox = blockRect,
                lines = lines
            )
        }

        val rawText = visionText.text

        PageOcrResult(
            pageId = pageId,
            sourceRevision = revision,
            rawText = rawText,
            userReviewedText = null,
            blocks = blocks,
            language = visionText.textBlocks.firstOrNull()?.recognizedLanguage ?: "en",
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun toNormalized(box: Rect, width: Float, height: Float): NormalizedRect {
        return NormalizedRect(
            left = (box.left.toFloat() / width).coerceIn(0f, 1f),
            top = (box.top.toFloat() / height).coerceIn(0f, 1f),
            right = (box.right.toFloat() / width).coerceIn(0f, 1f),
            bottom = (box.bottom.toFloat() / height).coerceIn(0f, 1f)
        )
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}
