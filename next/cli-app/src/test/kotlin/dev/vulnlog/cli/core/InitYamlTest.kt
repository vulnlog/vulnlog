package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.parse.YamlWriter
import dev.vulnlog.cli.parse.createYamlMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class InitYamlTest : FunSpec({

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
            |  project: "widget"
            |  author: "alice"
            |
            |releases: []
            |
            |vulnerabilities: []
            |
            """.trimMargin()
    }
})
