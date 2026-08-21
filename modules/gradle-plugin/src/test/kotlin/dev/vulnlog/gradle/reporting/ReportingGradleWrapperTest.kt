// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.reporting

import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Project
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.GradleException

private val acme = Project("Acme Corp", "Acme Web App", "Acme Corp Security Team")
private val other = Project("Other Corp", "Other App", "Other Security Team")

class ReportingGradleWrapperTest :
    FunSpec({

        context("files that agree on the project") {

            test("hands back the shared project") {
                val files = listOf(vulnlogFile(project = acme), vulnlogFile(project = acme))

                val project = sharedProjectOrFail(files)

                project shouldBe acme
            }

            test("hands back the project of a single file") {
                val files = listOf(vulnlogFile(project = acme))

                val project = sharedProjectOrFail(files)

                project shouldBe acme
            }
        }

        context("files that disagree on the project") {

            test("fails the build naming the project metadata") {
                val files = listOf(vulnlogFile(project = acme), vulnlogFile(project = other))

                val failure = shouldThrow<GradleException> { sharedProjectOrFail(files) }

                failure.message.orEmpty() shouldContain "must share the same project metadata"
            }
        }
    })
