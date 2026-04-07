package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.VulnlogFile
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
