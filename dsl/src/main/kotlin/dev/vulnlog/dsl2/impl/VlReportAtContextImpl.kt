package dev.vulnlog.dsl2.impl

import dev.vulnlog.dsl2.VlReleaseBranch
import dev.vulnlog.dsl2.VlReportAtContext
import dev.vulnlog.dsl2.VlReportForContext
import dev.vulnlog.dsl2.VlReporter
import java.time.LocalDate

class VlReportAtContextImpl(private val reporter: VlReporter, private val reportedAt: LocalDate) : VlReportAtContext {
    override infix fun exact(releaseBranch: VlReleaseBranch): VlReportForContext {
        return VlReportForContextImpl(reporter, reportedAt, releaseBranch)
    }

    override infix fun from(releaseBranch: VlReleaseBranch): VlReportForContext {
        return VlReportForContextImpl(reporter, reportedAt, releaseBranch)
    }
}
