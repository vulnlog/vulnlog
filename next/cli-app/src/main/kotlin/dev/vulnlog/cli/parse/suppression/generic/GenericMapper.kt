// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.parse.suppression.generic

import dev.vulnlog.cli.model.suppress.SuppressionOutput
import dev.vulnlog.cli.parse.suppression.generic.dto.GenericSuppressionDto
import dev.vulnlog.cli.parse.suppression.generic.dto.GenericVulnerabilityEntryDto

object GenericMapper {
    fun toDto(suppressionData: SuppressionOutput.GenericSuppression): GenericSuppressionDto {
        val entries =
            suppressionData.entries
                .map { entry ->
                    GenericVulnerabilityEntryDto(
                        id = entry.id.id,
                        expiredAt = entry.expiresAt,
                        statement = entry.reason,
                    )
                }
        return GenericSuppressionDto(entries)
    }
}
