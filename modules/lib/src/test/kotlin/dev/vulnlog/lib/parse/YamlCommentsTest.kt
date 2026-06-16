// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.VulnlogFileRaw
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class YamlCommentsTest :
    FunSpec({

        test("detects a block comment") {
            hasYamlComments(VulnlogFileRaw("key: value\n# note\nother: x\n")) shouldBe true
        }

        test("detects an inline comment") {
            hasYamlComments(VulnlogFileRaw("key: value # note\n")) shouldBe true
        }

        test("ignores the schema header") {
            val content = "# \$schema: https://vulnlog.dev/schema/vulnlog-v1.json\n---\nkey: value\n"
            hasYamlComments(VulnlogFileRaw(content)) shouldBe false
        }

        test("ignores blank lines") {
            hasYamlComments(VulnlogFileRaw("key: value\n\nother: x\n")) shouldBe false
        }

        test("returns false for comment-free content") {
            hasYamlComments(VulnlogFileRaw("key: value\n")) shouldBe false
        }
    })
