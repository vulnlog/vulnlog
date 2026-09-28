// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope

/** The scope a request resolved to, or the problems that stopped it resolving. */
sealed interface OpenVexScopeResult {
    /** Every requested release and tag exists in the file. */
    data class Resolved(
        val scope: OpenVexScope,
    ) : OpenVexScopeResult

    /** The request names a release or a tag the file does not define. */
    data class Rejected(
        val problems: List<FilterProblem>,
    ) : OpenVexScopeResult
}
