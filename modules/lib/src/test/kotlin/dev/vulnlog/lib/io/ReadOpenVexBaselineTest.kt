// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.io.path.writeText

class ReadOpenVexBaselineTest :
    FunSpec({

        test("reads the text of an existing file") {
            val file = tempdir().toPath().resolve("vex.json").apply { writeText("{}") }

            val read = readOpenVexBaseline(file)

            read shouldBe OpenVexBaselineRead.Present("{}")
        }

        test("finds nothing where no file exists") {
            val path = tempdir().toPath().resolve("vex.json")

            val read = readOpenVexBaseline(path)

            read shouldBe OpenVexBaselineRead.Absent
        }

        test("cannot read a directory, and says why") {
            val directory = tempdir().toPath()

            val read = readOpenVexBaseline(directory)

            read.shouldBeInstanceOf<OpenVexBaselineRead.Unreadable>().reason.shouldNotBeBlank()
        }
    })
