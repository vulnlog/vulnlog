package ch.addere.vulnlog.dsl2

import ch.addere.vulnlog.core.model.reporter.VlOwaspReporter
import ch.addere.vulnlog.core.model.reporter.VlReporter
import ch.addere.vulnlog.core.model.version.VlVersion

class DslOwaspReporter(vararg affectedVersions: VlVersion) : DslReporter(affectedVersions.toSet()) {
    override fun createReporter(): VlReporter {
        return VlOwaspReporter(affectedVersionSet())
    }
}
