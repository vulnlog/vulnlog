// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.vulnlog.cli.BuildInfo
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.result.Severity
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(runVulnlog(vulnlogCommand(), args.asList()))
}

fun vulnlogCommand(): VulnlogCli =
    VulnlogCli()
        .subcommands(InitCommand())
        .subcommands(ValidateCommand())
        .subcommands(FmtCommand())
        .subcommands(SuppressCommand())
        .subcommands(ReportCommand())
        .subcommands(ModifyCommand())

internal fun runVulnlog(
    cli: VulnlogCli,
    args: List<String>,
    printError: (String) -> Unit = { message -> cli.echoSafely(message) },
): Int =
    try {
        cli.parse(args)
        ExitCode.SUCCESS.code
    } catch (e: CliktError) {
        cli.echoFormattedHelp(e)
        e.statusCode
    } catch (e: Exception) {
        renderUnexpectedError(e, cli.verbosity.stackTraces).forEach(printError)
        ExitCode.GENERAL_ERROR.code
    }

internal fun renderUnexpectedError(
    e: Throwable,
    stackTrace: Boolean,
): List<String> =
    listOfNotNull(
        formatMessage(Severity.ERROR, e.message ?: e::class.simpleName ?: "unknown error"),
        e.stackTraceToString().takeIf { stackTrace },
    )

private fun VulnlogCli.echoSafely(message: String) =
    try {
        echo(message, err = true)
    } catch (_: IllegalStateException) {
        System.err.println(message)
    }

class VulnlogCli : CliktCommand(name = "vulnlog") {
    init {
        versionOption(BuildInfo.VERSION)
    }

    private val verbose: Int by option(
        "-v",
        "--verbose",
        help = "Print verbose diagnostics on stderr. Repeat (-vv) for more verbose output.",
    ).counted(limit = 2)

    private val quiet: Boolean by option(
        "-q",
        "--quiet",
        help = "Suppress status lines. Errors and warnings still print.",
    ).flag()

    var verbosity: Verbosity = Verbosity()

    override fun helpEpilog(context: Context): String =
        "Questions or feedback? Join the discussion at https://github.com/vulnlog/vulnlog/discussions"

    override fun run() {
        if (quiet && verbose > 0) {
            throw UsageError("Option --quiet cannot be combined with --verbose.")
        }
        verbosity = Verbosity(level = verbose, quiet = quiet)
        currentContext.obj = CliDiagnostics(verbosity) { message -> echo(message, err = true) }
    }
}
