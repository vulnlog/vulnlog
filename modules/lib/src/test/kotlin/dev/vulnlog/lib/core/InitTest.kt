// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.VulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InitTest :
    FunSpec({

        test("init returns VulnlogFile with given schema version") {
            val file: VulnlogFile = init(SchemaVersion(2, 1), "acme", "widget", "alice")

            file.schemaVersion shouldBe SchemaVersion(2, 1)
            file.project shouldBe Project("acme", "widget", "alice")
            file.releases shouldBe emptyList()
            file.vulnerabilities shouldBe emptyList()
        }
    })
