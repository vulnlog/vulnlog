package ch.addere.vulnlog.cli.suppressions

import ch.addere.vulnlog.core.model.vulnerability.VlVulnerability
import ch.addere.vulnlog.core.owasp
import ch.addere.vulnlog.core.suppress
import ch.addere.vulnlog.core.vulnerability
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import java.io.File

class OwaspDependencyCheckerSuppressorTest : FunSpec({

    test("test throws on non file template") {
        val exception =
            shouldThrow<IllegalArgumentException> {
                OwaspDependencyCheckerSuppressor(tempdir(), "marker")
            }

        exception.message shouldBe "suppressionFileTemplate must be a file"
    }

    test("test throws on blank marker") {
        val exception =
            shouldThrow<IllegalArgumentException> {
                OwaspDependencyCheckerSuppressor(tempfile(), "  \t ")
            }

        exception.message shouldBe "suppressionBlockMarker cannot be blank"
    }

    test("test correct suppression generation") {
        val suppressor = OwaspDependencyCheckerSuppressor(readTemplate(), "<vulnlog-marker/>")

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
            owasp("does not matter"),
            suppress = suppress(rationale = "fix in upcoming bug fix release"),
        ),
        vulnerability(
            "cve-id-2",
            owasp("does not matter"),
            suppress = suppress(rationale = "fix in upcoming bug fix release"),
        ),
    )

private val head =
    listOf(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<suppressions xmlns=\"https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd\">",
    )

private val tail = listOf("</suppressions>")

private val expected =
    SuppressionComposition(
        head,
        tail,
        setOf(
            listOf(
                "<suppress>",
                "    <notes><![CDATA[",
                "        fix in upcoming bug fix release",
                "    ]]></notes>",
                "    <vulnerabilityName>cve-id-1</vulnerabilityName>",
                "</suppress>",
            ),
            listOf(
                "<suppress>",
                "    <notes><![CDATA[",
                "        fix in upcoming bug fix release",
                "    ]]></notes>",
                "    <vulnerabilityName>cve-id-2</vulnerabilityName>",
                "</suppress>",
            ),
        ),
    )

private fun readTemplate(): File {
    val templatePath = "/templates/owasp-dependency-checker-suppressions-template.xml"
    return File(object {}.javaClass.getResource(templatePath)!!.toURI())
}
