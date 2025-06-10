package dev.vulnlog.cli.service

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dslinterpreter.repository.BranchRepository
import dev.vulnlog.service.BranchService

/**
 * All release branches are returned.
 */
class AllReleaseBranchService(
    private val repository: BranchRepository,
) : BranchService {
    override fun getBranches(): Set<ReleaseBranchData> {
        return repository.getAllBranches()
    }
}
