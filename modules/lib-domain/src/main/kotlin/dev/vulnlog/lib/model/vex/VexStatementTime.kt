// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

import java.time.Instant
import java.time.LocalDate

/**
 * When a statement became true. An untouched statement keeps its time across revisions, so a time the file does not
 * state is taken from the revision that first issued the statement and carried from then on.
 */
sealed interface VexStatementTime {
    data class Stated(
        val date: LocalDate,
    ) : VexStatementTime

    data class Carried(
        val at: Instant,
    ) : VexStatementTime

    data object Issued : VexStatementTime
}
