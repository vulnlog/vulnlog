package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionValue

internal class VlToFixRemoveActionBuilder(
    private val removeDependency: String,
) : VlToFixActionBuilder {
    override fun build(): VlFixActionValue = VlFixActionValueImpl(removeDependency)
}
