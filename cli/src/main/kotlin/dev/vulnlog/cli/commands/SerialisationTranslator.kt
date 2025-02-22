package dev.vulnlog.cli.commands

import dev.vulnlog.cli.serialisable.ReleaseBranch
import dev.vulnlog.cli.serialisable.ReleaseBranches
import dev.vulnlog.cli.serialisable.ReleaseVersion
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData

class SerialisationTranslator {
    fun translate(filteredResult: Filtered): ReleaseBranches {
        return filteredResult.releaseBranches.toReleaseBranches()
    }

    private fun Map<ReleaseBranchData, List<ReleaseVersionData>>.toReleaseBranches(): ReleaseBranches {
        val releaseBranchToReleaseVersions: Map<ReleaseBranch, List<ReleaseVersion>> =
            map { (releaseBranch, releaseVersions) ->
                releaseBranch.toReleaseBranch() to releaseVersions.map { it.toReleaseVersion() }
            }.toMap()
        return ReleaseBranches(releaseBranchToReleaseVersions)
    }

    private fun ReleaseBranchData.toReleaseBranch() =
        ReleaseBranch(
            releaseBranchName = name,
        )

    private fun ReleaseVersionData.toReleaseVersion() =
        ReleaseVersion(
            version = version,
            publicationDate = releaseDate,
        )
}
