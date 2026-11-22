// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

/**
 * The words of [text] in lines of at most [width] characters. A longer word is broken as [breakWord] does. The table
 * wraps its fact cells with this, because Mordant repeats a fragment when it breaks a long word itself.
 */
internal fun wrapWords(
    text: String,
    width: Int,
): String =
    fillLines(
        pieces = text.split(" ").filter { it.isNotEmpty() }.flatMap { breakWord(it, width) },
        separator = " ",
        width = width,
    )

/**
 * The [pieces] joined by [separator] in lines of at most [width] characters, never breaking a piece. A line that
 * continues on the next ends with the separator without its blanks, so "; " leaves a ";" at the end of the line.
 */
internal fun fillLines(
    pieces: List<String>,
    separator: String,
    width: Int,
): String {
    val lineEnd = separator.trimEnd()
    return pieces
        .fold(listOf<String>()) { lines, piece ->
            val last = lines.lastOrNull()
            when {
                last == null -> listOf(piece)
                last.length + separator.length + piece.length + lineEnd.length <= width ->
                    lines.dropLast(1) + (last + separator + piece)
                else ->
                    buildList {
                        addAll(lines.dropLast(1))
                        add(last + lineEnd)
                        add(piece)
                    }
            }
        }.joinToString("\n")
}

/**
 * The [word] in pieces of at most [width] characters, cut after the last hyphen that fits, or at [width] without one.
 */
internal fun breakWord(
    word: String,
    width: Int,
): List<String> {
    if (word.length <= width) return listOf(word)
    val cut = word.lastIndexOf('-', startIndex = width - 1).takeIf { it > 0 }?.plus(1) ?: width
    return listOf(word.take(cut)) + breakWord(word.drop(cut), width)
}
