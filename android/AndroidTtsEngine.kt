package com.nynaja.vui.android

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.nynaja.vui.Language
import com.nynaja.vui.SpeakResult
import com.nynaja.vui.TtsEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Speaks with the phone's built-in TTS engine (no billing needed). English works on most phones.
 * If the device has no Nyanja voice, returns [SpeakResult.LanguageUnsupported] so the caller can
 * fall back to on-screen text or pre-recorded audio. Keep each utterance under
 * TextToSpeech.getMaxSpeechInputLength() (about 4000 characters).
 */
class AndroidTtsEngine(context: Context) : TtsEngine {
    private val ready = CompletableDeferred<Boolean>()
    private val engine: TextToSpeech =
        TextToSpeech(context.applicationContext) { status -> ready.complete(status == TextToSpeech.SUCCESS) }

    override suspend fun speak(text: String, language: Language): SpeakResult {
        if (!ready.await()) return SpeakResult.Failed("TextToSpeech failed to initialise")

        val locale = Locale.forLanguageTag(language.tag)
        if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
        } else {
            engine.language = Locale.ENGLISH
        }

        return suspendCancellableCoroutine { cont ->
            val id = UUID.randomUUID().toString()
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(SpeakResult.Done)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(SpeakResult.Failed("TTS error"))
                }
            })
            cont.invokeOnCancellation { engine.stop() }
            val queued = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            if (queued != TextToSpeech.SUCCESS && cont.isActive) cont.resume(SpeakResult.Failed("TTS refused text"))
        }
    }

    fun shutdown() {
        engine.stop()
        engine.shutdown()
    }
}
