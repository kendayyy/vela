package com.vela.notes.data.local.search

object FtsQueryBuilder {
    private val termPattern = Regex("[\\p{L}\\p{N}_]+")

    fun build(rawQuery: String): String? {
        val terms = termPattern.findAll(rawQuery).map(MatchResult::value).toList()

        return terms
            .takeIf(List<String>::isNotEmpty)
            ?.joinToString(separator = " ") { term -> "$term*" }
    }
}
