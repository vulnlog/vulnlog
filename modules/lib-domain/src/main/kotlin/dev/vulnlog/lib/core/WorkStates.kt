// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Resolution
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VulnerabilityEntry
import dev.vulnlog.lib.model.reporting.WorkState

fun WorkState.canonical(): String =
    when (this) {
        WorkState.UNDER_INVESTIGATION -> "under investigation"
        WorkState.OPEN -> "open"
        WorkState.ACCEPTED -> "accepted"
        WorkState.RESOLVED -> "resolved"
        WorkState.NOT_APPLICABLE -> "not applicable"
    }

fun workStateTokens(): String = WorkState.entries.joinToString(", ") { it.canonical() }

fun findWorkState(vulnEntry: VulnerabilityEntry): WorkState =
    when (val verdict = vulnEntry.verdict) {
        Verdict.UnderInvestigation -> WorkState.UNDER_INVESTIGATION
        is Verdict.Affected -> findAffectedWorkState(vulnEntry.resolution, verdict)
        is Verdict.NotAffected -> findNotAffectedWorkState(vulnEntry.resolution)
    }

private fun findAffectedWorkState(
    resolution: Resolution?,
    verdict: Verdict.Affected,
): WorkState =
    when {
        resolution != null -> WorkState.RESOLVED
        verdict.disposition == Disposition.WONT_FIX -> WorkState.ACCEPTED
        else -> WorkState.OPEN
    }

private fun findNotAffectedWorkState(resolution: Resolution?): WorkState =
    if (resolution != null) WorkState.RESOLVED else WorkState.NOT_APPLICABLE
