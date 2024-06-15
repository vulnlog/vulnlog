package ch.addere.vulnlog.cli.suppressions

import ch.addere.vulnlog.core.model.reporter.VlOwaspReporter
import ch.addere.vulnlog.core.model.vulnerability.VlVulnerability
import java.io.File

class OwaspDependencyCheckerSuppressor(suppressionFileTemplate: File) : Suppressor(suppressionFileTemplate) {
    override val vulnLogMarker: Regex
        get() = Regex("\\s+<!-- vulnlog-marker -->")

    override val vulnLogTemplateMarker: Regex
        get() = Regex("<!-- VulnLog template file for OWASP Dependency Checker -->")

    override val suppressionBlockTemplate: String
        get() =
            """|<suppress>
               |    <notes><![CDATA[
               |        vulnlog-reason
               |    ]]></notes>
               |    <vulnerabilityName>vulnlog-cve</vulnerabilityName>
               |</suppress>
            """.trimMargin()

    override val outputfileName: String
        get() = "owasp-suppression.xml"

    override fun filterRelevant(vulnerabilities: Set<VlVulnerability>): Set<VlVulnerability> =
        vulnerabilities
            .filter { it.reporter?.reporters?.any { scanner -> scanner is VlOwaspReporter } ?: false }
            .filter { it.suppressResolution != null }
            .toSet()

    override fun transform(filtered: Set<VlVulnerability>): Set<SuppressionBlock> =
        filtered.map { vulnerability ->
            val cve = vulnerability.id
            val reason = vulnerability.suppressResolution?.rationale ?: ""
            suppressionBlockTemplate
                .replace("vulnlog-reason", reason)
                .replace("vulnlog-cve", cve)
                .lines()
        }.toSet()
}
