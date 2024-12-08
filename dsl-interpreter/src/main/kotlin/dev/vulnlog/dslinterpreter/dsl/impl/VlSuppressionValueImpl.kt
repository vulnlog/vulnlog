package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlReporterValue
import dev.vulnlog.dsl.VlSuppressionValue

internal data class VlSuppressionValueImpl(
    override val reporter: VlReporterValue?,
    override val onAllVulnerabilities: Boolean,
    override val genericFilters: List<String>,
) : VlSuppressionValue
