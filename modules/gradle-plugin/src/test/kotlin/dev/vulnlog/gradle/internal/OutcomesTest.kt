// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineProblem
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
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
    })
