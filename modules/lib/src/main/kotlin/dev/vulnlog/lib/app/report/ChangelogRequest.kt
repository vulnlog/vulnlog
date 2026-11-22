// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.model.reporting.ChangelogDetail

/** Configuration for the changelog report as requested by the caller. */
data class ChangelogRequest(
    val filter: FilterRequest,
    val format: ChangelogFormat,
    val detail: ChangelogDetail,
)
