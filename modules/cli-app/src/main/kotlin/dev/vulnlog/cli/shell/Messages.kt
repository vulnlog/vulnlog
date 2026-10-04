// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.core.CliktCommand
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.formatMessage

/** Mirrors [log] for the CLI */
fun CliktCommand.echoMessage(message: Message) =
    when (message) {
        is Message.Status -> echoStatus(message.text)
        is Message.Warning -> echoMessage(formatMessage(FindingSeverity.WARNING, message.text))
        is Message.Verbose -> diagnosticSink().verbose(message.text)
        is Message.Debug -> diagnosticSink().debug(message.text)
    }
