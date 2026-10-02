// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.finding.ValidationFinding

sealed interface ValidationOutcome<out T> {
    val findings: List<ValidationFinding>

    data class Ok<out T>(
        val project: T,
        override val findings: List<ValidationFinding>,
    ) : ValidationOutcome<T>
}
