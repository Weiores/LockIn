package com.lockin.app.hardware.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcCardReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(context)
    }

    val isSupported: Boolean
        get() = nfcAdapter != null

    val isEnabled: Boolean
        get() = nfcAdapter?.isEnabled == true

    companion object {
        const val READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
    }

    /**
     * Observes NFC Tag UIDs while the given [Activity] is active in foreground reader mode.
     * Emits the tag hardware UID formatted as hexadecimal bytes (e.g., "04:A2:3B:18:C4:80").
     */
    fun observeCardUid(activity: Activity): Flow<String> = callbackFlow {
        val adapter = nfcAdapter ?: run {
            close()
            return@callbackFlow
        }

        if (!adapter.isEnabled) {
            close()
            return@callbackFlow
        }

        val readerCallback = NfcAdapter.ReaderCallback { tag: Tag ->
            val idBytes = tag.id
            if (idBytes != null && idBytes.isNotEmpty()) {
                val hexUid = idBytes.joinToString(":") { byte -> "%02X".format(byte) }
                trySend(hexUid)
            }
        }

        val options = Bundle()
        adapter.enableReaderMode(activity, readerCallback, READER_FLAGS, options)

        awaitClose {
            adapter.disableReaderMode(activity)
        }
    }
}
