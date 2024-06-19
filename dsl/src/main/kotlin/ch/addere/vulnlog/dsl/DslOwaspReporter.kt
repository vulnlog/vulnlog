package ch.addere.vulnlog.dsl

import ch.addere.vulnlog.core.model.reporter.VlOwaspReporter
import ch.addere.vulnlog.core.model.reporter.VlReporter
import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

class DslOwaspReporter(
    affectedVersions: VlAffectedVersionSet,
) : DslReporter(affectedVersions) {
    override fun createReporter(): VlReporter = VlOwaspReporter(affectedVersions)
}
