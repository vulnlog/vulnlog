package dev.vulnlog.suppression

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.VlReporterImpl

data class SuppressionRecord(
    val releaseBranch: ReleaseBranchData,
    val reporter: VlReporterImpl,
    val suppression: String,
)
