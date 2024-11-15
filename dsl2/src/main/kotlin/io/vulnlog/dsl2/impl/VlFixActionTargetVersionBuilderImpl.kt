package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlFixActionTargetVersionBuilder
import io.vulnlog.dsl2.VlFixActionValue

class VlFixActionTargetVersionBuilderImpl(
    private val action: String,
    private val target: String,
) : VlFixActionTargetVersionBuilder,
    VlToFixActionBuilder {
    private var version: String? = null

    override fun toOrHigher(version: String) {
        this.version = version
    }

    override fun build(): VlFixActionValue = VlFixActionValueImpl("$action $target >= $version")
}
