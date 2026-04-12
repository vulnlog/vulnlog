package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.ReporterType

/**
 * Parses a reporter string and returns the corresponding ReporterType enumeration value.
 *
 * @param reporter The string identifier of the reporter, expected to match one of the predefined reporter names.
 *                 Supported values include: "dependency-check", "github_advisory", "grype", "npm_audit",
 *                 "other", "cargo-audit", "semgrep", "snyk", and "trivy".
 * @return The corresponding ReporterType enumeration value for the specified reporter.
 * @throws IllegalArgumentException If the specified reporter is not supported.
 */
fun parseReporter(reporter: String): ReporterType =
    when (reporter) {
        "dependency-check" -> ReporterType.DEPENDENCY_CHECK
        "github_advisory" -> ReporterType.GITHUB_SECURITY_ADVISORY
        "grype" -> ReporterType.GRYPE
        "npm_audit" -> ReporterType.NPM_AUDIT
        "other" -> ReporterType.OTHER
        "cargo-audit" -> ReporterType.CARGO_AUDIT
        "semgrep" -> ReporterType.SEMGREP
        "snyk" -> ReporterType.SNYK
        "trivy" -> ReporterType.TRIVY
        else -> throw IllegalArgumentException("Unsupported reporter: $reporter")
    }

/**
 * Converts a ReporterType value to its canonical string representation.
 *
 * @return The canonical string representation of the ReporterType value. Each ReporterType
 *         corresponds to a specific lowercase string identifier.
 */
fun ReporterType.canonical(): String =
    when (this) {
        ReporterType.DEPENDENCY_CHECK -> "dependency-check"
        ReporterType.GITHUB_SECURITY_ADVISORY -> "github_advisory"
        ReporterType.GRYPE -> "grype"
        ReporterType.NPM_AUDIT -> "npm_audit"
        ReporterType.OTHER -> "other"
        ReporterType.CARGO_AUDIT -> "cargo-audit"
        ReporterType.SEMGREP -> "semgrep"
        ReporterType.SNYK -> "snyk"
        ReporterType.TRIVY -> "trivy"
    }
