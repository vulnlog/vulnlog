package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlFixActionValue

internal class VlToFixRemoveActionBuilder(
    private val removeDependency: String,
) : VlToFixActionBuilder {
    override fun build(): VlFixActionValue = VlFixActionValueImpl(removeDependency)
}
