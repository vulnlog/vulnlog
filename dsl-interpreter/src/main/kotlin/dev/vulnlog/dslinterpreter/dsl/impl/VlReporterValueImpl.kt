package dev.vulnlog.dslinterpreter.dsl.impl

import dev.vulnlog.dsl.VlReporterValue

internal data class VlReporterValueImpl(
    override val name: String,
) : VlReporterValue
