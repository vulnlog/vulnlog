package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlReporterValue
import io.vulnlog.dsl2.VlSuppressionBuilder

internal class VlSuppressionBuilder : VlSuppressionBuilder {
    override fun forReporter(reporter: VlReporterValue): VlSuppressionBuilder = this

    override fun onAllVulnerabilities(): VlSuppressionBuilder = this

    override fun addFilter(genericFilter: String): VlSuppressionBuilder = this
}
