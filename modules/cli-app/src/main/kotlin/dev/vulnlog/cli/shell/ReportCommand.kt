// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class ReportCommand : CliktCommand(name = "report") {
    override fun help(context: Context): String = "Generate reports from Vulnlog files."

    init {
        subcommands(
            ImpactReportCommand(),
        )
    }

    override val invokeWithoutSubcommand: Boolean = true

    override fun run() = requireSubcommand()
}
