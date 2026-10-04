// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

/** Each driver installs a sink that filters by its verbosity and writes to its own channel. */
fun interface DiagnosticSink {
    fun accept(event: DiagnosticEvent)

    fun verbose(message: String) = accept(DiagnosticEvent(DiagnosticLevel.VERBOSE, message))

    fun debug(message: String) = accept(DiagnosticEvent(DiagnosticLevel.DEBUG, message))
}

data class DiagnosticEvent(
    val level: DiagnosticLevel,
    val message: String,
)

enum class DiagnosticLevel { VERBOSE, DEBUG }

fun renderDiagnostic(event: DiagnosticEvent): String = "${event.level.name.lowercase()}: ${event.message}"
