package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlFixActionTargetVersionBuilder
import io.vulnlog.dsl.VlFixActionValue

internal class VlToFixActionTargetVersionBuilder(
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
