package dev.vulnlog.common.repository

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import java.time.LocalDate

interface BranchRepository {
    /**
     * Add a release branch with a release versions.
     * Merges the release versions, if the release branch already exists.
     */
    fun add(
        releaseBranch: ReleaseBranchData,
        releaseVersions: List<ReleaseVersionData>,
    )

    /**
     * Returns all release versions for a specific release branch.
     * An empty list, if the release branch does not exist.
     */
    fun getReleaseVersions(releaseBranch: ReleaseBranchData): Result<List<ReleaseVersionData>>

    /**
     * Returns the next release version before [date].
     */
    fun getNextReleaseVersionBefore(
        releaseBranch: ReleaseBranchData,
        date: LocalDate,
    ): Result<ReleaseVersionData>

    /**
     * Returns the next release version after [date].
     */
    fun getNextReleaseVersionAfter(
        releaseBranch: ReleaseBranchData,
        date: LocalDate,
    ): Result<ReleaseVersionData>

    /**
     * Returns a set of all release branches
     */
    fun getAllBranches(): Set<ReleaseBranchData>

    /**
     * Returns a map of release branches to release versions.
     */
    fun getBranchesToReleaseVersions(): Map<ReleaseBranchData, List<ReleaseVersionData>>
}
