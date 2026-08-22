// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

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
