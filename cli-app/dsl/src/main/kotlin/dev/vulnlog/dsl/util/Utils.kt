package dev.vulnlog.dsl.util

import java.util.Locale

/**
 * Transforms a string into a camel case representation. Punctuation is removed.
 */
public fun String.toCamelCase(): String =
    trim()
        .lowercase()
        .split("(\\s+|\\p{Punct}+)".toRegex()).joinToString("") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        .replace(Regex("\\p{Punct}+"), "")
        .replaceFirstChar { it.lowercase(Locale.getDefault()) }
