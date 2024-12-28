package dev.vulnlog.dsl2

interface VlRelease {
    fun releases(block: VlReleaseBranchContext.() -> Unit): Array<VlReleaseBranch>
}
