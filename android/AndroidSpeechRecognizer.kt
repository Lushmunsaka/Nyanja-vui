package com.nynaja.vui.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.nynaja.vui.AsrEngine
import com.nynaja.vui.Language
import com.nynaja.vui.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class AndroidSpeechRecognizer(private val context: Context) : AsrEngine {
    override suspend fun listen(language: Language): RecognitionResult = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                cont.resume(RecognitionResult.Failure("Speech recognition not available"))
                return@suspendCancellableCoroutine
            }

            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag(language.tag))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            var resumed = false

            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (!resumed && cont.isActive) {
                        resumed = true
                        try { speechRecognizer.destroy() } catch (_: Exception) {}
                        val result = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> RecognitionResult.NoSpeech
                            else -> RecognitionResult.Failure("Speech error code: $error")
                        }
                        cont.resume(result)
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (!resumed && cont.isActive) {
                        resumed = true
                        try { speechRecognizer.destroy() } catch (_: Exception) {}
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val transcript = matches?.firstOrNull()
                        if (!transcript.isNullOrBlank()) {
                            cont.resume(RecognitionResult.Success(transcript))
                        } else {
                            cont.resume(RecognitionResult.NoSpeech)
                        }
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            speechRecognizer.setRecognitionListener(listener)

            try {
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                if (!resumed && cont.isActive) {
                    resumed = true
                    try { speechRecognizer.destroy() } catch (_: Exception) {}
                    cont.resume(RecognitionResult.Failure(e.message ?: "Failed to start speech recognition"))
                }
            }

            cont.invokeOnCancellation {
                try {
                    speechRecognizer.destroy()
                } catch (_: Exception) {}
            }
        }
    }
}
