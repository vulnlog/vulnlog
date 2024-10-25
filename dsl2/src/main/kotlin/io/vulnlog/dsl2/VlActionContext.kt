package io.vulnlog.dsl2

import io.vulnlog.dsl2.definition.VlDslMarker

interface VlActionContext<out T> : VlDslMarker {
    /**
     * Do not apply any action.
     */
    fun noAction(): T

    /**
     * 	Suppress the reported vulnerability until the fixed fixIn versions release date for the matching branch has
     * 	arrived. Requires fixIn version for the matching branch.
     */
    fun suppressUntilNextReleaseInBranch(): T

    /**
     * Suppress the reported vulnerability.
     */
    fun suppressPermanent(): T

    /**
     * Suppress the reported vulnerability temporarily until and including the specified date (YYYY-MM-dd).
     */
    fun suppressTemporarily(untilDate: String): T
}
