package com.lifuyue.kora.feature.chat

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

internal fun Context.chatString(
    name: String,
    vararg args: Any,
): String {
    val id = resources.getIdentifier(name, "string", packageName)
    if (id == 0) {
        return name
    }
    return if (args.isEmpty()) {
        getString(id)
    } else {
        getString(id, *args)
    }
}

internal fun Context.appString(
    name: String,
    vararg args: Any,
): String {
    val id = resources.getIdentifier(name, "string", applicationContext.packageName)
    if (id == 0) {
        return name
    }
    return if (args.isEmpty()) {
        getString(id)
    } else {
        getString(id, *args)
    }
}

@Composable
internal fun chatString(
    name: String,
    vararg args: Any,
): String = LocalContext.current.chatString(name, *args)

@Composable
internal fun appString(
    name: String,
    vararg args: Any,
): String = LocalContext.current.appString(name, *args)
