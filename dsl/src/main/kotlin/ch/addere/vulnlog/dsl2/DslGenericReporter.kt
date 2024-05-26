package ch.addere.vulnlog.dsl2

import ch.addere.vulnlog.core.model.reporter.VlGenericReporter
import ch.addere.vulnlog.core.model.reporter.VlReporter
import ch.addere.vulnlog.core.model.version.VlVersion

class DslGenericReporter(vararg affectedVersions: VlVersion) : DslReporter(affectedVersions.toSet()) {
    override fun createReporter(): VlReporter {
        return VlGenericReporter(affectedVersionSet())
    }
}
