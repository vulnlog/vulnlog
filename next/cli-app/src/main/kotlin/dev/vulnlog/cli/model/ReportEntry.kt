package dev.vulnlog.cli.model

import java.time.LocalDate

enum class ReporterType {
    DEPENDENCY_CHECK,
    GITHUB_SECURITY_ADVISORY,
    GRYPE,
    NPM_AUDIT,
    OTHER,
    RUST_AUDIT,
    SEMGREP,
    SNYK,
    TRIVY,
}

data class ReportEntry(
    val reporter: ReporterType,
    val at: LocalDate? = null,
)
