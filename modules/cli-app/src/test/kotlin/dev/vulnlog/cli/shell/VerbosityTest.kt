// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VerbosityTest :
    FunSpec({

        test("each -v adds a level: verbose lines from 1, debug lines and stack traces from 2") {
            val verbosities = (0..2).map { Verbosity(level = it) }

            val enabled =
                verbosities.map {
                    listOf(it.enables(DiagnosticLevel.VERBOSE), it.enables(DiagnosticLevel.DEBUG), it.stackTraces)
                }

            enabled shouldBe
                listOf(listOf(false, false, false), listOf(true, false, false), listOf(true, true, true))
        }

        test("quiet turns the status lines off") {
            val verbosities = listOf(Verbosity(), Verbosity(quiet = true))

            val enabled = verbosities.map { it.statusEnabled }

            enabled shouldBe listOf(true, false)
        }
    })
