package ch.addere.cli.command

import com.github.ajalt.clikt.core.CliktCommand

class MainCommand : CliktCommand(
    help =
        """
        CLI tool to interpret vulnerability log files.
        """.trimIndent(),
) {
    override fun run() {
        // currently only subcommands are support
    }
}
