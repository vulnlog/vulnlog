package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlBranchBuilder
import io.vulnlog.dsl2.VlReleaseValue

internal class VlBranchBuilderImpl : VlBranchBuilder {
    override fun supersededBy(version: VlReleaseValue): VlBranchBuilder = VlBranchBuilderImpl()

    fun build(): VlBranchBuilder = this
}
