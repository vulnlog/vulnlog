package ch.addere.cli.suppressions

import ch.addere.dsl.Ignore
import ch.addere.dsl.Mitigation
import ch.addere.dsl.OwaspDependencyChecker
import ch.addere.dsl.Reporter
import ch.addere.dsl.Resolution
import ch.addere.dsl.Snyk
import ch.addere.dsl.Suppression
import ch.addere.dsl.Version
import ch.addere.dsl.Vulnerability
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

private val vulnerabilities: Set<Vulnerability> = setOf(
    Vulnerability(
        "cve-id-0", Reporter(
            listOf(
                OwaspDependencyChecker(setOf(Version(1, 0, 0))),
                Snyk("snyk-id-0", setOf(Version(1, 0, 0)))
            )
        ),
        Resolution(
            null,
            Suppression("fix in upcoming bug fix release", setOf(Version(1, 0, 0)), setOf(Version(1, 0, 1))),
            null
        )
    ),
    Vulnerability(
        "cve-id-1", Reporter(
            listOf(
                Snyk("snyk-id-1", setOf(Version(1, 0, 0)))
            )
        ),
        Resolution(
            null,
            Suppression("fix in upcoming bug fix release", setOf(Version(1, 0, 0)), setOf(Version(1, 0, 1))),
            null
        )
    ),
    Vulnerability(
        "cve-id-2", Reporter(
            listOf(
                OwaspDependencyChecker(setOf(Version(1, 0, 0))),
            )
        ),
        Resolution(
            null,
            Suppression("fix in upcoming bug fix release", setOf(Version(1, 0, 1)), setOf(Version(1, 0, 2))),
            null
        )
    ),
    Vulnerability(
        "cve-id-3", Reporter(
            listOf(
                Snyk("snyk-id-3", setOf(Version(1, 0, 0))),
            )
        ),
        Resolution(
            Ignore("This is a false positive"),
            null,
            null
        )
    ),
    Vulnerability(
        "cve-id-4", Reporter(
            listOf(
                Snyk("snyk-id-4", setOf(Version(1, 0, 0))),
            )
        ),
        Resolution(
            null,
            null,
            Mitigation(setOf(Version(1, 2, 0)))
        )
    )
)

private val head = listOf(
    "ignore:"
)

private val tail = emptyList<String>()

private val expected = SuppressionComposition(
    head, tail,
    setOf(
        listOf(
            "snyk-id-0:",
            "  - '*':",
            "    reason: fix in upcoming bug fix release"
        ),
        listOf(
            "snyk-id-1:",
            "  - '*':",
            "    reason: fix in upcoming bug fix release"
        )
    )
)

private fun readTemplate(): File = File(
    object {}.javaClass.getResource("/templates/.snyk-template")!!.toURI()
)
