// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.trivy

import dev.vulnlog.lib.codec.suppression.trivy.dto.TrivySuppressionDto
import dev.vulnlog.lib.codec.suppression.trivy.dto.TrivyVulnerabilityEntryDto
import dev.vulnlog.lib.model.suppression.SuppressionList

internal object TrivyMapper {
    fun toDto(list: SuppressionList): TrivySuppressionDto {
        val entries =
            list.entries
                .map { entry ->
                    TrivyVulnerabilityEntryDto(
                        id = entry.id.id,
                        expiredAt = entry.expiresAt,
                        statement = entry.reason,
                    )
                }
        return TrivySuppressionDto(entries)
    }
}
