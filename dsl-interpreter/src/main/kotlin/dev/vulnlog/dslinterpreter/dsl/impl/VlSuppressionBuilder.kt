package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlReporterValue
import dev.vulnlog.dsl.VlSuppressionBuilder

internal class VlSuppressionBuilder : VlSuppressionBuilder {
    override fun forReporter(reporter: VlReporterValue): VlSuppressionBuilder = this

    override fun onAllVulnerabilities(): VlSuppressionBuilder = this

    override fun addFilter(genericFilter: String): VlSuppressionBuilder = this
}
