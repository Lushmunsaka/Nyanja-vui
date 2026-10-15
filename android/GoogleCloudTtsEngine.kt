package com.nynaja.vui.android

import android.content.Context
import android.media.MediaPlayer
import com.nynaja.vui.Language
import com.nynaja.vui.SpeakResult
import com.nynaja.vui.TtsEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import kotlin.coroutines.resume

class GoogleCloudTtsEngine(
    private val context: Context,
    private val apiKey: String,
) : TtsEngine {

    override suspend fun speak(text: String, language: Language): SpeakResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE") {
            return@withContext SpeakResult.Failed("Google Cloud API Key not configured")
        }

        try {
            val endpoint = "https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey"

            val voiceLangCode = when (language) {
                Language.ENGLISH -> "en-US"
                Language.NYANJA -> "sw-KE"
            }

            val requestJson = JSONObject().apply {
                put("input", JSONObject().apply { put("text", text) })
                put("voice", JSONObject().apply {
                    put("languageCode", voiceLangCode)
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
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

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseStr)
                val audioBase64 = responseJson.optString("audioContent", "")
                if (audioBase64.isNotEmpty()) {
                    val audioBytes = Base64.getDecoder().decode(audioBase64)
                    val tempFile = File.createTempFile("tts_audio", ".mp3", context.cacheDir)
                    FileOutputStream(tempFile).use { it.write(audioBytes) }

                    playAudioFile(tempFile)
                    tempFile.delete()
                    return@withContext SpeakResult.Done
                }
            }
            return@withContext SpeakResult.Failed("Cloud TTS HTTP ${connection.responseCode}")
        } catch (e: Exception) {
            return@withContext SpeakResult.Failed("Cloud TTS failed: ${e.localizedMessage}")
        }
    }

    private suspend fun playAudioFile(file: File) = suspendCancellableCoroutine<Unit> { cont ->
        val mediaPlayer = MediaPlayer()
        try {
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.setOnCompletionListener {
                mediaPlayer.release()
                if (cont.isActive) cont.resume(Unit)
            }
            mediaPlayer.setOnErrorListener { mp, _, _ ->
                mp.release()
                if (cont.isActive) cont.resume(Unit)
                true
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            mediaPlayer.release()
            if (cont.isActive) cont.resume(Unit)
        }
        cont.invokeOnCancellation {
            try {
                mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }
}
