package com.lifuyue.kora.testing

import androidx.compose.runtime.Composable
import com.lifuyue.kora.core.common.ConnectionSnapshot
import kotlinx.coroutines.flow.StateFlow

// Test-only override used by Robolectric acceptance tests to start the app from a stable snapshot.
internal object KoraTestOverrides {
    interface ConnectionRouteOverride {
        @Composable
        fun Render(onConnectionSaved: () -> Unit)
    }

    interface ShellRouteOverride {
        @Composable
        fun Render(snapshot: ConnectionSnapshot)
    }

    var snapshotOverride: StateFlow<ConnectionSnapshot>? = null

    var connectionRouteOverride: ConnectionRouteOverride? = null

    var shellRouteOverride: ShellRouteOverride? = null

    fun reset() {
        snapshotOverride = null
        connectionRouteOverride = null
        shellRouteOverride = null
    }
}
