package dev.vulnlog.dsl2

import java.time.LocalDate

data class VlReportFor(
    val reporter: VlReporter,
    val at: LocalDate,
    val releaseBranchToReleases: Map<String, VlReportedRelease>,
) {
    fun getAllForReleaseBranch(releaseBranch: String): VlReportedRelease? {
        return releaseBranchToReleases[releaseBranch]
    }
}
