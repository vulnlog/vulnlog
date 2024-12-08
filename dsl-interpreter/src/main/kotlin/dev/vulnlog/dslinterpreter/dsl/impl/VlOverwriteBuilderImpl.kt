package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlFixActionTargetDependencyBuilder
import dev.vulnlog.dsl.VlFixActionTargetVersionBuilder
import dev.vulnlog.dsl.VlNoActionValue
import dev.vulnlog.dsl.VlOverwriteBuilder
import dev.vulnlog.dsl.VlOverwriteValue
import dev.vulnlog.dsl.VlRatingValue
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReportByValue
import dev.vulnlog.dsl.VlReporterValue
import dev.vulnlog.dsl.VlSuppressionBuilder
import dev.vulnlog.dsl.VlVariantValue
import java.time.LocalDate

@Suppress("TooManyFunctions")
internal class VlOverwriteBuilderImpl(
    private val variant: VlVariantValue,
    private val releases: Set<VlReleaseValue>,
) : VlOverwriteBuilder {
    private var reportBy: Set<VlReportByValue> = emptySet()
    private var rating: VlRatingValue? = null
    private var toFix: VlToFixActionBuilder? = null
    private var fixIn: Set<VlReleaseValue> = emptySet()
    private var noAction: VlNoActionValue? = null
    private var suppressionActionBuilder: VlSuppressionBuilderImpl? = null

    override fun reportBy(vararg reporters: VlReporterValue): VlOverwriteBuilder {
        reportBy = reporters.map(VlReporterValue::name).map(::VlReportByValueImpl).toSet()
        return this
    }

    override fun critical(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder {
        val parsedDateOfAnalysing: LocalDate = LocalDate.parse(dateOfAnalysing)
        rating = VlRatingCriticalValueImpl(parsedDateOfAnalysing, reasoning)
        return this
    }

    override fun high(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder {
        val parsedDateOfAnalysing: LocalDate = LocalDate.parse(dateOfAnalysing)
        rating = VlRatingHighValueImpl(parsedDateOfAnalysing, reasoning)
        return this
    }

    override fun moderate(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder {
        val parsedDateOfAnalysing: LocalDate = LocalDate.parse(dateOfAnalysing)
        rating = VlRatingModerateValueImpl(parsedDateOfAnalysing, reasoning)
        return this
    }

    override fun low(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder {
        val parsedDateOfAnalysing: LocalDate = LocalDate.parse(dateOfAnalysing)
        rating = VlRatingLowValueImpl(parsedDateOfAnalysing, reasoning)
        return this
    }

    override fun notAffected(
        dateOfAnalysing: String,
        reasoning: String,
    ): VlOverwriteBuilder {
        val parsedDateOfAnalysing: LocalDate = LocalDate.parse(dateOfAnalysing)
        rating = VlRatingNotAffectedValueImpl(parsedDateOfAnalysing, reasoning)
        return this
    }

    override fun update(dependency: String): VlFixActionTargetVersionBuilder {
        val fixAction = VlToFixActionTargetVersionBuilder("update", dependency)
        toFix = fixAction
        return fixAction
    }

    override fun remove(dependency: String): VlOverwriteBuilder {
        val fixAction = VlToFixRemoveActionBuilder("remove $dependency")
        toFix = fixAction
        return this
    }

    override fun replace(dependency: String): VlFixActionTargetDependencyBuilder {
        val fixAction = VlToFixActionTargetDependencyBuilder("replace", dependency)
        toFix = fixAction
        return fixAction
    }

    override fun fixIn(vararg versions: VlReleaseValue): VlOverwriteBuilder {
        fixIn = versions.toSet()
        return this
    }

    override fun noAction(): VlOverwriteBuilder {
        noAction = VlNoActionValueImpl()
        return this
    }

    override fun suppressUntilNextReleaseInBranch(): VlSuppressionBuilder {
        val suppressionBuilder = VlSuppressionBuilderImpl()
        suppressionActionBuilder = suppressionBuilder
        return suppressionBuilder
    }

    override fun suppressPermanent(): VlSuppressionBuilder {
        val suppressionBuilder = VlSuppressionBuilderImpl()
        suppressionActionBuilder = suppressionBuilder
        return suppressionBuilder
    }

    override fun suppressTemporarily(untilDate: String): VlSuppressionBuilder {
        val suppressionBuilder = VlSuppressionBuilderImpl()
        suppressionActionBuilder = suppressionBuilder
        return suppressionBuilder
    }

    fun build(): VlOverwriteValue {
        val reportedFor = releases.map { VlReportForValueImpl(variant, it) }.toSet()
        return VlOverwriteValueImpl(
            reportedFor,
            reportBy,
            rating,
            toFix?.build(),
            fixIn,
            noAction,
            suppressionActionBuilder?.build(),
        )
    }
}
