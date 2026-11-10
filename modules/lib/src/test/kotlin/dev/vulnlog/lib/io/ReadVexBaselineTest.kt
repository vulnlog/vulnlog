// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.io

import dev.vulnlog.lib.model.vex.VexBaselineRead
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.io.path.writeText

class ReadVexBaselineTest :
    FunSpec({

        test("reads the text of an existing file") {
            val file = tempdir().toPath().resolve("vex.json").apply { writeText("{}") }

            val read = readVexBaseline(file)

            read shouldBe VexBaselineRead.Present("{}")
        }

        test("finds nothing where no file exists") {
            val path = tempdir().toPath().resolve("vex.json")

            val read = readVexBaseline(path)

            read shouldBe VexBaselineRead.Absent
        }

        test("cannot read a directory, and says why") {
            val directory = tempdir().toPath()

            val read = readVexBaseline(directory)

            read.shouldBeInstanceOf<VexBaselineRead.Unreadable>().reason.shouldNotBeBlank()
        }
    })
