package io.vulnlog.dsl.impl

import io.vulnlog.dsl.VlReportByValue

internal data class VlReportByValueImpl(
    override val reporterName: String,
) : VlReportByValue
