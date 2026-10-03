// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.core.filter.FilterProblem

data class FilterRejected(
    val problems: List<FilterProblem>,
) : OpenVexOutcome.Failed,
    SuppressionOutcome.Failed,
    ImpactReportOutcome.Failed
