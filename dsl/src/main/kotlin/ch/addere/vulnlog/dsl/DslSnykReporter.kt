package ch.addere.vulnlog.dsl

import ch.addere.vulnlog.core.model.reporter.VlReporter
import ch.addere.vulnlog.core.model.reporter.VlSnykReporter
import ch.addere.vulnlog.core.model.version.VlVersion

class DslSnykReporter(
    private val snykId: String,
    vararg affectedVersions: VlVersion,
    val init: (VlSnykBlock.() -> Unit)?,
) : DslReporter(affectedVersions.toSet()) {
    override fun createReporter(): VlReporter {
        val block = VlSnykBlock()
        init?.invoke(block)
        val filters: Set<String> = block.filters
        return VlSnykReporter(snykId, affectedVersionSet(), filters)
    }
}
