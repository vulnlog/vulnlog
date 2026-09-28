// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem

/** What parsing a baseline produced: a baseline to continue, or why it cannot be continued. */
sealed interface OpenVexBaselineResult {
    /** An OpenVEX document in the required format version with an identity to continue. */
    data class Parsed(
        val baseline: OpenVexBaseline,
    ) : OpenVexBaselineResult

    /** A baseline that cannot be continued, for the caller to reject. */
    data class Rejected(
        val problem: OpenVexBaselineProblem,
    ) : OpenVexBaselineResult
}
