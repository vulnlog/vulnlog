// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Tag

/** What the caller asked to filter [dev.vulnlog.lib.model.VulnerabilityEntry] on, before any check against a file. */
data class FilterRequest(
    val reporter: ReporterType? = null,
    val release: Release? = null,
    val tags: Set<Tag> = emptySet(),
)
