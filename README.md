# Nynaja VUI — Voice User Interface & Health Menu

**Nynaja VUI** is a menu-driven Android Voice User Interface (VUI) designed for accessible, spoken health information delivery in English and Nyanja. The app features voice-driven screen navigation, on-device speech recognition, text-to-speech synthesis, and persistent local topic management.

---

## Key Features

- **On-Device Speech Recognition (ASR)**: Live speech recognition via `AndroidSpeechRecognizer` using Android's native SpeechRecognizer API, backed by an interactive keyboard input fallback.
- **Voice-Driven Navigation**: Switch screens and menus using voice commands (e.g., say `"keywords"` or `"admin"` to access topic administration, or `"dialog"` / `"menu"` to return to the voice dialog).
- **Text-to-Speech (TTS)**: Spoken audio feedback using Android's built-in `AndroidTtsEngine`, synchronized with real-time on-screen text captions.
- **Keyword & Command Matching**: Intelligent topic lookup and voice command handling (`stop`, `repeat`, `list`, `next`, `keywords`, `dialog`) powered by `KeywordMatcher` and `CommandVocabulary`.
- **Persistent Local Storage**: `PersistentTopicRepository` automatically saves modified topic keywords across app restarts using local JSON storage (`topics_v1.json`), running 100% offline on-device.
- **Keyword Administration**: An admin screen (`KeywordAdminScreen`) to view, add, or remove topic keywords per language with automatic conflict and reserved-word checks.
- **Clean Architecture**: Pure Kotlin core logic decoupled from Android UI/SDK dependencies, making it simple to test and extend.

---

## Project Structure

```text
app/       Android application module (MainActivity, Compose UI, Jetpack Compose setup)
core/      Pure Kotlin domain logic (DialogManager, KeywordMatcher, CommandVocabulary, TopicRepository)
android/   Android platform adapters (AndroidSpeechRecognizer, AndroidTtsEngine, PersistentTopicRepository, KeywordAdminScreen)
test/      Unit tests for dialog state machine, navigation, and keyword matching (JUnit + Kotlinx Coroutines Test)
```

---

## Future Extensibility (Google Cloud Speech-to-Text V2)

The app is architected using a Ports and Adapters (Hexagonal) design. To integrate Google Cloud Speech-to-Text V2 for Nyanja when billing is ready:
1. Implement `AudioCapture` to record raw PCM16 microphone audio.
2. Implement `GoogleV2SpeechApi` in `core/SpeechApi.kt` to transcribe audio using Google's V2 API or your backend proxy.
3. Pass `CaptureAndTranscribeAsr(audioCapture, GoogleV2SpeechApi())` into `DialogManager`.

---

## Running the App

### Requirements
- Android Studio Ladybug / Koala / AGP 8.8+
- JDK 17
- Android Device or Emulator (Android 8.0+, API level 26+)

### Build & Run
1. Open the project in Android Studio and let Gradle sync.
2. Run the `:app` configuration on an emulator or physical device.
3. Grant **Microphone Permission** when prompted.
4. Click **Start session** to begin voice recognition.

---

## Unit Testing

Run unit tests via Gradle:
```bash
./gradlew :app:testDebugUnitTest
```

All core dialog state transitions, keyword disambiguation, longest-match algorithms, and admin conflict rules are verified by unit tests.
