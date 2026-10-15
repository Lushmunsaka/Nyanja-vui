package com.nynaja.vui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

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
 * Google Cloud Speech-to-Text V2 REST API implementation.
 * Provide your API key and Google Cloud Project ID to transcribe spoken audio.
 */
class GoogleV2SpeechApi(
    private val apiKey: String = "",
    private val projectId: String = "",
    private val location: String = "global",
    private val languageMap: Map<Language, String> = mapOf(
        Language.ENGLISH to "en-US",
        Language.NYANJA to "ny-ZM",
    ),
) : SpeechApi {

    override suspend fun transcribe(pcm16: ByteArray, language: Language): ApiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ApiResult.Error("Google Speech API Key is not configured")
        }

        try {
            val endpoint = "https://speech.googleapis.com/v1/speech:recognize?key=$apiKey"
            val langCode = languageMap[language] ?: "en-US"

            val audioBase64 = Base64.getEncoder().encodeToString(pcm16)

            val requestJson = JSONObject().apply {
                put("config", JSONObject().apply {
                    put("encoding", "LINEAR16")
                    put("sampleRateHertz", 16000)
                    put("languageCode", langCode)
                    if (language == Language.NYANJA) {
                        put("alternativeLanguageCodes", JSONArray().apply {
                            put("ny")
                            put("ny-MW")
                        })
                    }
                })
                put("audio", JSONObject().apply {
                    put("content", audioBase64)
                })
            }

            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            connection.outputStream.use { os ->
                os.write(requestJson.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseStr)
                val results = responseJson.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val firstResult = results.getJSONObject(0)
                    val alternatives = firstResult.optJSONArray("alternatives")
                    if (alternatives != null && alternatives.length() > 0) {
                        val top = alternatives.getJSONObject(0)
                        val transcript = top.optString("transcript", "").trim()
                        val confidence = top.optDouble("confidence", 0.0).toFloat()
                        if (transcript.isNotEmpty()) {
                            return@withContext ApiResult.Text(transcript, confidence)
                        }
                    }
                }
                return@withContext ApiResult.Empty
            } else {
                val errorStr = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                return@withContext ApiResult.Error("API HTTP $responseCode: $errorStr")
            }
        } catch (e: Exception) {
            return@withContext ApiResult.Error("STT Request failed: ${e.localizedMessage}")
        }
    }
}
