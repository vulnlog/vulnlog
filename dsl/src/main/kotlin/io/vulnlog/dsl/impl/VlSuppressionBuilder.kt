package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlReporterValue
import io.vulnlog.dsl.VlSuppressionBuilder

internal class VlSuppressionBuilder : VlSuppressionBuilder {
    override fun forReporter(reporter: VlReporterValue): VlSuppressionBuilder = this

    override fun onAllVulnerabilities(): VlSuppressionBuilder = this

    override fun addFilter(genericFilter: String): VlSuppressionBuilder = this
}
