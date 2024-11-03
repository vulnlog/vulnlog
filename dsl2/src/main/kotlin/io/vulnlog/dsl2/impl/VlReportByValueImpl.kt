package io.vulnlog.dsl2.impl

import io.vulnlog.dsl2.VlReportByValue

data class VlReportByValueImpl(
    override val reporterName: String,
) : VlReportByValue
