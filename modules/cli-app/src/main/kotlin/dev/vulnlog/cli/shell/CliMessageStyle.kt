// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import dev.vulnlog.lib.core.StatusVerb

private val errorStyle = TextColors.red + TextStyles.bold
private val warningStyle = TextColors.yellow
private val hintStyle = TextColors.cyan
private val verbStyle = TextColors.green
private val countStyle = TextStyles.bold

private val statusVerbs = StatusVerb.entries.map(StatusVerb::display).toSet()
private val summaryLine = Regex("""\d+ (errors?|warnings?|infos?)(, \d+ (errors?|warnings?|infos?))*""")
private val count = Regex("""\d+""")

/**
 * Applies the semantic styles of the output message concept to a rendered message: severity and
 * hint prefixes, status verbs, and summary counts. Everything else stays unstyled. Mordant strips
 * the styles when the stream is no TTY or NO_COLOR is set, so stripped output reads identically.
 */
fun styleMessage(message: String): String = message.lines().joinToString("\n", transform = ::styleLine)

/** Styles [message] and echoes it to stderr. */
fun CliktCommand.echoMessage(message: String) = echo(styleMessage(message), err = true)

private fun styleLine(line: String): String {
    val verb = line.substringBefore(": ", missingDelimiterValue = "")
    return when {
        line.startsWith("error: ") -> errorStyle("error:") + line.removePrefix("error:")
        line.startsWith("warning: ") -> warningStyle("warning:") + line.removePrefix("warning:")
        line.startsWith("info: ") -> line
        line.startsWith("  hint: ") -> "  " + hintStyle("hint:") + line.removePrefix("  hint:")
        verb in statusVerbs -> verbStyle(verb) + line.removePrefix(verb)
        summaryLine.matches(line) -> line.replace(count) { countStyle(it.value) }
        else -> line
    }
}
