// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DiagnosticsTest :
    FunSpec({

        test("NONE discards events") {
            DiagnosticSink.NONE.verbose("ignored")
            DiagnosticSink.NONE.debug("ignored")
        }

        test("verbose builds a verbose event") {
            val events = mutableListOf<DiagnosticEvent>()
            val sink = DiagnosticSink(events::add)

            sink.verbose("parsed file")

            events shouldBe listOf(DiagnosticEvent(DiagnosticLevel.VERBOSE, "parsed file"))
        }

        test("debug builds a debug event") {
            val events = mutableListOf<DiagnosticEvent>()
            val sink = DiagnosticSink(events::add)

            sink.debug("timing")

            events shouldBe listOf(DiagnosticEvent(DiagnosticLevel.DEBUG, "timing"))
        }

        context("renderDiagnostic") {

            test("prefixes verbose events") {
                renderDiagnostic(DiagnosticEvent(DiagnosticLevel.VERBOSE, "parsed file")) shouldBe
                    "verbose: parsed file"
            }

            test("prefixes debug events") {
                renderDiagnostic(DiagnosticEvent(DiagnosticLevel.DEBUG, "timing")) shouldBe "debug: timing"
            }
        }
    })
