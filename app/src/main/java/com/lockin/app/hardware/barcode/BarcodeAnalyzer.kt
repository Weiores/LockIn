package com.lockin.app.hardware.barcode

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX ImageAnalysis.Analyzer that scans video frames for barcodes using Google ML Kit.
 * Bundled offline model with zero network overhead.
 */
class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build(),
    )

    private var isScanning = true

    fun pauseScanning() {
        isScanning = false
    }

    fun resumeScanning() {
        isScanning = true
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (!isScanning || mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees,
        )

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: barcode.displayValue
                    if (!rawValue.isNullOrBlank()) {
                        isScanning = false
                        onBarcodeScanned(rawValue)
                        break
                    }
                }
            }
            .addOnFailureListener {
                // Ignore transient frame analysis failures
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

