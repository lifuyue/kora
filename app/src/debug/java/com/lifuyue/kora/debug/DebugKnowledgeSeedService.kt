package com.lifuyue.kora.debug

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lifuyue.kora.R
import com.lifuyue.kora.core.database.LocalKnowledgeIndexStatus
import com.lifuyue.kora.core.database.LocalKnowledgeSeedDocument
import com.lifuyue.kora.core.database.LocalKnowledgeSeedPayload
import com.lifuyue.kora.core.database.LocalKnowledgeStore
import com.lifuyue.kora.core.database.parseLocalKnowledgeSeedPayload
import com.lifuyue.kora.core.database.seedLocalKnowledge
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DebugKnowledgeSeedService : Service() {
    @Inject
    lateinit var localKnowledgeStore: LocalKnowledgeStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val payloadPath = resolveSeedPath(this, intent?.getStringExtra(EXTRA_PAYLOAD_PATH))
        val replaceExisting = intent?.getBooleanExtra(EXTRA_REPLACE_EXISTING, false) ?: false
        val batchSize = (intent?.getIntExtra(EXTRA_BATCH_SIZE, DEFAULT_BATCH_SIZE) ?: DEFAULT_BATCH_SIZE).coerceAtLeast(1)
        val statusFile = resolveStatusFile(this)

        if (!isRunning.compareAndSet(false, true)) {
            writeStatus(statusFile, "failure", 0, 0, 0, "seed already running")
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        writeStatus(statusFile, "running", 0, 0, 0, "starting")

        serviceScope.launch {
            try {
                val payload = parseLocalKnowledgeSeedPayload(payloadPath.readText())
                val importedCount = payload.documents.size
                if (importedCount == 0) {
                    writeStatus(statusFile, "failure", 0, 0, 0, "payload has no documents")
                    return@launch
                }

                val elapsedMs =
                    measureTimeMillis {
                        if (replaceExisting) {
                            localKnowledgeStore.clearLocalKnowledge()
                        }
                        var importedSoFar = 0
                        payload.documents.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                            localKnowledgeStore.seedLocalKnowledge(
                                payload =
                                    LocalKnowledgeSeedPayload(
                                        documents =
                                            batch.map { document ->
                                                LocalKnowledgeSeedDocument(
                                                    title = document.title,
                                                    sourceLabel = document.sourceLabel,
                                                    body = document.body,
                                                )
                                            },
                                    ),
                                replaceExisting = false,
                                nowBase = System.currentTimeMillis() + (batchIndex * 10_000L),
                            )
                            importedSoFar += batch.size
                            waitUntilReady(expectedReady = importedSoFar, timeoutMs = SEED_TIMEOUT_MS)
                            writeStatus(statusFile, "running", importedSoFar, importedSoFar, 0, "batch completed")
                        }
                    }

                val readyCount =
                    localKnowledgeStore.observeDocuments().first().count {
                        it.indexStatus == LocalKnowledgeIndexStatus.Ready
                    }
                writeStatus(statusFile, "success", importedCount, readyCount, elapsedMs, "seed completed")
            } catch (error: Throwable) {
                writeStatus(
                    statusFile,
                    "failure",
                    0,
                    0,
                    0,
                    error.message ?: error::class.java.simpleName,
                )
            } finally {
                isRunning.set(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelfResult(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun waitUntilReady(
        expectedReady: Int,
        timeoutMs: Long,
    ) {
        val startedAt = System.currentTimeMillis()
        while (true) {
            val documents = localKnowledgeStore.observeDocuments().first()
            val readyCount = documents.count { it.indexStatus == LocalKnowledgeIndexStatus.Ready }
            val failedCount = documents.count { it.indexStatus == LocalKnowledgeIndexStatus.Failed }
            if (readyCount >= expectedReady) {
                return
            }
            if (failedCount > 0) {
                throw IllegalStateException("local knowledge indexing failed")
            }
            if (System.currentTimeMillis() - startedAt > timeoutMs) {
                throw IllegalStateException("local knowledge indexing timed out")
            }
            delay(200)
        }
    }

    private fun createNotification(): Notification {
        ensureNotificationChannel()
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(getString(R.string.debug_seed_notification_title))
            .setContentText(getString(R.string.debug_seed_notification_body))
            .setOngoing(true)
            .build()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) {
            return
        }
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.debug_seed_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "debug-knowledge-seed"
        private const val NOTIFICATION_ID = 1042
        private val isRunning = AtomicBoolean(false)
    }
}
