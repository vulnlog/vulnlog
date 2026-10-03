// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.reporting

import dev.vulnlog.lib.core.findDisposition
import dev.vulnlog.lib.core.findWorkState
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState

/** The project every file declares, or null when they differ: a report merges files of one project only. */
fun sharedProject(files: Collection<VulnlogFile>): Project? = files.map { it.project }.distinct().singleOrNull()

fun collectImpactEntries(vulnlogFile: VulnlogFile): Set<ImpactEntry> =
    vulnlogFile.vulnerabilities
        .asSequence()
        .map { vuln ->
            ImpactEntry(
                state = findWorkState(vuln),
                primaryId = vuln.id,
                ids = vuln.aliases.toSet(),
                shortDescription = vuln.description,
                impact = defineImpact(vuln),
                disposition = findDisposition(vuln.verdict),
                analysis = vuln.analysis,
                reportFor = vuln.releases.toSet(),
                fixedIn = setOfNotNull(vuln.resolution?.release),
            )
        }.toSet()

/**
 * The same vulnerability from several files becomes one row. Entries that disagree on state, impact, disposition or
 * analysis stay separate rows, so the report does not hide the disagreement.
 */
fun mergeImpactEntries(entries: List<ImpactEntry>): List<ImpactEntry> =
    entries
        .groupBy { it.primaryId }
        .flatMap { (_, group) ->
            group
                .groupBy { MergeKey(it.state, it.impact, it.disposition, it.analysis) }
                .map { (_, mergeable) -> mergeable.reduce(::mergeTwo) }
        }

private data class MergeKey(
    val state: WorkState,
    val impact: Impact,
    val disposition: Disposition?,
    val analysis: String?,
)

private fun mergeTwo(
    a: ImpactEntry,
    b: ImpactEntry,
): ImpactEntry =
    a.copy(
        ids = a.ids + b.ids,
        shortDescription = a.shortDescription ?: b.shortDescription,
        reportFor = a.reportFor + b.reportFor,
        fixedIn = a.fixedIn + b.fixedIn,
    )

internal fun severityOf(impact: Impact): Severity? =
    when (impact) {
        is Impact.Affected -> impact.severity
        is Impact.NotAffected, Impact.Unknown -> null
    }

internal fun defineImpact(vulnEntry: VulnerabilityEntry): Impact =
    when (val verdict = vulnEntry.verdict) {
        is Verdict.Affected -> Impact.Affected(verdict.severity)
        is Verdict.NotAffected -> Impact.NotAffected(verdict.justification.value)
        Verdict.UnderInvestigation -> Impact.Unknown
    }
