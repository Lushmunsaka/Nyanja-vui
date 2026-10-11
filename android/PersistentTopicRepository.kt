package com.nynaja.vui.android

import android.content.Context
import com.nynaja.vui.InMemoryTopicRepository
import com.nynaja.vui.KeywordResult
import com.nynaja.vui.Language
import com.nynaja.vui.Topic
import com.nynaja.vui.TopicRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private fun Topic.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)

        val titlesObj = JSONObject()
        titles.forEach { (lang, title) -> titlesObj.put(lang.name, title) }
        put("titles", titlesObj)

        val contentObj = JSONObject()
        content.forEach { (lang, text) -> contentObj.put(lang.name, text) }
        put("content", contentObj)

        val keywordsObj = JSONObject()
        keywords.forEach { (lang, words) ->
            val arr = JSONArray()
            words.forEach { arr.put(it) }
            keywordsObj.put(lang.name, arr)
        }
        put("keywords", keywordsObj)
    }
}

private fun topicFromJson(json: JSONObject): Topic {
    val id = json.getString("id")

    val titlesObj = json.getJSONObject("titles")
    val titles = mutableMapOf<Language, String>()
    for (key in titlesObj.keys()) {
        titles[Language.valueOf(key)] = titlesObj.getString(key)
    }

    val contentObj = json.optJSONObject("content") ?: JSONObject()
    val content = mutableMapOf<Language, String>()
    for (key in contentObj.keys()) {
        content[Language.valueOf(key)] = contentObj.getString(key)
    }

    val keywordsObj = json.optJSONObject("keywords") ?: JSONObject()
    val keywords = mutableMapOf<Language, Set<String>>()
    for (key in keywordsObj.keys()) {
        val arr = keywordsObj.getJSONArray(key)
        val set = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            set.add(arr.getString(i))
        }
        keywords[Language.valueOf(key)] = set
    }

    return Topic(id, titles, content, keywords)
}

class PersistentTopicRepository(
    private val context: Context,
    initialTopics: List<Topic> = emptyList(),
    reservedWords: (Language) -> Set<String> = { emptySet() },
) : TopicRepository {
    private val file = File(context.filesDir, "topics_v1.json")

    private val delegate: InMemoryTopicRepository by lazy {
        val topics = loadTopics() ?: initialTopics
        InMemoryTopicRepository(topics, reservedWords)
    }

    private fun loadTopics(): List<Topic>? {
        if (!file.exists()) return null
        try {
            val jsonStr = file.readText()
            val jsonArr = JSONArray(jsonStr)
            val list = mutableListOf<Topic>()
            for (i in 0 until jsonArr.length()) {
                list.add(topicFromJson(jsonArr.getJSONObject(i)))
            }
            return list
        } catch (e: Exception) {
            return null
        }
    }

    private fun saveTopics() {
        try {
            val jsonArr = JSONArray()
            delegate.all().forEach { topic ->
                jsonArr.put(topic.toJson())
            }
            file.writeText(jsonArr.toString())
        } catch (_: Exception) {}
    }

    override fun all(): List<Topic> = delegate.all()

    override fun get(id: String): Topic? = delegate.get(id)

    override fun addKeyword(topicId: String, language: Language, keyword: String): KeywordResult {
        val result = delegate.addKeyword(topicId, language, keyword)
        if (result is KeywordResult.Added) {
            saveTopics()
        }
        return result
    }

    override fun removeKeyword(topicId: String, language: Language, keyword: String): Boolean {
        val removed = delegate.removeKeyword(topicId, language, keyword)
        if (removed) {
            saveTopics()
        }
        return removed
    }
}
