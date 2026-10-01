// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.finding.Rule
import dev.vulnlog.lib.model.finding.ValidationFinding

val v1DtoRules =
    listOf(
        ::validateNoDeprecatedVerdict,
    )

private fun validateNoDeprecatedVerdict(dto: VulnlogFileV1Dto): List<ValidationFinding> =
    dto.vulnerabilities
        .filter { entry -> entry.verdict == "risk acceptable" }
        .map { entry ->
            ValidationFinding(
                severity = FindingSeverity.WARNING,
                rule = Rule.DEPRECATED_VERDICT,
                path = "vulnerabilities[${entry.id}].verdict",
                message =
                    "Deprecated verdict 'risk acceptable'; use verdict 'affected' with disposition 'wont fix' " +
                        "instead. The legacy value is accepted until the 1.0 release.",
            )
        }
