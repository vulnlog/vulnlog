package ch.addere.vulnlog.dsl

import ch.addere.vulnlog.core.model.reporter.VlGenericReporter
import ch.addere.vulnlog.core.model.reporter.VlReporter
import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

class DslGenericReporter(
    affectedVersions: VlAffectedVersionSet,
) : DslReporter(affectedVersions) {
    override fun createReporter(): VlReporter = VlGenericReporter(affectedVersions)
}
