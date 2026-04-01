package com.lifuyue.kora.core.database

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class LocalKnowledgeBenchmarkDocument(
    val id: String,
    val title: String,
    val sourceLabel: String,
    val body: String,
)

data class LocalKnowledgeBenchmarkQuery(
    val query: String,
    val expectedDocumentId: String,
    val minimumRank: Int,
    val weight: Double,
    val tag: String,
)

data class LocalKnowledgeBenchmarkData(
    val documents: List<LocalKnowledgeBenchmarkDocument>,
    val queries: List<LocalKnowledgeBenchmarkQuery>,
)

private const val LOCAL_KNOWLEDGE_BENCHMARK_RESOURCE = "local_knowledge_benchmark.json"

internal fun loadLocalKnowledgeBenchmarkData(): LocalKnowledgeBenchmarkData {
    val inputStream =
        checkNotNull(LocalKnowledgeBenchmarkData::class.java.classLoader?.getResourceAsStream(LOCAL_KNOWLEDGE_BENCHMARK_RESOURCE)) {
            "Missing benchmark resource: $LOCAL_KNOWLEDGE_BENCHMARK_RESOURCE"
        }
    val root =
        Json.parseToJsonElement(inputStream.bufferedReader().use { it.readText() })
            .jsonObject
    return LocalKnowledgeBenchmarkData(
        documents = root.requireArray("documents").map { it.jsonObject.toBenchmarkDocument() },
        queries = root.requireArray("queries").map { it.jsonObject.toBenchmarkQuery() },
    )
}

internal fun LocalKnowledgeStore.seedBenchmarkData(
    benchmarkData: LocalKnowledgeBenchmarkData,
    nowBase: Long = 10_000L,
) {
    seedLocalKnowledge(
        LocalKnowledgeSeedPayload(
            documents = benchmarkData.documents.map { document ->
                LocalKnowledgeSeedDocument(
                    title = document.title,
                    sourceLabel = document.sourceLabel,
                    body = document.body,
                )
            },
        ),
        nowBase = nowBase,
    )
}

private fun JsonObject.toBenchmarkDocument(): LocalKnowledgeBenchmarkDocument =
    LocalKnowledgeBenchmarkDocument(
        id = requireString("id"),
        title = requireString("title"),
        sourceLabel = requireString("sourceLabel"),
        body = requireString("body"),
    )

private fun JsonObject.toBenchmarkQuery(): LocalKnowledgeBenchmarkQuery =
    LocalKnowledgeBenchmarkQuery(
        query = requireString("query"),
        expectedDocumentId = requireString("expectedDocumentId"),
        minimumRank = requireInt("minimumRank"),
        weight = requireDouble("weight"),
        tag = requireString("tag"),
    )

private fun JsonObject.requireArray(key: String): JsonArray = getValue(key).jsonArray

private fun JsonObject.requireString(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.requireInt(key: String): Int = getValue(key).jsonPrimitive.int

private fun JsonObject.requireDouble(key: String): Double = getValue(key).jsonPrimitive.double
