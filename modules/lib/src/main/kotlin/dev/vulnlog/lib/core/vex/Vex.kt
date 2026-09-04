// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex

import dev.vulnlog.lib.core.findDisposition
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.vex.VexStatus

/**
 * Resolves the [VexStatus] of [vulnEntry] for one [release]. The release named by the resolution is fixed, whatever
 * the verdict says; every other release follows the verdict.
 */
fun deriveVexStatus(
    vulnEntry: VulnerabilityEntry,
    release: Release,
): VexStatus =
    when {
        vulnEntry.resolution?.release == release -> VexStatus.Fixed
        else -> deriveUnresolvedStatus(vulnEntry)
    }

private fun deriveUnresolvedStatus(vulnEntry: VulnerabilityEntry): VexStatus =
    when (val verdict = vulnEntry.verdict) {
        Verdict.UnderInvestigation -> VexStatus.UnderInvestigation
        is Verdict.NotAffected -> VexStatus.NotAffected(verdict.justification)
        is Verdict.Affected -> VexStatus.Affected(vexActionStatement(vulnEntry))
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

// TODO the resolution.note field is primarily for internal use and describes: "Brief description of how the vulnerability was resolved"
// This is not relevant for consumers of the VEX document. Re-think this approach but leave it for now.
private fun updateAction(vulnEntry: VulnerabilityEntry): String {
    val resolution = vulnEntry.resolution ?: error("update action requires a resolution")
    val update = "Update to release ${resolution.release.value}."
    return resolution.note?.takeIf(String::isNotBlank)?.let { note -> "$update $note" } ?: update
}
