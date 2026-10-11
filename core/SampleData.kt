package com.nynaja.vui

/** Placeholder topics so the app runs end to end. Replace with the verified content pack (~20 topics). */
object SampleData {
    private const val PLACEHOLDER = "[Verified English text goes here]"

    fun topics(): List<Topic> = listOf(
        sample("antenatal-visits", "Antenatal care visits", setOf("antenatal", "clinic visit", "check up")),
        sample("danger-signs", "Danger signs in pregnancy", setOf("danger", "warning signs")),
        sample("nutrition", "Eating well in pregnancy", setOf("food", "nutrition", "eating")),
    )

    private fun sample(id: String, title: String, keywords: Set<String>) = Topic(
        id = id,
        titles = mapOf(Language.ENGLISH to title),
        content = mapOf(Language.ENGLISH to PLACEHOLDER),
        keywords = mapOf(Language.ENGLISH to keywords),
    )
}
