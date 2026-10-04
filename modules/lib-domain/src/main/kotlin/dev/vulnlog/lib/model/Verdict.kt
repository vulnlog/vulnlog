// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

sealed interface Verdict {
    /**
     * Not yet triaged.
     */
    data object UnderInvestigation : Verdict

    /**
     * The vulnerability affects this release.
     */
    data class Affected(
        /**
         * Severity of the vulnerability.
         */
        val severity: Severity,
        /**
         * Stated remediation intent. Absence means the intent is not stated.
         */
        val disposition: Disposition? = null,
    ) : Verdict

    /**
     * The vulnerability does not affect this release.
     */
    data class NotAffected(
        /**
         * Justification for the verdict.
         */
        val justification: VexJustification,
    ) : Verdict
}
