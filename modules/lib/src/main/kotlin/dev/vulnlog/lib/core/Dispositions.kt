// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Verdict

fun parseDisposition(disposition: String): Disposition =
    when (disposition) {
        "will fix" -> Disposition.WILL_FIX
        "wont fix" -> Disposition.WONT_FIX
        else -> throw IllegalArgumentException("Invalid disposition: $disposition")
    }

fun canonical(disposition: Disposition): String =
    when (disposition) {
        Disposition.WILL_FIX -> "will fix"
        Disposition.WONT_FIX -> "wont fix"
    }

fun findDisposition(verdict: Verdict): Disposition? =
    when (verdict) {
        is Verdict.Affected -> verdict.disposition
        is Verdict.NotAffected, Verdict.UnderInvestigation -> null
    }
