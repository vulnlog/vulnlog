// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.cyclonedx

import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaseline
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaselineProblem

sealed interface CycloneDxVexBaselineResult {
    data class Parsed(
        val baseline: CycloneDxVexBaseline,
    ) : CycloneDxVexBaselineResult

    data class Rejected(
        val problem: CycloneDxVexBaselineProblem,
    ) : CycloneDxVexBaselineResult
}
