// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import dev.vulnlog.lib.shell.DiagnosticSink
import dev.vulnlog.lib.shell.renderDiagnostic

class CliDiagnostics(
    val verbosity: Verbosity,
    private val echoErr: (String) -> Unit,
) {
    val sink: DiagnosticSink =
        DiagnosticSink { event ->
            if (verbosity.enables(event.level)) echoErr(renderDiagnostic(event))
        }
}

fun CliktCommand.diagnostics(): CliDiagnostics =
    currentContext.findObject<CliDiagnostics>() ?: CliDiagnostics(Verbosity()) { echo(it, err = true) }

fun CliktCommand.diagnosticSink(): DiagnosticSink = diagnostics().sink

fun CliktCommand.echoStatus(message: String) {
    if (diagnostics().verbosity.statusEnabled) echoMessage(message)
}
