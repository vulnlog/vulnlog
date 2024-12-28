package dev.vulnlog.dsl2

interface VlReportAtContext {
    infix fun exact(releaseBranch: VlReleaseBranch): VlReportForContext

    infix fun from(releaseBranch: VlReleaseBranch): VlReportForContext
}
