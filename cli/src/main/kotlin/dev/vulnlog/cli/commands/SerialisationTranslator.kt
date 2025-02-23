package dev.vulnlog.cli.commands

import dev.vulnlog.cli.serialisable.ReleaseBranche
import dev.vulnlog.cli.serialisable.ReleaseVersion
import dev.vulnlog.cli.serialisable.Vulnlog
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData

class SerialisationTranslator {
    fun translate(filteredResult: Filtered): Vulnlog {
        return Vulnlog(filteredResult.releaseBranches.toReleaseBranches())
    }

    private fun Map<ReleaseBranchData, List<ReleaseVersionData>>.toReleaseBranches(): List<ReleaseBranche> {
        return map { (releaseBranch, releaseVersions) ->
            ReleaseBranche(releaseBranch.name, releaseVersions.map { it.toReleaseVersion() })
        }
    }

    private fun ReleaseVersionData.toReleaseVersion() =
        ReleaseVersion(
            version = version,
            publicationDate = releaseDate,
        )
}
