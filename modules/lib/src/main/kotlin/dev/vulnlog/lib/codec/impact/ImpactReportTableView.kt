// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.rendering.Widget
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.SectionBuilder
import com.github.ajalt.mordant.table.grid
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import dev.vulnlog.lib.codec.report.sortInDeclaredOrder
import dev.vulnlog.lib.codec.report.sortTagValues
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.core.reporting.severityOf
import dev.vulnlog.lib.core.severityOrder
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Below this, the fact columns no longer fit with their padding. A narrower terminal wraps the lines instead.
private const val MIN_WIDTH = 80

// Wider paragraphs are hard to read, and the fact columns would only spread into empty space.
private const val MAX_WIDTH = 120

private val GENERATED_AT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC)

private const val NO_ENTRIES = "No vulnerability entries to display."

private val labelStyle = TextStyles.dim.style
private val aliasStyle = TextStyles.dim.style

/**
 * The impact report as a table for the terminal: the project, the metadata and the counts, then one block per entry
 * under a heading per state. Styles are always written. The [Terminal] drops them when the layout allows no colours.
 */
internal object ImpactReportTableView {
    fun render(
        report: ImpactReport,
        layout: TextLayout,
    ): String {
        val terminal = createTerminal(layout)
        val width = terminal.size.width
        val entries = report.entries.sortedWith(impactEntryOrder).map(::describeEntry)
        val heading = terminal.render(buildHeading(report))
        val overview = terminal.render(buildOverview(report, width))
        val table = if (entries.isEmpty()) NO_ENTRIES else terminal.render(buildTable(entries, width))
        val text = listOf(heading, overview, table).joinToString("\n\n")
        return finishLines(text)
    }
}

/** A recorder instead of the standard interface, so rendering reads no terminal and no environment. */
private fun createTerminal(layout: TextLayout): Terminal {
    val ansiLevel = if (layout.colors) AnsiLevel.ANSI16 else AnsiLevel.NONE
    val width = layout.width.coerceIn(MIN_WIDTH, MAX_WIDTH)
    return Terminal(
        ansiLevel = ansiLevel,
        width = width,
        terminalInterface = TerminalRecorder(ansiLevel = ansiLevel, width = width),
    )
}

private fun buildHeading(report: ImpactReport): Widget =
    grid {
        row { cell(TextStyles.bold("Vulnerability Impact Report: ${sanitize(report.project.name)}")) }
        row { cell(sanitize("${report.project.organization}, ${report.project.author}")) }
    }

private val OVERVIEW_LABELS =
    listOf("Generated", "Inputs", "Filter", "Vulnlog", "Total", "State", "Open severity", "Accepted severity")

/**
 * The metadata, a blank line, then the counts. They share one grid, so all values start in the same column. The label
 * column fits the longest label any report can have. So the values of every report line up, the filter knows its width
 * before the rows exist, and a narrow terminal doesn't cut the labels.
 */
private fun buildOverview(
    report: ImpactReport,
    width: Int,
): Widget {
    val labelWidth = OVERVIEW_LABELS.maxOf { it.length } + 1
    // The grid puts one blank between the label and the value.
    val valueWidth = width - labelWidth - 1
    val metadata =
        listOfNotNull(
            "Generated" to GENERATED_AT_FORMAT.format(report.generatedAt),
            report.inputs.takeIf { it.isNotEmpty() }?.let { "Inputs" to sanitize(it.joinToString(", ")) },
            "Filter" to describeFilter(report, valueWidth),
            "Vulnlog" to report.vulnlogVersion,
        )
    val counts =
        listOfNotNull(
            "Total" to describeTotal(report.entries.size),
            report.entries.takeIf { it.isNotEmpty() }?.let { "State" to describeStates(it) },
            describeSeverities(report.entries, WorkState.OPEN)?.let { "Open severity" to it },
            describeSeverities(report.entries, WorkState.ACCEPTED)?.let { "Accepted severity" to it },
        )
    return grid {
        whitespace = Whitespace.PRE_WRAP
        column(0) { this.width = ColumnWidth.Fixed(labelWidth) }
        (metadata + ("" to "") + counts).forEach { (label, value) -> row(labelStyle(label), value) }
    }
}

/**
 * The active filter dimensions, each as a name and its values, as the JSON view lists them. The filter breaks only
 * between dimensions, so a name never ends one line while its values start the next.
 */
private fun describeFilter(
    report: ImpactReport,
    width: Int,
): String {
    val filter = report.filter
    val dimensions =
        listOfNotNull(
            report.asOf?.let { "as of ${it.value}" },
            describeFilterValues("tags", sortTagValues(filter)),
            filter.reporter?.let { "reporter ${it.canonical()}" },
            describeFilterValues("state", sortInDeclaredOrder(filter.states).map { it.canonical() }),
            describeFilterValues("verdict", sortInDeclaredOrder(filter.verdicts).map { it.canonical() }),
            describeFilterValues("disposition", sortInDeclaredOrder(filter.dispositions).map { canonical(it) }),
        ).map(::sanitize)
    return if (dimensions.isEmpty()) "none" else fillLines(dimensions, separator = "; ", width = width)
}

private fun describeFilterValues(
    name: String,
    values: List<String>,
): String? = values.takeIf { it.isNotEmpty() }?.let { "$name ${it.joinToString(", ")}" }

private fun describeTotal(count: Int): String = if (count == 1) "1 entry" else "$count entries"

private fun describeStates(entries: List<ImpactEntry>): String {
    val counts = entries.groupingBy { it.state }.eachCount()
    return reportStates
        .mapNotNull { state -> counts[state]?.let { "$it ${state.canonical()}" } }
        .joinToString(", ")
}

private fun describeSeverities(
    entries: List<ImpactEntry>,
    state: WorkState,
): String? {
    val counts =
        entries
            .filter { it.state == state }
            .mapNotNull { severityOf(it.impact) }
            .groupingBy { it }
            .eachCount()
    return Severity.entries
        .sortedBy(::severityOrder)
        .mapNotNull { severity -> counts[severity]?.let { severityStyle(severity)("$it ${canonical(severity)}") } }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
}

/**
 * A heading per state, then one block per entry. A block shows the IDs on the left. Beside them are the short facts
 * and, under the facts, the description and the analysis. Those are the long parts, so they get the width of all the
 * other columns.
 */
private fun buildTable(
    entries: List<EntryText>,
    tableWidth: Int,
): Widget {
    val widths = measureFactColumns(entries, tableWidth)
    return table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        borderStyle = TextStyles.dim.style
        whitespace = Whitespace.PRE_WRAP
        overflowWrap = OverflowWrap.BREAK_WORD
        widths.toList().forEachIndexed { index, width -> column(index) { this.width = ColumnWidth.Fixed(width) } }
        header {
            style = TextStyles.bold.style
            whitespace = Whitespace.PRE
            row(*FactColumn.entries.map { it.header }.toTypedArray())
        }
        body {
            entries.groupBy { it.state }.forEach { (state, group) ->
                appendStateHeading(state, group.size)
                group.forEach { entry -> appendEntry(entry, widths) }
            }
        }
    }
}

private fun SectionBuilder.appendStateHeading(
    state: WorkState,
    count: Int,
) {
    row {
        cell(stateStyle(state)("${state.canonical().uppercase()} ($count)")) { columnSpan = FactColumn.entries.size }
    }
}

/** The fact cells are wrapped here and rendered as they are. See [wrapWords]. */
private fun SectionBuilder.appendEntry(
    entry: EntryText,
    widths: FactWidths,
) {
    val paragraphs = describeParagraphs(entry)
    row {
        whitespace = Whitespace.PRE
        cell(describeIds(entry, widths.id)) { if (paragraphs.isNotEmpty()) rowSpan = 2 }
        cells(
            describeImpact(entry, widths.impact),
            entry.disposition,
            wrapWords(entry.releases, widths.releases),
            wrapWords(entry.fixedIn, widths.fixedIn),
        )
    }
    if (paragraphs.isNotEmpty()) {
        row { cell(paragraphs.joinToString("\n\n")) { columnSpan = FactColumn.entries.size - 1 } }
    }
}

/** The primary ID in bold, then every alias dimmed, each starting on its own line. */
private fun describeIds(
    entry: EntryText,
    width: Int,
): String =
    entry.ids
        .mapIndexed { index, id ->
            val style = if (index == 0) TextStyles.bold.style else aliasStyle
            breakWord(id, width).joinToString("\n") { style(it) }
        }.joinToString("\n")

/** The impact label, coloured by severity, and under it the justification of an entry that is not affected. */
private fun describeImpact(
    entry: EntryText,
    width: Int,
): String {
    val label = entry.severity?.let { severityStyle(it)(entry.impact) } ?: entry.impact
    return listOfNotNull(label, entry.justification?.let { wrapWords(it, width) }).joinToString("\n")
}

/** What the vulnerability is and what was found, each as its own paragraph. */
private fun describeParagraphs(entry: EntryText): List<String> =
    listOfNotNull(entry.description, entry.analysis?.let { "${labelStyle("Analysis:")} $it" })

private fun stateStyle(state: WorkState): TextStyle =
    when (state) {
        WorkState.OPEN -> TextColors.red + TextStyles.bold
        WorkState.UNDER_INVESTIGATION -> TextColors.yellow + TextStyles.bold
        WorkState.ACCEPTED -> TextColors.magenta + TextStyles.bold
        WorkState.RESOLVED -> TextColors.green + TextStyles.bold
        WorkState.NOT_APPLICABLE -> TextStyles.bold.style
    }

private fun severityStyle(severity: Severity): TextStyle =
    when (severity) {
        Severity.CRITICAL -> TextColors.brightRed + TextStyles.bold
        Severity.HIGH -> TextColors.red
        Severity.MEDIUM -> TextColors.yellow
        Severity.LOW -> TextColors.blue
    }
