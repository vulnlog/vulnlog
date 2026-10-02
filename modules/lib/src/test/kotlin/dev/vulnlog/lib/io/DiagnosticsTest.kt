// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DiagnosticsTest :
    FunSpec({

        test("verbose and debug hand an event of their level to the sink") {
            val events = mutableListOf<DiagnosticEvent>()
            val sink = DiagnosticSink(events::add)

            sink.verbose("parsed file")
            sink.debug("timing")

            events shouldBe
                listOf(
                    DiagnosticEvent(DiagnosticLevel.VERBOSE, "parsed file"),
                    DiagnosticEvent(DiagnosticLevel.DEBUG, "timing"),
                )
        }

        test("renderDiagnostic prefixes the message with its level") {
            val events =
                listOf(
                    DiagnosticEvent(DiagnosticLevel.VERBOSE, "parsed file"),
                    DiagnosticEvent(DiagnosticLevel.DEBUG, "timing"),
                )

            val lines = events.map(::renderDiagnostic)

            lines shouldBe listOf("verbose: parsed file", "debug: timing")
        }
    })
