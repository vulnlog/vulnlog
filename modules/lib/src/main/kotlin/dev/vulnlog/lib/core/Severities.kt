// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Severity

fun canonical(severity: Severity): String =
    when (severity) {
        Severity.LOW -> "low"
        Severity.MEDIUM -> "medium"
        Severity.HIGH -> "high"
        Severity.CRITICAL -> "critical"
    }

/** Sort key placing the most severe first and an absent severity last. */
fun severityOrder(severity: Severity?): Int =
    when (severity) {
        Severity.CRITICAL -> 0
        Severity.HIGH -> 1
        Severity.MEDIUM -> 2
        Severity.LOW -> 3
        null -> 4
    }

fun severityTokens(): String = Severity.entries.joinToString(", ") { canonical(it) }
