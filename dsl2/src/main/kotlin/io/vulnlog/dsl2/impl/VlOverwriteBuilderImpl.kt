package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionTargetVersionBuilder
import io.vulnlog.dsl2.VlOverwriteBuilder
import io.vulnlog.dsl2.VlReleaseValue
import io.vulnlog.dsl2.VlReporterValue

@Suppress("TooManyFunctions")
class VlOverwriteBuilderImpl : VlOverwriteBuilder {
    override fun reportBy(vararg reporters: VlReporterValue): VlOverwriteBuilder = this

    override fun critical(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder = this

    override fun high(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder = this

    override fun moderate(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder = this

    override fun low(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder = this

    override fun notAffected(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder = this

    override fun update(dependency: String): VlFixActionTargetVersionBuilder =
        VlFixActionTargetVersionBuilderImpl("update", dependency)

    override fun remove(dependency: String): VlOverwriteBuilder = this

    override fun replace(dependency: String): VlOverwriteBuilder = this

    override fun fixIn(vararg versions: VlReleaseValue): VlOverwriteBuilder = this

    override fun noAction(): VlOverwriteBuilder = this

    override fun suppressUntilNextReleaseInBranch(): VlOverwriteBuilder = this

    override fun suppressPermanent(): VlOverwriteBuilder = this

    override fun suppressTemporarily(untilDate: String): VlOverwriteBuilder = this
}
