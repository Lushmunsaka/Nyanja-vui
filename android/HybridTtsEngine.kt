package com.nynaja.vui.android

import com.nynaja.vui.Language
import com.nynaja.vui.SpeakResult
import com.nynaja.vui.TtsEngine

class HybridTtsEngine(
    private val deviceTts: AndroidTtsEngine,
    private val cloudTts: GoogleCloudTtsEngine,
) : TtsEngine {

    override suspend fun speak(text: String, language: Language): SpeakResult {
        if (language == Language.NYANJA) {
            val cloudResult = cloudTts.speak(text, language)
            if (cloudResult is SpeakResult.Done) {
                return cloudResult
            }
        }
        val deviceResult = deviceTts.speak(text, language)
        if (deviceResult is SpeakResult.Done) {
            return deviceResult
        }
        return cloudTts.speak(text, language)
    }
}
