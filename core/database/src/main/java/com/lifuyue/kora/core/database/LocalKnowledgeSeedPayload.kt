package com.lifuyue.kora.core.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class LocalKnowledgeSeedDocument(
    val title: String,
    val sourceLabel: String,
    val body: String,
)

data class LocalKnowledgeSeedPayload(
    val documents: List<LocalKnowledgeSeedDocument>,
)

private val LOCAL_KNOWLEDGE_SEED_JSON = Json

fun parseLocalKnowledgeSeedPayload(jsonText: String): LocalKnowledgeSeedPayload {
    val root =
        LOCAL_KNOWLEDGE_SEED_JSON
            .parseToJsonElement(jsonText)
            .jsonObject
    return LocalKnowledgeSeedPayload(
        documents = root.requireArray("documents").map { it.jsonObject.toSeedDocument() },
    )
}

fun LocalKnowledgeStore.seedLocalKnowledge(
    payload: LocalKnowledgeSeedPayload,
    replaceExisting: Boolean = true,
    nowBase: Long = System.currentTimeMillis(),
) {
    if (replaceExisting) {
        clearLocalKnowledge()
    }
    payload.documents.forEachIndexed { index, document ->
        importText(
            title = document.title,
            text = document.body,
            sourceLabel = document.sourceLabel,
            now = nowBase + index,
        )
    }
}

private fun JsonObject.toSeedDocument(): LocalKnowledgeSeedDocument =
    LocalKnowledgeSeedDocument(
        title = requireString("title"),
        sourceLabel = requireString("sourceLabel"),
        body = requireString("body"),
    )

private fun JsonObject.requireArray(key: String): JsonArray = getValue(key).jsonArray

private fun JsonObject.requireString(key: String): String = getValue(key).jsonPrimitive.content
