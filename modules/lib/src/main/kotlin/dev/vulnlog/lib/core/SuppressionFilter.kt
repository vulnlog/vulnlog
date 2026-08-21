// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.model.suppress.SuppressedVulnerability
import java.time.LocalDate

data class SuppressionFilter(
    val filter: ResolvedFilter = ResolvedFilter(),
    val today: LocalDate = LocalDate.now(),
)

fun Sequence<SuppressedVulnerability>.applyFilter(filter: ResolvedFilter): Sequence<SuppressedVulnerability> =
    this
        .filter { filter.releases.isEmpty() || it.releases.any { release -> release in filter.releases } }
        .filter { filter.tags.isEmpty() || filter.tags.any { tag -> it.tags.contains(tag) } }
        .filter { filter.reporter == null || filter.reporter == it.reporter }
