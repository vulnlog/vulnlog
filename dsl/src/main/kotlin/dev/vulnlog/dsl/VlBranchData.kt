package dev.vulnlog.dsl

import java.time.LocalDate

public sealed interface ReleaseBranchData {
    public val name: String
}

public data class ReleaseBranchDataImpl(override val name: String) : ReleaseBranchData

public data object DefaultReleaseBranchDataImpl : ReleaseBranchData {
    override val name: String = "Default Release Branch"
}

public sealed interface ReleaseVersionData {
    public val version: String
    public val releaseDate: LocalDate?
}

public data class ReleaseVersionDataImpl(override val version: String, override val releaseDate: LocalDate?) :
    ReleaseVersionData
