// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.reporting

import dev.vulnlog.lib.model.Severity

/** How many vulnerabilities a release fixed, in total and per severity, most severe first. */
data class ChangelogSummary(
    val total: Int = 0,
    val bySeverity: Map<Severity, Int> = emptyMap(),
)
