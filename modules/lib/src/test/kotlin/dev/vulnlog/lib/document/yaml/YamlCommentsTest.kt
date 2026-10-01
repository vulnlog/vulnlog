// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.yaml

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.nodes.Node

private const val WITH_SCHEMA_HEADER = "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json\n---\nkey: value\n"

private fun rootOf(content: String): Node =
    Compose(LoadSettings.builder().setParseComments(true).build()).composeString(content).get()

class YamlCommentsTest :
    FunSpec({

        test("detects block, inline and trailing comments") {
            val contents = listOf("key: value\n# note\nother: x\n", "key: value # note\n", "key: value\n# trailing\n")

            val detected = contents.map { hasYamlComments(rootOf(it)) }

            detected shouldBe listOf(true, true, true)
        }

        test("does not count the schema header or blank lines as comments") {
            val contents = listOf(WITH_SCHEMA_HEADER, "key: value\n\nother: x\n", "key: value\n")

            val detected = contents.map { hasYamlComments(rootOf(it)) }

            detected shouldBe listOf(false, false, false)
        }

        test("detects the schema header but no other comment as one") {
            val contents = listOf(WITH_SCHEMA_HEADER, "key: value\n# note\n")

            val detected = contents.map { hasSchemaHeader(rootOf(it)) }

            detected shouldBe listOf(true, false)
        }
    })
