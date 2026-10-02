// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.finding.ParseFailure
import dev.vulnlog.lib.finding.ValidationFinding

sealed interface InputRejected : ValidationOutcome<Nothing> {
    /** A stage could not produce the next representation of the document. */
    data class Unparsable(
        val problems: List<ParseFailure>,
        override val findings: List<ValidationFinding>,
    ) : InputRejected

    /** Every stage produced its representation, but at least one rule refused the result. */
    data class Invalid(
        override val findings: List<ValidationFinding>,
    ) : InputRejected
}
