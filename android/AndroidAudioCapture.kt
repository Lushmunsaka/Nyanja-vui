package com.nynaja.vui.android

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.nynaja.vui.AudioCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class AndroidAudioCapture(
    private val sampleRate: Int = 16000,
    private val maxDurationMs: Int = 3500,
    private val silenceThresholdAmplitude: Int = 250,
    private val silenceTimeoutMs: Int = 600,
) : AudioCapture {

    @SuppressLint("MissingPermission")
    override suspend fun captureUtterance(): ByteArray? = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(2048)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            return@withContext null
        }

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)

        try {
            recorder.startRecording()
            val startTime = System.currentTimeMillis()
            var lastVoiceTime = System.currentTimeMillis()
            var speechDetected = false

            while (System.currentTimeMillis() - startTime < maxDurationMs) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    output.write(buffer, 0, read)

                    var maxAmplitude = 0
                    for (i in 0 until read step 2) {
                        if (i + 1 < read) {
                            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            val shortVal = sample.toShort()
                            maxAmplitude = maxOf(maxAmplitude, abs(shortVal.toInt()))
                        }
                    }

                    if (maxAmplitude > silenceThresholdAmplitude) {
                        speechDetected = true
                        lastVoiceTime = System.currentTimeMillis()
                    } else if (speechDetected && (System.currentTimeMillis() - lastVoiceTime > silenceTimeoutMs)) {
                        break
                    }
                }
            }
            recorder.stop()

            val bytes = output.toByteArray()
            if (bytes.isNotEmpty() && speechDetected) bytes else null
        } catch (e: Exception) {
            null
        } finally {
            try {
                recorder.release()
            } catch (_: Exception) {}
        }
    }
}
