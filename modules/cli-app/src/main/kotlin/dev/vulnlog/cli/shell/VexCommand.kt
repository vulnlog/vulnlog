// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class VexCommand : CliktCommand(name = "vex") {
    override fun help(context: Context): String =
        "Generate Vulnerability Exploitability eXchange (VEX) files from Vulnlog files."

    init {
        subcommands(
            OpenVexCommand(),
        )
    }

    override val invokeWithoutSubcommand: Boolean = true

    override fun run() = requireSubcommand()
}
