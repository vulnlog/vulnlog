// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.io.DiagnosticLevel
import dev.vulnlog.lib.io.DiagnosticSink
import dev.vulnlog.lib.render.Message
import org.gradle.api.DefaultTask

fun DefaultTask.diagnosticSink(): DiagnosticSink =
    DiagnosticSink { event ->
        when (event.level) {
            DiagnosticLevel.VERBOSE -> logger.log(Message.Verbose(event.message))
            DiagnosticLevel.DEBUG -> logger.log(Message.Debug(event.message))
        }
    }
