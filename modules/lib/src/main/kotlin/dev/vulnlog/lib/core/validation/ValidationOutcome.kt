// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

import dev.vulnlog.lib.model.finding.ParseFailure
import dev.vulnlog.lib.model.finding.ValidationFinding

sealed interface ValidationOutcome<out T> {
    val findings: List<ValidationFinding>

    data class Ok<out T>(
        val project: T,
        override val findings: List<ValidationFinding>,
    ) : ValidationOutcome<T>

    sealed interface Stopped : ValidationOutcome<Nothing> {
        /** A stage could not produce the next representation of the document. */
        data class Unreadable(
            val problems: List<ParseFailure>,
            override val findings: List<ValidationFinding>,
        ) : Stopped

        /** Every stage produced its representation, but at least one rule refused the result. */
        data class Rejected(
            override val findings: List<ValidationFinding>,
        ) : Stopped
    }
}
