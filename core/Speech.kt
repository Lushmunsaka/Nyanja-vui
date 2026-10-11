package com.nynaja.vui

sealed interface RecognitionResult {
    data class Success(val transcript: String, val confidence: Float? = null) : RecognitionResult
    object NoSpeech : RecognitionResult
    data class Failure(val reason: String) : RecognitionResult
}

sealed interface SpeakResult {
    object Done : SpeakResult
    object LanguageUnsupported : SpeakResult
    data class Failed(val reason: String) : SpeakResult
}

/** Speech in. Implement with Google Speech-to-Text V2 (see SpeechApi.kt) or a fake. */
interface AsrEngine {
    suspend fun listen(language: Language): RecognitionResult
}

/** Speech out. Implement with Android TTS, Google Cloud TTS, or pre-recorded clips. */
interface TtsEngine {
    suspend fun speak(text: String, language: Language): SpeakResult
}

// ---- Fakes: use these before the Google API is set up, and in tests ----

/** Plays back a fixed list of results; reports Failure once the script runs out. */
class ScriptedAsr(results: List<RecognitionResult>) : AsrEngine {
    private val queue = ArrayDeque(results)

    override suspend fun listen(language: Language): RecognitionResult =
        queue.removeFirstOrNull() ?: RecognitionResult.Failure("script exhausted")

    companion object {
        fun saying(vararg phrases: String) =
            ScriptedAsr(phrases.map { RecognitionResult.Success(it) })
    }
}

/** For an emulator/debug screen: returns whatever [next] supplies (e.g. a typed text field). */
class CallbackAsr(private val next: suspend (Language) -> String?) : AsrEngine {
    override suspend fun listen(language: Language): RecognitionResult {
        val text = next(language)
        return if (text.isNullOrBlank()) RecognitionResult.NoSpeech else RecognitionResult.Success(text)
    }
}

class RecordingTts : TtsEngine {
    val spoken = mutableListOf<Pair<Language, String>>()

    override suspend fun speak(text: String, language: Language): SpeakResult {
        spoken += language to text
        return SpeakResult.Done
    }
}
