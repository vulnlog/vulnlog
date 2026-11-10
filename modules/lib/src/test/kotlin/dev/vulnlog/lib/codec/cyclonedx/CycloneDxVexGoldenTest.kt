// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.cyclonedx

import com.networknt.schema.InputFormat
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SpecVersion
import dev.vulnlog.lib.core.vex.cyclonedx.buildCycloneDxVexDocument
import dev.vulnlog.lib.core.vex.cyclonedx.collectCycloneDxVexVulnerabilities
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.ghsa
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.report
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentity
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate

private val GOLDEN_SOURCE_DIR: Path = Path.of("src/test/resources/vex")

private val SERIAL_NUMBER = CycloneDxSerialNumber("urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79")
private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")

private val releaseV1 = release("1.0.0")
private val releaseV2 = release("1.0.1")

/**
 * The entries of the OpenVEX golden, so the two formats can be compared statement by statement. The contact has a
 * registered top-level domain, because the schema validator checks `email` against the public suffix list.
 */
private val file: VulnlogFile =
    vulnlogFile(
        project = Project("Acme Corp", "Acme Web App", "Acme Security Team", "security@example.com"),
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls =
                        listOf(
                            mavenPurlEntry("pkg:maven/com.acme/acme-web-app@1.0.0", tags = listOf("app")),
                            mavenPurlEntry("pkg:maven/com.acme/acme-cli@1.0.0", tags = listOf("app")),
                        ),
                    publishedAt = LocalDate.of(2026, 3, 2),
                ),
                releaseEntry(
                    "1.0.1",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/acme-cli@1.0.1", tags = listOf("app"))),
                    publishedAt = LocalDate.of(2026, 4, 21),
                ),
            ),
        vulnerabilities =
            listOf(
                vulnerability(
                    id = cve("CVE-2026-1111"),
                    tags = listOf(tag("app")),
                    aliases = listOf(ghsa("GHSA-jfh8-c2jp-5v3q")),
                    releases = listOf(releaseV1),
                    description = "Remote code execution in example-lib",
                    packages = listOf(Purl.Npm("pkg:npm/example-lib@2.3.0")),
                    reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 1))),
                    analyzedAt = LocalDate.of(2026, 4, 6),
                    verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                    analysis = "Message lookups are disabled by configuration. The JNDI code path is never reached.",
                ),
                vulnerability(
                    id = cve("CVE-2026-2222"),
                    tags = listOf(tag("app")),
                    releases = listOf(releaseV1),
                    description = "Denial of service in example-parser",
                    packages = listOf(Purl.Npm("pkg:npm/example-parser@1.0.0")),
                    reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 7))),
                    verdict = Verdict.Affected(Severity.CRITICAL),
                    analysis = "The parser is reachable from the upload endpoint.",
                    resolution =
                        resolution(
                            release = "1.0.1",
                            note = "Bumped example-parser to 1.1.0.",
                            at = LocalDate.of(2026, 4, 20),
                        ),
                ),
                vulnerability(
                    id = cve("CVE-2026-3333"),
                    tags = listOf(tag("app")),
                    releases = listOf(releaseV2),
                    packages = listOf(Purl.Npm("pkg:npm/example-lib@2.3.0")),
                    reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 18))),
                ),
            ),
    )

private fun documentFor(release: String): String {
    val collection = collectCycloneDxVexVulnerabilities(file, CycloneDxVexScope(release(release)))
    val document =
        buildCycloneDxVexDocument(
            file.project,
            CycloneDxVexIdentity(SERIAL_NUMBER, ISSUED_AT, VexDocumentVersion.FIRST),
            collection,
            tooling = VexTooling("CLI", "0.18.0"),
        )
    return CycloneDxVexEncoder.encode(document)
}

/**
 * Other tools and diffs read these bytes, so key order, indentation and the trailing newline are pinned along with
 * every field. Rewrite the goldens with UPDATE_GOLDEN=1 only for an intended change. Both documents together cover
 * every status, and each must validate against the CycloneDX schema Vulnlog claims in `$schema`.
 */
class CycloneDxVexGoldenTest :
    FunSpec({

        test(
            "the document of the first release, with an affected and a not affected package, matches the golden bytes",
        ) {
            val actual = documentFor("1.0.0")

            actual shouldBe golden("golden-cyclonedx-1.0.0.json", actual)
        }

        test("the document of the fix release, with a resolved and an investigated package, matches the golden bytes") {
            val actual = documentFor("1.0.1")

            actual shouldBe golden("golden-cyclonedx-1.0.1.json", actual)
        }

        test("every golden document validates against the CycloneDX schema") {
            listOf("1.0.0", "1.0.1").forEach { release ->
                val errors =
                    cycloneDxJsonSchema(
                        CycloneDxVexFormatVersion.LATEST,
                    ).validate(documentFor(release), InputFormat.JSON)

                errors.map { "$release: $it" }.shouldBeEmpty()
            }
        }
    })

/** The published schema and the ones it references, read from the classpath instead of cyclonedx.org. */
internal fun cycloneDxJsonSchema(formatVersion: CycloneDxVexFormatVersion): JsonSchema =
    JsonSchemaFactory
        .getInstance(SpecVersion.VersionFlag.V7) { builder ->
            builder.schemaMappers { mappers ->
                mappers.mapPrefix("http://cyclonedx.org/schema/", "classpath:cyclonedx/")
            }
        }.getSchema(SchemaLocation.of(cycloneDxSchema(formatVersion)))

private fun golden(
    name: String,
    actual: String,
): String {
    if (System.getenv("UPDATE_GOLDEN") in listOf("1", "true")) {
        Files.createDirectories(GOLDEN_SOURCE_DIR)
        Files.writeString(GOLDEN_SOURCE_DIR.resolve(name), actual)
        return actual
    }
    return CycloneDxVexGoldenTest::class.java
        .getResourceAsStream("/vex/$name")
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Golden file missing at classpath /vex/$name. Run with UPDATE_GOLDEN=1 to create it.")
}
