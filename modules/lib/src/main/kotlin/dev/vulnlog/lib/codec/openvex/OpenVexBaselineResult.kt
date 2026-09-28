// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem

sealed interface OpenVexBaselineResult {
    data class Parsed(
        val baseline: OpenVexBaseline,
    ) : OpenVexBaselineResult

    data class Rejected(
        val problem: OpenVexBaselineProblem,
    ) : OpenVexBaselineResult
}
