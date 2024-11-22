package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlReporterValue

internal data class VlReporterValueImpl(
    override val name: String,
) : VlReporterValue
