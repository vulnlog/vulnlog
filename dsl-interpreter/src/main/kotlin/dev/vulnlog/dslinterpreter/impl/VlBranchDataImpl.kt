package dev.vulnlog.dslinterpreter.impl

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import java.time.LocalDate

data class ReleaseBranchDataImpl(override val name: String) : ReleaseBranchData

data object DefaultReleaseBranchDataImpl : ReleaseBranchData {
    override val name: String = "Default Release Branch"
}

data class ReleaseVersionDataImpl(override val version: String, override val releaseDate: LocalDate?) :
    ReleaseVersionData
