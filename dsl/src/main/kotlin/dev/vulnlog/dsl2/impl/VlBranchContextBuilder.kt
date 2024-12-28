package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.VlBranchContext
import dev.vulnlog.dsl2.VlReleaseBranch

interface VlBranchContextBuilder : VlBranchContext {
    fun build(): VlReleaseBranch

    fun build(predecessor: VlReleaseBranch?): VlReleaseBranch
}
