package dev.vulnlog.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.file
import dev.vulnlog.dslinterpreter.ScriptingHost
import java.io.File

class MainCommand : CliktCommand(
    help =
        """
        CLI application to parse and interpret Vulnlog files.
        """.trimIndent(),
) {
    private val vulnlogFile: File by argument()
        .file(mustExist = true, canBeDir = false)
        .help("The Vulnlog files to read.")

    private val filterVulnerabilities: List<String>? by option(
        "--vuln",
        help = "Filter to specific vulnerabilities.",
    )
        .varargValues()
    private val filterBranches: List<String>? by option(
        "--branch",
        help = "Filter to specific branches.",
    )
        .varargValues()

    private val filterVersions: List<String>? by option(
        "--release",
        help = "Filter to specific releases.",
    )
        .varargValues()

    override fun run() {
        echo("File to read: ${vulnlogFile.name}")
        val host = ScriptingHost()
        val result = host.eval(vulnlogFile)
        val printer = SimplePrinter(::echo, filterVulnerabilities, filterBranches, filterVersions)
        printer.printNicely(result)
    }
}
