package io.vulnlog.dsl

import io.vulnlog.dsl.definition.VlDslMarker

interface VlActionContextOverwrite : VlDslMarker {
    /**
     * Do not apply any action.
     */
    fun noAction(): VlOverwriteBuilder

    /**
     * 	Suppress the reported vulnerability until the fixed fixIn versions release date for the matching branch has
     * 	arrived. Requires fixIn version for the matching branch.
     */
    fun suppressUntilNextReleaseInBranch(): VlOverwriteBuilder

    /**
     * Suppress the reported vulnerability.
     */
    fun suppressPermanent(): VlOverwriteBuilder

    /**
     * Suppress the reported vulnerability temporarily until and including the specified date (YYYY-MM-dd).
     */
    fun suppressTemporarily(untilDate: String): VlOverwriteBuilder
}
