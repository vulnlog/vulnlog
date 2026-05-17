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
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.parse.createYamlMapper
import dev.vulnlog.lib.parse.v1.V1Mapper
import tools.jackson.databind.ObjectMapper
import java.nio.file.Path
import java.time.LocalDate

data class AddVulnerabilityOptions(
    val vulnId: VulnId,
    val releases: Set<Release>,
    val packages: Set<Purl>,
    val tags: Set<Tag>,
    val reporter: ReporterType?,
)

data class AddOutcome(
    val newContent: String,
    val vulnId: VulnId,
)

/**
 * Builds a [VulnerabilityEntry] from [options] and serializes it as a YAML list item suitable for
 * pasting under a Vulnlog file's `vulnerabilities:` section. The verdict defaults to
 * [Verdict.UnderInvestigation]; if a reporter is supplied, a single report entry is added with the
 * current date.
 */
fun createVulnerabilityEntry(options: AddVulnerabilityOptions): String =
    serializeEntryYaml(V1Mapper.vulnerabilityToDto(buildVulnerabilityEntry(options)), createYamlMapper())

/**
 * Validates [options] against [destination] and inserts a new vulnerability entry into
 * [destinationContent]. When [options.releases] is empty, defaults to the latest published
 * release of [destination] (see [lastReleaseFavoringPublished]).
 *
 * Throws [IllegalArgumentException] if the vuln id already exists in [destination], any release
 * or tag in [options] is not defined in [destination], or [options.releases] is empty and
 * [destination] has no releases at all.
 */
fun addVulnerabilityToFile(
    destination: VulnlogFile,
    destinationContent: String,
    options: AddVulnerabilityOptions,
    mapper: ObjectMapper = createYamlMapper(),
): AddOutcome {
    require(options.vulnId !in destination.vulnerabilities.map { it.id }) {
        "Vulnerability ${options.vulnId.id} already exists in file"
    }

    val knownReleases = destination.releases.map { it.id }.toSet()
    val missingReleases = options.releases - knownReleases
    require(missingReleases.isEmpty()) {
        "Releases not defined in file: ${missingReleases.joinToString(", ") { it.value }}"
    }

    val knownTags = destination.tags.map { it.id }.toSet()
    val missingTags = options.tags - knownTags
    require(missingTags.isEmpty()) {
        "Tags not defined in file: ${missingTags.joinToString(", ") { it.value }}"
    }

    val effectiveReleases =
        options.releases.ifEmpty {
            require(destination.releases.isNotEmpty()) {
                "File has no releases defined; cannot determine default release"
            }
            setOf(lastReleaseFavoringPublished(destination.releases))
        }

    val entry = buildVulnerabilityEntry(options.copy(releases = effectiveReleases))
    val entryYaml = serializeEntryYaml(V1Mapper.vulnerabilityToDto(entry), mapper)
    val newContent = insertEntryAfterVulnerabilitiesHeader(destinationContent, entryYaml)
    return AddOutcome(newContent, options.vulnId)
}

fun formatAddedMessage(
    destinationPath: Path,
    vulnId: VulnId,
): String = "Added to $destinationPath: ${vulnId.id}"

private fun buildVulnerabilityEntry(options: AddVulnerabilityOptions): VulnerabilityEntry =
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
