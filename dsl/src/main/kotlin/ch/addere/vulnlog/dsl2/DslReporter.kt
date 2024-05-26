package ch.addere.vulnlog.dsl2

import ch.addere.vulnlog.core.model.reporter.VlReporter
import ch.addere.vulnlog.core.model.version.VlAffectedVersionSet
import ch.addere.vulnlog.core.model.version.VlVersion

abstract class DslReporter(private val affectedVersions: Set<VlVersion>) {
    fun affectedVersionSet() = VlAffectedVersionSet(affectedVersions.toSet())

    abstract fun createReporter(): VlReporter
}
