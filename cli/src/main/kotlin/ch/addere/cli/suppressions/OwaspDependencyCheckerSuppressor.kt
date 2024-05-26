package ch.addere.cli.suppressions

import ch.addere.vulnlog.core.model.reporter.VlOwaspReporter
import ch.addere.vulnlog.core.model.vulnerability.VlVulnerability
import java.io.File

class OwaspDependencyCheckerSuppressor(
    suppressionFileTemplate: File,
    suppressionBlockMarker: String,
) : Suppressor(suppressionFileTemplate, suppressionBlockMarker) {
    override val suppressionBlockTemplate: String
        get() =
            """|<suppress>
                   |    <notes><![CDATA[
                   |        vulnlog-reason
                   |    ]]></notes>
                   |    <vulnerabilityName>vulnlog-cve</vulnerabilityName>
                   |</suppress>
            """.trimMargin()

    override fun filterRelevant(vulnerabilities: Set<VlVulnerability>): Set<VlVulnerability> {
        return vulnerabilities
            .filter { it.reporter?.reporters?.any { scanner -> scanner is VlOwaspReporter } ?: false }
            .filter { it.suppressResolution != null }
            .toSet()
    }

    override fun transform(filtered: Set<VlVulnerability>): Set<SuppressionBlock> {
        return filtered.map { vulnerability ->
            val cve = vulnerability.id
            val reason = vulnerability.suppressResolution?.rationale ?: ""
            suppressionBlockTemplate
                .replace("vulnlog-reason", reason)
                .replace("vulnlog-cve", cve)
                .lines()
        }.toSet()
    }
}
