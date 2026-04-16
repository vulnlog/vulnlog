// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.parse.suppression.trivy

import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.parse.suppression.trivy.dto.TrivySuppressionDto
import dev.vulnlog.lib.parse.suppression.trivy.dto.TrivyVulnerabilityEntryDto

object TrivyMapper {
    fun toDto(suppressionData: SuppressionOutput.TrivySuppression): TrivySuppressionDto {
        val entries =
            suppressionData.entries
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
