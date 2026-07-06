// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.result

import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressedVulnerability
import dev.vulnlog.lib.model.suppress.SuppressionFormat
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import java.time.LocalDate

/**
 * A suppression entry excluded from an output, carrying the data needed to explain why. The
 * shells decide whether to render exclusions as diagnostics or warnings.
 */
sealed interface SuppressionExclusion {
    val id: VulnId

    data class UnsupportedIdType(
        override val id: VulnId,
        val fileName: String,
        val format: SuppressionFormat,
    ) : SuppressionExclusion

    data class UnsupportedReporter(
        override val id: VulnId,
        val reporter: ReporterType,
    ) : SuppressionExclusion

    data class ResolvedVulnerability(
        override val id: VulnId,
    ) : SuppressionExclusion

    data class ExpiredSuppression(
        override val id: VulnId,
        val reporter: ReporterType,
        val expiredAt: LocalDate,
    ) : SuppressionExclusion
}

data class SuppressionOutputsResult(
    val outputs: Set<SuppressionOutput>,
    val exclusions: List<SuppressionExclusion>,
)

data class SuppressionCollectionResult(
    val included: Map<ReporterType, List<SuppressedVulnerability>>,
    val exclusions: List<SuppressionExclusion>,
)
