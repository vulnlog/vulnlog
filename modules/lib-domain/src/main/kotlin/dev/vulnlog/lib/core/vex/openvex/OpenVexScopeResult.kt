// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope

sealed interface OpenVexScopeResult {
    data class Resolved(
        val scope: OpenVexScope,
    ) : OpenVexScopeResult

    data class Rejected(
        val problems: List<FilterProblem>,
    ) : OpenVexScopeResult
}
