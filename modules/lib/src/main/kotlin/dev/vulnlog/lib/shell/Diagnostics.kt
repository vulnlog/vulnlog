// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

/**
 * Receiver for optional diagnostic events. Shells install a sink that filters by the requested
 * verbosity and writes to their own output channel; [NONE] discards everything.
 */
fun interface DiagnosticSink {
    fun accept(event: DiagnosticEvent)

    fun verbose(message: String) = accept(DiagnosticEvent(DiagnosticLevel.VERBOSE, message))

    fun debug(message: String) = accept(DiagnosticEvent(DiagnosticLevel.DEBUG, message))

    companion object {
        val NONE: DiagnosticSink = DiagnosticSink { }
    }
}

data class DiagnosticEvent(
    val level: DiagnosticLevel,
    val message: String,
)

enum class DiagnosticLevel { VERBOSE, DEBUG }

fun renderDiagnostic(event: DiagnosticEvent): String = "${event.level.name.lowercase()}: ${event.message}"
