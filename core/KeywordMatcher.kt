package com.nynaja.vui

sealed interface MatchResult {
    data class Single(val topic: Topic) : MatchResult
    data class Multiple(val topics: List<Topic>) : MatchResult
    object None : MatchResult
}

/**
 * Whole-word/phrase keyword matching. If several topics match, the topic with the
 * longest matching keyword wins ("back pain" beats "pain"); a tie returns [MatchResult.Multiple].
 */
class KeywordMatcher {
    fun match(utterance: String, language: Language, topics: List<Topic>): MatchResult {
        val padded = " ${normalize(utterance)} "
        val hits = topics.mapNotNull { topic ->
            val best = topic.keywords[language].orEmpty()
                .map { normalize(it) }
                .filter { it.isNotEmpty() && padded.contains(" $it ") }
                .maxOfOrNull { it.length }
            best?.let { topic to it }
        }
        if (hits.isEmpty()) return MatchResult.None
        val top = hits.maxOf { it.second }
        val winners = hits.filter { it.second == top }.map { it.first }
        return if (winners.size == 1) MatchResult.Single(winners.first()) else MatchResult.Multiple(winners)
    }
}
