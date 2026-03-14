package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.vulnlog.cli.BuildInfo

fun main(args: Array<String>) =
    VulnlogCli()
        .subcommands(InitCommand())
        .main(args)

class VulnlogCli : CliktCommand(name = "vulnlog") {
    init {
        versionOption(BuildInfo.VERSION)
    }

    override fun run() = Unit
}
