package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlBranchValue
import io.vulnlog.dsl2.VlPhaseValue
import io.vulnlog.dsl2.VlReleaseValue

internal data class VlBranchValueImpl(
    override val name: String,
    override val initialVersion: VlReleaseValue,
    override val releases: List<VlReleaseValue>,
    override val phases: List<VlPhaseValue>,
) : VlBranchValue
