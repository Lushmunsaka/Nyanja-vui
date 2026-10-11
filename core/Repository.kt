package com.nynaja.vui

sealed interface KeywordResult {
    data class Added(val keyword: String) : KeywordResult
    object Duplicate : KeywordResult
    /** Keyword already belongs to another topic in this language. */
    data class Conflict(val topicId: String) : KeywordResult
    /** Keyword is a voice command or number word (stop, list, one...). */
    object Reserved : KeywordResult
    object Invalid : KeywordResult
    object TopicNotFound : KeywordResult
}

interface TopicRepository {
    fun all(): List<Topic>
    fun get(id: String): Topic?
    fun addKeyword(topicId: String, language: Language, keyword: String): KeywordResult
    fun removeKeyword(topicId: String, language: Language, keyword: String): Boolean
}

/**
 * Prototype store. Keywords added in the admin screen are lost on restart:
 * swap for a Room- or JSON-backed implementation of [TopicRepository] when ready.
 */
class InMemoryTopicRepository(
    topics: List<Topic> = emptyList(),
    private val reservedWords: (Language) -> Set<String> = { emptySet() },
) : TopicRepository {

    private val store = LinkedHashMap<String, Topic>().apply {
        topics.forEach { put(it.id, it.withNormalizedKeywords()) }
    }

    override fun all(): List<Topic> = store.values.toList()

    override fun get(id: String): Topic? = store[id]

    override fun addKeyword(topicId: String, language: Language, keyword: String): KeywordResult {
        val topic = store[topicId] ?: return KeywordResult.TopicNotFound
        val kw = normalize(keyword)
        if (kw.isEmpty()) return KeywordResult.Invalid
        if (reservedWords(language).any { normalize(it) == kw }) return KeywordResult.Reserved

        val owner = store.values.firstOrNull { it.id != topicId && kw in it.keywords[language].orEmpty() }
        if (owner != null) return KeywordResult.Conflict(owner.id)

        val existing = topic.keywords[language].orEmpty()
        if (kw in existing) return KeywordResult.Duplicate

        store[topicId] = topic.copy(keywords = topic.keywords + (language to (existing + kw)))
        return KeywordResult.Added(kw)
    }

    override fun removeKeyword(topicId: String, language: Language, keyword: String): Boolean {
        val topic = store[topicId] ?: return false
        val kw = normalize(keyword)
        val existing = topic.keywords[language].orEmpty()
        if (kw !in existing) return false
        store[topicId] = topic.copy(keywords = topic.keywords + (language to (existing - kw)))
        return true
    }
}
