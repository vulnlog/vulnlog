package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionTargetDependencyBuilder
import io.vulnlog.dsl2.VlFixActionValue

class VlFixActionTargetDependencyBuilderImpl(
    private val action: String,
) : VlFixActionTargetDependencyBuilder {
    override fun with(dependencyName: String): VlFixActionValue = VlFixActionValueImpl("$action with $dependencyName")
}
