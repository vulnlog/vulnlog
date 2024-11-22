package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.VlFixActionTargetVersionBuilder
import dev.vulnlog.dsl.VlFixActionValue

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
