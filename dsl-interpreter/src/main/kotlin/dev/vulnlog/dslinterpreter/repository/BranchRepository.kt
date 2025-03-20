package dev.vulnlog.dslinterpreter.repository

import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dslinterpreter.impl.ReleaseBranchDataImpl
import java.time.LocalDate

val DEFAULT_BRANCH = ReleaseBranchDataImpl("default release branch")

sealed class BranchRepositoryException(message: String?) : RuntimeException(message)

data class BranchNotExisting(override val message: String?) : BranchRepositoryException(message)

data class ReleaseVersionNotExisting(override val message: String?) : BranchRepositoryException(message)

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

class BranchRepositoryImpl :
    BranchRepository {
    private val data: MutableMap<ReleaseBranchData, List<ReleaseVersionData>> =
        mutableMapOf(DEFAULT_BRANCH to listOf())

    override fun add(
        releaseBranch: ReleaseBranchData,
        releaseVersions: List<ReleaseVersionData>,
    ) {
        if (data.containsKey(releaseBranch)) {
            data[releaseBranch] = data[releaseBranch]!! + releaseVersions
        } else {
            data[releaseBranch] = releaseVersions
        }
    }

    override fun getReleaseVersions(releaseBranch: ReleaseBranchData): Result<List<ReleaseVersionData>> {
        if (!data.containsKey(releaseBranch)) {
            return Result.failure(BranchNotExisting("Release branch does not exist: ${releaseBranch.name}"))
        }
        return Result.success(data[releaseBranch]!!)
    }

    @Suppress("ReturnCount")
    override fun getNextReleaseVersionBefore(
        releaseBranch: ReleaseBranchData,
        date: LocalDate,
    ): Result<ReleaseVersionData> {
        if (!data.containsKey(releaseBranch)) {
            return Result.failure(BranchNotExisting("Release branch does not exist: ${releaseBranch.name}"))
        }

        val releaseVersion =
            data[releaseBranch]!!
                .filter { it.releaseDate != null }
                .reversed()
                .firstOrNull { date.isAfter(it.releaseDate) }
        if (releaseVersion == null) {
            return Result.failure(ReleaseVersionNotExisting("No release version exist before: $date"))
        }
        return Result.success(releaseVersion)
    }

    @Suppress("ReturnCount")
    override fun getNextReleaseVersionAfter(
        releaseBranch: ReleaseBranchData,
        date: LocalDate,
    ): Result<ReleaseVersionData> {
        if (!data.containsKey(releaseBranch)) {
            return Result.failure(BranchNotExisting("Release branch does not exist: ${releaseBranch.name}"))
        }

        val releaseVersion =
            data[releaseBranch]!!
                .filter { it.releaseDate != null }
                .firstOrNull { date.isBefore(it.releaseDate) }
        if (releaseVersion == null) {
            val firstUnpublished = data[releaseBranch]!!.firstOrNull { it.releaseDate == null }
            return if (firstUnpublished != null) {
                Result.success(firstUnpublished)
            } else {
                Result.failure(ReleaseVersionNotExisting("No release version exist after: $date"))
            }
        }
        return Result.success(releaseVersion)
    }

    override fun getAllBranches(): Set<ReleaseBranchData> {
        return data.keys
    }

    override fun getBranchesToReleaseVersions(): Map<ReleaseBranchData, List<ReleaseVersionData>> {
        return data
    }
}
