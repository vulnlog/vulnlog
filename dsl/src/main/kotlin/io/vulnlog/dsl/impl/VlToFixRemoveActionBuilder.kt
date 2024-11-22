package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlFixActionValue

internal class VlToFixRemoveActionBuilder(
    private val removeDependency: String,
) : VlToFixActionBuilder {
    override fun build(): VlFixActionValue = VlFixActionValueImpl(removeDependency)
}
