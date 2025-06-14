package dev.vulnlog.cli.commands

import Filtered
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
import dev.vulnlog.cli.service.RawVulnlogDslParser
import dev.vulnlog.cli.service.StatusService
import dev.vulnlog.cli.service.ruleSet
import dev.vulnlog.common.VulnerabilityDataPerBranch
import dev.vulnlog.dsl.ReleaseBranchData
import dev.vulnlog.dslinterpreter.repository.BranchRepository
import dev.vulnlog.dslinterpreter.repository.VulnerabilityDataRepository
import dev.vulnlog.dslinterpreter.splitter.vulnerabilityPerBranch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.io.File

data class ConfigAndDataForSubcommand(
    var cliVersion: String,
    var releaseBranches: List<String>,
    var vulnlogs: List<String>,
    var filteredResult: Filtered?,
)

class MainCommand : CliktCommand(), KoinComponent {
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

    private val config by findOrSetObject { ConfigAndDataForSubcommand("", emptyList(), emptyList(), null) }
    private val cliVersion: String = object {}.javaClass.getResource("/version.txt")?.readText()?.lines()?.first() ?: ""

    init {
        eagerOption("-v", "--version", help = "Show version number and exit.") {
            throw PrintMessage("Vulnlog $cliVersion")
        }
    }

    private val rawVulnlogDslParser: RawVulnlogDslParser by inject { parametersOf(Output(::echo)) }
    private val translator: SerialisationTranslator by inject()
    private val statusService: StatusService by inject { parametersOf(ruleSet) }
    private val printer: JsonPrinter by inject { parametersOf(Output(::echo)) }
    private val branchRepository: BranchRepository by inject()
    private val vulnerabilityRepository: VulnerabilityDataRepository by inject()

    override fun run() {
        // 1. parse vulnlog files
        // 2. enhance with additional information before split (e.g. calculate the status)
        // 3. split to single id entry
        // 4. filter branch, vulnerability and reporter
        // 5. enhance with additional information (optional step)
        // 6. pass data to subseteps

        // parse the raw Vulnlog DSL definition file and the surrounding Vulnlog files.
        rawVulnlogDslParser.readAndParse(vulnlogFile)

        val splitVulnToBranches: Map<ReleaseBranchData, List<VulnerabilityDataPerBranch>> =
            vulnerabilityPerBranch(vulnerabilityRepository)

        val splitVulnsWithStatus = statusService.calculateStatus(splitVulnToBranches)

        val filterDsl = DslResultFilter(filterVulnerabilities, filterBranches)
        val filteredResult: Filtered =
            filterDsl.filter(splitVulnsWithStatus, branchRepository.getBranchesToReleaseVersions())

        val serialisableData: Vulnlog = translator.translate(filteredResult)

        config.cliVersion = cliVersion

        when (currentContext.invokedSubcommand) {
            is ReportCommand -> configureReportCommand(serialisableData)
            is SuppressCommand -> configureSuppressCommand(filteredResult)
            else -> printer.print(serialisableData)
        }
    }

    private fun configureReportCommand(serialisableData: Vulnlog) {
        val relevantReleaseBranches = serialisableData.releaseBranches
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

    private fun configureSuppressCommand(filteredResult: Filtered) {
        config.filteredResult = filteredResult
    }
}
