package com.nynaja.vui

enum class PromptKey {
    CHOOSE_LANGUAGE, WELCOME, ASK_TOPIC, NOT_UNDERSTOOD, AFTER_TOPIC, LIST_INTRO,
    LIST_MORE, MULTIPLE_MATCHES, SERVICE_PROBLEM, CONTENT_UNAVAILABLE, NO_TOPICS, GOODBYE,
}

class Prompts(private val table: Map<PromptKey, Map<Language, String>>) {
    fun get(key: PromptKey, language: Language): String =
        table[key]?.get(language) ?: table[key]?.get(Language.ENGLISH) ?: key.name

    companion object {
        // TODO(verify): every Nyanja prompt below is a placeholder. Replace with text written
        // and checked by a fluent Nyanja speaker before any user hears it.
        private fun ny(english: String) = "[NY-TODO] $english"

        private fun both(english: String) = mapOf(
            Language.ENGLISH to english,
            Language.NYANJA to ny(english),
        )

        fun default() = Prompts(
            mapOf(
                PromptKey.CHOOSE_LANGUAGE to both("Welcome. For English, say English. For Nyanja, say Nyanja."),
                PromptKey.WELCOME to both("You can ask about a health topic by saying its name. Say list to hear all topics, or stop to finish."),
                PromptKey.ASK_TOPIC to both("Which topic would you like to hear about?"),
                PromptKey.NOT_UNDERSTOOD to both("Sorry, I did not understand. Please say the topic name again, or say list."),
                PromptKey.AFTER_TOPIC to both("Say repeat to hear it again, list for all topics, or say another topic name."),
                PromptKey.LIST_INTRO to both("Here are the topics. Say a topic name or its number."),
                PromptKey.LIST_MORE to both("Say next for more topics."),
                PromptKey.MULTIPLE_MATCHES to both("I found more than one topic. Say the number of the one you want."),
                PromptKey.SERVICE_PROBLEM to both("Sorry, there is a problem. Please try again."),
                PromptKey.CONTENT_UNAVAILABLE to both("Sorry, this topic is not available in this language yet."),
                PromptKey.NO_TOPICS to both("There are no topics yet."),
                PromptKey.GOODBYE to both("Goodbye. Stay healthy."),
            ),
        )
    }
}

enum class Command { STOP, REPEAT, LIST, NEXT, KEYWORDS, DIALOG }

class CommandVocabulary(
    private val words: Map<Language, Map<Command, Set<String>>>,
    private val numberWords: Map<Language, Map<String, Int>>,
    private val languageWords: Map<String, Language>,
) {
    /** Commands are checked in enum order (STOP first). Whole-word match inside the utterance. */
    fun commandFor(language: Language, utterance: String): Command? {
        val padded = " ${normalize(utterance)} "
        val table = words[language] ?: return null
        for (cmd in Command.values()) {
            if (table[cmd].orEmpty().any { padded.contains(" ${normalize(it)} ") }) return cmd
        }
        return null
    }

    /** First spoken number in the utterance: digits ("2") or number words for [language]. */
    fun numberFor(language: Language, utterance: String): Int? {
        for (token in normalize(utterance).split(" ")) {
            token.toIntOrNull()?.let { return it }
            numberWords[language]?.get(token)?.let { return it }
        }
        return null
    }

    fun languageFor(utterance: String): Language? {
        for (token in normalize(utterance).split(" ")) {
            languageWords[token]?.let { return it }
        }
        return null
    }

    /** Words that must not be assigned as topic keywords. */
    fun reserved(language: Language): Set<String> =
        words[language].orEmpty().values.flatten().toSet() + numberWords[language].orEmpty().keys

    companion object {
        fun default() = CommandVocabulary(
            words = mapOf(
                Language.ENGLISH to mapOf(
                    Command.STOP to setOf("stop", "exit", "quit", "goodbye"),
                    Command.REPEAT to setOf("repeat", "again"),
                    Command.LIST to setOf("list", "topics", "menu"),
                    Command.NEXT to setOf("next", "more"),
                    Command.KEYWORDS to setOf("keywords", "admin", "keyword screen", "admin screen"),
                    Command.DIALOG to setOf("dialog", "dialog screen", "chat", "main"),
                ),
                // TODO(verify with a native Nyanja speaker): add Nyanja command words.
                // Until then Nyanja users navigate by keyword or by spoken digits only.
                Language.NYANJA to emptyMap(),
            ),
            numberWords = mapOf(
                Language.ENGLISH to listOf(
                    "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
                    "eighteen", "nineteen", "twenty",
                ).mapIndexed { i, w -> w to (i + 1) }.toMap(),
                // TODO(verify): Nyanja number words.
                Language.NYANJA to emptyMap(),
            ),
            languageWords = mapOf(
                "english" to Language.ENGLISH, "1" to Language.ENGLISH, "one" to Language.ENGLISH,
                "nyanja" to Language.NYANJA, "chinyanja" to Language.NYANJA,
                "chichewa" to Language.NYANJA, "2" to Language.NYANJA, "two" to Language.NYANJA,
            ),
        )
    }
}
