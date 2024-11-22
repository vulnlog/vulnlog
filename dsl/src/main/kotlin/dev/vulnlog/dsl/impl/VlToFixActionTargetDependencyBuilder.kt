package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.VlFixActionTargetDependencyBuilder
import dev.vulnlog.dsl.VlFixActionValue

internal class VlToFixActionTargetDependencyBuilder(
    private val action: String,
    private val target: String,
) : VlFixActionTargetDependencyBuilder,
    VlToFixActionBuilder {
    private var dependencyName: String? = null

    override fun with(dependencyName: String) {
        this.dependencyName = dependencyName
    }

    override fun build(): VlFixActionValue = VlFixActionValueImpl("$action $target with $dependencyName")
}
