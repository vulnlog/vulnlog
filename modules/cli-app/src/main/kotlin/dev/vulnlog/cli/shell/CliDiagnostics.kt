// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand

class CliDiagnostics(
    val verbosity: Verbosity,
    private val echoErr: (String) -> Unit,
) {
    fun echoDiagnostic(
        level: DiagnosticLevel,
        text: String,
    ) {
        if (verbosity.enables(level)) echoErr("${level.name.lowercase()}: $text")
    }
}

fun CliktCommand.diagnostics(): CliDiagnostics =
    currentContext.findObject<CliDiagnostics>() ?: CliDiagnostics(Verbosity()) { echo(it, err = true) }
