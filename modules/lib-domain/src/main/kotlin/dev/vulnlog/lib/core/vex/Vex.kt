// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex

import dev.vulnlog.lib.core.findDisposition
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.PurlEntry
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.ReleaseVexStatus
import dev.vulnlog.lib.model.vex.Remediation
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.VexStatusKind
import java.time.LocalDate

/**
 * Derives the VEX status [vulnEntry] has in each release, in release order. From the first release the entry lists, it
 * has the status of its verdict, and in the release that resolves it, the status `Fixed`. Later releases get no status,
 * because they no longer ship the vulnerable package.
 */
fun deriveReleaseVexStatuses(
    vulnEntry: VulnerabilityEntry,
    vulnlogFile: VulnlogFile,
): List<ReleaseVexStatus> {
    val releases = vulnlogFile.releases
    val first = findFirstListedReleaseIndex(releases, vulnEntry.releases) ?: return emptyList()
    return deriveStatusesBeforeFix(vulnEntry, releases, first) + listOfNotNull(deriveFixedStatus(vulnEntry, releases))
}

fun filterReleasePurlsMatchingVulnerabilityEntryTags(
    release: ReleaseEntry,
    vulnEntry: VulnerabilityEntry,
): List<PurlEntry> = release.purls.filter { purlEntry -> purlEntry.tags.any { tag -> tag in vulnEntry.tags } }

fun remediationOf(
    vulnEntry: VulnerabilityEntry,
    releases: List<ReleaseEntry>,
): Remediation {
    val fixRelease = vulnEntry.resolution?.release
    val shipped = releases.any { release -> release.id == fixRelease && release.publicationDate != null }
    val fix =
        fixRelease?.let { release ->
            if (shipped) Remediation.UpdateTo(release) else Remediation.FixPlanned(release)
        }
    return when (findDisposition(vulnEntry.verdict)) {
        Disposition.WONT_FIX -> Remediation.RiskAccepted(fixRelease)
        Disposition.WILL_FIX -> fix ?: Remediation.FixPlanned(null)
        null -> fix ?: Remediation.NoneAvailable
    }
}

fun vexStatusKind(status: VexStatus): VexStatusKind =
    when (status) {
        is VexStatus.Affected -> VexStatusKind.AFFECTED
        VexStatus.Fixed -> VexStatusKind.FIXED
        is VexStatus.NotAffected -> VexStatusKind.NOT_AFFECTED
        is VexStatus.UnderInvestigation -> VexStatusKind.UNDER_INVESTIGATION
    }

private fun findFirstListedReleaseIndex(
    releases: List<ReleaseEntry>,
    listedReleases: List<Release>,
): Int? = listedReleases.mapNotNull { release -> findReleaseIndex(releases, release) }.minOrNull()

/** Derives one status per release from [first] up to the fix, or none when the fix comes before [first]. */
private fun deriveStatusesBeforeFix(
    vulnEntry: VulnerabilityEntry,
    releases: List<ReleaseEntry>,
    first: Int,
): List<ReleaseVexStatus> {
    val fix = vulnEntry.resolution?.let { resolution -> findReleaseIndex(releases, resolution.release) }
    val status = deriveUnresolvedStatus(vulnEntry, releases)
    val since = findUnresolvedSince(vulnEntry)
    return releases.subList(first, maxOf(first, fix ?: releases.size)).map { release ->
        ReleaseVexStatus(release.id, status, since ?: release.publicationDate)
    }
}

/** Derives the `Fixed` status of the release that resolves the entry, or null without a declared fix release. */
private fun deriveFixedStatus(
    vulnEntry: VulnerabilityEntry,
    releases: List<ReleaseEntry>,
): ReleaseVexStatus? {
    val resolution = vulnEntry.resolution ?: return null
    val release = releases.find { entry -> entry.id == resolution.release } ?: return null
    return ReleaseVexStatus(release.id, VexStatus.Fixed, resolution.at ?: release.publicationDate)
}

/** Finds the position of [release] in the file's release order, or null when the file does not declare it. */
private fun findReleaseIndex(
    releases: List<ReleaseEntry>,
    release: Release,
): Int? = releases.indexOfFirst { entry -> entry.id == release }.takeIf { index -> index >= 0 }

/** Derives the status the entry's verdict gives it in the releases before its fix. */
private fun deriveUnresolvedStatus(
    vulnEntry: VulnerabilityEntry,
    releases: List<ReleaseEntry>,
): VexStatus {
    val analysis = vulnEntry.analysis?.takeIf(String::isNotBlank)
    return when (val verdict = vulnEntry.verdict) {
        Verdict.UnderInvestigation -> VexStatus.UnderInvestigation(analysis)
        is Verdict.NotAffected -> VexStatus.NotAffected(verdict.justification, analysis)
        is Verdict.Affected -> VexStatus.Affected(remediationOf(vulnEntry, releases), analysis)
    }
}

/**
 * Finds the day the unresolved status became true: the analysis date, else the first report.
 * An open investigation has no verdict to date, so only its first report counts.
 */
private fun findUnresolvedSince(vulnEntry: VulnerabilityEntry): LocalDate? {
    val firstReport = vulnEntry.reports.mapNotNull(ReportEntry::at).minOrNull()
    return when (vulnEntry.verdict) {
        Verdict.UnderInvestigation -> firstReport
        is Verdict.NotAffected, is Verdict.Affected -> vulnEntry.analyzedAt ?: firstReport
    }
}
