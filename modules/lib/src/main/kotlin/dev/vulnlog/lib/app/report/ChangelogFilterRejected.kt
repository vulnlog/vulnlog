// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app.report

import dev.vulnlog.lib.core.filter.FilterProblem

data class ChangelogFilterRejected(
    val problems: List<FilterProblem>,
) : ChangelogOutcome.Failed
