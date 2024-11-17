package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlReportByValue

internal data class VlReportByValueImpl(
    override val reporterName: String,
) : VlReportByValue
