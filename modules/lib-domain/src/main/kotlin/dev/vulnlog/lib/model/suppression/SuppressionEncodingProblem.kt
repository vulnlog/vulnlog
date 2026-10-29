// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.suppression

import dev.vulnlog.lib.model.VulnId
import java.time.LocalDate

/** An entry that cannot be represented in the requested output format. */
sealed interface SuppressionEncodingProblem {
    val format: SuppressionFormat
    val id: VulnId

    data class UnencodableReason(
        override val format: SuppressionFormat,
        override val id: VulnId,
        val codePoint: Int,
    ) : SuppressionEncodingProblem

    data class UnencodableExpiry(
        override val format: SuppressionFormat,
        override val id: VulnId,
        val expiresAt: LocalDate,
    ) : SuppressionEncodingProblem
}
