package com.lockin.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.ui.components.GlassBadge
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.GlassSectionHeader
import com.lockin.app.ui.components.StatusDot
import com.lockin.app.ui.theme.GlassRadius
import com.lockin.app.ui.theme.GlassTokens
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette

/**
 * Screen 4 — Trigger Vault.
 *
 * Saved physical keys (optical barcode / NFC UID / campus router BSSID) plus a
 * live sensor diagnostics card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerVaultScreen(
    onOpenBarcodeScanner: () -> Unit = {},
    onOpenNfcScanner: () -> Unit = {},
    viewModel: TriggerVaultViewModel = hiltViewModel(),
) {
    val triggers by viewModel.triggers.collectAsState()
    val scannedCardUid by viewModel.scannedCardUid.collectAsState()

    var showRegisterMenu by remember { mutableStateOf(false) }
    var triggerToDelete by remember { mutableStateOf<TriggerProfile?>(null) }

    // Enable/disable is a local presentation concern: TriggerProfile has no persisted flag.
    val enabledStates = remember { mutableStateMapOf<Long, Boolean>() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ------------------------------------------------------------ header
            item {
                Text(
                    text = "Trigger Vault",
                    style = GlassType.ScreenTitle,
                    color = LockInPalette.HeaderInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // -------------------------------------------------- saved keys header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassSectionHeader(
                        title = "Saved Physical Keys",
                        modifier = Modifier.weight(1f),
                    )
                    GlassBadge(
                        text = "${triggers.size} REGISTERED",
                        color = LockInPalette.HeaderInk.copy(alpha = 0.85f),
                        background = LockInPalette.HeaderInk.copy(alpha = 0.16f),
                    )
                }
            }

            if (triggers.isEmpty()) {
                item { EmptyVaultCard(onRegister = onOpenBarcodeScanner) }
            } else {
                items(triggers, key = { it.id }) { trigger ->
                    val enabled = enabledStates[trigger.id] ?: (trigger.type != TriggerType.WIFI)
                    TriggerKeyRow(
                        trigger = trigger,
                        enabled = enabled,
                        onToggle = { enabledStates[trigger.id] = it },
                        onLongPress = { triggerToDelete = trigger },
                    )
                }
            }

            // ------------------------------------------------------ register new
            item {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(GlassRadius.Item))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        LockInPalette.Cyan.copy(alpha = 0.09f),
                                        LockInPalette.Cyan.copy(alpha = 0.03f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                brush = GlassTokens.rimLight(LockInPalette.Cyan),
                                shape = RoundedCornerShape(GlassRadius.Item),
                            )
                            .clickable { showRegisterMenu = true }
                            .padding(vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "+  Register New",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LockInPalette.Cyan,
                        )
                    }

                    DropdownMenu(
                        expanded = showRegisterMenu,
                        onDismissRequest = { showRegisterMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Scan Barcode", color = LockInPalette.TextPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = null,
                                    tint = LockInPalette.Cyan,
                                )
                            },
                            onClick = {
                                showRegisterMenu = false
                                onOpenBarcodeScanner()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Tap NFC Card", color = LockInPalette.TextPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Contactless,
                                    contentDescription = null,
                                    tint = LockInPalette.Mint,
                                )
                            },
                            onClick = {
                                showRegisterMenu = false
                                onOpenNfcScanner()
                            },
                        )
                    }
                }
            }

            // ------------------------------------------------ unlock behaviour
            item {
                Text(
                    text = "Keys only arm the lock or release it early — nothing interrupts a running session.",
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = LockInPalette.TextTertiary,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp),
                )
            }
        }
    }

    // ------------------------------------------------------- delete confirm
    if (triggerToDelete != null) {
        AlertDialog(
            onDismissRequest = { triggerToDelete = null },
            title = {
                Text(
                    text = "Delete Physical Key?",
                    fontWeight = FontWeight.Bold,
                    color = LockInPalette.TextPrimary,
                )
            },
            text = {
                Text(
                    text = "Delete '${triggerToDelete!!.label}'? You will no longer be able to use it as a session trigger.",
                    color = LockInPalette.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTrigger(triggerToDelete!!)
                        triggerToDelete = null
                    },
                ) {
                    Text("Delete", color = LockInPalette.Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { triggerToDelete = null }) {
                    Text("Cancel", color = LockInPalette.Cyan)
                }
            },
            containerColor = LockInPalette.SheetSurface,
            titleContentColor = LockInPalette.TextPrimary,
            textContentColor = LockInPalette.TextSecondary,
        )
    }
}

// ============================================================================
//  Key row
// ============================================================================

@Composable
private fun TriggerKeyRow(
    trigger: TriggerProfile,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onLongPress: () -> Unit,
) {
    val icon: ImageVector = when (trigger.type) {
        TriggerType.BARCODE -> Icons.Filled.QrCodeScanner
        TriggerType.NFC -> Icons.Filled.Contactless
        TriggerType.WIFI -> Icons.Filled.Wifi
        else -> Icons.Filled.Key
    }

    val accent: Color = when (trigger.type) {
        TriggerType.BARCODE -> LockInPalette.Cyan
        TriggerType.NFC -> LockInPalette.Mint
        TriggerType.WIFI -> LockInPalette.Amber
        else -> LockInPalette.SkyBlue
    }

    val title = when (trigger.type) {
        TriggerType.BARCODE -> "${trigger.label} (Optical)"
        TriggerType.NFC -> "${trigger.label} (UID: ${trigger.value.take(11)})"
        TriggerType.WIFI -> "${trigger.label} (BSSID)"
        else -> trigger.label
    }

    val (status, statusColor) = when (trigger.type) {
        TriggerType.BARCODE ->
            if (enabled) "Ready to scan" to LockInPalette.Mint else "Optical · Paused" to LockInPalette.TextTertiary
        TriggerType.NFC ->
            if (enabled) "NFC · Ready to tap" to LockInPalette.Mint else "NFC · Paused" to LockInPalette.TextTertiary
        TriggerType.WIFI ->
            if (enabled) "Wi-Fi · In range" to LockInPalette.Mint else "Wi-Fi · Not in range" to LockInPalette.Amber
        else ->
            if (enabled) "Ready" to LockInPalette.Mint else "Paused" to LockInPalette.TextTertiary
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(trigger.id) {
                detectTapGestures(onLongPress = { onLongPress() })
            },
        cornerRadius = GlassRadius.Item,
        contentPadding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        brush = GlassTokens.rimLight(accent),
                        shape = RoundedCornerShape(13.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = trigger.type.name,
                    tint = accent,
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(modifier = Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = LockInPalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(color = statusColor, size = 4.dp, halo = false)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                        maxLines = 1,
                    )
                }
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = glassSwitchColors(),
            )
        }
    }
}

@Composable
private fun glassSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = LockInPalette.Mint,
    checkedBorderColor = LockInPalette.Mint.copy(alpha = 0.6f),
    uncheckedThumbColor = LockInPalette.SurfaceTint.copy(alpha = 0.55f),
    uncheckedTrackColor = LockInPalette.SurfaceTint.copy(alpha = 0.10f),
    uncheckedBorderColor = LockInPalette.SurfaceTint.copy(alpha = 0.10f),
)

// ============================================================================
//  Empty state
// ============================================================================

@Composable
private fun EmptyVaultCard(onRegister: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Item,
        contentPadding = 22.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LockInPalette.SurfaceTint.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCode,
                    contentDescription = null,
                    tint = LockInPalette.TextTertiary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No physical keys yet",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LockInPalette.TextPrimary,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Register your desk bottle, student card or campus router to anchor every session to something physical.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = LockInPalette.TextTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))
            GlassButton(
                onClick = onRegister,
                containerColor = LockInPalette.Cyan.copy(alpha = 0.16f),
            ) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Scan a key",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LockInPalette.Cyan,
                    )
                }
            }
        }
    }
}
