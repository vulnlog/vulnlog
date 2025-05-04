package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReporterProvider
import dev.vulnlog.dsl.VlReporter
import dev.vulnlog.dsl.VlReporterConfig
import dev.vulnlog.dsl.VlReporterContext
import dev.vulnlog.dsl.VlReportersContext

class VlReportersContextImpl : VlReportersContext {
    val reporters = mutableListOf<VlReporter>()

    override fun reporter(
        reporter: String,
        block: ((VlReporterContext).() -> Unit)?,
    ) {
        val reporterContext: VlReporterConfig? = block?.let { VlReporterContextImpl().apply(it) }?.config
        reporters += ReporterProvider.create(reporter, reporterContext)
    }
}
