// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.core.filter.FilterProblem

/**
 * Shared by the use cases whose request names releases or tags. A sealed interface only admits subtypes from its own
 * package, which is why every use case and its outcome live in this package.
 */
data class FilterRejected(
    val problems: List<FilterProblem>,
) : OpenVexOutcome
