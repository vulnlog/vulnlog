package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.MyEffectiveReporter
import dev.vulnlog.dsl2.VlReleaseBranch
import dev.vulnlog.dsl2.VlReleaseBranchContext
import dev.vulnlog.dsl2.VlReporter
import dev.vulnlog.dsl2.VlVuln
import dev.vulnlog.dsl2.VlVulnerabilityContext
import dev.vulnlog.dsl2.VlVulnerabilityIdentifier

class VlVulnlogContextImpl : VlVulnlogContextBuilder {
    private val releaseBranches = mutableSetOf<VlReleaseBranch>()
    private val reporters = mutableSetOf<MyEffectiveReporter>()
    private val vulnerabilities = mutableListOf<VlVuln>()

    override fun reporter(name: String): MyEffectiveReporter {
        val reporter = { VlReporter(name) }
        reporters += reporter
        return reporter
    }

    override fun reporters(vararg names: String): Array<MyEffectiveReporter> {
        val reporters = names.map { { VlReporter(it) } }.toTypedArray()
        this.reporters += reporters
        return reporters
    }

    override fun releases(block: VlReleaseBranchContext.() -> Unit): Array<VlReleaseBranch> {
        with(VlReleaseBranchContextImpl()) {
            block()
            val releaseBranches: Array<VlReleaseBranch> = build()
            this@VlVulnlogContextImpl.releaseBranches += releaseBranches
            return releaseBranches
        }
    }

    override fun vuln(
        vararg ids: String,
        block: (VlVulnerabilityContext.() -> Unit)?,
    ) = with(VlVulnerabilityContextImpl(ids.map(::VlVulnerabilityIdentifier).toList())) {
        block?.let { block() }
        val vulnerability: VlVuln = build()
        this@VlVulnlogContextImpl.vulnerabilities += vulnerability
    }

    override fun build(): Vulnlog2FileData {
        return Vulnlog2FileData(releaseBranches, reporters, vulnerabilities)
    }
}
