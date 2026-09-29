// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.collectOpenVexStatements
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
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate

private val GOLDEN_SOURCE_DIR: Path = Path.of("src/test/resources/vex")

private val DOCUMENT_ID = OpenVexDocumentId("https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79")
private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")

private val releaseV1 = release("1.0.0")
private val releaseV2 = release("1.0.1")

/** The third release lists nothing itself, so it is reached only through the range of every entry. */
private val file: VulnlogFile =
    vulnlogFile(
        project = Project("Acme Corp", "Acme Web App", "Acme Security Team", "security@acme.example"),
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls =
                        listOf(
                            mavenPurlEntry("pkg:maven/com.acme/acme-web-app@1.0.0", tags = listOf("app")),
                            mavenPurlEntry("pkg:maven/com.acme/acme-cli@1.0.0", tags = listOf("app")),
                        ),
                ),
                releaseEntry(
                    "1.0.1",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/acme-cli@1.0.1", tags = listOf("app"))),
                ),
                releaseEntry(
                    "1.1.0",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/acme-cli@1.1.0", tags = listOf("app"))),
                    publishedAt = LocalDate.of(2026, 5, 1),
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
                    reports = listOf(report(ReporterType.TRIVY, at = LocalDate.of(2026, 4, 18))),
                ),
            ),
    )

/**
 * Other tools and diffs read these bytes, so key order, indentation and the trailing newline are pinned along with
 * every field. Rewrite the golden with UPDATE_GOLDEN=1 only for an intended change.
 */
class OpenVexGoldenTest :
    FunSpec({

        test("an OpenVEX document covering every status matches the golden bytes") {
            val document =
                buildOpenVexDocument(
                    file.project,
                    OpenVexIdentity(DOCUMENT_ID, ISSUED_AT, OpenVexDocumentVersion.FIRST),
                    collectOpenVexStatements(file).statements,
                    tooling = OpenVexTooling("CLI", "0.18.0"),
                )

            val actual = OpenVexEncoder.encode(document)

            actual shouldBe golden("golden-openvex.json", actual)
        }
    })

private fun golden(
    name: String,
    actual: String,
): String {
    if (System.getenv("UPDATE_GOLDEN") in listOf("1", "true")) {
        Files.createDirectories(GOLDEN_SOURCE_DIR)
        Files.writeString(GOLDEN_SOURCE_DIR.resolve(name), actual)
        return actual
    }
    return OpenVexGoldenTest::class.java
        .getResourceAsStream("/vex/$name")
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Golden file missing at classpath /vex/$name. Run with UPDATE_GOLDEN=1 to create it.")
}
