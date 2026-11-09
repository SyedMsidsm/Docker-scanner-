package com.doclens.scanner.core.database

import androidx.room.TypeConverter
import com.doclens.scanner.core.model.BackupState
import com.doclens.scanner.core.model.CropQuad
import com.doclens.scanner.core.model.EditRecipe
import com.doclens.scanner.core.model.ImageFilter
import com.doclens.scanner.core.model.LocalSaveState
import com.doclens.scanner.core.model.NormalizedPoint
import com.doclens.scanner.core.model.SessionState

class Converters {

    @TypeConverter
    fun fromLocalSaveState(value: LocalSaveState): String = value.name

    @TypeConverter
    fun toLocalSaveState(value: String): LocalSaveState = runCatching {
        LocalSaveState.valueOf(value)
    }.getOrDefault(LocalSaveState.SAVED_LOCALLY)

    @TypeConverter
    fun fromBackupState(value: BackupState): String = value.name

    @TypeConverter
    fun toBackupState(value: String): BackupState = runCatching {
        BackupState.valueOf(value)
    }.getOrDefault(BackupState.NOT_BACKED_UP)

    @TypeConverter
    fun fromSessionState(value: SessionState): String = value.name

    @TypeConverter
    fun toSessionState(value: String): SessionState = runCatching {
        SessionState.valueOf(value)
    }.getOrDefault(SessionState.ACTIVE)

    @TypeConverter
    fun fromEditRecipe(recipe: EditRecipe): String {
        val q = recipe.cropQuad
        return "v1|${recipe.rotationDegrees}|${recipe.filter.name}|${recipe.brightness}|${recipe.contrast}|" +
                "${q.topLeft.x},${q.topLeft.y},${q.topRight.x},${q.topRight.y}," +
                "${q.bottomRight.x},${q.bottomRight.y},${q.bottomLeft.x},${q.bottomLeft.y}"
    }

    @TypeConverter
    fun toEditRecipe(data: String?): EditRecipe {
        if (data.isNullOrBlank()) return EditRecipe()
        return try {
            val parts = data.split("|")
            if (parts.size >= 6 && parts[0] == "v1") {
                val rot = parts[1].toIntOrNull() ?: 0
                val filter = runCatching { ImageFilter.valueOf(parts[2]) }.getOrDefault(ImageFilter.ORIGINAL)
                val brightness = parts[3].toFloatOrNull() ?: 0.0f
                val contrast = parts[4].toFloatOrNull() ?: 1.0f

                val coords = parts[5].split(",").mapNotNull { it.toFloatOrNull() }
                val quad = if (coords.size == 8) {
                    CropQuad(
                        topLeft = NormalizedPoint(coords[0], coords[1]),
                        topRight = NormalizedPoint(coords[2], coords[3]),
                        bottomRight = NormalizedPoint(coords[4], coords[5]),
                        bottomLeft = NormalizedPoint(coords[6], coords[7])
                    )
                } else {
                    CropQuad()
                }

                EditRecipe(
                    schemaVersion = 1,
                    cropQuad = quad,
                    rotationDegrees = rot,
                    filter = filter,
                    brightness = brightness,
                    contrast = contrast
                )
            } else {
                EditRecipe()
            }
        } catch (e: Exception) {
            EditRecipe()
        }
    }
}
