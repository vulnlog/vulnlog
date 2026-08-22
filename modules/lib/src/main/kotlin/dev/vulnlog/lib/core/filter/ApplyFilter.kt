// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.core.kind
import dev.vulnlog.lib.core.reporting.findWorkState
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.VulnlogFile

/** Returns a copy of this file holding only the vulnerabilities [filter] matches, scoped to it. */
fun VulnlogFile.applyFilter(filter: ResolvedFilter): VulnlogFile =
    copy(
        vulnerabilities =
            vulnerabilities
                .map { vuln -> scopeResolution(vuln, filter.releases) }
                .filter { vuln -> filter.matches(vuln) },
    )

/**
 * Drops a resolution whose release lies outside [releases].
 *
 * Under a release filter, a fix only counts once it has shipped at or before the filtered release.
 * An empty [releases] means no release filter is active, so every resolution counts.
 */
fun scopeResolution(
    vuln: VulnerabilityEntry,
    releases: Set<Release>,
): VulnerabilityEntry =
    if (releases.isEmpty() || vuln.resolution == null || vuln.resolution.release in releases) {
        vuln
    } else {
        vuln.copy(resolution = null)
    }

private fun ResolvedFilter.matches(vuln: VulnerabilityEntry): Boolean =
    (releases.isEmpty() || vuln.releases.any { it in releases }) &&
        (tags.isEmpty() || vuln.tags.any { it in tags }) &&
        (reporter == null || vuln.reports.any { it.reporter == reporter }) &&
        (states.isEmpty() || findWorkState(vuln) in states) &&
        (verdicts.isEmpty() || vuln.verdict.kind() in verdicts)
