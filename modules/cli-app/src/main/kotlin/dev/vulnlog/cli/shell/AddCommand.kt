// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class AddCommand : CliktCommand(name = "add") {
    override fun help(context: Context): String = "Add a new vulnerability entry to Vulnlog files."

    init {
        subcommands(AddVulnerabilitiesCommand())
    }

    override fun run() = Unit
}
