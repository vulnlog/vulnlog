package dev.vulnlog.common

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData

data class Filtered(
    val releaseBranches: Map<ReleaseBranchData, List<ReleaseVersionData>>,
    val vulnerabilitiesPerBranch: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>>,
)
