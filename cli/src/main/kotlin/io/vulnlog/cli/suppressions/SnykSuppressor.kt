package io.vulnlog.cli.suppressions

import io.vulnlog.core.model.reporter.VlSnykReporter
import io.vulnlog.core.model.vulnerability.VlVulnerability
import java.io.File

class SnykSuppressor(suppressionFileTemplate: File) : Suppressor(suppressionFileTemplate) {
    override val vulnLogMarker: Regex
        get() = Regex("\\s+# vulnlog-marker")

    override val vulnLogTemplateMarker: Regex
        get() = Regex("# VulnLog template file for Snyk")

    override val suppressionBlockTemplate: String
        get() =
            """|vulnlog-snyk-id:
               |  - '*':
               |    reason: vulnlog-reason
            """.trimMargin()

    override val outputfileName: String
        get() = ".snyk"

    override fun filterRelevant(vulnerabilities: Set<VlVulnerability>): Set<VlVulnerability> {
        return vulnerabilities
            .filter { it.reporter?.reporters?.any { scanner -> scanner is VlSnykReporter } ?: false }
            .filter { it.suppressResolution != null }
            .toSet()
    }

    override fun transform(filtered: Set<VlVulnerability>): Set<SuppressionBlock> {
        return filtered.map { vulnerability ->
            val snykId: String? =
                vulnerability.reporter?.reporters?.filterIsInstance<VlSnykReporter>()?.map { snyk ->
                    snyk.snykId
                }?.first()
            val rationale = vulnerability.suppressResolution?.rationale ?: ""
            suppressionBlockTemplate
                .replace("vulnlog-reason", rationale)
                .replace("vulnlog-snyk-id", snykId ?: "")
                .lines()
        }.toSet()
    }
}
