package com.lockin.app.ui.scanner

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.GlassIconButton
import com.lockin.app.ui.components.StatusDot
import com.lockin.app.ui.theme.GlassRadius
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette
import com.lockin.app.ui.vault.components.CameraBarcodeScannerView

// ============================================================================
//  Barcode / QR scanner page
// ============================================================================

/**
 * Full-screen barcode page.
 *
 * The camera sits in a single rounded card so the page can stay on the app's
 * normal canvas — light in light mode, night in dark mode. Everything outside
 * the card is ordinary theme type, so nothing fights the preview.
 */
@Composable
fun BarcodeScannerScreen(
    onSaved: () -> Unit,
    onClose: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val scanned by viewModel.scannedValue.collectAsState()
    val activity = LocalContext.current as? Activity
    val accent = LockInPalette.Cyan

    var torchOn by remember { mutableStateOf(false) }
    var torchSupported by remember { mutableStateOf(true) }
    var manualEntry by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            ScannerHeader(
                title = "Scan a key",
                subtitle = "Point at a barcode or QR code",
                onClose = onClose,
            )
            Spacer(modifier = Modifier.height(16.dp))

            val detected = scanned
            if (detected != null) {
                Spacer(modifier = Modifier.weight(1f))
                DetectedKeyPanel(
                    type = TriggerType.BARCODE,
                    detectedValue = detected,
                    onRescan = {
                        viewModel.resetScan()
                        manualEntry = false
                        manualCode = ""
                    },
                    onSave = { label, strict ->
                        val ok = viewModel.saveTrigger(
                            type = TriggerType.BARCODE,
                            label = label,
                            value = detected,
                            isStrict = strict,
                        )
                        if (ok) onSaved()
                    },
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                // ------------------------------------------------- camera card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(GlassRadius.Card))
                        .background(Color.Black),
                ) {
                    CameraBarcodeScannerView(
                        modifier = Modifier.fillMaxSize(),
                        showOverlay = false,
                        torchEnabled = torchOn,
                        onBarcodeDetected = { value -> viewModel.onBarcodeScanned(value) },
                        onTorchAvailabilityChanged = { torchSupported = it },
                    )
                    ScanReticle(accent = accent)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --------------------------------------------------- controls
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (torchSupported) {
                        ScannerChip(
                            icon = if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            label = "Torch",
                            active = torchOn,
                            accent = accent,
                            onClick = { torchOn = !torchOn },
                        )
                    }
                    ScannerChip(
                        icon = Icons.Filled.Keyboard,
                        label = "Enter code",
                        active = manualEntry,
                        accent = accent,
                        onClick = { manualEntry = !manualEntry },
                    )
                }

                if (manualEntry) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = manualCode,
                            onValueChange = { manualCode = it },
                            placeholder = {
                                Text(
                                    "Type the code",
                                    fontSize = 13.sp,
                                    color = LockInPalette.TextTertiary,
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(GlassRadius.Item),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = LockInPalette.SurfaceTint.copy(alpha = 0.14f),
                                focusedTextColor = LockInPalette.TextPrimary,
                                unfocusedTextColor = LockInPalette.TextPrimary,
                                cursorColor = accent,
                            ),
                        )
                        GlassButton(
                            onClick = {
                                viewModel.onManualEntry(manualCode)
                                manualEntry = false
                                manualCode = ""
                            },
                            containerColor = accent,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Use code",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Codes are stored on this device only.",
                    fontSize = 10.5.sp,
                    color = LockInPalette.TextTertiary,
                    modifier = Modifier.padding(top = 14.dp, bottom = 20.dp),
                )
            }
        }
    }
}

// ============================================================================
//  NFC scanner page
// ============================================================================

/**
 * Full-screen NFC page. One pulsing tile, one status line — the page is mostly
 * empty on purpose so it reads at a glance.
 */
@Composable
fun NfcScannerScreen(
    onSaved: () -> Unit,
    onClose: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val scanned by viewModel.scannedValue.collectAsState()
    val activity = LocalContext.current as? Activity
    val accent = LockInPalette.Mint

    // Reader mode is only active while this page is on screen.
    DisposableEffect(Unit) {
        activity?.let { viewModel.startNfcListening(it) }
        onDispose { viewModel.stopNfcListening() }
    }

    val statusText = when {
        !viewModel.isNfcSupported -> "No NFC hardware found"
        !viewModel.isNfcEnabled -> "Turn NFC on in Settings"
        else -> "Reader ready"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            ScannerHeader(
                title = "Tap your card",
                subtitle = "Hold it flat against the back of your phone",
                onClose = onClose,
            )

            val detected = scanned
            if (detected != null) {
                Spacer(modifier = Modifier.weight(1f))
                DetectedKeyPanel(
                    type = TriggerType.NFC,
                    detectedValue = detected,
                    onRescan = {
                        viewModel.resetScan()
                        activity?.let { viewModel.startNfcListening(it) }
                    },
                    onSave = { label, strict ->
                        val ok = viewModel.saveTrigger(
                            type = TriggerType.NFC,
                            label = label,
                            value = detected,
                            isStrict = strict,
                        )
                        if (ok) onSaved()
                    },
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    NfcPulse(accent = accent)

                    Spacer(modifier = Modifier.height(26.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(color = accent, size = 6.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = LockInPalette.TextSecondary,
                        )
                    }
                }

                Text(
                    text = "Only the tag ID is read — never the balance or contents.",
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                    color = LockInPalette.TextTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                )
            }
        }
    }
}

// ============================================================================
//  Shared scanner chrome
// ============================================================================

/** Title + one-line hint + close, using the same type scale as every tab. */
@Composable
private fun ScannerHeader(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlassType.ScreenTitle,
                color = LockInPalette.HeaderInk,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = LockInPalette.TextSecondary,
            )
        }
        GlassIconButton(
            icon = Icons.Filled.Close,
            contentDescription = "Close scanner",
            onClick = onClose,
            size = 42.dp,
            tint = LockInPalette.HeaderInk,
            onSky = true,
        )
    }
}

/**
 * Centred frame with a single sweeping line. Deliberately just one stroke —
 * corner brackets plus a laser plus a scrim was three ideas competing.
 */
@Composable
private fun ScanReticle(accent: Color) {
    val frameSize = 232.dp
    val transition = rememberInfiniteTransition(label = "scanLine")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanLineProgress",
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(frameSize)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(30.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx()),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(2.dp)
                    .offset(y = 12.dp + (frameSize - 26.dp) * progress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                accent.copy(alpha = 0.9f),
                                Color.White,
                                accent.copy(alpha = 0.9f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}

/**
 * Quiet sonar: two rings drifting outward from a tinted tile. No scrim, no
 * gradient wash — the page background does the framing.
 */
@Composable
private fun NfcPulse(accent: Color) {
    val transition = rememberInfiniteTransition(label = "nfcPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "nfcPulseProgress",
    )

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Two rings half a cycle apart keeps the pulse continuous but calm.
        listOf(0f, 0.5f).forEach { offset ->
            val local = (pulse + offset) % 1f
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .drawPulsingRing(local, accent),
            )
        }

        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(accent.copy(alpha = 0.12f))
                .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Contactless,
                contentDescription = "Waiting for card",
                tint = accent,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

/** Ring that expands and fades as [progress] runs 0 → 1. */
private fun Modifier.drawPulsingRing(progress: Float, accent: Color): Modifier =
    this.drawBehind {
        val radius = (size.minDimension / 2f) * (0.42f + 0.58f * progress)
        val alpha = (1f - progress).coerceIn(0f, 1f) * 0.34f
        drawCircle(
            color = accent.copy(alpha = alpha),
            radius = radius,
            style = Stroke(width = 1.4.dp.toPx()),
        )
    }

/** Small outlined chip — the only two controls the barcode page needs. */
@Composable
private fun ScannerChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(GlassRadius.Pill)
    val tint = if (active) accent else LockInPalette.TextSecondary

    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                if (active) {
                    accent.copy(alpha = 0.12f)
                } else {
                    LockInPalette.SurfaceTint.copy(alpha = 0.05f)
                },
            )
            .border(
                width = 1.dp,
                color = if (active) {
                    accent.copy(alpha = 0.30f)
                } else {
                    LockInPalette.SurfaceTint.copy(alpha = 0.10f)
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

// ============================================================================
//  "Detected" panel shared by both scanner pages
// ============================================================================

@Composable
private fun DetectedKeyPanel(
    type: TriggerType,
    detectedValue: String,
    onRescan: () -> Unit,
    onSave: (label: String, isStrict: Boolean) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var isStrict by remember { mutableStateOf(false) }

    val accent = when (type) {
        TriggerType.NFC -> LockInPalette.Mint
        else -> LockInPalette.Cyan
    }
    val title = when (type) {
        TriggerType.NFC -> "Card detected"
        else -> "Code detected"
    }
    val placeholder = when (type) {
        TriggerType.NFC -> "Label (e.g., Student ID Card)"
        else -> "Label (e.g., Desk Water Bottle)"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Card,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                StatusDot(color = accent, size = 6.dp)
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LockInPalette.TextPrimary,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GlassRadius.Item))
                    .background(accent.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = detectedValue,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 2,
                )
            }

            Text(
                text = if (type == TriggerType.NFC) {
                    "Tag UID — works with student cards, transit cards and contactless payment cards."
                } else {
                    "Use the same barcode every time you want to start or stop a session."
                },
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = LockInPalette.TextTertiary,
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                placeholder = { Text(placeholder, color = LockInPalette.TextTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(GlassRadius.Item),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = LockInPalette.SurfaceTint.copy(alpha = 0.12f),
                    focusedTextColor = LockInPalette.TextPrimary,
                    unfocusedTextColor = LockInPalette.TextPrimary,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Strict unlock required",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LockInPalette.TextPrimary,
                    )
                    Text(
                        text = if (type == TriggerType.NFC) {
                            "Must tap this card to abort early"
                        } else {
                            "Must scan this barcode to abort early"
                        },
                        fontSize = 10.sp,
                        color = LockInPalette.TextTertiary,
                    )
                }
                Switch(
                    checked = isStrict,
                    onCheckedChange = { isStrict = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent,
                        checkedBorderColor = accent.copy(alpha = 0.6f),
                        uncheckedThumbColor = LockInPalette.SurfaceTint.copy(alpha = 0.55f),
                        uncheckedTrackColor = LockInPalette.SurfaceTint.copy(alpha = 0.10f),
                        uncheckedBorderColor = LockInPalette.SurfaceTint.copy(alpha = 0.10f),
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassButton(
                    onClick = onRescan,
                    modifier = Modifier.weight(1f),
                    containerColor = LockInPalette.SurfaceTint.copy(alpha = 0.06f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (type == TriggerType.NFC) "Tap again" else "Scan again",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LockInPalette.TextSecondary,
                        )
                    }
                }
                GlassButton(
                    onClick = { onSave(label, isStrict) },
                    modifier = Modifier.weight(1f),
                    containerColor = accent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Save key",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
