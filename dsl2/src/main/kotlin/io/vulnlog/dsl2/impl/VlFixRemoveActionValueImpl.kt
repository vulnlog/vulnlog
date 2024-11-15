package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionValue

class VlFixRemoveActionValueImpl(
    val removeDependency: String,
) : VlToFixActionBuilder {
    override fun build(): VlFixActionValue = VlFixActionValueImpl(removeDependency)
}
