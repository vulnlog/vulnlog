// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import java.time.Instant
import java.time.LocalDate

/**
 * When the information a statement expresses became true, and where that time comes from.
 *
 * The specification requires that an untouched statement keeps its time when a document is revised, so a time the
 * file does not state is taken from the revision that first issued the statement and carried from then on.
 */
sealed interface OpenVexStatementTime {
    /** The day the Vulnlog file states. */
    data class Stated(
        val date: LocalDate,
    ) : OpenVexStatementTime

    /** The time an equal statement carries in the baseline, kept so a revision does not re-date it. */
    data class Carried(
        val at: Instant,
    ) : OpenVexStatementTime

    /** Neither the file nor the baseline dates the statement: it is dated by the revision that issues it. */
    data object Issued : OpenVexStatementTime
}
