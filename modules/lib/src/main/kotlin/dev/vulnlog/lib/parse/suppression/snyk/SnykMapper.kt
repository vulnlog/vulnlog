// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression.snyk

import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.parse.suppression.snyk.dto.SnykIgnoreEntryDto
import dev.vulnlog.lib.parse.suppression.snyk.dto.SnykSuppressionDto

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
