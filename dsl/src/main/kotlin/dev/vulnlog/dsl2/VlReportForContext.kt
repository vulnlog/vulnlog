package dev.vulnlog.dsl2

interface VlReportForContext {
    infix fun andExact(release: VlReleaseBranch): VlReportForContext

    infix fun to(release: VlReleaseBranch): VlReportForContext

    infix fun onVariant(variant: String): VlReportForContext
}
