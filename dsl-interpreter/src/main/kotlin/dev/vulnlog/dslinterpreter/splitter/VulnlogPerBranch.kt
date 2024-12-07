package dev.vulnlog.dslinterpreter.splitter

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dslinterpreter.dsl.VlVulnerabilityData

data class VulnlogPerBranch(val branch: VlBranchValue?, val vulnerabilities: List<VlVulnerabilityData>)
