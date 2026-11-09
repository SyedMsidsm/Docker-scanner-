package com.doclens.scanner

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.doclens.scanner.core.barcode.QrDecoder
import com.doclens.scanner.core.billing.BillingManager
import com.doclens.scanner.core.billing.SubscriptionPlan
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrAndEntitlementTest {

    @Test
    fun testQrCodeDecodingFromBitmap() {
        val testPayload = "https://doclens.local/verify/test-qr-payload-987"

        // Generate synthetic QR Bitmap using ZXing QRCodeWriter
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(testPayload, BarcodeFormat.QR_CODE, 200, 200)

        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        for (x in 0 until 200) {
            for (y in 0 until 200) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }

        // Decode using QrDecoder
        val decoded = QrDecoder.decodeBitmap(bmp)
        assertNotNull("QR decoder should successfully decode valid QR bitmap", decoded)
        assertEquals(testPayload, decoded)
    }

    @Test
    fun testQrDecoderReturnsNullForBlankImage() {
        // Create blank white bitmap with no QR code
        val blankBmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }

        val decoded = QrDecoder.decodeBitmap(blankBmp)
        assertNull("QR decoder should safely return null for non-QR images", decoded)
    }

    @Test
    fun testBillingEntitlementsAndNonDestructiveDowngrade() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val billing = BillingManager(context)

        // 1. Initially FREE tier
        billing.setPlan(SubscriptionPlan.FREE)
        val initialEnt = billing.entitlements.value
        assertEquals(SubscriptionPlan.FREE, initialEnt.plan)
        assertTrue("OCR must always be unlimited even on Free plan", initialEnt.ocrUnlimited)
        assertEquals(2_000_000_000L, initialEnt.cloudStorageLimitBytes)
        assertEquals(10, initialEnt.serverConversionQuotaPerMonth)
        assertFalse(initialEnt.teamSharing)

        // Storage quota checks on Free
        assertTrue(billing.canUploadCloudBytes(0L, 500_000_000L))
        assertFalse(billing.canUploadCloudBytes(1_900_000_000L, 200_000_000L))

        // 2. Upgrade to PRO
        billing.purchasePro()
        val proEnt = billing.entitlements.value
        assertEquals(SubscriptionPlan.PRO, proEnt.plan)
        assertEquals(50_000_000_000L, proEnt.cloudStorageLimitBytes)
        assertEquals(200, proEnt.serverConversionQuotaPerMonth)
        assertTrue(proEnt.teamSharing)

        // Storage quota checks on Pro
        assertTrue(billing.canUploadCloudBytes(1_900_000_000L, 200_000_000L))
        assertTrue(billing.canPerformServerConversion(50))

        // 3. Downgrade back to FREE (Non-destructive document ownership)
        billing.setPlan(SubscriptionPlan.FREE)
        val downgradedEnt = billing.entitlements.value
        assertEquals(SubscriptionPlan.FREE, downgradedEnt.plan)
        assertTrue("Local OCR and scans remain unlimited after downgrade", downgradedEnt.ocrUnlimited)
        assertEquals(2_000_000_000L, downgradedEnt.cloudStorageLimitBytes)
    }
}
