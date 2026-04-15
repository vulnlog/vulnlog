// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.Release
import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.Tag
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.suppress.SuppressedVulnerability
import java.time.LocalDate
import kotlin.sequences.filter

data class VulnlogFilter(
    val releases: Set<Release> = emptySet(),
    val tags: Set<Tag> = emptySet(),
    val reporter: ReporterType? = null,
)

data class SuppressionFilter(
    val filter: VulnlogFilter = VulnlogFilter(),
    val today: LocalDate = LocalDate.now(),
)

fun Sequence<VulnerabilityEntry>.applyFilter(filter: VulnlogFilter): Sequence<VulnerabilityEntry> =
    this
        .filter { filter.releases.isEmpty() || it.releases.any { release -> release in filter.releases } }
        .filter { filter.tags.isEmpty() || filter.tags.any { tag -> it.tags.contains(tag) } }

fun Sequence<SuppressedVulnerability>.applyFilter(filter: SuppressionFilter): Sequence<SuppressedVulnerability> =
    this
        .filter { filter.filter.releases.isEmpty() || it.releases.any { release -> release in filter.filter.releases } }
        .filter { filter.filter.tags.isEmpty() || filter.filter.tags.any { tag -> it.tags.contains(tag) } }
