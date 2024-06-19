package ch.addere.vulnlog.dsl

import ch.addere.vulnlog.core.model.reporter.VlReporter
import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet

abstract class DslReporter(
    val affectedVersions: VlAffectedVersionSet,
) {
    abstract fun createReporter(): VlReporter
}
