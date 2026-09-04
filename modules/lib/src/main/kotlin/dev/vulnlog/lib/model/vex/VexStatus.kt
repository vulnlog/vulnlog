// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

import dev.vulnlog.lib.model.VexJustification

/**
 * The status of one vulnerability in one product, shared by every VEX format.
 * Each variant carries the field its status requires, so a statement missing a mandatory field cannot be built.
 */
sealed interface VexStatus {
    /** Not yet triaged. */
    data object UnderInvestigation : VexStatus

    /** The vulnerability was remediated in this product. */
    data object Fixed : VexStatus

    /** The vulnerable component is present but the product is not impacted. */
    data class NotAffected(
        val justification: VexJustification,
    ) : VexStatus

    /** The vulnerability impacts this product. */
    data class Affected(
        val actionStatement: String,
    ) : VexStatus
}
