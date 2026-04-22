// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.report.Impact
import dev.vulnlog.lib.model.report.ReportingEntry
import dev.vulnlog.lib.model.report.WorkState

/**
 * Validates that all provided Vulnlog files share the same project metadata.
 *
 * @return the shared [Project] if all files match, or null if the projects differ.
 */
fun validateSharedProject(files: Collection<VulnlogFile>): Project? {
    val projects = files.map { it.project }.distinct()
    return if (projects.size == 1) projects.first() else null
}

/**
 * Collects reporting entries based on the vulnerabilities present in the given Vulnlog file
 * and applies the specified filter to refine the results.
 *
 * @param vulnlogFile The Vulnlog file containing vulnerability definitions and related metadata.
 * @param filter An optional filter to apply to the vulnerabilities, specifying criteria such as releases, tags, or reporter type.
 *               Defaults to an empty filter which includes all vulnerabilities.
 * @return A set of reporting entries representing the filtered and processed vulnerabilities from the Vulnlog file.
 */
fun collectReportingEntries(
    vulnlogFile: VulnlogFile,
    filter: VulnlogFilter = VulnlogFilter(),
): Set<ReportingEntry> =
    vulnlogFile.vulnerabilities
        .asSequence()
        .applyFilter(filter)
        .map { vuln ->
            ReportingEntry(
                state = findWorkState(vuln),
                primaryId = vuln.id,
                ids = vuln.aliases.toSet(),
                shortDescription = vuln.description,
                impact = defineImpact(vuln),
                analysis = vuln.analysis,
                reportFor = vuln.releases.toSet(),
                fixedIn = setOfNotNull(vuln.resolution?.release),
            )
        }.toSet()

/**
 * Merges reporting entries from multiple sources by primary vulnerability ID.
 *
 * Entries with the same primary ID, state, impact, and analysis are merged by unioning
 * their alias IDs, releases, and fix releases. Entries with the same primary ID but
 * conflicting verdict or analysis are kept as separate rows.
 */
fun mergeReportingEntries(entries: List<ReportingEntry>): List<ReportingEntry> =
    entries
        .groupBy { it.primaryId }
        .flatMap { (_, group) ->
            group
                .groupBy { MergeKey(it.state, it.impact, it.analysis) }
                .map { (_, mergeable) -> mergeable.reduce(::mergeTwo) }
        }

private data class MergeKey(
    val state: WorkState,
    val impact: Impact,
    val analysis: String?,
)

private fun mergeTwo(
    a: ReportingEntry,
    b: ReportingEntry,
): ReportingEntry =
    a.copy(
        ids = a.ids + b.ids,
        shortDescription = a.shortDescription ?: b.shortDescription,
        reportFor = a.reportFor + b.reportFor,
        fixedIn = a.fixedIn + b.fixedIn,
    )

private fun findWorkState(vulnEntry: VulnerabilityEntry): WorkState =
    when (vulnEntry.verdict) {
        Verdict.UnderInvestigation -> WorkState.UNDER_INVESTIGATION
        is Verdict.Affected -> if (vulnEntry.resolution != null) WorkState.RESOLVED else WorkState.OPEN
        is Verdict.NotAffected -> if (vulnEntry.resolution != null) WorkState.RESOLVED else WorkState.DISMISSED
        is Verdict.RiskAcceptable -> if (vulnEntry.resolution != null) WorkState.RESOLVED else WorkState.DISMISSED
    }

private fun defineImpact(vulnEntry: VulnerabilityEntry): Impact =
    when (vulnEntry.verdict) {
        is Verdict.Affected -> Impact.Affected(vulnEntry.verdict.severity)
        is Verdict.NotAffected -> Impact.NotAffected(vulnEntry.verdict.justification.value)
        is Verdict.RiskAcceptable -> Impact.AcceptableRisk(vulnEntry.verdict.severity)
        Verdict.UnderInvestigation -> Impact.Unknown
    }
