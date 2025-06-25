package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.eagerOption
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.cli.service.RawVulnlogDslParserService
import dev.vulnlog.cli.service.VulnEntryFilterService
import dev.vulnlog.cli.utils.Output
import dev.vulnlog.common.SubcommandData
import dev.vulnlog.common.model.BranchName
import dev.vulnlog.common.model.VulnEntry
import dev.vulnlog.common.repository.BranchRepository
import dev.vulnlog.dslinterpreter.splitter.VulnEntrySplitter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import java.io.File

class MainCommand : CliktCommand(), KoinComponent {
    override fun help(context: Context): String = "CLI application to parse Vulnlog files."

    override val invokeWithoutSubcommand = true

    private val vulnlogFile: File by argument()
        .file(mustExist = true, canBeDir = false)
        .help("The Vulnlog definition files to read.")
        .check("File must be a Vulnlog definition file (definitions.vl.kts)") { it.name == "definitions.vl.kts" }

    private val filterVulnerabilities: List<String> by option(
        "--vuln",
        help = "Filter to specific vulnerabilities",
    )
        .varargValues()
        .default(emptyList())

    private val filterBranches: List<String> by option(
        "--branch",
        help = "Filter to specific branches",
    )
        .varargValues()
        .default(emptyList())

    private val config by findOrSetObject { SubcommandData("", emptyList(), emptySet()) }
    private val cliVersion: String = object {}.javaClass.getResource("/version.txt")?.readText()?.lines()?.first() ?: ""

    init {
        eagerOption("-v", "--version", help = "Show version number and exit.") {
            throw PrintMessage("Vulnlog $cliVersion")
        }
    }

    private val rawVulnlogDslParserService: RawVulnlogDslParserService by inject { parametersOf(Output(::echo)) }
    private val vulnEntrySplitter: VulnEntrySplitter by inject()
    private val filter: VulnEntryFilterService by inject { parametersOf(filterVulnerabilities, filterBranches) }
    private val releaseBranchRepository: BranchRepository by inject()

    override fun run() {
        config.cliVersion = cliVersion

        rawVulnlogDslParserService.readAndParse(vulnlogFile)

        val splitVulnEntries: List<VulnEntry> = vulnEntrySplitter.split()
        val filteredVulnEntries: List<VulnEntry> = filter.filterVulnEntries(splitVulnEntries)
        val filteredReleaseBranches: Set<BranchName> =
            filter.filterReleaseBranches(releaseBranchRepository.getAllBranches())

        when (currentContext.invokedSubcommand) {
            is ReportCommand -> configureReportCommand(filteredVulnEntries, filteredReleaseBranches)
            is SuppressCommand -> configureSuppressCommand(filteredVulnEntries, filteredReleaseBranches)
        }
    }

    private fun configureReportCommand(
        vulnEntries: List<VulnEntry>,
        releaseBranches: Set<BranchName>,
    ) {
        config.vulnEntriesFiltered = vulnEntries
        config.releaseBranchesFiltered = releaseBranches
    }

    private fun configureSuppressCommand(
        vulnEntries: List<VulnEntry>,
        releaseBranches: Set<BranchName>,
    ) {
        config.vulnEntriesFiltered = vulnEntries
        config.releaseBranchesFiltered = releaseBranches
    }
}
