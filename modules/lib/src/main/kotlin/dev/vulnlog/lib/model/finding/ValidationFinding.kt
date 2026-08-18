// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.finding

/** One rule broken by one place in a Vulnlog file. */
data class ValidationFinding(
    val severity: FindingSeverity,
    val rule: Rule,
    val path: String,
    val message: String,
    val location: FailureLocation? = null,
)

val List<ValidationFinding>.errors: List<ValidationFinding>
    get() = filter { it.severity == FindingSeverity.ERROR }

val List<ValidationFinding>.warnings: List<ValidationFinding>
    get() = filter { it.severity == FindingSeverity.WARNING }

val List<ValidationFinding>.infos: List<ValidationFinding>
    get() = filter { it.severity == FindingSeverity.INFO }

val List<ValidationFinding>.highestSeverity: FindingSeverity
    get() = maxByOrNull { it.severity.ordinal }?.severity ?: FindingSeverity.INFO
