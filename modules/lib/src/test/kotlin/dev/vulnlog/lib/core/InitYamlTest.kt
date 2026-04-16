// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.createYamlMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InitYamlTest :
    FunSpec({

        test("init produces valid yaml output") {
            val file: VulnlogFile = init(SchemaVersion(1, 0), "acme", "widget", "alice")
            val yaml: String = YamlWriter.write(file, createYamlMapper())

            yaml shouldBe
                """
            |---
            |schemaVersion: "1"
            |
            |project:
            |  organization: "acme"
            |  name: "widget"
            |  author: "alice"
            |
            |releases: []
            |
            |vulnerabilities: []
            |
                """.trimMargin()
        }
    })
