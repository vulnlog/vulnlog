// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.fixtures

import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.finding.Rule
import dev.vulnlog.lib.finding.ValidationFinding

fun finding(
    severity: FindingSeverity,
    rule: Rule = Rule.UNREFERENCED_RELEASE_ID,
    path: String = "fixture path",
    message: String = "fixture message",
): ValidationFinding = ValidationFinding(severity = severity, rule = rule, path = path, message = message)
