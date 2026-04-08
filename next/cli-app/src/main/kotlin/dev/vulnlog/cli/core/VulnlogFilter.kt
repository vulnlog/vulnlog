package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.Tag
import dev.vulnlog.cli.model.VulnerabilityEntry

data class VulnlogFilter(
    val releases: Set<Release> = emptySet(),
    val tags: Set<Tag> = emptySet(),
    val reporter: ReporterType? = null,
)

fun Sequence<VulnerabilityEntry>.applyFilter(filter: VulnlogFilter): Sequence<VulnerabilityEntry> =
    this
        .filter { filter.releases.isEmpty() || it.releases.any { release -> release in filter.releases } }
        .filter { filter.tags.isEmpty() || filter.tags.any { tag -> it.tags.contains(tag) } }
