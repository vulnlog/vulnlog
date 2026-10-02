// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import dev.vulnlog.lib.document.InputRead
import dev.vulnlog.lib.model.OutputWrite
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly

class ExitsTest :
    FunSpec({

        test("an absent baseline is a bad flag value, an unreadable one an I/O error") {
            val unavailable = listOf(OpenVexBaselineRead.Absent, OpenVexBaselineRead.Unreadable("Permission denied"))

            val codes = unavailable.map { exitCode(it) }

            codes shouldContainExactly listOf(ExitCode.INVALID_FLAG_VALUE, ExitCode.GENERAL_ERROR)
        }

        test("an input that cannot be read is an I/O error, whatever the reason") {
            val failed =
                listOf(
                    InputRead.Missing("a.vl.yaml"),
                    InputRead.Denied("a.vl.yaml"),
                    InputRead.Unreadable("a.vl.yaml", "Is a directory"),
                )

            val codes = failed.map { exitCode(it) }

            codes shouldContainExactly List(3) { ExitCode.GENERAL_ERROR }
        }

        test("an output that cannot be written is an I/O error, whatever the reason") {
            val failed =
                listOf(
                    OutputWrite.MissingDirectory("out/report.html"),
                    OutputWrite.Denied("out/report.html"),
                    OutputWrite.Unwritable("out/report.html", "Is a directory"),
                )

            val codes = failed.map { exitCode(it) }

            codes shouldContainExactly List(3) { ExitCode.GENERAL_ERROR }
        }
    })
