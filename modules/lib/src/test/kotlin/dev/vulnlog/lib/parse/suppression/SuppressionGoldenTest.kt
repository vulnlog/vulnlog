// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.suppression

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.model.suppress.SuppressionVuln
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

private const val GOLDEN_DIR = "/suppress"
private val GOLDEN_SOURCE_DIR: Path = Path.of("src/test/resources/suppress")

private val EXPIRY = LocalDate.of(2026, 8, 1)
private const val SHORT_REASON = "Vulnerable code not present."
private const val LONG_REASON =
    "Logback is a ktlint configuration dependency and never reaches the application classpath, " +
        "so the deserialization gadget cannot be triggered."

/**
 * Pins the bytes of the two YAML suppression formats. Both are emitted by
 * [dev.vulnlog.lib.parse.CanonicalYaml], so this also guards the canonical style against drift in
 * indentation, quoting and block folding.
 */
class SuppressionGoldenTest :
    FunSpec({

        test("Trivy suppression file matches golden bytes") {
            val output =
                SuppressionOutput.TrivySuppression(
                    entries =
                        setOf(
                            SuppressionVuln.TrivySuppressionEntry(
                                id = VulnId.Cve("CVE-2026-1111"),
                                expiresAt = EXPIRY,
                                reason = SHORT_REASON,
                            ),
                            SuppressionVuln.TrivySuppressionEntry(
                                id = VulnId.Ghsa("GHSA-aaaa-bbbb-cccc"),
                                reason = LONG_REASON,
                            ),
                        ),
                )

            val actual = SuppressionWriter.writeSuppressionOutput(output).content

            actual shouldBe golden("trivyignore.yaml", actual)
        }

        test("Snyk suppression file matches golden bytes") {
            val output =
                SuppressionOutput.SnykSuppression(
                    entries =
                        setOf(
                            SuppressionVuln.SnykSuppressionEntry(
                                id = VulnId.Snyk("SNYK-JAVA-CHQOSLOGBACK-8539867"),
                                expiresAt = EXPIRY,
                                reason = SHORT_REASON,
                            ),
                            SuppressionVuln.SnykSuppressionEntry(
                                id = VulnId.Snyk("SNYK-JAVA-CHQOSLOGBACK-8539866"),
                                reason = LONG_REASON,
                            ),
                        ),
                )

            val actual = SuppressionWriter.writeSuppressionOutput(output).content

            actual shouldBe golden("snyk.yaml", actual)
        }
    })

private fun golden(
    name: String,
    actual: String,
): String {
    val fileName = "golden-$name"
    if (System.getenv("UPDATE_GOLDEN") in listOf("1", "true")) {
        Files.createDirectories(GOLDEN_SOURCE_DIR)
        Files.writeString(GOLDEN_SOURCE_DIR.resolve(fileName), actual)
        println("Golden file updated at ${GOLDEN_SOURCE_DIR.resolve(fileName)}")
        return actual
    }
    return SuppressionGoldenTest::class.java
        .getResourceAsStream("$GOLDEN_DIR/$fileName")
        ?.bufferedReader()
        ?.use { it.readText() }
        ?: error("Golden file missing at classpath $GOLDEN_DIR/$fileName. Run with UPDATE_GOLDEN=1 to create it.")
}
