// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.model.vex.VexBaselineRead

// Shared by every VEX format: reading the baseline file is the same whatever it contains.

fun renderVexBaselineFailure(
    unavailable: VexBaselineRead.Unavailable,
    baseline: String,
    baselineOption: String,
): Failure =
    when (unavailable) {
        VexBaselineRead.Absent -> Failure("baseline '$baseline' does not exist", omitBaselineHint(baselineOption))

        is VexBaselineRead.Unreadable ->
            Failure(
                "cannot read baseline '$baseline': ${unavailable.reason}",
                "pass a readable file to $baselineOption, or omit it to issue a new document",
            )
    }

fun renderVexNewDocument(baseline: String): Message =
    Message.Verbose("baseline '$baseline' does not exist yet, issuing a new document")

internal fun omitBaselineHint(baselineOption: String): String = "omit $baselineOption to issue a new document"
