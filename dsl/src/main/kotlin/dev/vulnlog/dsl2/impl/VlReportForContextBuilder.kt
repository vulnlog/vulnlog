package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.VlReportFor
import dev.vulnlog.dsl2.VlReportForContext

interface VlReportForContextBuilder : VlReportForContext {
    fun build(): VlReportFor
}
