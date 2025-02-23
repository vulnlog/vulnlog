package dev.vulnlog.cli.commands

import dev.vulnlog.cli.serialisable.Analysis
import dev.vulnlog.cli.serialisable.ReleaseBranchVulnerabilities
import dev.vulnlog.cli.serialisable.ReleaseBranche
import dev.vulnlog.cli.serialisable.ReleaseVersion
import dev.vulnlog.cli.serialisable.Report
import dev.vulnlog.cli.serialisable.Vulnerability
import dev.vulnlog.cli.serialisable.Vulnlog
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dsl.ReleaseVersionData
import dev.vulnlog.dsl.VulnlogAnalysisData
import dev.vulnlog.dsl.VulnlogData
import dev.vulnlog.dsl.VulnlogReportData

class SerialisationTranslator {
    fun translate(filteredResult: Filtered): Vulnlog {
        val releaseBranches = filteredResult.releaseBranches.toReleaseBranches()
        val releaseBrancheVulnerabilities = filteredResult.vulnerabilitiesPerBranch.toReleaseBranchVulnerabilities()
        return Vulnlog(releaseBranches, releaseBrancheVulnerabilities)
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

    private fun Map<ReleaseBranchData, List<VulnlogData>>.toReleaseBranchVulnerabilities() =
        map { (releaseBranch, vulnerabilities) ->
            ReleaseBranchVulnerabilities(releaseBranch.name, vulnerabilities.toVulnerability())
        }

    private fun List<VulnlogData>.toVulnerability(): List<Vulnerability> {
        return map { vulnlogData ->
            Vulnerability(
                vulnlogData.ids,
                vulnlogData.reportData.toReport(),
                vulnlogData.analysisData.toAnalysis(),
            )
        }
    }

    private fun VulnlogReportData.toReport(): Report {
        return Report(scanner, awareAt)
    }

    private fun VulnlogAnalysisData.toAnalysis(): Analysis {
        return Analysis(analysedAt, verdict, reasoning)
    }
}
