package dev.vulnlog.cli.parse.v1

import dev.vulnlog.cli.model.Project
import dev.vulnlog.cli.model.ReleaseEntry
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.VulnerabilityEntry
import dev.vulnlog.cli.model.VulnlogFile
import dev.vulnlog.cli.parse.v1.dto.ProjectDto
import dev.vulnlog.cli.parse.v1.dto.ReleaseEntryDto
import dev.vulnlog.cli.parse.v1.dto.VulnerabilityEntryDto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun vulnlogFile(
    schemaVersion: SchemaVersion = SchemaVersion(1, 0),
    organization: String = "acme",
    project: String = "widget",
    author: String = "alice",
    releases: List<ReleaseEntry> = emptyList(),
    vulnerabilities: List<VulnerabilityEntry> = emptyList(),
) = VulnlogFile(
    schemaVersion = schemaVersion,
    project = Project(organization, project, author),
    releases = releases,
    vulnerabilities = vulnerabilities,
)

class V1MapperTest : FunSpec({

    test("schema version with zero minor is formatted as major only") {
        val dto = V1Mapper.fromDomain(vulnlogFile(schemaVersion = SchemaVersion(1, 0)))

        dto.schemaVersion shouldBe "1"
    }

    test("schema version with non-zero minor is formatted as major.minor") {
        val dto = V1Mapper.fromDomain(vulnlogFile(schemaVersion = SchemaVersion(1, 2)))

        dto.schemaVersion shouldBe "1.2"
    }

    test("project fields are mapped to dto") {
        val dto = V1Mapper.fromDomain(vulnlogFile(organization = "acme", project = "widget", author = "alice"))

        dto.project shouldBe ProjectDto("acme", "widget", "alice")
    }

    test("releases are mapped to dto") {
        val dto = V1Mapper.fromDomain(vulnlogFile(releases = listOf(ReleaseEntry("v1.0"), ReleaseEntry("v2.0"))))

        dto.releases shouldBe listOf(ReleaseEntryDto("v1.0"), ReleaseEntryDto("v2.0"))
    }

    test("vulnerabilities are mapped to dto") {
        val dto = V1Mapper.fromDomain(vulnlogFile(vulnerabilities = listOf(VulnerabilityEntry("CVE-2024-1234"))))

        dto.vulnerabilities shouldBe listOf(VulnerabilityEntryDto("CVE-2024-1234"))
    }
})
