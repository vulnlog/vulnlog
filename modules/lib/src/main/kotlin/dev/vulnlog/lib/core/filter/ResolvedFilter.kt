// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VerdictKind
import dev.vulnlog.lib.model.report.WorkState

/**
 * A [FilterRequest] checked against the Vulnlog files it will be applied to.
 *
 * [releases] holds the requested release and every release before it, so a filter on a release
 * covers everything shipped up to and including it. An empty set means the dimension is inactive.
 */
data class ResolvedFilter(
    val reporter: ReporterType? = null,
    val releases: Set<Release> = emptySet(),
    val tags: Set<Tag> = emptySet(),
    val states: Set<WorkState> = emptySet(),
    val verdicts: Set<VerdictKind> = emptySet(),
    val dispositions: Set<Disposition> = emptySet(),
)
