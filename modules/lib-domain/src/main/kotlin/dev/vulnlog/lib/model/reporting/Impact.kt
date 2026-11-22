// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.reporting

import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.VexJustification

sealed interface Impact {
    data object Unknown : Impact

    data class Affected(
        val severity: Severity,
    ) : Impact

    data class NotAffected(
        val justification: VexJustification,
    ) : Impact
}
