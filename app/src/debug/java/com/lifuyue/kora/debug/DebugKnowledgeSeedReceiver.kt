package com.lifuyue.kora.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DebugKnowledgeSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SEED_LOCAL_KNOWLEDGE) {
            return
        }

        val appContext = context.applicationContext
        val statusFile = resolveStatusFile(appContext)
        if (!consumeSeedToken(appContext, intent.getStringExtra(EXTRA_SEED_TOKEN))) {
            Log.w(TAG, "Rejected debug seed request without a valid token")
            writeStatus(statusFile, "failure", 0, 0, 0, "invalid seed token")
            return
        }

        val serviceIntent =
            Intent(appContext, DebugKnowledgeSeedService::class.java).apply {
                action = ACTION_SEED_LOCAL_KNOWLEDGE
                putExtra(EXTRA_PAYLOAD_PATH, intent.getStringExtra(EXTRA_PAYLOAD_PATH))
                putExtra(EXTRA_REPLACE_EXISTING, intent.getBooleanExtra(EXTRA_REPLACE_EXISTING, false))
                putExtra(EXTRA_BATCH_SIZE, intent.getIntExtra(EXTRA_BATCH_SIZE, DEFAULT_BATCH_SIZE))
            }
        runCatching {
            ContextCompat.startForegroundService(appContext, serviceIntent)
        }.onFailure { error ->
            Log.e(TAG, "Failed to start debug seed service", error)
            writeStatus(
                statusFile,
                "failure",
                0,
                0,
                0,
                error.message ?: error::class.java.simpleName,
            )
        }
    }

    companion object {
        private const val TAG = "DebugKnowledgeSeed"
    }
}
