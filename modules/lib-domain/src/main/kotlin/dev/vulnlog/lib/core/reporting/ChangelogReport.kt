// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.reporting

import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.reporting.Changelog
import java.time.Instant

/** The changelog with the run that generated it, for the formats that carry metadata. */
data class ChangelogReport(
    val changelog: Changelog,
    val generatedAt: Instant,
    val vulnlogVersion: String,
    val inputs: List<String>,
    val filter: ResolvedFilter,
    /** The release as requested. [filter] holds the releases it expands to. */
    val asOf: Release?,
)
