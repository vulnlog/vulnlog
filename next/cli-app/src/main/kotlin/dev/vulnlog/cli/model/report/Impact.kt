// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.model.report

import dev.vulnlog.cli.model.Severity

sealed interface Impact {
    data object Unknown : Impact

    data class Affected(
        val severity: Severity,
    ) : Impact

    data class AcceptableRisk(
        val severity: Severity,
    ) : Impact

    data class NotAffected(
        val reason: String,
    ) : Impact
}
