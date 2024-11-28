package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlFixActionTargetVersionBuilder
import dev.vulnlog.dsl.VlOverwriteBuilder
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReporterValue

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
        VlToFixActionTargetVersionBuilder("update", dependency)

    override fun remove(dependency: String): VlOverwriteBuilder = this

    override fun replace(dependency: String): VlOverwriteBuilder = this

    override fun fixIn(vararg versions: VlReleaseValue): VlOverwriteBuilder = this

    override fun noAction(): VlOverwriteBuilder = this

    override fun suppressUntilNextReleaseInBranch(): VlOverwriteBuilder = this

    override fun suppressPermanent(): VlOverwriteBuilder = this

    override fun suppressTemporarily(untilDate: String): VlOverwriteBuilder = this
}
