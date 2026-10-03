// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.generic

import dev.vulnlog.lib.codec.suppression.generic.dto.GenericSuppressionDto
import dev.vulnlog.lib.codec.suppression.generic.dto.GenericVulnerabilityEntryDto
import dev.vulnlog.lib.model.suppression.SuppressionList

internal object GenericMapper {
    fun toDto(list: SuppressionList): GenericSuppressionDto {
        val entries =
            list.entries
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
