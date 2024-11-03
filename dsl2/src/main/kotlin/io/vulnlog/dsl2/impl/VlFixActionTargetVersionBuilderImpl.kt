package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionTargetVersionBuilder

class VlFixActionTargetVersionBuilderImpl(
    private val action: String,
) : VlFixActionTargetVersionBuilder {
    override fun toOrHigher(version: String) = VlFixActionValueImpl("$action >= $version")
}
