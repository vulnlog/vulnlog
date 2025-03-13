package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReporterProvider
import dev.vulnlog.dsl.VlReporter
import dev.vulnlog.dsl.VlReporterContext

class VlReporterContextImpl : VlReporterContext {
    val reporters = mutableListOf<VlReporter>()

    override fun reporter(reporter: String) {
        reporters += ReporterProvider.create(reporter)
    }
}
