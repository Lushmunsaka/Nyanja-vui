package com.nynaja.vui

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DialogManagerTest {
    private val en = Language.ENGLISH
    private val vocab = CommandVocabulary.default()

    private fun repo() = InMemoryTopicRepository(
        listOf(
            Topic("anc", mapOf(en to "Antenatal visits"), mapOf(en to "ANC TEXT"),
                mapOf(en to setOf("antenatal", "clinic visit"))),
            Topic("danger", mapOf(en to "Danger signs"), mapOf(en to "DANGER TEXT"),
                mapOf(en to setOf("danger", "warning"))),
            Topic("food", mapOf(en to "Eating well"), mapOf(en to "FOOD TEXT"),
                mapOf(en to setOf("food", "eating"))),
        ),
        reservedWords = vocab::reserved,
    )

    private suspend fun spokenFor(asr: AsrEngine): List<String> {
        val tts = RecordingTts()
        DialogManager(repo(), asr, tts, askForLanguage = true).run()
        return tts.spoken.map { it.second }
    }

    private suspend fun saying(vararg phrases: String) = spokenFor(ScriptedAsr.saying(*phrases))

    @Test fun keywordPlaysTopic() = runTest {
        assertTrue("ANC TEXT" in saying("english", "antenatal", "stop"))
    }

    @Test fun keywordInsideLongerSentence() = runTest {
        assertTrue("DANGER TEXT" in saying("english", "tell me about the danger please", "stop"))
    }

    @Test fun repeatPlaysTopicAgain() = runTest {
        assertEquals(2, saying("english", "danger", "repeat", "stop").count { it == "DANGER TEXT" })
    }

    @Test fun listThenNumberPlaysThatTopic() = runTest {
        assertTrue("DANGER TEXT" in saying("english", "list", "2", "stop"))
    }

    @Test fun twoMissesReadTheNumberedList() = runTest {
        assertTrue("1. Antenatal visits" in saying("english", "blah", "blah", "stop"))
    }

    @Test fun repeatedApiFailuresEndTheSessionCleanly() = runTest {
        val asr = ScriptedAsr(
            listOf(
                RecognitionResult.Success("english"),
                RecognitionResult.Failure("x"), RecognitionResult.Failure("x"), RecognitionResult.Failure("x"),
            ),
        )
        val spoken = spokenFor(asr)
        assertTrue(Prompts.default().get(PromptKey.SERVICE_PROBLEM, en) in spoken)
        assertEquals(Prompts.default().get(PromptKey.GOODBYE, en), spoken.last())
    }

    @Test fun longestKeywordWins() {
        val topics = listOf(
            Topic("a", mapOf(en to "A"), emptyMap(), mapOf(en to setOf("pain"))),
            Topic("b", mapOf(en to "B"), emptyMap(), mapOf(en to setOf("back pain"))),
        )
        val result = KeywordMatcher().match("I have back pain", en, topics)
        assertEquals("b", (result as MatchResult.Single).topic.id)
    }

    @Test fun adminRejectsConflictsAndReservedWords() {
        val repo = repo()
        assertTrue(repo.addKeyword("food", en, "danger") is KeywordResult.Conflict)
        assertEquals(KeywordResult.Reserved, repo.addKeyword("food", en, "stop"))
        assertEquals(KeywordResult.Duplicate, repo.addKeyword("food", en, "Food"))
        assertTrue(repo.addKeyword("food", en, "Nutrition!") is KeywordResult.Added)
        assertTrue("nutrition" in repo.get("food")!!.keywords[en]!!)
    }
}
