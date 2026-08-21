// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag

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
)
