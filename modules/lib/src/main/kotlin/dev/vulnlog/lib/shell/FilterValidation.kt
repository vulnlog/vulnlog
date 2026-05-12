// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.core.VulnlogFilter
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.parseReporter
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile

/**
 * Resolves a set of releases to filter on, based on a given release option and the release information
 * in the provided Vulnlog file. This method includes all releases up to and including the specified release
 * if a valid release option is provided. If no release option is specified, an empty set of releases is returned.
 * Validation ensures that the specified release exists in the Vulnlog file, and an exception is thrown if
 * the release is invalid or not found.
 *
 * @param releaseOption The release identifier string to filter on. If null, no releases are included in the result.
 * @param vulnlogFile The parsed Vulnlog file containing metadata and release definitions used for validation.
 * @return A set of releases up to and including the specified release, or an empty set if no release option is provided.
 * @throws FilterValidationException If the specified release is not valid or does not exist in the Vulnlog file.
 */
fun resolveReleaseFilter(
    releaseOption: String?,
    vulnlogFile: VulnlogFile,
): Set<Release> =
    if (releaseOption != null) {
        try {
            val release = Release(releaseOption)
            val orderedReleases = vulnlogFile.releases.map { it.id }
            val index = orderedReleases.indexOf(release)
            if (index == -1) {
                throw FilterValidationException(
                    "Release not found: $releaseOption",
                    "Known releases: ${orderedReleases.joinToString(", ") { it.value }}",
                )
            }
            orderedReleases.take(index + 1).toSet()
        } catch (e: IllegalArgumentException) {
            throw FilterValidationException(
                "Invalid release: ${e.message}",
                "Known releases: ${vulnlogFile.releases.joinToString(", ") { it.id.value }}",
            )
        }
    } else {
        emptySet()
    }

/**
 * Resolves and validates the tag filters provided as options against the tags defined in a given Vulnlog file.
 *
 * @param tagsOptions The set of tag strings provided as filter options. Empty set if no tags are specified.
 * @param vulnlogFile The `VulnlogFile` containing the available tags to validate against.
 * @return A set of `Tag` objects that match the specified filter options. Returns an empty set if no filters are specified.
 * @throws FilterValidationException If any tag in the `tagsOptions` is invalid or not found in the Vulnlog file.
 */
fun resolveTagsFilter(
    tagsOptions: Set<String>,
    vulnlogFile: VulnlogFile,
): Set<Tag> =
    if (tagsOptions.isNotEmpty()) {
        try {
            val tags = tagsOptions.map(::Tag).toSet()
            val unknownTags = tags.filter { tag -> tag !in vulnlogFile.tags.map { it.id } }.toSet()
            if (unknownTags.isNotEmpty()) {
                throw FilterValidationException(
                    "Tag not found: ${unknownTags.joinToString(", ") { it.value }}",
                    "Known tags: ${vulnlogFile.tags.joinToString(", ") { it.id.value }}",
                )
            }
            tags
        } catch (e: IllegalArgumentException) {
            throw FilterValidationException(
                "Invalid tag: ${e.message}",
                "Known tags: ${vulnlogFile.tags.joinToString(", ") { it.id.value }}",
            )
        }
    } else {
        emptySet()
    }

/**
 * Resolves and validates a reporter filter option. Accepts the canonical lowercase identifier
 * (e.g. `"dependency-check"`) and translates it into a [ReporterType] using [parseReporter].
 *
 * @param reporterOption The canonical reporter identifier, or null when no reporter filter is requested.
 * @return The resolved [ReporterType], or null if [reporterOption] is null.
 * @throws FilterValidationException If [reporterOption] is not a known canonical reporter identifier.
 */
fun resolveReporterFilter(reporterOption: String?): ReporterType? =
    reporterOption?.let { value ->
        try {
            parseReporter(value)
        } catch (_: IllegalArgumentException) {
            throw FilterValidationException(
                "Invalid reporter: $value",
                "Supported reporters: ${ReporterType.entries.joinToString(", ") { it.canonical() }}",
            )
        }
    }

/**
 * Builds a [VulnlogFilter] from raw option strings, validating each against the given Vulnlog file.
 * Wraps [resolveReleaseFilter], [resolveTagsFilter], and [resolveReporterFilter] so callers that
 * receive options as strings (e.g. Gradle tasks) get a single validated entry point with a uniform
 * error contract.
 *
 * @throws FilterValidationException If any of the filter options is invalid.
 */
fun buildFilter(
    vulnlogFile: VulnlogFile,
    reporterOption: String?,
    releaseOption: String?,
    tagsOptions: Set<String>,
): VulnlogFilter =
    VulnlogFilter(
        releases = resolveReleaseFilter(releaseOption, vulnlogFile),
        tags = resolveTagsFilter(tagsOptions, vulnlogFile),
        reporter = resolveReporterFilter(reporterOption),
    )

class FilterValidationException(
    message: String,
    val detail: String,
) : RuntimeException(message)
