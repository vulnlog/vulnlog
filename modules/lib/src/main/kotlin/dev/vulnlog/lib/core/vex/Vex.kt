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
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexStatusKind
import java.time.LocalDate

/**
 * An entry applies from the earliest release it lists through every later release, and from its resolution release
 * on it is fixed, whatever the verdict says. A status is dated from the file only, falling back to the release's
 * publication date, so that the document does not change with the clock.
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
    val fixedOn = fix?.let { vulnEntry.resolution.at ?: declared[it].publicationDate }
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

/** Never derived from the analysis or the resolution note: both are written for the team, not for consumers. */
fun remediationOf(vulnEntry: VulnerabilityEntry): Remediation {
    val fixRelease = vulnEntry.resolution?.release
    return when (findDisposition(vulnEntry.verdict)) {
        Disposition.WONT_FIX -> Remediation.RiskAccepted(fixRelease)
        Disposition.WILL_FIX -> fixRelease?.let(Remediation::UpdateTo) ?: Remediation.FixPlanned
        null -> fixRelease?.let(Remediation::UpdateTo) ?: Remediation.NoneAvailable
    }
}

fun vexStatusKind(status: VexStatus): VexStatusKind =
    when (status) {
        is VexStatus.Affected -> VexStatusKind.AFFECTED
        VexStatus.Fixed -> VexStatusKind.FIXED
        is VexStatus.NotAffected -> VexStatusKind.NOT_AFFECTED
        is VexStatus.UnderInvestigation -> VexStatusKind.UNDER_INVESTIGATION
    }

private fun unresolvedStatus(vulnEntry: VulnerabilityEntry): VexStatus {
    val analysis = vulnEntry.analysis?.takeIf(String::isNotBlank)
    return when (val verdict = vulnEntry.verdict) {
        Verdict.UnderInvestigation -> VexStatus.UnderInvestigation(analysis)
        is Verdict.NotAffected -> VexStatus.NotAffected(verdict.justification, analysis)
        is Verdict.Affected -> VexStatus.Affected(remediationOf(vulnEntry), analysis)
    }
}

/** An open investigation has no verdict to date, so only its first report counts. */
private fun unresolvedOn(vulnEntry: VulnerabilityEntry): LocalDate? {
    val firstReport = vulnEntry.reports.mapNotNull(ReportEntry::at).minOrNull()
    return when (vulnEntry.verdict) {
        Verdict.UnderInvestigation -> firstReport
        is Verdict.NotAffected, is Verdict.Affected -> vulnEntry.analyzedAt ?: firstReport
    }
}
