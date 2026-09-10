// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

import dev.vulnlog.lib.model.VexJustification

/**
 * The status of one vulnerability in one product, shared by every VEX format.
 * Each variant carries the fields its status requires, so a statement missing a mandatory field cannot be built, and
 * the free text each status may carry, so no consumer has to guess which text belongs to which status.
 */
sealed interface VexStatus {
    /** Not yet triaged. */
    data class UnderInvestigation(
        /**
         * What is known so far, when the entry records an analysis.
         */
        val statusNotes: String? = null,
    ) : VexStatus

    /** The vulnerability was remediated in this product. Carries no analysis text: the fix speaks for itself. */
    data object Fixed : VexStatus

    /** The vulnerable component is present but the product is not impacted. */
    data class NotAffected(
        /**
         * The machine-readable reason. Consumers act on this label.
         */
        val justification: VexJustification,
        /**
         * The analysis behind the label, for people reading the document.
         */
        val impactStatement: String? = null,
    ) : VexStatus

    /** The vulnerability impacts this product. */
    data class Affected(
        /**
         * What a consumer of the product should do.
         */
        val actionStatement: String,
        /**
         * How the status was determined, when the entry records an analysis.
         */
        val statusNotes: String? = null,
    ) : VexStatus
}
