package dev.vulnlog.dslinterpreter.repository

import dev.vulnlog.common.ReleaseBranchDataImpl
import dev.vulnlog.common.repository.BranchRepository
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import java.time.LocalDate

val DEFAULT_BRANCH = ReleaseBranchDataImpl("default release branch")

sealed class BranchRepositoryException(message: String?) : RuntimeException(message)

data class BranchNotExisting(override val message: String?) : BranchRepositoryException(message)

data class ReleaseVersionNotExisting(override val message: String?) : BranchRepositoryException(message)

class BranchRepositoryImpl : BranchRepository {
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
