// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.codec.report.labelImpact
import dev.vulnlog.lib.codec.report.labelJustification
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState

/**
 * The texts the table shows for one entry, made safe for a terminal and named as the other reports name them.
 * They are not yet wrapped or styled, so the table can measure its columns from them.
 */
internal data class EntryText(
    val state: WorkState,
    /** The primary ID first, then the aliases. */
    val ids: List<String>,
    val impact: String,
    /** Only an entry that is affected has a severity. */
    val severity: Severity?,
    /** Only an entry that is not affected has a justification. */
    val justification: String?,
    val disposition: String,
    val releases: String,
    val fixedIn: String,
    val description: String?,
    val analysis: String?,
)

internal fun describeEntry(entry: ImpactEntry): EntryText =
    EntryText(
        state = entry.state,
        ids = (listOf(entry.primaryId) + entry.aliases).map { sanitize(it.id) },
        impact = labelImpact(entry.impact),
        severity = (entry.impact as? Impact.Affected)?.severity,
        justification = labelJustification(entry.impact),
        disposition = entry.disposition?.let(::canonical).orEmpty(),
        releases = listReleases(entry.reportFor),
        fixedIn = listReleases(entry.fixedIn),
        description = cleanText(entry.shortDescription),
        analysis = cleanText(entry.analysis),
    )

private fun listReleases(releases: Set<Release>): String = sanitize(releases.joinToString(", ") { it.value })
