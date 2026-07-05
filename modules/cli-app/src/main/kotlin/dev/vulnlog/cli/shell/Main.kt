// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.vulnlog.cli.BuildInfo

fun main(args: Array<String>) =
    VulnlogCli()
        .subcommands(InitCommand())
        .subcommands(ValidateCommand())
        .subcommands(FmtCommand())
        .subcommands(SuppressCommand())
        .subcommands(ReportCommand())
        .subcommands(ModifyCommand())
        .main(args)

class VulnlogCli : CliktCommand(name = "vulnlog") {
    init {
        versionOption(BuildInfo.VERSION)
    }

    override fun helpEpilog(context: Context): String =
        "Questions or feedback? Join the discussion at https://github.com/vulnlog/vulnlog/discussions"

    override fun run() = Unit
}
