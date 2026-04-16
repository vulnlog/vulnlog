// Copyright 2024 the Vulnlog contributors
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
    ) : Verdict

    /**
     * The vulnerability affects this release but the risk has been assessed and accepted.
     */
    data class RiskAcceptable(
        /**
         * Severity of the vulnerability.
         */
        val severity: Severity,
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
