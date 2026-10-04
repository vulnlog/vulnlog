// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.suppression

import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import java.time.LocalDate

data class SuppressionCollection(
    val included: Map<ReporterType, List<SuppressedVulnerability>>,
    val exclusions: List<SuppressionExclusion>,
)

sealed interface SuppressionExclusion {
    val id: VulnId

    data class UnsupportedIdType(
        override val id: VulnId,
        val format: SuppressionFormat,
    ) : SuppressionExclusion

    data class UnsupportedReporter(
        override val id: VulnId,
        val reporter: ReporterType,
    ) : SuppressionExclusion

    data class Resolved(
        override val id: VulnId,
    ) : SuppressionExclusion

    data class Expired(
        override val id: VulnId,
        val reporter: ReporterType,
        val expiredAt: LocalDate,
    ) : SuppressionExclusion
}
