// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

private const val ESC = "\u001B"
private const val BEL = "\u0007"

// Mordant writes styles as SGR codes (Select Graphic Rendition), for example ESC [1m for bold.
private const val STYLE_CODE = "$ESC\\[[0-9;]*m"

// A CSI sequence (Control Sequence Introducer) starts with ESC [. It styles text, moves the cursor or clears the
// screen.
private const val CSI = "\\[[0-?]*[ -/]*[@-~]"

// An OSC sequence (Operating System Command) starts with ESC ] and ends with BEL or ESC \. It sets the window title or
// turns text into a link.
private const val OSC = "\\][^$BEL$ESC]*(?:$BEL|$ESC\\\\)?"

private val styleCode = Regex(STYLE_CODE)
private val escapeSequence = Regex("$ESC(?:$CSI|$OSC|.?)")
private val controlCharacter = Regex("[\u0000-\u0008\u000B-\u001F\u007F-\u009F]")
private val trailingPadding = Regex(" +((?:$STYLE_CODE)*)$")

/**
 * Text from a Vulnlog file, made safe for a terminal. With escape sequences, a file could style, hide or link text in
 * the report. Other control characters would move the cursor and break the table. Escape sequences are removed. Any
 * other control character except a line break or a tab shows as U+FFFD.
 */
internal fun sanitize(text: String): String =
    text.replace("\r\n", "\n").replace(escapeSequence, "").replace(controlCharacter, "\uFFFD")

/** The [text] made safe by [sanitize] and trimmed, or null when nothing is left to show. */
internal fun cleanText(text: String?): String? = text?.let(::sanitize)?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Rendered Mordant output, ready for a terminal or a file. Mordant can give a spanning cell one line more than its
 * text needs, and no row of the table ends blank, so those lines go. Mordant also pads every line to the width of its
 * widget. That padding only adds noise to files and diffs, so it goes too, also in front of closing style codes.
 */
internal fun finishLines(text: String): String {
    val lines = text.lines().map { it.replace(trailingPadding, "$1") }
    return lines
        .filterIndexed { index, _ -> !isExtraBlankLine(lines, index) }
        .joinToString("\n", postfix = "\n")
}

/** A table line whose cells are all empty, right above the border that closes its row. */
private fun isExtraBlankLine(
    lines: List<String>,
    index: Int,
): Boolean {
    val line = lines[index].replace(styleCode, "")
    val next = lines.getOrNull(index + 1)?.replace(styleCode, "").orEmpty()
    val isBlankCellLine = line.length > 2 && line.first() == '│' && line.all { it == ' ' || it == '│' }
    return isBlankCellLine && (next.startsWith("├") || next.startsWith("└"))
}
