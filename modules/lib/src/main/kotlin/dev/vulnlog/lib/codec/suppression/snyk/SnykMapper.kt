// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.snyk

import dev.vulnlog.lib.codec.suppression.snyk.dto.SnykIgnoreEntryDto
import dev.vulnlog.lib.codec.suppression.snyk.dto.SnykSuppressionDto
import dev.vulnlog.lib.model.suppression.SuppressionOutput

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
                                    expires = entry.expiresAt?.atStartOfDay(),
                                ),
                        ),
                    )
            }
        return SnykSuppressionDto(ignore = ignore)
    }
}
