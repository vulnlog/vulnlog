package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReporterData
import dev.vulnlog.dsl.VlReporterConfig

data class ReporterDataImpl(
    override val name: String,
    val config: VlReporterConfig?,
) : ReporterData
