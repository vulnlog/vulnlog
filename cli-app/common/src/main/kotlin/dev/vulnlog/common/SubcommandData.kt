package dev.vulnlog.common

import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.VulnEntry

data class SubcommandData(
    var cliVersion: String,
    var vulnEntriesFiltered: List<VulnEntry>,
    var releaseBranchesFiltered: Set<BranchName>,
)
