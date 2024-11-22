package io.vulnlog.dsl

interface VlBranchValue {
    val name: String
    val initialVersion: VlReleaseValue
    val releases: List<VlReleaseValue>
    val phases: List<VlPhaseValue>
}
