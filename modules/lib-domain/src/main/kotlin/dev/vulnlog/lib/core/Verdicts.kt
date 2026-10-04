// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VerdictKind

fun Verdict.kind(): VerdictKind =
    when (this) {
        Verdict.UnderInvestigation -> VerdictKind.UNDER_INVESTIGATION
        is Verdict.Affected -> VerdictKind.AFFECTED
        is Verdict.NotAffected -> VerdictKind.NOT_AFFECTED
    }

fun VerdictKind.canonical(): String =
    when (this) {
        VerdictKind.UNDER_INVESTIGATION -> "under investigation"
        VerdictKind.AFFECTED -> "affected"
        VerdictKind.NOT_AFFECTED -> "not affected"
    }

fun verdictKindTokens(): String = VerdictKind.entries.joinToString(", ") { it.canonical() }
