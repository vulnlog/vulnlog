package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.eagerOption
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.dslinterpreter.ScriptingHost
import java.io.File

class MainCommand : CliktCommand(
    help =
        """
        CLI application to parse Vulnlog files.
        """.trimIndent(),
) {
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

    init {
        eagerOption("-v", "--version", help = "Show version number and exit.") {
            val version = object {}.javaClass.getResource("/version.txt")?.readText()?.lines()?.first() ?: ""
            throw PrintMessage("Vulnlog $version")
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

        val filterDsl = DslResultFilter(filterVulnerabilities, filterBranches)
        val filteredResult = filterDsl.filter(result.getOrThrow())

        val translator = SerialisationTranslator()
        val serialisableData = translator.translate(filteredResult)

        val printer = JsonPrinter(::echo)
        printer.print(serialisableData)
    }
}
