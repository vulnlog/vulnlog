// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.model

import java.time.LocalDate

enum class ReporterType {
    DEPENDENCY_CHECK,
    GITHUB_SECURITY_ADVISORY,
    GRYPE,
    NPM_AUDIT,
    OTHER,
    CARGO_AUDIT,
    SEMGREP,
    SNYK,
    TRIVY,
}

data class ReportEntry(
    /**
     * The scanner or source type. See [ReporterType].
     */
    val reporter: ReporterType,
    /**
     * Date the vulnerability was reported.
     */
    val at: LocalDate? = null,
    /**
     * Description of the report source. Optional for scanner reporters. Required by the CLI for reporter type other.
     */
    val source: String? = null,
    /**
     * Scanner-specific vulnerability identifiers. Used for scanner-specific suppression files.
     */
    val vulnIds: Set<VulnId> = emptySet(),
    /**
     * Scanner-specific suppression configuration. When present without [Suppression.expiresAt], the suppression is permanent.
     */
    val suppress: Suppression? = null,
)
