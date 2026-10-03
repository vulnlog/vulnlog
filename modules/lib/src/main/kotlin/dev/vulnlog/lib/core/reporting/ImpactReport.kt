// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.reporting

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.reporting.ImpactEntry
import java.time.Instant

data class ImpactReport(
    val project: Project,
    val entries: List<ImpactEntry>,
    val generatedAt: Instant,
    val vulnlogVersion: String,
    val inputs: List<String>,
    val filter: ResolvedFilter,
    /** The release as requested, for the banner; [filter] holds the releases it expands to. */
    val asOf: Release?,
)
