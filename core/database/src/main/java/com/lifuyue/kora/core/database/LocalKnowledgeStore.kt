package com.lifuyue.kora.core.database

import com.lifuyue.kora.core.database.dao.LocalKnowledgeChunkDao
import com.lifuyue.kora.core.database.dao.LocalKnowledgeDocumentDao
import com.lifuyue.kora.core.database.dao.LocalKnowledgePostingDao
import com.lifuyue.kora.core.database.entity.LocalKnowledgeChunkEntity
import com.lifuyue.kora.core.database.entity.LocalKnowledgeDocumentEntity
import com.lifuyue.kora.core.database.entity.LocalKnowledgePostingEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln

private const val LOCAL_SEARCH_CHUNK_SIZE = 400
private const val LOCAL_SEARCH_CHUNK_OVERLAP = 60
private const val LOCAL_SEARCH_SNIPPET_LIMIT = 280
private const val LOCAL_SEARCH_TITLE_BOOST = 1.8
private const val LOCAL_SEARCH_EXACT_BOOST = 1.0
private const val LOCAL_SEARCH_TITLE_EXACT_BOOST = 8.0
private const val LOCAL_SEARCH_TITLE_COVERAGE_BOOST = 6.0
private const val LOCAL_SEARCH_TITLE_FULL_MATCH_BOOST = 4.0
private const val LOCAL_SEARCH_POSITION_WEIGHT = 0.15
private const val LOCAL_SEARCH_MAX_HITS_PER_DOCUMENT = 2
private const val LOCAL_SEARCH_BM25_K1 = 1.2
private const val LOCAL_SEARCH_BM25_B = 0.75
private val NON_SEARCH_TEXT_REGEX = Regex("[^\\p{L}\\p{N}\\s]")
private val SEARCH_WHITESPACE_REGEX = Regex("\\s+")
private val SEARCH_PART_REGEX = Regex("[\\p{IsHan}]+|[\\p{L}\\p{N}]+")
private val PRIMARY_SENTENCE_BOUNDARIES = charArrayOf('。', '！', '？', '；', '.', '!', '?', '\n')
private val SECONDARY_SENTENCE_BOUNDARIES = charArrayOf('，', '、', ',')

enum class LocalKnowledgeIndexStatus {
    Indexing,
    Ready,
    Failed,
}

data class LocalKnowledgeDocument(
    val documentId: String,
    val title: String,
    val sourceLabel: String,
    val previewText: String,
    val chunkCount: Int,
    val isEnabled: Boolean,
    val indexStatus: LocalKnowledgeIndexStatus,
    val indexErrorMessage: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

data class LocalKnowledgeChunk(
    val chunkId: String,
    val documentId: String,
    val chunkIndex: Int,
    val text: String,
)

data class LocalKnowledgeHit(
    val documentId: String,
    val chunkId: String,
    val title: String,
    val sourceLabel: String,
    val snippet: String,
    val score: Double,
)

@Singleton
class LocalKnowledgeStore private constructor(
    private val documentDao: LocalKnowledgeDocumentDao,
    private val chunkDao: LocalKnowledgeChunkDao,
    private val postingDao: LocalKnowledgePostingDao,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(
        documentDao: LocalKnowledgeDocumentDao,
        chunkDao: LocalKnowledgeChunkDao,
        postingDao: LocalKnowledgePostingDao,
    ) : this(
        documentDao = documentDao,
        chunkDao = chunkDao,
        postingDao = postingDao,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    constructor(
        documentDao: LocalKnowledgeDocumentDao,
        chunkDao: LocalKnowledgeChunkDao,
        postingDao: LocalKnowledgePostingDao,
        ioDispatcher: CoroutineDispatcher,
    ) : this(
        documentDao = documentDao,
        chunkDao = chunkDao,
        postingDao = postingDao,
        scope = CoroutineScope(SupervisorJob() + ioDispatcher),
    )

    fun observeDocuments(): Flow<List<LocalKnowledgeDocument>> =
        documentDao.observeDocuments().map { items -> items.map(LocalKnowledgeDocumentEntity::toModel) }

    fun observeDocument(documentId: String): Flow<LocalKnowledgeDocument?> =
        documentDao.observeDocument(documentId).map { it?.toModel() }

    fun observeChunks(documentId: String): Flow<List<LocalKnowledgeChunk>> =
        chunkDao.observeChunks(documentId).map { items -> items.map(LocalKnowledgeChunkEntity::toModel) }

    fun observeDocumentWithChunks(documentId: String): Flow<Pair<LocalKnowledgeDocument?, List<LocalKnowledgeChunk>>> =
        combine(observeDocument(documentId), observeChunks(documentId)) { document, chunks -> document to chunks }

    fun importText(
        title: String,
        text: String,
        sourceLabel: String,
        enabled: Boolean = true,
        now: Long = System.currentTimeMillis(),
    ) {
        val trimmedTitle = title.trim()
        val normalizedText = text.trim()
        require(trimmedTitle.isNotBlank()) { "标题不能为空" }
        require(normalizedText.isNotBlank()) { "内容不能为空" }
        val documentId = "local-doc-$now"
        val normalizedTitle = trimmedTitle.normalizeForSearch()
        documentDao.upsert(
            LocalKnowledgeDocumentEntity(
                documentId = documentId,
                title = trimmedTitle,
                normalizedTitle = normalizedTitle,
                sourceLabel = sourceLabel.trim().ifBlank { "本地资料" },
                rawText = normalizedText,
                previewText = normalizedText.take(160),
                chunkCount = 0,
                isEnabled = enabled,
                indexStatus = LocalKnowledgeIndexStatus.Indexing.name,
                indexErrorMessage = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        scope.launch {
            runCatching {
                reindexDocument(documentId = documentId, now = now)
            }.onFailure { error ->
                val current = documentDao.getDocument(documentId) ?: return@onFailure
                documentDao.upsert(
                    current.copy(
                        indexStatus = LocalKnowledgeIndexStatus.Failed.name,
                        indexErrorMessage = error.message,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    fun setEnabled(
        documentId: String,
        enabled: Boolean,
        now: Long = System.currentTimeMillis(),
    ) {
        val current = documentDao.getDocument(documentId) ?: return
        documentDao.upsert(current.copy(isEnabled = enabled, updatedAt = now))
    }

    fun deleteDocument(documentId: String) {
        postingDao.deleteByDocumentId(documentId)
        chunkDao.deleteByDocumentId(documentId)
        documentDao.delete(documentId)
    }

    fun search(
        query: String,
        limit: Int = 4,
    ): List<LocalKnowledgeHit> {
        val normalizedQuery = query.normalizeForSearch()
        val queryTokens = query.extractSearchTokens().distinct()
        if (queryTokens.isEmpty()) {
            return emptyList()
        }
        val readyDocuments =
            documentDao.getDocuments()
                .filter { it.isEnabled && it.indexStatus == LocalKnowledgeIndexStatus.Ready.name }
        if (readyDocuments.isEmpty()) {
            return emptyList()
        }
        val readyDocumentIds = readyDocuments.map(LocalKnowledgeDocumentEntity::documentId)
        val readyDocumentsById = readyDocuments.associateBy(LocalKnowledgeDocumentEntity::documentId)
        val titleTokensByDocumentId =
            readyDocuments.associate { document ->
                document.documentId to document.title.extractSearchTokens().toSet()
            }
        val postings =
            postingDao.getPostingsForTerms(queryTokens)
                .filter { it.documentId in readyDocumentIds }
        if (postings.isEmpty()) {
            return emptyList()
        }
        val postingsByChunkId = postings.groupBy(LocalKnowledgePostingEntity::chunkId)
        val chunks =
            chunkDao.getChunksByIds(postingsByChunkId.keys.toList())
                .filter { it.documentId in readyDocumentIds }
        if (chunks.isEmpty()) {
            return emptyList()
        }
        val averageTokenCount = chunkDao.getAverageTokenCountForDocuments(readyDocumentIds)?.takeIf { it > 0.0 } ?: 1.0
        val totalChunkCount = readyDocuments.sumOf { it.chunkCount }.coerceAtLeast(1)
        val documentFrequencyByTerm =
            postings.groupBy(LocalKnowledgePostingEntity::term).mapValues { (_, items) ->
                items.map(LocalKnowledgePostingEntity::chunkId).distinct().size
            }

        val ranked =
            chunks.mapNotNull { chunk ->
                val document = readyDocumentsById[chunk.documentId] ?: return@mapNotNull null
                val titleTokens = titleTokensByDocumentId[document.documentId].orEmpty()
                val chunkPostings = postingsByChunkId[chunk.chunkId].orEmpty()
                val bm25Score =
                    chunkPostings.sumOf { posting ->
                        val documentFrequency = documentFrequencyByTerm[posting.term] ?: 1
                        val idf = ln(1.0 + ((totalChunkCount - documentFrequency + 0.5) / (documentFrequency + 0.5)))
                        val tf = posting.termFrequency.toDouble()
                        val docLength = chunk.tokenCount.toDouble().coerceAtLeast(1.0)
                        val numerator = tf * (LOCAL_SEARCH_BM25_K1 + 1.0)
                        val denominator =
                            tf + LOCAL_SEARCH_BM25_K1 * (1.0 - LOCAL_SEARCH_BM25_B + LOCAL_SEARCH_BM25_B * (docLength / averageTokenCount))
                        idf * (numerator / denominator)
                    }
                val titleBoost =
                    chunkPostings.count { it.isTitleTerm } * LOCAL_SEARCH_TITLE_BOOST
                val titleCoverageCount = queryTokens.count { it in titleTokens }
                val titleCoverageBoost =
                    if (titleCoverageCount == 0) {
                        0.0
                    } else {
                        titleCoverageCount.toDouble() / queryTokens.size * LOCAL_SEARCH_TITLE_COVERAGE_BOOST
                    }
                val titleFullMatchBoost =
                    if (titleCoverageCount == queryTokens.size) {
                        LOCAL_SEARCH_TITLE_FULL_MATCH_BOOST
                    } else {
                        0.0
                    }
                val exactBoost =
                    if (chunk.normalizedText.contains(normalizedQuery)) {
                        LOCAL_SEARCH_EXACT_BOOST
                    } else {
                        0.0
                    }
                val titleExactBoost =
                    if (document.normalizedTitle.contains(normalizedQuery)) {
                        LOCAL_SEARCH_TITLE_EXACT_BOOST
                    } else {
                        0.0
                    }
                val positionBoost = 1.0 / (1.0 + chunk.chunkIndex * LOCAL_SEARCH_POSITION_WEIGHT)
                val totalScore =
                    bm25Score +
                        titleBoost +
                        titleCoverageBoost +
                        titleFullMatchBoost +
                        exactBoost +
                        titleExactBoost +
                        positionBoost
                if (totalScore <= 0.0) {
                    null
                } else {
                    LocalKnowledgeHit(
                        documentId = document.documentId,
                        chunkId = chunk.chunkId,
                        title = document.title,
                        sourceLabel = document.sourceLabel,
                        snippet = chunk.text.extractSnippet(queryTokens),
                        score = totalScore,
                    )
                }
            }.sortedByDescending(LocalKnowledgeHit::score)

        val hits = mutableListOf<LocalKnowledgeHit>()
        val hitsPerDocument = linkedMapOf<String, Int>()
        ranked.forEach { hit ->
            val currentCount = hitsPerDocument[hit.documentId] ?: 0
            if (currentCount < LOCAL_SEARCH_MAX_HITS_PER_DOCUMENT) {
                hits += hit
                hitsPerDocument[hit.documentId] = currentCount + 1
            }
            if (hits.size >= limit) {
                return hits
            }
        }
        return hits
    }

    private fun reindexDocument(
        documentId: String,
        now: Long,
    ) {
        val document = documentDao.getDocument(documentId) ?: return
        val titleTokens = document.title.extractSearchTokens()
        val titleTermCounts = titleTokens.groupingBy { it }.eachCount()
        val titleTokenSet = titleTokens.toSet()
        val chunks =
            document.rawText
                .chunkForLocalSearch()
                .mapIndexed { index, chunk ->
                    val chunkTokens = chunk.extractSearchTokens()
                    LocalKnowledgeChunkEntity(
                        chunkId = "$documentId-chunk-$index",
                        documentId = documentId,
                        chunkIndex = index,
                        text = chunk,
                        normalizedText = chunk.normalizeForSearch(),
                        tokenCount = chunkTokens.size,
                    )
                }
        val postings =
            chunks.flatMap { chunk ->
                val chunkTermCounts = chunk.text.extractSearchTokens().groupingBy { it }.eachCount().toMutableMap()
                if (chunk.chunkIndex == 0) {
                    titleTermCounts.forEach { (term, count) ->
                        chunkTermCounts[term] = (chunkTermCounts[term] ?: 0) + count
                    }
                }
                chunkTermCounts
                    .map { (term, count) ->
                        LocalKnowledgePostingEntity(
                            term = term,
                            chunkId = chunk.chunkId,
                            documentId = documentId,
                            termFrequency = count,
                            isTitleTerm = term in titleTokenSet,
                        )
                    }
            }

        postingDao.deleteByDocumentId(documentId)
        chunkDao.deleteByDocumentId(documentId)
        if (chunks.isNotEmpty()) {
            chunkDao.upsertAll(chunks)
        }
        if (postings.isNotEmpty()) {
            postingDao.upsertAll(postings)
        }
        documentDao.upsert(
            document.copy(
                previewText = chunks.firstOrNull()?.text.orEmpty().take(160),
                chunkCount = chunks.size,
                indexStatus = LocalKnowledgeIndexStatus.Ready.name,
                indexErrorMessage = null,
                updatedAt = maxOf(now, System.currentTimeMillis()),
            ),
        )
    }
}

private fun LocalKnowledgeDocumentEntity.toModel(): LocalKnowledgeDocument =
    LocalKnowledgeDocument(
        documentId = documentId,
        title = title,
        sourceLabel = sourceLabel,
        previewText = previewText,
        chunkCount = chunkCount,
        isEnabled = isEnabled,
        indexStatus = LocalKnowledgeIndexStatus.valueOf(indexStatus),
        indexErrorMessage = indexErrorMessage,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun LocalKnowledgeChunkEntity.toModel(): LocalKnowledgeChunk =
    LocalKnowledgeChunk(
        chunkId = chunkId,
        documentId = documentId,
        chunkIndex = chunkIndex,
        text = text,
    )

private fun String.chunkForLocalSearch(
    maxChunkLength: Int = LOCAL_SEARCH_CHUNK_SIZE,
    overlapLength: Int = LOCAL_SEARCH_CHUNK_OVERLAP,
): List<String> {
    val normalized = replace("\r\n", "\n").trim()
    if (normalized.isBlank()) {
        return emptyList()
    }
    if (normalized.length <= maxChunkLength) {
        return listOf(normalized)
    }
    val segments =
        normalized
            .splitIntoSemanticParagraphs()
            .flatMap { paragraph -> paragraph.splitLongParagraph(maxChunkLength) }

    val chunks = mutableListOf<String>()
    var current = ""
    segments.forEach { segment ->
        val trimmedSegment = segment.trim()
        if (trimmedSegment.isBlank()) {
            return@forEach
        }
        val candidate =
            if (current.isBlank()) {
                trimmedSegment
            } else {
                listOf(current, trimmedSegment).joinToString(separator = "\n").trim()
            }
        if (current.isNotBlank() && candidate.length > maxChunkLength) {
            chunks += current
            val overlap = current.extractSemanticOverlap(overlapLength)
            val overlapped =
                listOfNotNull(overlap.takeIf(String::isNotBlank), trimmedSegment)
                    .joinToString(separator = "\n")
                    .trim()
            current = if (overlapped.length <= maxChunkLength) overlapped else trimmedSegment
        } else {
            current = candidate
        }
    }
    if (current.isNotBlank()) {
        chunks += current
    }
    return chunks
}

private fun String.extractSnippet(
    queryTokens: List<String>,
    maxLength: Int = LOCAL_SEARCH_SNIPPET_LIMIT,
): String {
    val compact = trim().replace(SEARCH_WHITESPACE_REGEX, " ")
    if (compact.isBlank()) {
        return ""
    }
    if (compact.length <= maxLength) {
        return compact
    }

    val distinctTokens = queryTokens.distinct()
    val lowered = compact.lowercase()
    val sentenceRanges = compact.collectSentenceRanges()
    val candidateRanges =
        if (sentenceRanges.isNotEmpty()) {
            buildList {
                for (startIndex in sentenceRanges.indices) {
                    var endIndex = startIndex
                    while (endIndex < sentenceRanges.size) {
                        val candidate = sentenceRanges[startIndex].first..sentenceRanges[endIndex].last
                        if (candidate.last - candidate.first + 1 > maxLength && endIndex > startIndex) {
                            break
                        }
                        add(candidate)
                        if (candidate.last - candidate.first + 1 >= maxLength) {
                            break
                        }
                        endIndex += 1
                    }
                }
            }
        } else {
            listOf(0 until compact.length)
        }

    val bestRange =
        candidateRanges
            .map { range ->
                val snippet = compact.substring(range.first, range.last + 1).trim()
                val matchedTokens = distinctTokens.filter { token -> lowered.indexOf(token, range.first) in range }
                val totalOccurrences =
                    distinctTokens.sumOf { token ->
                        lowered.countOccurrencesInRange(token, range)
                    }
                val firstMatchIndex = distinctTokens.map { token -> lowered.indexOf(token, range.first) }.filter { it in range }.minOrNull()
                    ?: Int.MAX_VALUE
                SnippetCandidate(
                    range = range,
                    tokenCoverage = matchedTokens.size,
                    totalOccurrences = totalOccurrences,
                    firstMatchIndex = firstMatchIndex,
                    length = snippet.length,
                )
            }.maxWithOrNull(
                compareBy<SnippetCandidate> { it.tokenCoverage }
                    .thenBy { it.totalOccurrences }
                    .thenByDescending { Int.MAX_VALUE - it.firstMatchIndex }
                    .thenByDescending { maxLength - it.length },
            )?.range

    val selectedRange =
        when {
            bestRange == null -> compact.centeredSnippetRange(lowered, distinctTokens, maxLength)
            bestRange.last - bestRange.first + 1 <= maxLength -> bestRange
            else -> compact.centeredSnippetRange(lowered, distinctTokens, maxLength, bestRange.first)
        }

    val snippet = compact.substring(selectedRange.first, selectedRange.last + 1).trim()
    return buildString {
        if (selectedRange.first > 0) {
            append("...")
        }
        append(snippet)
        if (selectedRange.last < compact.lastIndex) {
            append("...")
        }
    }
}

private fun String.normalizeForSearch(): String =
    lowercase()
        .replace(NON_SEARCH_TEXT_REGEX, " ")
        .replace(SEARCH_WHITESPACE_REGEX, " ")
        .trim()

private fun String.extractSearchTokens(): List<String> {
    val normalized = normalizeForSearch()
    if (normalized.isBlank()) {
        return emptyList()
    }
    return buildList {
        SEARCH_PART_REGEX.findAll(normalized).forEach { match ->
            val part = match.value
            if (part.any { it.isHanCharacter() }) {
                if (part.length <= 4) {
                    add(part)
                }
                if (part.length >= 2) {
                    part.windowed(size = 2, step = 1, partialWindows = false).forEach(::add)
                }
            } else if (part.length >= 2) {
                add(part)
            }
        }
    }
}

private fun Char.isHanCharacter(): Boolean =
    Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN

private data class SnippetCandidate(
    val range: IntRange,
    val tokenCoverage: Int,
    val totalOccurrences: Int,
    val firstMatchIndex: Int,
    val length: Int,
)

private fun String.splitIntoSemanticParagraphs(): List<String> {
    val paragraphs = mutableListOf<String>()
    val current = StringBuilder()
    lines().forEach { line ->
        val trimmedLine = line.trim()
        when {
            trimmedLine.isBlank() -> {
                current.flushParagraphInto(paragraphs)
            }
            current.isEmpty() -> current.append(trimmedLine)
            current.last().isPrimarySentenceBoundary() -> {
                current.flushParagraphInto(paragraphs)
                current.append(trimmedLine)
            }
            else -> {
                current.append('\n')
                current.append(trimmedLine)
            }
        }
    }
    current.flushParagraphInto(paragraphs)
    return paragraphs
}

private fun String.splitLongParagraph(maxChunkLength: Int): List<String> {
    val trimmed = trim()
    if (trimmed.length <= maxChunkLength) {
        return listOf(trimmed)
    }
    val segments = mutableListOf<String>()
    var start = 0
    while (start < trimmed.length) {
        val remaining = trimmed.length - start
        if (remaining <= maxChunkLength) {
            segments += trimmed.substring(start).trim()
            break
        }
        val hardEnd = (start + maxChunkLength).coerceAtMost(trimmed.length)
        val preferredCut =
            trimmed.findSplitBoundary(
                start = start,
                end = hardEnd,
                preferredBoundaries = PRIMARY_SENTENCE_BOUNDARIES,
            ) ?: trimmed.findSplitBoundary(
                start = start,
                end = hardEnd,
                preferredBoundaries = SECONDARY_SENTENCE_BOUNDARIES,
            )
        val cutIndex = preferredCut ?: hardEnd - 1
        segments += trimmed.substring(start, cutIndex + 1).trim()
        start = (cutIndex + 1).coerceAtLeast(start + 1)
    }
    return segments.filter(String::isNotBlank)
}

private fun String.extractSemanticOverlap(overlapLength: Int): String {
    if (isBlank() || overlapLength <= 0) {
        return ""
    }
    val sentences = trim().collectSentenceRanges()
    val lastSentence =
        sentences
            .lastOrNull()
            ?.let { substring(it.first, it.last + 1).trim() }
            .orEmpty()
    if (lastSentence.isBlank()) {
        return takeLast(overlapLength).trim()
    }
    if (lastSentence.length <= overlapLength) {
        return lastSentence
    }
    val clauseStart =
        lastSentence.findLastAnyOf(SECONDARY_SENTENCE_BOUNDARIES.map(Char::toString))
            ?.takeIf { it.first >= lastSentence.length - overlapLength }
            ?.first
    return when {
        clauseStart != null -> lastSentence.substring(clauseStart + 1).trim()
        else -> lastSentence.takeLast(overlapLength).trim()
    }
}

private fun String.collectSentenceRanges(): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var start = 0
    forEachIndexed { index, char ->
        if (char.isPrimarySentenceBoundary()) {
            if (index >= start) {
                ranges += start..index
            }
            start = index + 1
        }
    }
    if (start < length) {
        ranges += start..lastIndex
    }
    return ranges.filter { substring(it.first, it.last + 1).trim().isNotBlank() }
}

private fun String.centeredSnippetRange(
    lowered: String,
    queryTokens: List<String>,
    maxLength: Int,
    fallbackStart: Int = 0,
): IntRange {
    val matchIndex =
        queryTokens
            .map { token -> lowered.indexOf(token) }
            .filter { it >= 0 }
            .minOrNull()
            ?: fallbackStart
    val leftBoundary = findBoundaryBefore(matchIndex)
    val rightBoundary = findBoundaryAfter(matchIndex)
    var start = leftBoundary ?: (matchIndex - maxLength / 3).coerceAtLeast(0)
    var end = rightBoundary ?: (start + maxLength).coerceAtMost(length)
    if (end - start > maxLength) {
        start = (end - maxLength).coerceAtLeast(0)
    }
    if (end - start < maxLength) {
        val extra = maxLength - (end - start)
        start = (start - extra / 2).coerceAtLeast(0)
        end = (start + maxLength).coerceAtMost(length)
    }
    return start until end
}

private fun String.findBoundaryBefore(index: Int): Int? {
    if (isBlank()) {
        return null
    }
    var cursor = index.coerceIn(0, lastIndex)
    while (cursor > 0) {
        if (this[cursor - 1].isPrimarySentenceBoundary()) {
            return cursor
        }
        cursor -= 1
    }
    return if (first().isPrimarySentenceBoundary()) 1 else 0
}

private fun String.findBoundaryAfter(index: Int): Int? {
    if (isBlank()) {
        return null
    }
    var cursor = index.coerceIn(0, lastIndex)
    while (cursor < length) {
        if (this[cursor].isPrimarySentenceBoundary()) {
            return cursor + 1
        }
        cursor += 1
    }
    return length
}

private fun String.findSplitBoundary(
    start: Int,
    end: Int,
    preferredBoundaries: CharArray,
): Int? {
    val minimum = start + (end - start) / 2
    for (index in end - 1 downTo minimum) {
        if (this[index] in preferredBoundaries) {
            return index
        }
    }
    return null
}

private fun StringBuilder.flushParagraphInto(target: MutableList<String>) {
    toString().trim().takeIf(String::isNotBlank)?.let(target::add)
    clear()
}

private fun Char.isPrimarySentenceBoundary(): Boolean = this in PRIMARY_SENTENCE_BOUNDARIES

private fun String.countOccurrencesInRange(token: String, range: IntRange): Int {
    var count = 0
    var searchIndex = range.first
    while (searchIndex in range) {
        val found = indexOf(token, searchIndex)
        if (found !in range) {
            break
        }
        count += 1
        searchIndex = found + token.length.coerceAtLeast(1)
    }
    return count
}
