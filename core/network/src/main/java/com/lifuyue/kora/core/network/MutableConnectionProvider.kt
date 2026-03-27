package com.lifuyue.kora.core.network

import com.lifuyue.kora.core.common.ConnectionSnapshot
import com.lifuyue.kora.core.common.ConnectionSnapshotProvider
import java.util.concurrent.atomic.AtomicReference

class MutableConnectionProvider(
    initialSnapshot: ConnectionSnapshot =
        ConnectionSnapshot(
            serverBaseUrl = ConnectionConfig.normalizeBaseUrl("https://localhost"),
            apiKey = null,
        ),
) : BaseUrlProvider, ApiKeyProvider, ConnectionSnapshotProvider {
    private val snapshotRef = AtomicReference(initialSnapshot)

    override fun getBaseUrl(): String =
        snapshotRef.get().serverBaseUrl ?: ConnectionConfig.normalizeBaseUrl("https://localhost")

    override fun getApiKey(): String? = snapshotRef.get().apiKey

    override fun getSnapshot(): ConnectionSnapshot = snapshotRef.get()

    fun update(snapshot: ConnectionSnapshot) {
        snapshotRef.set(snapshot)
    }
}
