// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.result

import dev.vulnlog.lib.model.validation.FailureLocation
import dev.vulnlog.lib.shell.FileInputOption

enum class Severity {
    /**
     * Observations that help the user improve their file.
     */
    INFO,

    /**
     * File is technically valid, but something is likely wrong or will cause problems in the future.
     */
    WARNING,

    /**
     * Indicate structurally invalid or semantically broken files that would produce incorrect output.
     */
    ERROR,
}

enum class Rule {
    ANALYZED_BEFORE_REPORTED,
    DANGLING_RELEASE_REFERENCE,
    DANGLING_TAG_REFERENCE,
    DUPLICATE_RELEASE_ID,
    DUPLICATE_TAG_ID,
    DUPLICATE_VULNERABILITY_ID,
    MISSING_REPORTER_INFORMATION,
    UNREFERENCED_RELEASE_ID,
    UNREFERENCED_TAG_ID,
}

data class ValidationFinding(
    val severity: Severity,
    val rule: Rule,
    val path: String,
    val message: String,
    /** Source position of the finding; none of the current rules resolve one yet. */
    val location: FailureLocation? = null,
)

data class ValidationResult(
    val findings: List<ValidationFinding>,
) {
    val errors: List<ValidationFinding> get() = findings.filter { it.severity == Severity.ERROR }
    val warnings: List<ValidationFinding> get() = findings.filter { it.severity == Severity.WARNING }
    val infos: List<ValidationFinding> get() = findings.filter { it.severity == Severity.INFO }
}

/** Findings per input file; rendering happens at the edges so tooling can consume the data. */
data class ValidationResults(
    val fileFindings: Map<FileInputOption, ValidationResult>,
) {
    val hasErrors: Boolean get() = fileFindings.values.any { it.errors.isNotEmpty() }
    val hasWarnings: Boolean get() = fileFindings.values.any { it.warnings.isNotEmpty() }
}
