// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.finding.Rule
import dev.vulnlog.lib.model.finding.ValidationFinding

fun finding(
    severity: FindingSeverity,
    rule: Rule = Rule.UNREFERENCED_RELEASE_ID,
    path: String = "fixture path",
    message: String = "fixture message",
): ValidationFinding = ValidationFinding(severity = severity, rule = rule, path = path, message = message)
