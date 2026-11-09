package com.doclens.scanner

import com.doclens.scanner.core.database.Converters
import com.doclens.scanner.core.model.CropQuad
import com.doclens.scanner.core.model.EditRecipe
import com.doclens.scanner.core.model.ImageFilter
import com.doclens.scanner.core.model.NormalizedPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditRecipeTest {

    private val converters = Converters()

    @Test
    fun testDefaultCropQuad() {
        val defaultQuad = CropQuad()
        assertTrue(defaultQuad.isDefault())
    }

    @Test
    fun testModifiedCropQuad() {
        val modifiedQuad = CropQuad(
            topLeft = NormalizedPoint(0.1f, 0.1f),
            topRight = NormalizedPoint(0.9f, 0.15f),
            bottomRight = NormalizedPoint(0.85f, 0.95f),
            bottomLeft = NormalizedPoint(0.05f, 0.9f)
        )
        assertFalse(modifiedQuad.isDefault())
    }

    @Test
    fun testRecipeSerializationRoundTrip() {
        val originalRecipe = EditRecipe(
            schemaVersion = 1,
            cropQuad = CropQuad(
                topLeft = NormalizedPoint(0.12f, 0.15f),
                topRight = NormalizedPoint(0.88f, 0.14f),
                bottomRight = NormalizedPoint(0.90f, 0.92f),
                bottomLeft = NormalizedPoint(0.10f, 0.89f)
            ),
            rotationDegrees = 90,
            filter = ImageFilter.BW_CLEAN,
            brightness = 0.15f,
            contrast = 1.35f
        )

        val jsonString = converters.fromEditRecipe(originalRecipe)
        val deserializedRecipe = converters.toEditRecipe(jsonString)

        assertEquals(originalRecipe.schemaVersion, deserializedRecipe.schemaVersion)
        assertEquals(originalRecipe.rotationDegrees, deserializedRecipe.rotationDegrees)
        assertEquals(originalRecipe.filter, deserializedRecipe.filter)
        assertEquals(originalRecipe.brightness, deserializedRecipe.brightness, 0.001f)
        assertEquals(originalRecipe.contrast, deserializedRecipe.contrast, 0.001f)

        assertEquals(originalRecipe.cropQuad.topLeft.x, deserializedRecipe.cropQuad.topLeft.x, 0.001f)
        assertEquals(originalRecipe.cropQuad.topLeft.y, deserializedRecipe.cropQuad.topLeft.y, 0.001f)
        assertEquals(originalRecipe.cropQuad.bottomRight.x, deserializedRecipe.cropQuad.bottomRight.x, 0.001f)
        assertEquals(originalRecipe.cropQuad.bottomRight.y, deserializedRecipe.cropQuad.bottomRight.y, 0.001f)
    }
}
