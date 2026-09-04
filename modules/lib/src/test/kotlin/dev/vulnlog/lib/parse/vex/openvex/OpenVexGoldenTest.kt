// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.vex.openvex

import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

private const val GOLDEN_RESOURCE = "/vex/golden-openvex.json"
private val GOLDEN_SOURCE_DIR: Path = Path.of("src/test/resources/vex")

private val releaseV1 = release("1.0.0")
private val releaseV2 = release("1.0.1")

/**
 * Pins the bytes of an OpenVEX document covering every status. It guards the `@`-prefixed keys, the
 * nested vulnerability and product objects, the status and justification vocabulary, key order,
 * indentation and the trailing newline in one go.
 */
class OpenVexGoldenTest :
    FunSpec({

        test("OpenVEX document matches golden bytes") {
            val file =
                vulnlogFile(
                    project = Project("Acme Corp", "Acme Web App", "Acme Security Team", "security@acme.example"),
                    releases =
                        listOf(
                            releaseEntry(
                                "1.0.0",
                                purls =
                                    listOf(
                                        mavenPurlEntry("pkg:maven/com.acme/acme-web-app@1.0.0"),
                                        mavenPurlEntry("pkg:maven/com.acme/acme-cli@1.0.0"),
                                    ),
                            ),
                            releaseEntry("1.0.1", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/acme-cli@1.0.1"))),
                        ),
                    vulnerabilities =
                        listOf(
                            vulnerability(
                                id = cve("CVE-2026-1111"),
                                releases = listOf(releaseV1),
                                verdict = Verdict.NotAffected(VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH),
                            ),
                            vulnerability(
                                id = cve("CVE-2026-2222"),
                                releases = listOf(releaseV1),
                                verdict = Verdict.Affected(Severity.CRITICAL),
                                resolution = resolution(release = "1.0.1", note = "Bumped log4j to 2.17.1."),
                            ),
                            vulnerability(id = cve("CVE-2026-3333"), releases = listOf(releaseV2)),
                        ),
                )
            val document =
                buildOpenVexDocument(
                    vulnlogFile = file,
                    id = "https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79",
                    timestamp = Instant.parse("2026-04-25T00:00:00Z"),
                )

            val actual = OpenVexWriter.write(document)

            actual shouldBe golden(actual)
        }
    })

private fun golden(actual: String): String {
    if (System.getenv("UPDATE_GOLDEN") in listOf("1", "true")) {
        Files.createDirectories(GOLDEN_SOURCE_DIR)
        Files.writeString(GOLDEN_SOURCE_DIR.resolve("golden-openvex.json"), actual)
        return actual
    }
    return OpenVexGoldenTest::class.java
        .getResourceAsStream(GOLDEN_RESOURCE)
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Golden file missing at classpath $GOLDEN_RESOURCE. Run with UPDATE_GOLDEN=1 to create it.")
}
