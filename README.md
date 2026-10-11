# nynaja-vui — menu-driven voice dialog (Kotlin)

core/     pure Kotlin (no Android dependencies): topics, keyword matching, dialog state machine, ASR/TTS interfaces + fakes
android/  AndroidTtsEngine (built-in TTS) and a Compose KeywordAdminScreen
test/     JUnit tests (kotlin.test + kotlinx-coroutines-test)

Wire-up (before the Google API exists):
    val vocab = CommandVocabulary.default()
    val repo  = InMemoryTopicRepository(SampleData.topics(), reservedWords = vocab::reserved)
    val tts   = AndroidTtsEngine(context)
    val asr   = CallbackAsr { language -> /* return text typed into a debug field, or null */ }
    scope.launch { DialogManager(repo, asr, tts, onText = { caption = it }).run() }

Android Studio:
    Open this folder as a project, let Gradle sync, then run the `app` configuration on an
    emulator or Android device. In the Dialog screen, start a session and type `English` (or
    `Nyanja`) to choose a language, then enter topic keywords or commands such as `list`, `repeat`,
    and `stop`. The app uses Android's built-in TTS; captions remain visible if a voice is missing.
    The sample topic text is placeholder content. Keyword edits are in-memory and reset on restart.

When billing is ready: implement GoogleV2SpeechApi (core/SpeechApi.kt), supply an AudioCapture
from your existing recording code, and replace CallbackAsr with CaptureAndTranscribeAsr(capture, api).

Not yet verified: this code has not been compiled or run. Nyanja prompts, command words and
number words are marked TODO(verify) and must come from a fluent speaker.
