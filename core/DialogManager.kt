package com.nynaja.vui

/**
 * Menu-driven voice dialog. No language understanding: the user says a topic keyword,
 * a number from the list just read out, or one of a few commands.
 *
 * Order of interpretation for each utterance: topic keyword -> command -> list number.
 * Call [run] from a coroutine; cancel the coroutine to end the session early.
 */
class DialogManager(
    private val repo: TopicRepository,
    private val asr: AsrEngine,
    private val tts: TtsEngine,
    private val prompts: Prompts = Prompts.default(),
    private val vocab: CommandVocabulary = CommandVocabulary.default(),
    private val matcher: KeywordMatcher = KeywordMatcher(),
    private val maxMisses: Int = 2,
    private val pageSize: Int = 5,
    /** Called with every line that is spoken, so the UI can show captions. */
    private val onText: (String) -> Unit = {},
    private val onNavigateToKeywords: () -> Unit = {},
    private val onNavigateToDialog: () -> Unit = {},
) {
    var language: Language = Language.ENGLISH
        private set

    private var lastTopic: Topic? = null
    private var listing: List<Topic> = emptyList()
    private var listOffset = 0

    private enum class Outcome { OK, MISS, STOP }

    suspend fun run() {
        language = chooseLanguage()
        if (repo.all().isEmpty()) {
            say(PromptKey.NO_TOPICS)
            say(PromptKey.GOODBYE)
            return
        }
        say(PromptKey.WELCOME)
        say(PromptKey.ASK_TOPIC)

        var misses = 0
        var failures = 0
        while (true) {
            when (val result = asr.listen(language)) {
                is RecognitionResult.Success -> {
                    failures = 0
                    when (handle(normalize(result.transcript))) {
                        Outcome.STOP -> { say(PromptKey.GOODBYE); return }
                        Outcome.OK -> misses = 0
                        Outcome.MISS -> misses = onMiss(misses)
                    }
                }
                RecognitionResult.NoSpeech -> misses = onMiss(misses)
                is RecognitionResult.Failure -> {
                    failures++
                    say(PromptKey.SERVICE_PROBLEM)
                    if (failures >= MAX_CONSECUTIVE_FAILURES) { say(PromptKey.GOODBYE); return }
                }
            }
        }
    }

    private suspend fun chooseLanguage(): Language {
        repeat(3) {
            sayAs(prompts.get(PromptKey.CHOOSE_LANGUAGE, Language.ENGLISH), Language.ENGLISH)
            val result = asr.listen(Language.ENGLISH)
            if (result is RecognitionResult.Success) {
                vocab.languageFor(result.transcript)?.let { return it }
            }
        }
        return Language.ENGLISH
    }

    private suspend fun handle(utterance: String): Outcome {
        if (utterance.isEmpty()) return Outcome.MISS

        when (val match = matcher.match(utterance, language, repo.all())) {
            is MatchResult.Single -> { playTopic(match.topic); return Outcome.OK }
            is MatchResult.Multiple -> {
                say(PromptKey.MULTIPLE_MATCHES)
                readList(match.topics, 0, intro = false)
                return Outcome.OK
            }
            MatchResult.None -> Unit
        }

        when (vocab.commandFor(language, utterance)) {
            Command.STOP -> return Outcome.STOP
            Command.KEYWORDS -> {
                onNavigateToKeywords()
                return Outcome.STOP
            }
            Command.DIALOG -> {
                onNavigateToDialog()
                return Outcome.OK
            }
            Command.REPEAT -> {
                val topic = lastTopic
                if (topic == null) say(PromptKey.ASK_TOPIC) else playTopic(topic)
                return Outcome.OK
            }
            Command.LIST -> { readList(repo.all(), 0, intro = true); return Outcome.OK }
            Command.NEXT -> {
                val next = listOffset + pageSize
                if (listing.isNotEmpty() && next < listing.size) readList(listing, next, intro = false)
                else say(PromptKey.ASK_TOPIC)
                return Outcome.OK
            }
            null -> Unit
        }

        val number = vocab.numberFor(language, utterance)
        if (number != null && number in 1..listing.size) {
            playTopic(listing[number - 1])
            return Outcome.OK
        }
        return Outcome.MISS
    }

    /** After repeated misses, read the numbered list so the user can answer with a number. */
    private suspend fun onMiss(misses: Int): Int {
        val next = misses + 1
        if (next >= maxMisses) {
            readList(repo.all(), 0, intro = true)
            return 0
        }
        say(PromptKey.NOT_UNDERSTOOD)
        return next
    }

    private suspend fun playTopic(topic: Topic) {
        lastTopic = topic
        val body = topic.text(language)
        if (body == null) {
            say(PromptKey.CONTENT_UNAVAILABLE)
            return
        }
        sayText(topic.title(language))
        sayText(body)
        say(PromptKey.AFTER_TOPIC)
    }

    private suspend fun readList(items: List<Topic>, offset: Int, intro: Boolean) {
        listing = items
        listOffset = offset
        if (intro) say(PromptKey.LIST_INTRO)
        val end = minOf(offset + pageSize, items.size)
        for (i in offset until end) sayText("${i + 1}. ${items[i].title(language)}")
        if (end < items.size) say(PromptKey.LIST_MORE)
    }

    private suspend fun say(key: PromptKey) = sayText(prompts.get(key, language))

    private suspend fun sayText(text: String) = sayAs(text, language)

    private suspend fun sayAs(text: String, lang: Language) {
        onText(text)
        tts.speak(text, lang)
    }

    private companion object {
        const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
