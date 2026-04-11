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

/**
 * Collects and filters suppressed vulnerabilities from a given Vulnlog file based on the specified suppression
 * filter criteria. The vulnerabilities are grouped by their reporter type.
 *
 * @param vulnlogFile The Vulnlog file containing vulnerability records to analyze.
 * @param filter The suppression filter to apply for selecting and grouping vulnerabilities.
 * @return A map where the keys are the reporter types, and the values are lists of suppressed vulnerabilities
 *         associated with each reporter.
 */
fun collectSuppressedVulnerabilities(
    vulnlogFile: VulnlogFile,
    filter: SuppressionFilter,
): Map<ReporterType, List<SuppressedVulnerability>> =
    vulnlogFile.vulnerabilities
        .asSequence()
        .flatMap { explodeAndMapToSuppressedVulnerabilities(it, filter.today) }
        .applyFilter(filter)
        .groupBy { it.reporter }
        .filter { filter.filter.reporter == null || it.key == filter.filter.reporter }

private fun explodeAndMapToSuppressedVulnerabilities(
    vulnerability: VulnerabilityEntry,
    today: LocalDate,
): List<SuppressedVulnerability> {
    if (vulnerability.verdict is Verdict.Affected && vulnerability.resolution != null) {
        return emptyList()
    }
    return vulnerability.reports.flatMap { report ->
        if (!isReportEligible(vulnerability.verdict, report.suppress, today)) {
            return@flatMap emptyList()
        }
        val ids: Collection<VulnId> = report.vulnIds.ifEmpty { listOf(vulnerability.id) }
        ids.map { id ->
            SuppressedVulnerability(
                id = id,
                releases = vulnerability.releases,
                reporter = report.reporter,
                expiresAt = report.suppress?.expiresAt,
                tags = vulnerability.tags,
                analysis = vulnerability.analysis ?: "",
            )
        }
    }
}

private fun isReportEligible(
    verdict: Verdict,
    suppress: dev.vulnlog.cli.model.Suppression?,
    today: LocalDate,
): Boolean {
    if (verdict is Verdict.NotAffected) return true
    if (suppress == null) return false
    val expiresAt = suppress.expiresAt ?: return true
    return !expiresAt.isBefore(today)
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
            .filter { suppression -> suppression.id::class in defaults.vulnIdTypes }
            .map { suppression ->
                SuppressionVuln.GenericSuppressionEntry(
                    id = suppression.id,
                    expiresAt = suppression.expiresAt,
                    reason = suppression.analysis,
                )
            }.toSet()
    return SuppressionOutput.GenericSuppression(
        fileName = defaults.reporter.name.lowercase() + ".generic.json",
        entries = entries,
    )
}

private fun createTrivySuppression(
    defaults: Suppressable.NativeFormat.Trivy,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .filter { suppression -> suppression.id::class in defaults.vulnIdTypes }
            .map { suppression ->
                SuppressionVuln.TrivySuppressionEntry(
                    id = suppression.id,
                    expiresAt = suppression.expiresAt,
                    reason = suppression.analysis,
                )
            }.toSet()
    return SuppressionOutput.TrivySuppression(entries = entries)
}

private fun createSnykSuppression(
    defaults: Suppressable.NativeFormat.Snyk,
    suppressions: List<SuppressedVulnerability>,
): SuppressionOutput {
    val entries =
        suppressions
            .filter { suppression -> suppression.id::class in defaults.vulnIdTypes }
            .map { suppression ->
                SuppressionVuln.SnykSuppressionEntry(
                    id = suppression.id,
                    expiresAt = suppression.expiresAt,
                    reason = suppression.analysis,
                )
            }.toSet()
    return SuppressionOutput.SnykSuppression(entries = entries)
}
