package com.nynaja.vui.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nynaja.vui.KeywordResult
import com.nynaja.vui.Language
import com.nynaja.vui.TopicRepository

private fun Language.label() = if (this == Language.ENGLISH) "English" else "Nyanja"

/** Admin section: pick a topic and language, then add or remove the words users can say to request it. */
@Composable
fun KeywordAdminScreen(repo: TopicRepository, modifier: Modifier = Modifier) {
    var version by remember { mutableStateOf(0) }          // bumped after each change to refresh the lists
    val topics = remember(version) { repo.all() }
    var selectedId by remember { mutableStateOf(topics.firstOrNull()?.id) }
    var language by remember { mutableStateOf(Language.ENGLISH) }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val selected = topics.firstOrNull { it.id == selectedId }
    val keywords = selected?.keywords?.get(language).orEmpty().sorted()

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Topic keywords", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Language.values().forEach { lang ->
                FilterChip(
                    selected = language == lang,
                    onClick = { language = lang; message = "" },
                    label = { Text(lang.label()) },
                )
            }
        }

        Text("Topics", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(topics, key = { it.id }) { topic ->
                Text(
                    topic.title(Language.ENGLISH),
                    fontWeight = if (topic.id == selectedId) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedId = topic.id; message = "" }
                        .padding(vertical = 10.dp),
                )
            }
        }

        if (selected != null) {
            Text(
                "Keywords for \"${selected.title(Language.ENGLISH)}\" (${language.label()})",
                style = MaterialTheme.typography.titleMedium,
            )
            if (keywords.isEmpty()) Text("No keywords yet.")
            keywords.forEach { kw ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(kw)
                    TextButton(onClick = {
                        repo.removeKeyword(selected.id, language, kw)
                        version++
                    }) { Text("Remove") }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("New keyword") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    message = when (val r = repo.addKeyword(selected.id, language, input)) {
                        is KeywordResult.Added -> { input = ""; version++; "Added \"${r.keyword}\"" }
                        KeywordResult.Duplicate -> "Already a keyword for this topic."
                        is KeywordResult.Conflict -> "Another topic already uses this keyword."
                        KeywordResult.Reserved -> "That word is a voice command, so it can't be a keyword."
                        KeywordResult.Invalid -> "Type a keyword first."
                        KeywordResult.TopicNotFound -> "Topic not found."
                    }
                }) { Text("Add") }
            }
            if (message.isNotEmpty()) Text(message)
        }
    }
}
