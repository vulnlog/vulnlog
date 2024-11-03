package io.vulnlog.dsl2

import io.vulnlog.dsl2.definition.VlDslMarker

interface VlActionContext : VlDslMarker {
    /**
     * Do not apply any action.
     */
    fun noAction(): VlNoActionValue

    /**
     * 	Suppress the reported vulnerability until the fixed fixIn versions release date for the matching branch has
     * 	arrived. Requires fixIn version for the matching branch.
     */
    fun suppressUntilNextReleaseInBranch(): VlSuppressionBuilder

    /**
     * Suppress the reported vulnerability.
     */
    fun suppressPermanent(): VlSuppressionBuilder

    /**
     * Suppress the reported vulnerability temporarily until and including the specified date (YYYY-MM-dd).
     */
    fun suppressTemporarily(untilDate: String): VlSuppressionBuilder
}
