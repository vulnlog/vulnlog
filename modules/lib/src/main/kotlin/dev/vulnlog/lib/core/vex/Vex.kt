// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex

import dev.vulnlog.lib.core.findDisposition
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.ReleaseStatus
import dev.vulnlog.lib.model.vex.VexStatus
import java.time.LocalDate

/**
 * Resolves the [VexStatus] of [vulnEntry] for every release of [vulnlogFile] it applies to, in declaration order.
 *
 * An entry is present from the earliest release it lists through every later release, until the release named by
 * its resolution; from that release on it is fixed, whatever the verdict says.
 *
 * Each status carries the day it became true: the first report for an open investigation, the analysis date for a
 * verdict, the resolution date for a fix. An entry that records none of those falls back to the day the release was
 * published, so a statement is dated from the file rather than from the clock.
 */
fun releaseStatuses(
    vulnEntry: VulnerabilityEntry,
    vulnlogFile: VulnlogFile,
): List<ReleaseStatus> {
    val declared = vulnlogFile.releases
    val order = declared.map(ReleaseEntry::id)
    val first =
        vulnEntry.releases
            .map(order::indexOf)
            .filter { index -> index >= 0 }
            .minOrNull() ?: return emptyList()
    val fix = vulnEntry.resolution?.let { resolution -> order.indexOf(resolution.release) }?.takeIf { it >= 0 }
    val fixedOn = fix?.let { vulnEntry.resolution?.at ?: declared[it].publicationDate }
    val unresolved = unresolvedStatus(vulnEntry)
    val unresolvedOn = unresolvedOn(vulnEntry)
    return declared.mapIndexedNotNull { index, entry ->
        when {
            fix != null && index >= fix -> ReleaseStatus(entry.id, VexStatus.Fixed, fixedOn ?: entry.publicationDate)
            index >= first -> ReleaseStatus(entry.id, unresolved, unresolvedOn ?: entry.publicationDate)
            else -> null
        }
    }
}

/**
 * Derives the action a consumer of an affected product should take.
 *
 * The text follows from the disposition and the fix release, never from the analysis.
 * The resolution note is appended to update actions only, so a note can never soften an accepted risk.
 */
fun vexActionStatement(vulnEntry: VulnerabilityEntry): String {
    val fixRelease = vulnEntry.resolution?.release
    return when (findDisposition(vulnEntry.verdict)) {
        Disposition.WONT_FIX ->
            if (fixRelease == null) {
                "The risk is accepted. No fix is planned."
            } else {
                "The risk is accepted for this release. A fix ships with release ${fixRelease.value}."
            }

        Disposition.WILL_FIX ->
            if (fixRelease == null) {
                "A fix is planned but not yet available."
            } else {
                updateAction(vulnEntry)
            }

        null ->
            if (fixRelease == null) {
                "No remediation is available yet."
            } else {
                updateAction(vulnEntry)
            }
    }
}

/** The status a release carries before the fix, with the entry's analysis routed to the field that status owns. */
private fun unresolvedStatus(vulnEntry: VulnerabilityEntry): VexStatus {
    val analysis = vulnEntry.analysis?.takeIf(String::isNotBlank)
    return when (val verdict = vulnEntry.verdict) {
        Verdict.UnderInvestigation -> VexStatus.UnderInvestigation(analysis)
        is Verdict.NotAffected -> VexStatus.NotAffected(verdict.justification, analysis)
        is Verdict.Affected -> VexStatus.Affected(vexActionStatement(vulnEntry), analysis)
    }
}

/** The day a verdict was made, else the day the entry was first reported. An open investigation has no verdict. */
private fun unresolvedOn(vulnEntry: VulnerabilityEntry): LocalDate? {
    val firstReport = vulnEntry.reports.mapNotNull(ReportEntry::at).minOrNull()
    return when (vulnEntry.verdict) {
        Verdict.UnderInvestigation -> firstReport
        is Verdict.NotAffected, is Verdict.Affected -> vulnEntry.analyzedAt ?: firstReport
    }
}

// TODO the resolution.note field is primarily for internal use and describes: "Brief description of how the vulnerability was resolved"
// This is not relevant for consumers of the VEX document. Re-think this approach but leave it for now.
private fun updateAction(vulnEntry: VulnerabilityEntry): String {
    val resolution = vulnEntry.resolution ?: error("update action requires a resolution")
    val update = "Update to release ${resolution.release.value}."
    return resolution.note?.takeIf(String::isNotBlank)?.let { note -> "$update $note" } ?: update
}
