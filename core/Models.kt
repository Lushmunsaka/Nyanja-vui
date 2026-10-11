package com.nynaja.vui

enum class Language(val tag: String) {
    ENGLISH("en"),
    NYANJA("ny"),
}

data class Topic(
    val id: String,
    val titles: Map<Language, String>,
    val content: Map<Language, String>,
    val keywords: Map<Language, Set<String>> = emptyMap(),
) {
    fun title(lang: Language): String = titles[lang] ?: titles[Language.ENGLISH] ?: id

    /** Null when this topic has no verified text in [lang]. */
    fun text(lang: Language): String? = content[lang]

    fun withNormalizedKeywords(): Topic = copy(
        keywords = keywords.mapValues { (_, words) ->
            words.map { normalize(it) }.filter { it.isNotEmpty() }.toSet()
        },
    )
}

private val NON_WORD = Regex("[^\\p{L}\\p{N}']+")

/** Lowercase, strip punctuation (keeps apostrophes), collapse spaces. Used for every comparison. */
fun normalize(text: String): String = text.lowercase().replace(NON_WORD, " ").trim()
