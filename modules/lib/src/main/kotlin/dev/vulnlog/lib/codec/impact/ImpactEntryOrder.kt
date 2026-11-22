// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.impact

import dev.vulnlog.lib.core.reporting.severityOf
import dev.vulnlog.lib.core.severityOrder
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState

/** The order every view of the impact report lists its entries in: by state, then most severe first. */
internal val impactEntryOrder: Comparator<ImpactEntry> =
    compareBy<ImpactEntry> { rankState(it.state) }
        .thenBy { severityOrder(severityOf(it.impact)) }
        .thenBy { it.primaryId.id }

/** The states in report order, for views that list or count by state. */
internal val reportStates: List<WorkState> = WorkState.entries.sortedBy(::rankState)

private fun rankState(state: WorkState): Int =
    when (state) {
        WorkState.OPEN -> 0
        WorkState.UNDER_INVESTIGATION -> 1
        WorkState.ACCEPTED -> 2
        WorkState.RESOLVED -> 3
        WorkState.NOT_APPLICABLE -> 4
    }
