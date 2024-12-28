package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
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
        .help("The Vulnlog files to read")

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

    private val filterVersions: List<String>? by option(
        "--release",
        help = "Filter to specific releases",
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

        if (vulnlogFile.name.contains("vl3")) {
            val files =
                vulnlogFile.parentFile
                    .listFiles { file -> file.name.endsWith("vl3.kts") && file.name != vulnlogFile.name }
                    ?.toList() ?: emptyList()
            if (files.isNotEmpty()) {
                echo("Also read: ${files.joinToString(", ") { it.name }}")
            }
            val defFirst: List<File> = listOf(vulnlogFile).plus(files)

            val result = host.eval3(defFirst)
            val printer = SimplePrinter3(::echo, filterVulnerabilities, filterBranches)
            printer.printNicely(result)
        } else if (vulnlogFile.name.contains("vl2")) {
            val result = host.eval2(vulnlogFile)
            val printer = SimplePrinter2(::echo, filterVulnerabilities, filterBranches, filterVersions)
            printer.printNicely(result)
        } else {
            val result = host.eval(vulnlogFile)
            val printer = SimplePrinter(::echo, filterVulnerabilities, filterBranches, filterVersions)
            printer.printNicely(result)
        }
    }
}
