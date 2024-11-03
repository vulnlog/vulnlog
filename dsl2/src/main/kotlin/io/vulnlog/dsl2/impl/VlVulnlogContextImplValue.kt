package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlBranchBuilder
import io.vulnlog.dsl2.VlLifeCycleFromBuilder
import io.vulnlog.dsl2.VlLifeCycleToBuilder
import io.vulnlog.dsl2.VlLifeCycleValue
import io.vulnlog.dsl2.VlReleasePublishedValue
import io.vulnlog.dsl2.VlReleaseValue
import io.vulnlog.dsl2.VlReporterValue
import io.vulnlog.dsl2.VlVariantValue
import io.vulnlog.dsl2.VlVuLnLogContextValue
import io.vulnlog.dsl2.data.VlData
import io.vulnlog.dsl2.data.VlVulnerabilityData

class VlVulnlogContextImplValue : VlVuLnLogContextValue {
    private val releases = mutableSetOf<VlReleaseValue>()
    private val publishedReleases = mutableSetOf<VlReleasePublishedValue>()
    private val lifeCyclePhases = mutableSetOf<VlLifeCycleFromBuilderImpl>()
    private val lifeCycles = mutableSetOf<VlLifeCycleValue>()
    private val branchBuilders = mutableSetOf<VlBranchBuilderImpl>()
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
        val release = VlReleasePublishedValueImpl(version, publicationDate)
        publishedReleases += release
        return release
    }

    override fun lifeCyclePhase(name: String): VlLifeCycleFromBuilder {
        val lifeCycle = VlLifeCycleFromBuilderImpl()
        lifeCyclePhases += lifeCycle
        return lifeCycle
    }

    override fun lifeCycle(vararg lifeCyclePhases: VlLifeCycleToBuilder): VlLifeCycleValue {
        val lifeCyle = VlLifeCycleValueImpl()
        lifeCycles += lifeCyle
        return lifeCyle
    }

    override fun branch(
        name: String,
        initialVersion: VlReleaseValue,
        lifeCycle: VlLifeCycleValue,
    ): VlBranchBuilder {
        val branchBuilder = VlBranchBuilderImpl()
        branchBuilders += branchBuilder
        return branchBuilder
    }

    override fun variants(vararg productVariant: String): Array<VlVariantValue> {
        val variants: Array<VlVariantValue> =
            productVariant
                .map { VlVariantValueImpl(it, setOf(VlReleasePublishedValueImpl("foo"))) }
                .toTypedArray()
        productVariants += variants.toSet()
        return variants
    }

    override fun reporter(vararg reporterName: String): Array<VlReporterValue> {
        val reporter: Array<VlReporterValue> =
            reporterName
                .map { VlReporterValueImpl(it) }
                .toTypedArray()
        reporters += reporter
        return reporter
    }

    override fun vuln(
        vararg vulnerabilityId: String,
        init: VlVulnerabilityContextBuilderImpl.() -> Unit,
    ) = with(VlVulnerabilityContextBuilderImpl()) {
        init()
        this@VlVulnlogContextImplValue.vulnerabilities += VlVulnerabilityData(vulnerabilityId.toSet(), reportFor)
    }

    fun build(): VlData {
        val lifeCyclePhaseData = lifeCyclePhases.map { it.build() }.toSet()
        val branches = branchBuilders.map { it.build() }.toSet()
        return VlData(
            releases,
            publishedReleases,
            lifeCyclePhaseData,
            lifeCycles,
            branches,
            productVariants,
            reporters,
            vulnerabilities,
        )
    }
}
