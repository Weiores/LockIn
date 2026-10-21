package com.lockin.app.domain.model

/**
 * How a session is unlocked. The accelerometer / "phone lifted" trigger was
 * removed, so every mode here is either a physical key, a network, or manual.
 */
enum class TriggerType {
    BARCODE,
    NFC,
    WIFI,
    MANUAL,
}

/** Short human name for the lock type, used wherever the armed key is shown. */
val TriggerType.displayName: String
    get() = when (this) {
        TriggerType.BARCODE -> "Barcode"
        TriggerType.NFC -> "Card"
        TriggerType.WIFI -> "Wi-Fi"
        TriggerType.MANUAL -> "Manual"
    }
