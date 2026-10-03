// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

/** Records CVE-2026-1234. */
private val SOURCE = vulnlogDocument()
private val TARGET = vulnlogDocument(vulnId = "CVE-2026-5678")

/** The plugin registers no copy task, so the build registers one as the docs show. */
private fun copyProject(vulnId: String) =
    gradleProject(
        buildFile(
            """
            tasks.register<dev.vulnlog.gradle.VulnlogCopyTask>("vulnlogCopy") {
                sourceFile.set(layout.projectDirectory.file("source.vl.yaml"))
                destinationFiles.from("target.vl.yaml")
                vulnIds.set(setOf("$vulnId"))
            }
            """.trimIndent(),
        ),
        "source.vl.yaml" to SOURCE,
        "target.vl.yaml" to TARGET,
    )

class VulnlogCopyTaskTest :
    FunSpec({

        test("copies the entry into the target and says so") {
            val dir = copyProject("CVE-2026-1234")

            val result = runner(dir, "vulnlogCopy").build()

            result.task(":vulnlogCopy")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "Copied: 1 entry to "
            dir.resolve("target.vl.yaml").readText() shouldContain "CVE-2026-1234"
        }

        test("fails and leaves the target alone when the source lacks an id") {
            val dir = copyProject("CVE-2026-0000")

            val result = runner(dir, "vulnlogCopy").buildAndFail()

            result.task(":vulnlogCopy")?.outcome shouldBe TaskOutcome.FAILED
            result.output shouldContain "Vulnerability IDs not found in "
            dir.resolve("target.vl.yaml").readText() shouldBe TARGET
        }
    })
