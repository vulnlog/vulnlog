package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.ReporterType
import dev.vulnlog.cli.model.Verdict
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.model.suppress.NotSuppressable
import dev.vulnlog.cli.model.suppress.Suppressable
import dev.vulnlog.cli.model.suppress.SuppressedVulnerability
import dev.vulnlog.cli.model.suppress.Suppression
import dev.vulnlog.cli.model.suppress.SuppressionOutput
import dev.vulnlog.cli.model.suppress.SuppressionVuln
import java.time.LocalDate

data class SuppressionFilter(
    val filter: VulnlogFilter = VulnlogFilter(),
    val today: LocalDate = LocalDate.now(),
)

/**
 * Collects and groups suppressed vulnerabilities from the provided VulnlogFile based on the specified suppression filter criteria.
 *
 * This method processes the vulnerabilities by applying the filter configuration, including release, tags, resolution, and reporter type.
 * Suppressed vulnerabilities are extracted from the reports, filtered for their expiration status, and grouped by the reporter type.
 *
 * @param vulnlogFile The VulnlogFile containing vulnerabilities to be processed.
 * @param filter The suppression filter that defines filtering criteria such as release, tags, reporter, and the current date for expiration checks.
 * @return A map where the keys are the reporter types, and the values are lists of suppressed vulnerabilities that match the filter criteria.
 */
fun collectSuppressedVulnerabilities(
    vulnlogFile: VulnlogFile,
    filter: SuppressionFilter,
): Map<ReporterType, List<SuppressedVulnerability>> =
    vulnlogFile.vulnerabilities
        .asSequence()
        .applyFilter(filter.filter)
        .flatMap { vulnerability -> collectEligibleReports(vulnerability, filter.today) }
        .groupBy { it.reports.reporter }
        .filter { filter.filter.reporter == null || it.key == filter.filter.reporter }

private fun collectEligibleReports(
    vulnerability: VulnerabilityEntry,
    today: LocalDate,
): List<SuppressedVulnerability> {
    if (vulnerability.resolution != null) {
        return emptyList()
    }

    val eligibleReports =
        when (vulnerability.verdict) {
            is Verdict.NotAffected, is Verdict.RiskAcceptable -> vulnerability.reports
            else ->
                vulnerability.reports
                    .filter { it.suppress != null }
                    .filter { it.suppress?.expiresAt?.isAfter(today) ?: true }
        }

    return eligibleReports.map { report ->
        SuppressedVulnerability(
            id = vulnerability.id,
            releases = vulnerability.releases,
            reports = report,
            tags = vulnerability.tags,
            analysis = vulnerability.analysis ?: "",
        )
    }
}

/**
 * Maps target reporters to their corresponding suppression outputs based on default settings
 * and the list of suppressed vulnerabilities. Only reporters with suppressible default
 * settings are processed.
 *
 * @param targetReporters A set of `ReporterType` enumeration values representing the reporters
 *                        for which suppression mappings should be generated.
 * @param reporterToSuppressions A map where the keys are `ReporterType` enumeration values
 *                                and the values are lists of `SuppressedVulnerability` objects
 *                                associated with those reporters.
 * @return A set of `SuppressionOutput` objects that represent the suppression configurations
 *         for the specified target reporters.
 */
fun mapToSuppression(
    targetReporters: Set<ReporterType>,
    reporterToSuppressions: Map<ReporterType, List<SuppressedVulnerability>>,
): Set<SuppressionOutput> =
    targetReporters
        .map { reporter ->
            getDefaultSettingsForReporter(reporter) to (reporterToSuppressions[reporter] ?: emptyList())
        }.filter { (defaults, _) -> defaults is Suppressable }
        .map { (defaults, suppressions) -> createSuppression(defaults as Suppressable, suppressions) }
        .toSet()

private fun getDefaultSettingsForReporter(reporter: ReporterType): Suppression =
    when (reporter) {
        ReporterType.DEPENDENCY_CHECK -> Suppressable.GenericFormat.Generic(reporter)
        ReporterType.GITHUB_SECURITY_ADVISORY -> Suppressable.GenericFormat.Generic(reporter)
        ReporterType.GRYPE -> Suppressable.GenericFormat.Generic(reporter)
        ReporterType.NPM_AUDIT -> Suppressable.GenericFormat.Generic(reporter)
        ReporterType.OTHER -> NotSuppressable
        ReporterType.RUST_AUDIT -> Suppressable.GenericFormat.Generic(reporter)
        ReporterType.SEMGREP -> Suppressable.GenericFormat.Generic(reporter)
        ReporterType.SNYK -> Suppressable.NativeFormat.Snyk
        ReporterType.TRIVY -> Suppressable.NativeFormat.Trivy
    }

private fun createSuppression(
    defaults: Suppressable,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput =
    when (defaults) {
        is Suppressable.GenericFormat.Generic -> createGenericSuppression(defaults, suppressions)
        is Suppressable.NativeFormat.Trivy -> createTrivySuppression(defaults, suppressions)
        is Suppressable.NativeFormat.Snyk -> createSnykSuppression(defaults, suppressions)
    }

private fun createGenericSuppression(
    defaults: Suppressable.GenericFormat.Generic,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .flatMap { entry ->
                resolveVulnIds(entry, defaults).map { id ->
                    SuppressionVuln.GenericSuppressionEntry(
                        id = id,
                        expiresAt = entry.reports.suppress?.expiresAt,
                        reason = entry.analysis,
                    )
                }
            }.toSet()
    return SuppressionOutput.GenericSuppression(
        fileName = defaults.reporter.name.lowercase() + ".generic.json",
        entries = entries,
    )
}

private fun createTrivySuppression(
    defaults: Suppressable,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .flatMap { entry ->
                resolveVulnIds(entry, defaults).map { id ->
                    SuppressionVuln.TrivySuppressionEntry(
                        id = id,
                        expiresAt = entry.reports.suppress?.expiresAt,
                        reason = entry.analysis,
                    )
                }
            }.toSet()
    return SuppressionOutput.TrivySuppression(entries = entries)
}

private fun createSnykSuppression(
    defaults: Suppressable,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .flatMap { entry ->
                resolveVulnIds(entry, defaults).map { id ->
                    SuppressionVuln.SnykSuppressionEntry(
                        id = id,
                        expiresAt = entry.reports.suppress?.expiresAt,
                        reason = entry.analysis,
                    )
                }
            }.toSet()
    return SuppressionOutput.SnykSuppression(entries = entries)
}

private fun resolveVulnIds(
    entry: SuppressedVulnerability,
    defaults: Suppressable,
): Set<VulnId> {
    val reporterSpecific =
        entry.reports.vulnIds
            .filter { it::class in defaults.vulnIdTypes }
            .toSet()
    return reporterSpecific.ifEmpty {
        if (entry.id::class in defaults.vulnIdTypes) setOf(entry.id) else emptySet()
    }
}
