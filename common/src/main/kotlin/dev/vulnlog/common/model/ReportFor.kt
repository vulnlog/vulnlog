package dev.vulnlog.common.model

data class BranchName(val name: String)

data class ReportFor(val branchName: BranchName, val branchVersion: BranchVersion?)
