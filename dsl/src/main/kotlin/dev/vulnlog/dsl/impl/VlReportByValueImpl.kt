package dev.vulnlog.dsl.impl

import dev.vulnlog.dsl.VlReportByValue

internal data class VlReportByValueImpl(
    override val reporterName: String,
) : VlReportByValue
