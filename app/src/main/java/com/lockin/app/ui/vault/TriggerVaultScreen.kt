package com.lockin.app.ui.vault

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.hardware.flip.FlipDetector
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.theme.AppleBlue
import com.lockin.app.ui.theme.AppleGreen
import com.lockin.app.ui.theme.AppleOrange
import com.lockin.app.ui.theme.AppleRed
import com.lockin.app.ui.theme.LocalDarkTheme
import com.lockin.app.ui.vault.components.CameraBarcodeScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerVaultScreen(
    viewModel: TriggerVaultViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isDark = LocalDarkTheme.current

    val triggers by viewModel.triggers.collectAsState()
    val flipState by viewModel.flipState.collectAsState()
    val scannedCardUid by viewModel.scannedCardUid.collectAsState()

    var showBarcodeScannerSheet by remember { mutableStateOf(false) }
    var showNfcScanSheet by remember { mutableStateOf(false) }
    var triggerToDelete by remember { mutableStateOf<TriggerProfile?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // --- 1. Top Header & Action Cards ---
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppleBlue.copy(alpha = if (isDark) 0.18f else 0.10f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = "Trigger Vault",
                                    tint = AppleBlue,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Trigger Vault",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Hardware Keys & Checkpoints",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Text(
                        text = "Bind study sessions to physical items in your room. Require an optical barcode or NFC card to unlock and maintain focus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )

                    // Add Trigger Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Scan Barcode Button
                        GlassButton(
                            onClick = { showBarcodeScannerSheet = true },
                            modifier = Modifier.weight(1f),
                            containerColor = AppleBlue,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Scan Barcode",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan Barcode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }

                        // Tap NFC Card Button
                        GlassButton(
                            onClick = {
                                if (activity != null) {
                                    viewModel.startNfcListening(activity)
                                }
                                showNfcScanSheet = true
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = AppleGreen,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Contactless,
                                    contentDescription = "Tap NFC Card",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tap NFC Card",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Live Diagnostics Telemetry Card ---
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "LIVE SENSOR TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Posture Sensor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ScreenRotation,
                                contentDescription = "Posture",
                                tint = if (flipState is FlipDetector.FlipState.FaceDown) AppleGreen else AppleOrange,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = "Flip Posture",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = when (flipState) {
                                        is FlipDetector.FlipState.FaceDown -> "Face-Down (Locked)"
                                        is FlipDetector.FlipState.FaceUp -> "Face-Up (Desk)"
                                        FlipDetector.FlipState.Unknown -> "Calibrating..."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        // NFC Hardware Reader
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Contactless,
                                contentDescription = "NFC",
                                tint = if (viewModel.isEnabled) AppleGreen else Color.Gray,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = "NFC Reader",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = if (viewModel.isNfcSupported) {
                                        if (viewModel.isNfcEnabled) "Ready (API 31+)" else "Disabled in Settings"
                                    } else {
                                        "No Hardware"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Registered Physical Keys Header ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "REGISTERED HARDWARE KEYS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp),
                )
                Text(
                    text = "${triggers.size} Saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // --- 4. Saved Keys List ---
        if (triggers.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode,
                                contentDescription = "No Keys",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            text = "No Physical Keys Registered",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Tap 'Scan Barcode' or 'Tap NFC Card' above to register your desk items or student matriculation card.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        } else {
            items(triggers, key = { it.id }) { trigger ->
                SavedTriggerItem(
                    trigger = trigger,
                    isDark = isDark,
                    onDelete = { triggerToDelete = trigger },
                )
            }
        }
    }

    // --- Barcode Scanner Bottom Sheet ---
    if (showBarcodeScannerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var scannedBarcode by remember { mutableStateOf<String?>(null) }
        var barcodeLabel by remember { mutableStateOf("") }
        var isStrictChecked by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showBarcodeScannerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Scan Physical Barcode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { showBarcodeScannerSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                if (scannedBarcode == null) {
                    // Active Camera Viewfinder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(20.dp)),
                    ) {
                        CameraBarcodeScannerView { detectedValue ->
                            scannedBarcode = detectedValue
                            viewModel.playSuccessHaptic()
                        }
                    }
                } else {
                    // Scanned Result & Registration Form
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "Detected", tint = AppleGreen)
                                Text(
                                    text = "Barcode Detected!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleGreen,
                                )
                            }
                            Text(
                                text = scannedBarcode!!,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = barcodeLabel,
                        onValueChange = { barcodeLabel = it },
                        label = { Text("Label (e.g., Desk Water Bottle, Calculus Book)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppleBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Strict Unlock Required",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Must scan this barcode to abort session early",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = isStrictChecked,
                            onCheckedChange = { isStrictChecked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppleBlue,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlassButton(
                            onClick = { scannedBarcode = null },
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Rescan", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        GlassButton(
                            onClick = {
                                viewModel.saveTrigger(
                                    type = TriggerType.BARCODE,
                                    label = barcodeLabel.ifBlank { "Desk Barcode" },
                                    value = scannedBarcode!!,
                                    isStrict = isStrictChecked,
                                )
                                showBarcodeScannerSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = AppleBlue,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Save Key", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- NFC Card Scan Bottom Sheet ---
    if (showNfcScanSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var cardLabel by remember { mutableStateOf("") }
        var isStrictChecked by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            onDispose {
                viewModel.stopNfcListening()
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                viewModel.stopNfcListening()
                showNfcScanSheet = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Register Contactless Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = {
                        viewModel.stopNfcListening()
                        showNfcScanSheet = false
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                if (scannedCardUid == null) {
                    // Pulsing NFC Tap Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "nfcPulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "nfcPulseScale",
                    )

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(AppleGreen.copy(alpha = if (isDark) 0.15f else 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Contactless,
                            contentDescription = "Hold card to phone",
                            tint = AppleGreen,
                            modifier = Modifier.size(60.dp),
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Hold Card Near Back of Phone",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Uses raw hardware tag UID. Works with university student cards, transit cards (EZ-Link, Oyster), and contactless payment cards.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 18.sp,
                        )
                    }
                } else {
                    // Detected Card UID & Save Form
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "Detected", tint = AppleGreen)
                                Text(
                                    text = "Card Hardware UID Read!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleGreen,
                                )
                            }
                            Text(
                                text = "Tag UID: ${scannedCardUid!!}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = cardLabel,
                        onValueChange = { cardLabel = it },
                        label = { Text("Label (e.g., Student ID Card, Transit Pass)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppleGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Strict Unlock Required",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Must tap this card to abort session early",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = isStrictChecked,
                            onCheckedChange = { isStrictChecked = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AppleGreen,
                            ),
                        )
                    }

                    GlassButton(
                        onClick = {
                            viewModel.saveTrigger(
                                type = TriggerType.NFC,
                                label = cardLabel.ifBlank { "NFC Card" },
                                value = scannedCardUid!!,
                                isStrict = isStrictChecked,
                            )
                            viewModel.stopNfcListening()
                            showNfcScanSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = AppleGreen,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Save Hardware Key", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // --- Delete Confirmation Dialog ---
    if (triggerToDelete != null) {
        AlertDialog(
            onDismissRequest = { triggerToDelete = null },
            title = {
                Text(
                    text = "Delete Physical Key?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${triggerToDelete!!.label}'? You will no longer be able to use it as a session trigger.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrigger(triggerToDelete!!)
                        triggerToDelete = null
                    },
                ) {
                    Text("Delete", color = AppleRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { triggerToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedTriggerItem(
    trigger: TriggerProfile,
    isDark: Boolean,
    onDelete: () -> Unit,
) {
    val icon = when (trigger.type) {
        TriggerType.BARCODE -> Icons.Filled.QrCodeScanner
        TriggerType.NFC -> Icons.Filled.Contactless
        TriggerType.WIFI -> Icons.Filled.Wifi
        else -> Icons.Filled.Key
    }

    val iconColor = when (trigger.type) {
        TriggerType.BARCODE -> AppleBlue
        TriggerType.NFC -> AppleGreen
        TriggerType.WIFI -> AppleOrange
        else -> AppleBlue
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = if (isDark) 0.18f else 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = trigger.type.name,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = trigger.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (trigger.isStrict) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppleRed.copy(alpha = if (isDark) 0.20f else 0.10f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "STRICT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = AppleRed,
                                )
                            }
                        }
                    }

                    Text(
                        text = trigger.value,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Key",
                    tint = AppleRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

