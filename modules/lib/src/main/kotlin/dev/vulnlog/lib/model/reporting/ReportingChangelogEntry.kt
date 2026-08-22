// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.reporting

import dev.vulnlog.lib.model.VulnId

/** One vulnerability a release shipped a fix for. */
data class ReportingChangelogEntry(
    val primaryId: VulnId,
    val aliases: Set<VulnId> = emptySet(),
    val name: String? = null,
    val description: String? = null,
    val impact: Impact = Impact.Unknown,
    val note: String? = null,
    val ref: String? = null,
)
