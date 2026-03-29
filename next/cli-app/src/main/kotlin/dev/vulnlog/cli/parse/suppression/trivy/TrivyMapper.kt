package dev.vulnlog.cli.parse.suppression.trivy

import dev.vulnlog.cli.model.suppress.SuppressionOutput
import dev.vulnlog.cli.parse.suppression.trivy.dto.TrivySuppressionDto
import dev.vulnlog.cli.parse.suppression.trivy.dto.TrivyVulnerabilityEntryDto

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
