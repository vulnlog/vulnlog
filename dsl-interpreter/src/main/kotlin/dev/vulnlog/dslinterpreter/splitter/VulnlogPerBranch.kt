package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlPhaseValue
import dev.vulnlog.dsl.VlReleaseValue
import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData
import dev.vulnlog.dslinterpreter.dsl.impl.VlReleaseValueImpl

data class VulnlogPerBranch(val branch: VlBranchValue = DefaultBranch, val vulnerabilities: List<VlVulnerabilityData>)

object DefaultBranch : VlBranchValue {
    override val name: String
        get() = "default"
    override val initialVersion: VlReleaseValue
        get() = VlReleaseValueImpl("default version")
    override val releases: List<VlReleaseValue>
        get() = emptyList()
    override val phases: List<VlPhaseValue>
        get() = emptyList()
}
