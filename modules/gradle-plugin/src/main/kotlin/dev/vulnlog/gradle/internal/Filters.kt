// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.core.VulnlogFilter
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import org.gradle.api.GradleException

internal fun buildFilter(
    vulnlogFile: VulnlogFile,
    reporter: String?,
    release: String?,
    tags: Set<String>,
): VulnlogFilter =
    VulnlogFilter(
        releases = resolveReleases(release, vulnlogFile),
        tags = resolveTags(tags, vulnlogFile),
        reporter = resolveReporter(reporter),
    )

private fun resolveReporter(reporter: String?): ReporterType? =
    reporter?.let { value ->
        runCatching { ReporterType.valueOf(value.uppercase()) }
            .getOrElse {
                val supported = ReporterType.entries.joinToString(", ") { it.name.lowercase() }
                throw GradleException("Invalid reporter: $value. Supported: $supported")
            }
    }

private fun resolveReleases(
    releaseOption: String?,
    vulnlogFile: VulnlogFile,
): Set<Release> {
    if (releaseOption == null) return emptySet()
    val release =
        runCatching { Release(releaseOption) }
            .getOrElse { throw GradleException("Invalid release: $releaseOption") }
    val ordered = vulnlogFile.releases.map { it.id }
    val index = ordered.indexOf(release)
    if (index == -1) {
        val known = ordered.joinToString(", ") { it.value }
        throw GradleException("Release not found: $releaseOption. Known releases: $known")
    }
    return ordered.take(index + 1).toSet()
}

private fun resolveTags(
    tagOptions: Set<String>,
    vulnlogFile: VulnlogFile,
): Set<Tag> {
    if (tagOptions.isEmpty()) return emptySet()
    val tags =
        runCatching { tagOptions.map(::Tag).toSet() }
            .getOrElse { throw GradleException("Invalid tag: ${it.message}") }
    val known = vulnlogFile.tags.map { it.id }.toSet()
    val unknown = tags.filterNot { it in known }
    if (unknown.isNotEmpty()) {
        val knownList = known.joinToString(", ") { it.value }
        throw GradleException(
            "Tag not found: ${unknown.joinToString(", ") { it.value }}. Known tags: $knownList",
        )
    }
    return tags
}
