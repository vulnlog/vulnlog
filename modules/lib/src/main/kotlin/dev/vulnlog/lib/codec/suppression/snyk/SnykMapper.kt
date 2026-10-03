// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.snyk

import dev.vulnlog.lib.codec.suppression.snyk.dto.SnykIgnoreEntryDto
import dev.vulnlog.lib.codec.suppression.snyk.dto.SnykSuppressionDto
import dev.vulnlog.lib.model.suppression.SuppressionList

internal object SnykMapper {
    fun toDto(list: SuppressionList): SnykSuppressionDto {
        val ignore =
            list.entries.associate { entry ->
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
