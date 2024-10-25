package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlBranchBuilder
import io.vulnlog.dsl2.VlLifeCycleFromBuilder
import io.vulnlog.dsl2.VlLifeCycleToBuilder
import io.vulnlog.dsl2.VlLifeCycleValue
import io.vulnlog.dsl2.VlReporterValue
import io.vulnlog.dsl2.VlVariantValue
import io.vulnlog.dsl2.VlVersionValue
import io.vulnlog.dsl2.VlVuLnLogContextValue
import io.vulnlog.dsl2.VlVulnerabilityContext
import io.vulnlog.dsl2.VlVulnerabilityValue

internal class VlVulnlogContextImplValue : VlVuLnLogContextValue {
    override var vendorName: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var productName: String?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun version(version: String): VlVersionValue {
        TODO("Not yet implemented")
    }

    override fun version(
        version: String,
        releaseDate: String,
    ): VlVersionValue {
        TODO("Not yet implemented")
    }

    override fun lifeCyclePhase(name: String): VlLifeCycleFromBuilder {
        TODO("Not yet implemented")
    }

    override fun lifeCycle(vararg lifeCyclePhases: VlLifeCycleToBuilder): VlLifeCycleValue {
        TODO("Not yet implemented")
    }

    override fun branch(
        name: String,
        initialVersion: VlVersionValue,
        lifeCycle: VlLifeCycleValue,
    ): VlBranchBuilder {
        TODO("Not yet implemented")
    }

    override fun variants(vararg productVariant: String): Array<VlVariantValue> {
        TODO("Not yet implemented")
    }

    override fun reporter(vararg reporterName: String): Array<VlReporterValue> {
        TODO("Not yet implemented")
    }

    override fun vuln(
        vararg vulnerabilityId: String,
        context: VlVulnerabilityContext.() -> Unit,
    ): VlVulnerabilityValue {
        TODO("Not yet implemented")
    }
}
