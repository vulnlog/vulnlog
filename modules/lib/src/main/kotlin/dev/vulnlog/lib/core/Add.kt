// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReportEntry
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.v1.V1Mapper
import java.time.LocalDate

data class AddVulnerabilityOptions(
    val vulnId: VulnId,
    val releases: Set<Release>,
    val packages: Set<Purl>,
    val tags: Set<Tag>,
    val reporter: ReporterType?,
)

/**
 * Builds a [VulnerabilityEntry] from [options] and serializes it as a YAML list item suitable for
 * pasting under a Vulnlog file's `vulnerabilities:` section. The verdict defaults to
 * [Verdict.UnderInvestigation]; if a reporter is supplied, a single report entry is added with the
 * current date.
 */
fun createVulnerabilityEntry(options: AddVulnerabilityOptions): String {
    val entry =
        VulnerabilityEntry(
            id = options.vulnId,
            releases = options.releases.toList(),
            packages = options.packages.toList(),
            reports =
                options.reporter?.let { listOf(ReportEntry(reporter = it, at = LocalDate.now())) }
                    ?: emptyList(),
            tags = options.tags.toList(),
            verdict = Verdict.UnderInvestigation,
        )
    return serializeEntryYaml(V1Mapper.vulnerabilityToDto(entry), createYamlMapper())
}
