// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.core.filter.FilterRequest
import java.time.Instant

/** Configuration for the impact report as requested by the caller. */
data class ImpactReportRequest(
    val filter: FilterRequest,
    val generatedAt: Instant,
    val vulnlogVersion: String,
    val format: ImpactReportFormat = ImpactReportFormat.HTML,
)
