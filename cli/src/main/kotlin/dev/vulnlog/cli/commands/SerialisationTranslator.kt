package dev.vulnlog.cli.commands

import dev.vulnlog.cli.serialisable.ReleaseBranch
import dev.vulnlog.cli.serialisable.ReleaseBranches
import dev.vulnlog.cli.serialisable.ReleaseVersion

class SerialisationTranslator {
    fun translate(filteredResult: Filtered): ReleaseBranches {
        val releaseBranchToReleaseVersions =
            filteredResult.releaseBranches.map { (releaseBranch, releaseVersions) ->
                val rb = ReleaseBranch(releaseBranch.name)
                val rVs = releaseVersions.map { rV -> ReleaseVersion(rV.version, rV.releaseDate) }
                rb to rVs
            }.toMap()
        return ReleaseBranches(releaseBranchToReleaseVersions)
    }
}
