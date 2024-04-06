package ch.addere.cli.suppressions

import ch.addere.dsl.Snyk
import ch.addere.dsl.Vulnerability
import java.io.File

class SnykSuppressor(
    suppressionFileTemplate: File,
    suppressionBlockMarker: String
) : Suppressor(suppressionFileTemplate, suppressionBlockMarker) {

    override val suppressionBlockTemplate: String
        get() = """|vulnlog-snyk-id:
                   |  - '*':
                   |    reason: vulnlog-reason""".trimMargin()

    override fun filterRelevant(vulnerabilities: Set<Vulnerability>): Set<Vulnerability> {
        return vulnerabilities
            .filter { it.reporter?.scanner?.any { scanner -> scanner is Snyk } ?: false }
            .filter { it.resolution?.suppress != null }
            .toSet()
    }

    override fun transform(filtered: Set<Vulnerability>): Set<SuppressionBlock> {
        return filtered.map { vulnerability ->
            val snykId: String? = vulnerability.reporter?.scanner?.filterIsInstance<Snyk>()?.map { snyk -> snyk.id }?.first()
            val reason = vulnerability.resolution?.suppress?.reason ?: ""
            suppressionBlockTemplate
                .replace("vulnlog-reason", reason)
                .replace("vulnlog-snyk-id", snykId ?: "")
                .lines()
        }.toSet()
    }
}
