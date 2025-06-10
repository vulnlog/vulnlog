package dev.vulnlog.service

import dev.vulnlog.dsl.ReleaseBranchData

interface BranchService {
    fun getBranches(): Set<ReleaseBranchData>
}
