package ch.addere.cli.suppressions

import ch.addere.dsl.OwaspDependencyChecker
import ch.addere.dsl.Vulnerability
import java.io.File

class OwaspDependencyCheckerSuppressor(
    suppressionFileTemplate: File,
    suppressionBlockMarker: String
) : Suppressor(suppressionFileTemplate, suppressionBlockMarker) {

    override val suppressionBlockTemplate: String
        get() = """|<suppress>
                   |    <notes><![CDATA[
                   |        vulnlog-reason
                   |    ]]></notes>
                   |    <vulnerabilityName>vulnlog-cve</vulnerabilityName>
                   |</suppress>""".trimMargin()

    override fun filterRelevant(vulnerabilities: Set<Vulnerability>): Set<Vulnerability> {
        return vulnerabilities
            .filter { it.reporter?.scanner?.any { scanner -> scanner is OwaspDependencyChecker } ?: false }
            .filter { it.resolution?.suppress != null }
            .toSet()
    }

    override fun transform(filtered: Set<Vulnerability>): Set<SuppressionBlock> {
        return filtered.map { vulnerability ->
            val cve = vulnerability.id
            val reason = vulnerability.resolution?.suppress?.reason ?: ""
            suppressionBlockTemplate
                .replace("vulnlog-reason", reason)
                .replace("vulnlog-cve", cve)
                .lines()
        }.toSet()
    }
}
