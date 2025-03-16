package dev.vulnlog.dsl

import java.time.LocalDate

public interface ReleaseBranchData {
    public val name: String
}

public interface ReleaseVersionData {
    public val version: String
    public val releaseDate: LocalDate?
}
