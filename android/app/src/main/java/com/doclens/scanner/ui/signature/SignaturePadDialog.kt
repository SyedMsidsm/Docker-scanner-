package com.doclens.scanner.ui.signature

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.doclens.scanner.core.designsystem.ScannerDestructiveButton
import com.doclens.scanner.core.designsystem.ScannerOutlinedButton
import com.doclens.scanner.core.designsystem.ScannerPrimaryButton
import com.doclens.scanner.core.designsystem.ScannerTheme
import java.io.File
import java.io.FileOutputStream

@Composable
fun SignaturePadDialog(
    outputFile: File,
    onDismiss: () -> Unit,
    onSignatureSaved: (File) -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Draw Signature",
                style = ScannerTheme.typography.cardTitle,
                color = ScannerTheme.colors.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sign using your finger or stylus inside the box.",
                    style = ScannerTheme.typography.supportingLabel,
                    color = ScannerTheme.colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Signature Drawing Canvas (White backing so user sees signature clearly)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .border(1.dp, ScannerTheme.colors.borderSubtle, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPath = currentPath + change.position
                                },
                                onDragEnd = {
                                    if (currentPath.isNotEmpty()) {
                                        paths.add(currentPath)
                                        currentPath = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                        val stroke = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val inkColor = androidx.compose.ui.graphics.Color(0xFF00153D)

                        // Draw completed paths
                        paths.forEach { points ->
                            if (points.size > 1) {
                                val p = Path()
                                p.moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    p.lineTo(points[i].x, points[i].y)
                                }
                                drawPath(p, color = inkColor, style = stroke)
                            }
                        }

                        // Draw active path
                        if (currentPath.size > 1) {
                            val p = Path()
                            p.moveTo(currentPath[0].x, currentPath[0].y)
                            for (i in 1 until currentPath.size) {
                                p.lineTo(currentPath[i].x, currentPath[i].y)
                            }
                            drawPath(p, color = inkColor, style = stroke)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Canvas Controls: Undo & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (paths.isNotEmpty()) paths.removeAt(paths.size - 1)
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Text("Undo Stroke")
                    }

                    TextButton(
                        onClick = {
                            paths.clear()
                            currentPath = emptyList()
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Text("Clear", color = ScannerTheme.colors.error)
                    }
                }
            }
        },
        confirmButton = {
            ScannerPrimaryButton(
                text = "Apply Signature",
                onClick = {
                    if (paths.isNotEmpty()) {
                        saveSignatureToPng(paths, outputFile, 600, 300)
                        onSignatureSaved(outputFile)
                    } else {
                        onDismiss()
                    }
                },
                leadingIcon = Icons.Default.Check
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ScannerTheme.colors.onSurfaceVariant)
            }
        }
    )
}

private fun saveSignatureToPng(
    paths: List<List<Offset>>,
    outputFile: File,
    width: Int,
    height: Int
) {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // Keep transparent background (Color.TRANSPARENT)
    canvas.drawColor(Color.TRANSPARENT)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00153D") // Deep navy ink
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Scale from preview canvas (approx 300x180 dp) to bitmap width/height
    paths.forEach { points ->
        if (points.size > 1) {
            val path = android.graphics.Path()
            path.moveTo(points[0].x * (width / 350f), points[0].y * (height / 180f))
            for (i in 1 until points.size) {
                path.lineTo(points[i].x * (width / 350f), points[i].y * (height / 180f))
            }
            canvas.drawPath(path, paint)
        }
    }

    outputFile.parentFile?.mkdirs()
    FileOutputStream(outputFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
    }
    bitmap.recycle()
}
