package io.vulnlog.dsl

import io.vulnlog.core.model.reporter.VlGenericReporter
import io.vulnlog.core.model.reporter.VlReporter
import io.vulnlog.core.model.version.VlAffectedVersionSet

class DslGenericReporter(
    affectedVersions: VlAffectedVersionSet,
) : DslReporter(affectedVersions) {
    override fun createReporter(): VlReporter = VlGenericReporter(affectedVersions)
}
