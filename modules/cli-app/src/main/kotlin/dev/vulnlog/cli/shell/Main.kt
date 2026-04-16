// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.vulnlog.cli.BuildInfo

fun main(args: Array<String>) =
    VulnlogCli()
        .subcommands(InitCommand())
        .subcommands(ValidateCommand())
        .subcommands(SuppressCommand())
        .subcommands(ReportCommand())
        .subcommands(CopyCommand())
        .main(args)

class VulnlogCli : CliktCommand(name = "vulnlog") {
    init {
        versionOption(BuildInfo.VERSION)
    }

    override fun run() = Unit
}
