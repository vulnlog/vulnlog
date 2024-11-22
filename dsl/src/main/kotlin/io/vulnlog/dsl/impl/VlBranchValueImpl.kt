package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlBranchValue
import io.vulnlog.dsl.VlPhaseValue
import io.vulnlog.dsl.VlReleaseValue

internal data class VlBranchValueImpl(
    override val name: String,
    override val initialVersion: VlReleaseValue,
    override val releases: List<VlReleaseValue>,
    override val phases: List<VlPhaseValue>,
) : VlBranchValue
