// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class ModifyCommand : CliktCommand(name = "modify") {
    override fun help(context: Context): String = "Modify Vulnlog files (e.g. copy entries between files)."

    init {
        subcommands(CopyCommand())
    }

    override fun run() = Unit
}
