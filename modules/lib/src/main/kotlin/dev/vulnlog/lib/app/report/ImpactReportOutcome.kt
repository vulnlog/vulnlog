// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.model.reporting.ImpactEntry

sealed interface ImpactReportOutcome {
    sealed interface Failed : ImpactReportOutcome

    data class Generated(
        val report: ImpactReport,
        /** Before merging, so the messages can tell how many rows the merge saved. */
        val collected: List<ImpactEntry>,
        val content: String,
    ) : ImpactReportOutcome
}
