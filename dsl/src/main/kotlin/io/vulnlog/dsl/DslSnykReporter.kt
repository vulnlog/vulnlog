package io.vulnlog.dsl

import io.vulnlog.core.model.reporter.VlReporter
import io.vulnlog.core.model.reporter.VlSnykReporter
import io.vulnlog.core.model.version.VlAffectedVersionSet

class DslSnykReporter(
    private val snykId: String,
    affectedVersions: VlAffectedVersionSet,
    val init: (VlSnykBlock.() -> Unit)?,
) : DslReporter(affectedVersions) {
    override fun createReporter(): VlReporter {
        val block = VlSnykBlock()
        init?.invoke(block)
        val filters: Set<String> = block.filters
        return VlSnykReporter(snykId, affectedVersions, filters)
    }
}
