package io.vulnlog.dsl2

import io.vulnlog.dsl2.impl.VlBranchBuilderImpl
import io.vulnlog.dsl2.impl.VlLifeCycleFromBuilderImpl
import io.vulnlog.dsl2.impl.VlLifeCycleValueImpl
import io.vulnlog.dsl2.impl.VlReleasePublishedValueImpl
import io.vulnlog.dsl2.impl.VlReleaseValueImpl
import io.vulnlog.dsl2.impl.VlReporterValueImpl
import io.vulnlog.dsl2.impl.VlVariantValueImpl
import io.vulnlog.dsl2.impl.VlVulnerabilityContextImpl
import io.vulnlog.dsl2.impl.VlVulnerabilityIdImpl
import io.vulnlog.dsl2.impl.VulnlogFileData
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
