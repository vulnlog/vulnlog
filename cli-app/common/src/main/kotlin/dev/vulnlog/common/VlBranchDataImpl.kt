package dev.vulnlog.common

import dev.vulnlog.dsl.InvolvedReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import java.time.LocalDate

data class ReleaseBranchDataImpl(override val name: String) : ReleaseBranchData

data object DefaultReleaseBranchDataImpl : ReleaseBranchData {
    override val name: String = "Default Release Branch"
}

data class ReleaseVersionDataImpl(
    override val version: String,
    override val releaseDate: LocalDate? = null,
) : ReleaseVersionData

data class InvolvedReleaseVersionImpl(
    override val affected: ReleaseVersionData?,
    override val upcoming: ReleaseVersionData?,
) : InvolvedReleaseVersion
