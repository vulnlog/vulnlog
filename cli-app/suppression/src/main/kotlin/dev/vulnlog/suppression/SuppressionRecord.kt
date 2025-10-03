package dev.vulnlog.suppression

import dev.vulnlog.common.model.BranchName

data class SuppressionRecord(
    val templateFilename: String,
    val branchToSuppressions: Map<BranchName, List<String>>,
)
