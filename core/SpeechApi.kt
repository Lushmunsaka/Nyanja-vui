package com.nynaja.vui

/** Records one utterance as PCM16, 16 kHz, mono. Return null if nothing was said. */
interface AudioCapture {
    suspend fun captureUtterance(): ByteArray?
}

sealed interface ApiResult {
    data class Text(val transcript: String, val confidence: Float? = null) : ApiResult
    object Empty : ApiResult
    data class Error(val reason: String) : ApiResult
}

/** The only piece that talks to Google. Everything else is already independent of it. */
interface SpeechApi {
    suspend fun transcribe(pcm16: ByteArray, language: Language): ApiResult
}

/** Record, then send to [api]. Plug in your existing recording code as [AudioCapture]. */
class CaptureAndTranscribeAsr(
    private val capture: AudioCapture,
    private val api: SpeechApi,
) : AsrEngine {
    override suspend fun listen(language: Language): RecognitionResult {
        val audio = capture.captureUtterance() ?: return RecognitionResult.NoSpeech
        return when (val r = api.transcribe(audio, language)) {
            is ApiResult.Text -> RecognitionResult.Success(r.transcript, r.confidence)
            ApiResult.Empty -> RecognitionResult.NoSpeech
            is ApiResult.Error -> RecognitionResult.Failure(r.reason)
        }
    }
}

/** Default until billing is set up: the app runs and reports "service problem" instead of crashing. */
class NotConfiguredSpeechApi : SpeechApi {
    override suspend fun transcribe(pcm16: ByteArray, language: Language): ApiResult =
        ApiResult.Error("Speech API not configured yet")
}

/**
 * TODO: implement once billing is set up. Either call Speech-to-Text V2 from the app
 * (Google's client library or REST) or POST the audio to your Flask backend and return its transcript.
 *
 * Per language you will need the language code, model and region from Google's
 * "supported languages" table (the Nyanja code is NOT filled in here on purpose).
 * Map Language.ENGLISH / Language.NYANJA to those codes in one place.
 */
class GoogleV2SpeechApi : SpeechApi {
    override suspend fun transcribe(pcm16: ByteArray, language: Language): ApiResult =
        ApiResult.Error("GoogleV2SpeechApi not implemented yet")
}
