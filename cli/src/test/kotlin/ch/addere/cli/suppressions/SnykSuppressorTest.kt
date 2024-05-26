package ch.addere.cli.suppressions

import ch.addere.vulnlog.core.model.vulnerability.VlVulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File

class SnykSuppressorTest : FunSpec({

    test("test correct suppression generation") {
        val suppressor = SnykSuppressor(readTemplate(), "vulnlog-marker")

        val result = suppressor.createSuppressions(vulnerabilities)

        result.head shouldBe expected.head
        result.tail shouldBe expected.tail
        result.suppressions shouldBe expected.suppressions
    }
})

private val vulnerabilities: Set<VlVulnerability> =
    setOf(
        vulnerability(
            "cve-id-1",
            snyk("snyk-id-1", "does not matter"),
            suppress = suppress("fix in upcoming bug fix release"),
        ),
        vulnerability(
            "cve-id-2",
            snyk("snyk-id-2", "does not matter"),
            suppress = suppress("fix in upcoming bug fix release"),
        ),
    )

private val head =
    listOf(
        "ignore:",
    )

private val tail = emptyList<String>()

private val expected =
    SuppressionComposition(
        head,
        tail,
        setOf(
            listOf(
                "snyk-id-1:",
                "  - '*':",
                "    reason: fix in upcoming bug fix release",
            ),
            listOf(
                "snyk-id-2:",
                "  - '*':",
                "    reason: fix in upcoming bug fix release",
            ),
        ),
    )

private fun readTemplate(): File {
    val templatePath = "/templates/.snyk-template"
    return File(object {}.javaClass.getResource(templatePath)!!.toURI())
}
