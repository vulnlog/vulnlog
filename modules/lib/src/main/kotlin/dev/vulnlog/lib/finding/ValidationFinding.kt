// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.finding

data class ValidationFinding(
    val severity: FindingSeverity,
    val rule: Rule,
    val path: String,
    val message: String,
    val location: FailureLocation? = null,
)

val List<ValidationFinding>.errors: List<ValidationFinding>
    get() = filter { it.severity == FindingSeverity.ERROR }

val List<ValidationFinding>.highestSeverity: FindingSeverity
    get() = maxOfOrNull { it.severity } ?: FindingSeverity.INFO
