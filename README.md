# Nynaja VUI — Voice User Interface & Health Menu

**Nynaja VUI** is a menu-driven Android Voice User Interface (VUI) designed for accessible, spoken health information delivery in **English** and **Nyanja**. The system features Google Cloud Speech-to-Text and Text-to-Speech integrations, voice-driven screen navigation, on-device audio capture, and persistent local topic management.

---

## Key Features

- **Google Cloud Speech-to-Text Integration**: Transcribes live microphone audio using Google Cloud Speech API (`GoogleV2SpeechApi`) with BCP-47 locale mapping (`ny-ZM` for Nyanja, `en-US` for English) and PCM16 16kHz audio capture (`AndroidAudioCapture`).
- **Google Cloud Text-to-Speech Neural Synthesis**: Synthesizes natural spoken audio responses via Google Cloud Text-to-Speech API (`GoogleCloudTtsEngine`), using Bantu phonetics (`sw-KE`) for fluent, natural Nyanja spoken responses.
- **Hybrid Speech & TTS Fallback Engine**: Seamlessly falls back to Android's built-in `SpeechRecognizer` and `TextToSpeech` engine (`AndroidTtsEngine`), as well as keyboard text input.
- **Voice-Driven Screen Navigation**: Navigate between app screens and menus using voice commands (e.g., say `"keywords"` or `"admin"` to switch to topic administration, or `"dialog"` / `"menu"` / `"kukambirana"` to return to the voice menu).
- **20 Verified Health Topics**: Pre-loaded dataset containing 20 health topics (Pregnancy, Danger signs, Malaria, Nutrition, Vaccine, Labour, Postnatal, Child health, etc.) in both English and Nyanja.
- **Persistent On-Device Local Storage**: `PersistentTopicRepository` automatically saves added topic keywords across app restarts using local JSON storage (`topics_v1.json`), running 100% offline on-device.
- **Keyword Administration**: Admin screen (`KeywordAdminScreen`) to view, add, or remove topic keywords per language with automatic conflict and reserved-word validation.
- **Optimized Voice Activity Detection (VAD)**: Snappy 0.6s silence detection for fast turnaround times.

---

## System Architecture

```text
app/       Android application module (MainActivity, Jetpack Compose UI, BuildConfig setup)
core/      Pure Kotlin domain logic (DialogManager, KeywordMatcher, CommandVocabulary, TopicRepository, SpeechApi)
android/   Platform adapters (AndroidAudioCapture, GoogleV2SpeechApi, GoogleCloudTtsEngine, HybridTtsEngine, PersistentTopicRepository, KeywordAdminScreen)
test/      Unit tests for dialog state transitions, navigation, and keyword matching (JUnit + Coroutines Test)
```

---

## Integrated Google Cloud APIs & Configuration

The app integrates two Google Cloud APIs configured via `app/build.gradle.kts`:

1. **Google Cloud Speech-to-Text API**: Used for transcribing audio utterances (`SPEECH_API_KEY`).
2. **Google Cloud Text-to-Speech API**: Used for synthesizing natural spoken responses (`TTS_API_KEY`).

### Configuring API Keys (`app/build.gradle.kts`):
```kotlin
buildConfigField("String", "SPEECH_API_KEY", "\"YOUR_SPEECH_API_KEY\"")
buildConfigField("String", "TTS_API_KEY", "\"YOUR_TTS_API_KEY\"")
buildConfigField("String", "SPEECH_PROJECT_ID", "\"_\"")
```

---

## Running the App

### Requirements
- Android Studio Ladybug / Koala / AGP 8.8+
- JDK 17
- Android Device or Emulator (Android 8.0+, API level 26+)

### Build & Run
1. Open the project in Android Studio and let Gradle sync complete.
2. Run the `:app` configuration on an emulator or physical device.
3. Grant **Microphone Permission** when prompted upon launching.
4. Click **Start session** to begin voice recognition.

---

## Unit Testing

Run unit tests via Gradle:
```bash
./gradlew :app:testDebugUnitTest
```

All core dialog state transitions, keyword disambiguation, longest-match algorithms, and admin conflict rules are verified by unit tests.
