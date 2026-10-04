// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.formatMessage

/** Mirrors `Logger.log` in the Gradle plugin, with `-q`, `-v` and `-vv` in place of Gradle's log levels. */
fun CliktCommand.echoMessage(message: Message) {
    val diagnostics = diagnostics()
    when (message) {
        is Message.Status -> if (diagnostics.verbosity.statusEnabled) echoMessage(message.text)
        is Message.Warning -> echoMessage(formatMessage(FindingSeverity.WARNING, message.text))
        is Message.Verbose -> diagnostics.echoDiagnostic(DiagnosticLevel.VERBOSE, message.text)
        is Message.Debug -> diagnostics.echoDiagnostic(DiagnosticLevel.DEBUG, message.text)
    }
}
