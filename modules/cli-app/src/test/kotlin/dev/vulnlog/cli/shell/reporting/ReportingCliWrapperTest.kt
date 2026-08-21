// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.reporting

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.testing.test
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.VulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val acme = Project("Acme Corp", "Acme Web App", "Acme Corp Security Team")
private val other = Project("Other Corp", "Other App", "Other Security Team")

/** Drives the wrapper on its own and echoes the project it settled on. */
private class WrapperCommand(
    private val files: List<VulnlogFile>,
) : CliktCommand(name = "wrapper") {
    override fun run() {
        val project = sharedProjectOrFail(files)
        echo("project=${project.name}", err = true)
    }
}

private fun sharedProject(files: List<VulnlogFile>) = WrapperCommand(files).test("")

class ReportingCliWrapperTest :
    FunSpec({

        context("files that agree on the project") {

            test("hands back the shared project") {
                val files = listOf(vulnlogFile(project = acme), vulnlogFile(project = acme))

                val result = sharedProject(files)

                result.statusCode shouldBe 0
                result.stderr shouldContain "project=Acme Web App"
            }

            test("hands back the project of a single file") {
                val files = listOf(vulnlogFile(project = acme))

                val result = sharedProject(files)

                result.statusCode shouldBe 0
                result.stderr shouldContain "project=Acme Web App"
            }
        }

        context("files that disagree on the project") {

            test("fails with the validation error exit code") {
                val files = listOf(vulnlogFile(project = acme), vulnlogFile(project = other))

                val result = sharedProject(files)

                result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
            }

            test("says the inputs must share their project metadata") {
                val files = listOf(vulnlogFile(project = acme), vulnlogFile(project = other))

                val result = sharedProject(files)

                result.stderr shouldContain "must share the same project metadata"
            }
        }
    })
