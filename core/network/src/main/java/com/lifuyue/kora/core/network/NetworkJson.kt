package com.lifuyue.kora.core.network

import kotlinx.serialization.json.Json

object NetworkJson {
    val default: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
            encodeDefaults = true
        }
}
