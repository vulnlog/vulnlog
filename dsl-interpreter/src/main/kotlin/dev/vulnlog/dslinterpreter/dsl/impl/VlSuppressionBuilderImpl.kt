package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlReporterValue
import dev.vulnlog.dsl.VlSuppressionBuilder
import dev.vulnlog.dsl.VlSuppressionValue

internal class VlSuppressionBuilderImpl : VlSuppressionBuilder {
    private var reporter: VlReporterValue? = null
    private var onAllVulnerabilities = false
    private var genericFilters = mutableListOf<String>()

    override fun forReporter(reporter: VlReporterValue): VlSuppressionBuilder {
        this.reporter = reporter
        return this
    }

    override fun onAllVulnerabilities(): VlSuppressionBuilder {
        this.onAllVulnerabilities = true
        return this
    }

    override fun addFilter(genericFilter: String): VlSuppressionBuilder {
        this.genericFilters += genericFilter
        return this
    }

    fun build(): VlSuppressionValue {
        return VlSuppressionValueImpl(reporter, onAllVulnerabilities, genericFilters)
    }
}
