// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import org.snakeyaml.engine.v2.nodes.Node

private fun rootOf(content: String): Node =
    Compose(LoadSettings.builder().setParseComments(true).build()).composeString(content).get()

class YamlCommentsTest :
    FunSpec({

        test("detects a block comment") {
            hasYamlComments(rootOf("key: value\n# note\nother: x\n")) shouldBe true
        }

        test("detects an inline comment") {
            hasYamlComments(rootOf("key: value # note\n")) shouldBe true
        }

        test("detects a trailing comment") {
            hasYamlComments(rootOf("key: value\n# trailing\n")) shouldBe true
        }

        test("ignores the schema header") {
            val content = "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json\n---\nkey: value\n"
            hasYamlComments(rootOf(content)) shouldBe false
        }

        test("ignores blank lines") {
            hasYamlComments(rootOf("key: value\n\nother: x\n")) shouldBe false
        }

        test("returns false for comment-free content") {
            hasYamlComments(rootOf("key: value\n")) shouldBe false
        }

        test("detects the schema header") {
            val content = "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json\n---\nkey: value\n"
            hasSchemaHeader(rootOf(content)) shouldBe true
        }

        test("reports no schema header for plain content") {
            hasSchemaHeader(rootOf("key: value\n# note\n")) shouldBe false
        }
    })
