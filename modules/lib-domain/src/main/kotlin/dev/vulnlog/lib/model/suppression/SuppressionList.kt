// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.suppression

import dev.vulnlog.lib.model.VulnId
import java.time.LocalDate

/** One reporter's suppression file before encoding; the format decides how it is written. */
data class SuppressionList(
    val format: SuppressionFormat,
    val entries: Set<SuppressionEntry>,
)

data class SuppressionEntry(
    val id: VulnId,
    val expiresAt: LocalDate? = null,
    val reason: String? = null,
)

data class SuppressionLists(
    val lists: List<SuppressionList>,
    val exclusions: List<SuppressionExclusion>,
)
