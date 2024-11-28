package dev.vulnlog.dslinterpreter.dsl

import dev.vulnlog.dsl.VlBranchBuilder
import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlLifeCycleFromBuilder
import dev.vulnlog.dsl.VlLifeCycleToBuilder
import dev.vulnlog.dsl.VlLifeCycleValue
import dev.vulnlog.dsl.VlReleasePublishedValue
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dsl.VlReporterValue
import dev.vulnlog.dsl.VlVariantValue
import dev.vulnlog.dsl.VlVulnLogContextValue
import dev.vulnlog.dsl.VlVulnerabilityContext
import dev.vulnlog.dslinterpreter.dsl.impl.VlBranchBuilderImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlLifeCycleFromBuilderImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlLifeCycleValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReleasePublishedValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReleaseValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlReporterValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVariantValueImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityContextImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VlVulnerabilityIdImpl
import dev.vulnlog.dslinterpreter.dsl.impl.VulnlogFileData
import java.time.LocalDate

class VlVulnLogContextValueImpl : VlVulnLogContextValue {
    private val releases = mutableSetOf<VlReleaseValue>()
    private val publishedReleases = mutableSetOf<VlReleasePublishedValue>()
    private val branchBuilders = mutableListOf<VlBranchBuilderImpl>()
    private val productVariants = mutableSetOf<VlVariantValue>()
    private val reporters = mutableSetOf<VlReporterValue>()
    private val vulnerabilities = mutableListOf<VlVulnerabilityData>()

    override fun release(version: String): VlReleaseValue {
        val release = VlReleaseValueImpl(version)
        releases += release
        return release
    }

    override fun release(
        version: String,
        publicationDate: String,
    ): VlReleasePublishedValue {
        val release = VlReleasePublishedValueImpl(version, LocalDate.parse(publicationDate))
        publishedReleases += release
        return release
    }

    override fun lifeCyclePhase(name: String): VlLifeCycleFromBuilder {
        val lifeCycle = VlLifeCycleFromBuilderImpl(name)
        return lifeCycle
    }

    override fun lifeCycle(vararg lifeCyclePhases: VlLifeCycleToBuilder): VlLifeCycleValue {
        val lifeCycle = VlLifeCycleValueImpl(lifeCyclePhases.toList())
        return lifeCycle
    }

    override fun branch(
        name: String,
        initialVersion: VlReleaseValue,
        lifeCycle: VlLifeCycleValue,
    ): VlBranchBuilder {
        val branchBuilder = VlBranchBuilderImpl(name, initialVersion, lifeCycle)
        branchBuilders += branchBuilder
        return branchBuilder
    }

    override fun variants(vararg productVariant: String): Array<VlVariantValue> {
        val variants: Array<VlVariantValue> = productVariant.map(::VlVariantValueImpl).toTypedArray()
        productVariants += variants.toSet()
        return variants
    }

    override fun reporter(vararg reporterName: String): Array<VlReporterValue> {
        val reporter: Array<VlReporterValue> = reporterName.map(::VlReporterValueImpl).toTypedArray()
        reporters += reporter
        return reporter
    }

    override fun vuln(
        vararg vulnerabilityId: String,
        init: VlVulnerabilityContext.() -> Unit,
    ) = with(VlVulnerabilityContextImpl()) {
        init()
        val ids = vulnerabilityId.map(::VlVulnerabilityIdImpl).toSet()
        this@VlVulnLogContextValueImpl.vulnerabilities += VlVulnerabilityData(ids, build())
    }

    fun build(): VulnlogFileData {
        val branches: List<VlBranchValue> = branchBuilders.map { it.build() }.toList()
        return VulnlogFileData(
            releases,
            publishedReleases,
            branches,
            productVariants,
            reporters,
            vulnerabilities,
        )
    }
}
