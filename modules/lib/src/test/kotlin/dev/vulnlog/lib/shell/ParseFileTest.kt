// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.core.parsed
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

private val MINIMAL_YAML =
    """
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15

    vulnerabilities:

      - id: CVE-2026-1234
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
        analysis: not reachable
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

class ParseFileTest :
    FunSpec({

        context("renderParsedInputs") {

            test("states the schema version and entry counts") {
                val input = FileInputOption.File(Path.of("test.vl.yaml"))

                val lines = renderParsedInputs(mapOf(input to parsed(MINIMAL_YAML)))

                lines shouldBe
                    listOf(
                        "parsed test.vl.yaml: schema version 1, releases: 1, tags: 0, vulnerabilities: 1",
                    )
            }

            test("sorts lines by file name") {
                val second = FileInputOption.File(Path.of("b.vl.yaml"))
                val first = FileInputOption.File(Path.of("a.vl.yaml"))
                val ok = parsed(MINIMAL_YAML)

                val lines = renderParsedInputs(mapOf(second to ok, first to ok))

                lines.map { it.substringBefore(":") } shouldBe listOf("parsed a.vl.yaml", "parsed b.vl.yaml")
            }

            test("renders nothing for an empty map") {
                renderParsedInputs(emptyMap()) shouldBe emptyList()
            }
        }
    })
