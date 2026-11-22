// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.codec.impact.FactColumn.DISPOSITION
import dev.vulnlog.lib.codec.impact.FactColumn.FIXED_IN
import dev.vulnlog.lib.codec.impact.FactColumn.ID
import dev.vulnlog.lib.codec.impact.FactColumn.IMPACT
import dev.vulnlog.lib.codec.impact.FactColumn.RELEASES

/** The columns of the table, in the order they appear. */
internal enum class FactColumn(
    val header: String,
) {
    ID("ID"),
    IMPACT("Impact"),
    DISPOSITION("Disposition"),
    RELEASES("Releases"),
    FIXED_IN("Fixed in"),
}

// One blank on either side of the text of a cell.
private const val CELL_PADDING = 2

// A GHSA ID fits on one line. A longer ID breaks, so the paragraphs beside it keep their width.
private const val MAX_ID_WIDTH = 24

// A longer list of releases wraps, so it doesn't take the room the IDs and the justifications need.
private const val MAX_RELEASES_WIDTH = 24

/** The text width of each fact column. With borders and padding, they fill the table width. */
internal data class FactWidths(
    val id: Int,
    val impact: Int,
    val disposition: Int,
    val releases: Int,
    val fixedIn: Int,
) {
    fun toList(): List<Int> = listOf(id, impact, disposition, releases, fixedIn)
}

/**
 * Sizes the fact columns so that they fill [tableWidth]. Mordant would size them from the paragraph rows instead. That
 * squeezes the short columns and drops Fixed in next to a long ID. The columns get their room in this order:
 *
 * 1. Impact and Disposition get their longest label.
 * 2. ID gets its longest ID, up to [MAX_ID_WIDTH]. Releases and Fixed in keep the room for their headers.
 * 3. Releases gets its longest value, up to [MAX_RELEASES_WIDTH].
 * 4. Fixed in keeps what its longest value needs, up to [MAX_RELEASES_WIDTH]. Impact gets the rest it needs for its
 *    longest justification, so a justification can stand on one line. Fixed in then takes whatever is left.
 */
internal fun measureFactColumns(
    entries: List<EntryText>,
    tableWidth: Int,
): FactWidths {
    val columns = FactColumn.entries.size
    val text = tableWidth - (columns + 1) - columns * CELL_PADDING
    val label = measureLongest(IMPACT, entries.map { it.impact })
    val disposition = measureLongest(DISPOSITION, entries.map { it.disposition })
    val headers = RELEASES.header.length + FIXED_IN.header.length
    val id = minOf(measureLongest(ID, entries.flatMap { it.ids }), MAX_ID_WIDTH, text - label - disposition - headers)
    val releases =
        minOf(
            measureLongest(RELEASES, entries.map { it.releases }),
            MAX_RELEASES_WIDTH,
            text - id - label - disposition - FIXED_IN.header.length,
        )
    val shared = text - id - disposition - releases
    val fixedIn = minOf(measureLongest(FIXED_IN, entries.map { it.fixedIn }), MAX_RELEASES_WIDTH)
    val justification = entries.maxOfOrNull { it.justification?.length ?: 0 } ?: 0
    val impact = maxOf(label, minOf(justification, shared - fixedIn))
    return FactWidths(id, impact, disposition, releases, shared - impact)
}

private fun measureLongest(
    column: FactColumn,
    values: List<String>,
): Int = (values + column.header).maxOf { it.length }
