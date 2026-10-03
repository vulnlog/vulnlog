// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.core.formatFinding
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.finding.FormatFinding

fun formatCommentsDroppedWarning(source: String): String =
    formatFinding(FindingSeverity.WARNING, source, message = "contains YAML comments; they are removed on write") +
        "\n" + formatHint("record notes in schema fields (e.g. comment, analysis)")

fun renderFormatFinding(finding: FormatFinding): String {
    val ruleName = finding.rule.name
    val id = ruleName.lowercase().replace('_', '-')
    return if (finding.path.isEmpty()) "[$id] ${finding.message}" else "[$id] ${finding.path}: ${finding.message}"
}
