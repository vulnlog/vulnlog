package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlBranchBuilder
import io.vulnlog.dsl2.VlLifeCycle
import io.vulnlog.dsl2.VlLifeCycleFromBuilder
import io.vulnlog.dsl2.VlLifeCycleToBuilder
import io.vulnlog.dsl2.VlReporter
import io.vulnlog.dsl2.VlVariant
import io.vulnlog.dsl2.VlVersion
import io.vulnlog.dsl2.VlVulnLogContext
import io.vulnlog.dsl2.VlVulnerability
import io.vulnlog.dsl2.VlVulnerabilityContext

internal class VlVuLnLogContextImpl : VlVulnLogContext {
    override var vendorName: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var productName: String?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun version(version: String): VlVersion {
        TODO("Not yet implemented")
    }

    override fun version(
        version: String,
        releaseDate: String,
    ): VlVersion {
        TODO("Not yet implemented")
    }

    override fun lifeCyclePhase(name: String): VlLifeCycleFromBuilder {
        TODO("Not yet implemented")
    }

    override fun lifeCycle(vararg lifeCyclePhases: VlLifeCycleToBuilder): VlLifeCycle {
        TODO("Not yet implemented")
    }

    override fun branch(
        name: String,
        initialVersion: VlVersion,
        lifeCycle: VlLifeCycle,
    ): VlBranchBuilder {
        TODO("Not yet implemented")
    }

    override fun variants(vararg productVariant: String): Array<VlVariant> {
        TODO("Not yet implemented")
    }

    override fun reporter(vararg reporterName: String): Array<VlReporter> {
        TODO("Not yet implemented")
    }

    override fun vuln(
        vararg vulnerabilityId: String,
        context: VlVulnerabilityContext.() -> Unit,
    ): VlVulnerability {
        TODO("Not yet implemented")
    }
}
