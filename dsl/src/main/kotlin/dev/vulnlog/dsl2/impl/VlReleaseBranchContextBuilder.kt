package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.VlReleaseBranch

interface VlReleaseBranchContextBuilder {
    fun build(): Array<VlReleaseBranch>
}
