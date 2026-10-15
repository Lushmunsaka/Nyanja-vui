package com.nynaja.vui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nynaja.vui.android.AndroidAudioCapture
import com.nynaja.vui.android.AndroidSpeechRecognizer
import com.nynaja.vui.android.AndroidTtsEngine
import com.nynaja.vui.android.GoogleCloudTtsEngine
import com.nynaja.vui.android.HybridTtsEngine
import com.nynaja.vui.android.KeywordAdminScreen
import com.nynaja.vui.android.PersistentTopicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var androidTts: AndroidTtsEngine
    private lateinit var tts: TtsEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidTts = AndroidTtsEngine(this)
        tts = HybridTtsEngine(androidTts, GoogleCloudTtsEngine(this, BuildConfig.TTS_API_KEY))
        val vocabulary = CommandVocabulary.default()
        val repository = PersistentTopicRepository(
            applicationContext,
            SampleData.topics(),
            reservedWords = vocabulary::reserved,
        )

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    RequestAudioPermission {
                        NynajaApp(repository, tts)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        androidTts.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun RequestAudioPermission(onGranted: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    if (hasPermission) {
        onGranted()
    } else {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Microphone permission is required for voice navigation and recognition.", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Grant Microphone Permission")
            }
        }
    }
}

@Composable
private fun NynajaApp(repository: TopicRepository, tts: TtsEngine) {
    var showAdmin by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Nynaja voice menu", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAdmin = false }, enabled = showAdmin) { Text("Dialog") }
            Button(onClick = { showAdmin = true }, enabled = !showAdmin) { Text("Keywords") }
        }
        if (showAdmin) {
            KeywordAdminScreen(repository, Modifier.weight(1f).fillMaxWidth())
        } else {
            DialogTestScreen(
                repository = repository,
                tts = tts,
                onNavigateToKeywords = { showAdmin = true },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DialogTestScreen(
    repository: TopicRepository,
    tts: TtsEngine,
    onNavigateToKeywords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speechRecognizer = remember { AndroidSpeechRecognizer(context) }
    val textInputChannel = remember { Channel<String>(Channel.UNLIMITED) }
    val captions = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    var input by rememberSaveable { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    val cloudAsr = remember {
        CaptureAndTranscribeAsr(
            capture = AndroidAudioCapture(),
            api = GoogleV2SpeechApi(
                apiKey = BuildConfig.SPEECH_API_KEY,
                projectId = BuildConfig.SPEECH_PROJECT_ID,
            ),
        )
    }

    val asr = remember {
        object : AsrEngine {
            override suspend fun listen(language: Language): RecognitionResult {
                if (BuildConfig.SPEECH_API_KEY.isNotBlank() && BuildConfig.SPEECH_API_KEY != "YOUR_API_KEY_HERE") {
                    val typed = textInputChannel.tryReceive().getOrNull()
                    if (!typed.isNullOrBlank()) return RecognitionResult.Success(typed.trim())

                    val cloudResult = cloudAsr.listen(language)
                    if (cloudResult is RecognitionResult.Success) {
                        return cloudResult
                    }
                    val typedAfter = textInputChannel.tryReceive().getOrNull()
                    if (!typedAfter.isNullOrBlank()) return RecognitionResult.Success(typedAfter.trim())

                    return cloudResult
                }
                val speechResult = speechRecognizer.listen(language)
                if (speechResult is RecognitionResult.Success) {
                    return speechResult
                }
                // Fallback / wait for text input
                val text = textInputChannel.receive()
                return if (text.isBlank()) RecognitionResult.NoSpeech else RecognitionResult.Success(text)
            }
        }
    }

    LaunchedEffect(captions.size) {
        if (captions.isNotEmpty()) listState.animateScrollToItem(captions.lastIndex)
    }

    Column(
        modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !running,
                onClick = {
                    captions.clear()
                    running = true
                    scope.launch {
                        try {
                            DialogManager(
                                repo = repository,
                                asr = asr,
                                tts = tts,
                                onText = captions::add,
                                onNavigateToKeywords = onNavigateToKeywords,
                            ).run()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            running = false
                        }
                    }
                },
            ) { Text(if (running) "Session running (Voice active)" else "Start session") }
            TextButton(onClick = { captions.clear() }) { Text("Clear captions") }
        }
        Text(
            if (running) "Speak into microphone or type a command/keyword (e.g. 'keywords' to switch screen)." else "Start a session to begin voice recognition.",
            style = MaterialTheme.typography.bodyMedium,
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(captions) { _, caption ->
                Text(caption, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                enabled = running,
                label = { Text("Type input / fallback") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = running && input.isNotBlank(),
                onClick = {
                    textInputChannel.trySend(input.trim())
                    input = ""
                    keyboard?.hide()
                },
            ) { Text("Send") }
        }
    }
}
