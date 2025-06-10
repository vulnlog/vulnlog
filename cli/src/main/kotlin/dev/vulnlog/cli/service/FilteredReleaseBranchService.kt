package dev.vulnlog.cli.service

import dev.vulnlog.cli.BranchFilter
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dslinterpreter.repository.BranchRepository
import dev.vulnlog.service.BranchService

/**
 * Filtered release branches. Only release branches that are specified in the input filter are returned.
 */
class FilteredReleaseBranchService(
    private val repository: BranchRepository,
    private val branches: List<BranchFilter>,
) : BranchService {
    override fun getBranches(): Set<ReleaseBranchData> {
        return repository.getAllBranches()
            .filter { branches.map { branch -> branch.releaseBranch }.contains(it.name) }
            .toSet()
    }
}
