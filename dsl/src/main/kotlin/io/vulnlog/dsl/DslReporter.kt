package io.vulnlog.dsl

import io.vulnlog.core.model.reporter.VlReporter
import io.vulnlog.core.model.version.VlAffectedVersionSet

abstract class DslReporter(
    val affectedVersions: VlAffectedVersionSet,
) {
    abstract fun createReporter(): VlReporter
}
