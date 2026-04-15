// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.parse.suppression.snyk

import dev.vulnlog.cli.model.suppress.SuppressionOutput
import dev.vulnlog.cli.parse.suppression.snyk.dto.SnykIgnoreEntryDto
import dev.vulnlog.cli.parse.suppression.snyk.dto.SnykSuppressionDto

object SnykMapper {
    fun toDto(suppressionData: SuppressionOutput.SnykSuppression): SnykSuppressionDto {
        val ignore =
            suppressionData.entries.associate { entry ->
                entry.id.id to
                    listOf(
                        mapOf(
                            "*" to
                                SnykIgnoreEntryDto(
                                    reason = entry.reason,
                                    expires = entry.expiresAt,
                                ),
                        ),
                    )
            }
        return SnykSuppressionDto(ignore)
    }
}
