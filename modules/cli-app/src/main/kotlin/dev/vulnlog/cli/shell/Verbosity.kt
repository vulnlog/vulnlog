// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

data class Verbosity(
    val level: Int = 0,
    val quiet: Boolean = false,
) {
    val stackTraces: Boolean get() = level >= 2

    val statusEnabled: Boolean get() = !quiet

    fun enables(diagnosticLevel: DiagnosticLevel): Boolean =
        when (diagnosticLevel) {
            DiagnosticLevel.VERBOSE -> level >= 1
            DiagnosticLevel.DEBUG -> level >= 2
        }
}
