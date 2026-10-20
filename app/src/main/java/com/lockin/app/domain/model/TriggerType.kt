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
