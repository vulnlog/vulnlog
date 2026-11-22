// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.model.reporting.ChangelogDetail
import java.time.Instant

/** Configuration for the changelog report as requested by the caller. */
data class ChangelogRequest(
    val filter: FilterRequest,
    val format: ChangelogFormat,
    /** Only the text and markdown formats read it. JSON always holds every field. */
    val detail: ChangelogDetail,
    val generatedAt: Instant,
    val vulnlogVersion: String,
)
