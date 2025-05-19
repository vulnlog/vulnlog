package dev.vulnlog.suppression

import dev.vulnlog.dsl.ReleaseBranchData

data class SuppressionRecord(
    val templateFilename: String,
    val branchToSuppressions: Map<ReleaseBranchData, List<String>>,
)
