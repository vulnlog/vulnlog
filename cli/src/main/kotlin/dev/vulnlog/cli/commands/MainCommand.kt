package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.eagerOption
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.cli.serialisable.ReleaseBranche
import dev.vulnlog.cli.serialisable.Vulnlog
import dev.vulnlog.cli.service.StatusService
import dev.vulnlog.cli.service.ruleSet
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dslinterpreter.ScriptingHost
import dev.vulnlog.dslinterpreter.impl.VlDslRootImpl
import dev.vulnlog.dslinterpreter.splitter.VulnerabilityDataPerBranch
import dev.vulnlog.dslinterpreter.splitter.vulnerabilityPerBranch
import java.io.File

data class ConfigAndDataForSubcommand(
    var cliVerison: String,
    var releaseBranches: List<String>,
    var vulnlogs: List<String>,
)

class MainCommand : CliktCommand() {
    override fun help(context: Context): String = "CLI application to parse Vulnlog files."

    override val invokeWithoutSubcommand = true

    private val vulnlogFile: File by argument()
        .file(mustExist = true, canBeDir = false)
        .help("The Vulnlog definition files to read.")
        .check("File must be a Vulnlog definition file (definitions.vl.kts)") { it.name == "definitions.vl.kts" }

    private val filterVulnerabilities: List<String>? by option(
        "--vuln",
        help = "Filter to specific vulnerabilities",
    )
        .varargValues()

    private val filterBranches: List<String>? by option(
        "--branch",
        help = "Filter to specific branches",
    )
        .varargValues()

    private val config by findOrSetObject { ConfigAndDataForSubcommand("", emptyList(), emptyList()) }
    private val cliVersion: String = object {}.javaClass.getResource("/version.txt")?.readText()?.lines()?.first() ?: ""

    init {
        eagerOption("-v", "--version", help = "Show version number and exit.") {
            throw PrintMessage("Vulnlog $cliVersion")
        }
    }

    override fun run() {
        echo("File to read: ${vulnlogFile.name}")

        val host = ScriptingHost()

        val files =
            vulnlogFile.parentFile
                .listFiles { file -> file.name.endsWith("vl.kts") && file.name != vulnlogFile.name }
                ?.toList() ?: emptyList()
        if (files.isNotEmpty()) {
            echo("Also read: ${files.joinToString(", ") { it.name }}")
        }
        val defFirst: List<File> = listOf(vulnlogFile).plus(files)

        val result = host.eval(defFirst)
        result.onFailure { error(it) }

        val vlDslRoot = result.getOrThrow() as VlDslRootImpl
        val branchRepository = vlDslRoot.branchRepository
        val vulnerabilityRepository = vlDslRoot.vulnerabilityDataRepository
        val splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> =
            vulnerabilityPerBranch(vulnerabilityRepository)

        val statusService = StatusService(ruleSet)
        val splitVulnsWithStatus = statusService.calculateStatus(splitVulnToBranches)

        val filterDsl = DslResultFilter(filterVulnerabilities, filterBranches)
        val filteredResult = filterDsl.filter(splitVulnsWithStatus, branchRepository.getBranchesToReleaseVersions())

        val translator = SerialisationTranslator()
        val serialisableData: Vulnlog = translator.translate(filteredResult)

        val printer = JsonPrinter(::echo)

        val subcommand = currentContext.invokedSubcommand
        if (subcommand == null) {
            printer.print(serialisableData)
        } else {
            val relevantReleaseBranches = serialisableData.releaseBranches
            config.cliVerison = cliVersion
            config.releaseBranches = relevantReleaseBranches.map(ReleaseBranche::releaseBranch)
            config.vulnlogs =
                relevantReleaseBranches.map { branch ->
                    Vulnlog(
                        listOf(branch),
                        serialisableData.releaseBrancheVulnerabilities
                            .filter { it.releaseBranch == branch.releaseBranch },
                    )
                }.map { printer.translate(it) }
        }
    }
}
