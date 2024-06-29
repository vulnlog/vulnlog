package io.vulnlog.dsl

import io.vulnlog.core.model.reporter.VlOwaspReporter
import io.vulnlog.core.model.reporter.VlReporter
import io.vulnlog.core.model.version.VlAffectedVersionSet

class DslOwaspReporter(
    affectedVersions: VlAffectedVersionSet,
) : DslReporter(affectedVersions) {
    override fun createReporter(): VlReporter = VlOwaspReporter(affectedVersions)
}
