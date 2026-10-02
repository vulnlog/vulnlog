// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.document.InputRead
import dev.vulnlog.lib.model.OutputWrite
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.VerificationException

class OutcomesTest :
    FunSpec({

        test("a bad scope or baseline is configuration to fix, an empty document is a verdict") {
            val empty = OpenVexCollection(OpenVexScope(), emptyList(), emptyMap(), emptyList(), emptyList())
            val failures =
                listOf(
                    FilterRejected(emptyList()),
                    OpenVexOutcome.BaselineRejected(OpenVexBaselineProblem.NotOpenVex),
                    OpenVexOutcome.NoStatementApplies(empty, OpenVexEmptyReason.NO_RELEASE_DECLARES_PURLS),
                )

            val exceptions = failures.map { failure(it, "vex.json") }

            exceptions.map { it::class } shouldContainExactly
                listOf(InvalidUserDataException::class, InvalidUserDataException::class, VerificationException::class)
        }

        test("an unreadable baseline is configuration to fix") {
            val unreadable = OpenVexBaselineRead.Unreadable("Is a directory")

            val exception = failure(unreadable, "vex.json")

            exception::class shouldBe InvalidUserDataException::class
        }

        test("an input that cannot be read is configuration to fix, worded in sentences") {
            val failed =
                listOf(
                    InputRead.Missing("a.vl.yaml"),
                    InputRead.Denied("a.vl.yaml"),
                    InputRead.Unreadable("a.vl.yaml", "Is a directory"),
                )

            val exceptions = failed.map(::failure)

            exceptions.map { it::class } shouldContainExactly List(3) { InvalidUserDataException::class }
            exceptions.first().message shouldBe "Cannot read a.vl.yaml: it does not exist. Check the path."
        }

        test("an output that cannot be written is configuration to fix, worded in sentences") {
            val failed =
                listOf(
                    OutputWrite.MissingDirectory("out/report.html"),
                    OutputWrite.Denied("out/report.html"),
                    OutputWrite.Unwritable("out/report.html", "Is a directory"),
                )

            val exceptions = failed.map(::failure)

            exceptions.map { it::class } shouldContainExactly List(3) { InvalidUserDataException::class }
            exceptions[1].message shouldBe
                "Cannot write out/report.html: permission denied. Make the location writable for this user."
        }
    })
