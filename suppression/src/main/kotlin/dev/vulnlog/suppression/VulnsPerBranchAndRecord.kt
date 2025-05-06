package dev.vulnlog.suppression

import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VlReporterImpl

data class VulnsPerBranchAndRecord(
    val name: ReleaseBranchData,
    val entry: VlReporterImpl,
    val vuln: Set<VulnerabilityDataPerBranch>,
)
