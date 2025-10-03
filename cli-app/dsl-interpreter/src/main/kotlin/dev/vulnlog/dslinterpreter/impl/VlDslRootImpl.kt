package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.common.repository.BranchRepository
import dev.vulnlog.common.repository.VulnerabilityDataRepository
import dev.vulnlog.dsl.VlDslRoot
import dev.vulnlog.dsl.VlReleasesDslRoot
import dev.vulnlog.dsl.VlVulnerabilityDslRoot

class VlDslRootImpl(
    val branchRepository: BranchRepository,
    val vulnerabilityDataRepository: VulnerabilityDataRepository,
    private val releasesDsl: VlReleasesDslRoot,
    private val vulnerabilityDsl: VlVulnerabilityDslRoot,
) : VlDslRoot,
    VlReleasesDslRoot by releasesDsl,
    VlVulnerabilityDslRoot by vulnerabilityDsl
