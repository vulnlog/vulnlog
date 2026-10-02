// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.io.DiagnosticLevel
import dev.vulnlog.lib.io.DiagnosticSink
import org.gradle.api.DefaultTask

/**
 * Maps diagnostic events to the Gradle logger, so `gradle --info` and `--debug` show them without
 * extra task properties. Gradle's own log levels replace the CLI's `verbose:`/`debug:` prefixes.
 */
fun DefaultTask.diagnosticSink(): DiagnosticSink =
    DiagnosticSink { event ->
        when (event.level) {
            DiagnosticLevel.VERBOSE -> logger.info(event.message)
            DiagnosticLevel.DEBUG -> logger.debug(event.message)
        }
    }
