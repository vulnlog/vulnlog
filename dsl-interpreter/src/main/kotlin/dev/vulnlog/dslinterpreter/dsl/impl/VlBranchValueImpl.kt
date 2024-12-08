package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlBranchValue
import dev.vulnlog.dsl.VlPhaseValue
import dev.vulnlog.dsl.VlReleaseValue

internal data class VlBranchValueImpl(
    override val name: String,
    override val initialVersion: VlReleaseValue,
    private val subsequentReleases: List<VlReleaseValue>,
    override val phases: List<VlPhaseValue>,
) : VlBranchValue {
    override val releases: List<VlReleaseValue>
        get() = listOf(initialVersion) + subsequentReleases
}
