// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.cyclonedx

import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope

sealed interface CycloneDxVexScopeResult {
    data class Resolved(
        val scope: CycloneDxVexScope,
    ) : CycloneDxVexScopeResult

    data class Rejected(
        val problems: List<FilterProblem>,
    ) : CycloneDxVexScopeResult
}
