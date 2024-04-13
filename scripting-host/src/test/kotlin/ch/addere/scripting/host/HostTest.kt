package ch.addere.scripting.host

import ch.addere.dsl.OwaspDependencyChecker
import ch.addere.dsl.ReleaseBranch
import ch.addere.dsl.Reporter
import ch.addere.dsl.Resolution
import ch.addere.dsl.SupportedBranches
import ch.addere.dsl.Suppression
import ch.addere.dsl.Version
import ch.addere.dsl.Vulnerability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HostTest : FunSpec({

    val host = ScriptingHost()

    test("test empty script should not throw") {
        val script = readLinesFrom("empty.vulnlog.kts")

        val result = host.evalScript(script)

        result.branches shouldBe null
        result.releaseBranch shouldBe emptySet()
        result.vulnerabilities shouldBe emptySet()
    }

    test("test non related content should not throw") {
        val script = readLinesFrom("something.vulnlog.kts")

        val result = host.evalScript(script)

        result.branches shouldBe null
        result.releaseBranch shouldBe emptySet()
        result.vulnerabilities shouldBe emptySet()
    }

    test("test minimal script should validate") {
        val script = readLinesFrom("minimal.vulnlog.kts")
        val v100 = Version(1, 0, 0)
        val v101 = Version(1, 0, 1)
        val releaseBranch = ReleaseBranch(name = "r1", upComing = v101, published = setOf(v100))
        val supportedBranches = SupportedBranches(supported = setOf(releaseBranch), unsupported = emptySet())
        val vulnerability =
            Vulnerability(
                id = "cve1",
                reporter = Reporter(listOf(OwaspDependencyChecker(affected = setOf(v100)))),
                resolution =
                    Resolution(
                        null,
                        Suppression(
                            reason =
                                "Version 1.0.0 is not immediately affected. " +
                                    "Nevertheless, dependency shall be fixed in upcoming release.",
                            inVersion = setOf(v100),
                            untilVersion = setOf(v101),
                        ),
                        null,
                    ),
            )

        val result = host.evalScript(script)

        result.branches shouldBe supportedBranches
        result.releaseBranch shouldBe setOf(releaseBranch)
        result.vulnerabilities shouldBe setOf(vulnerability)
    }
})

private fun readLinesFrom(filename: String): String =
    object {}.javaClass.getResourceAsStream("/$filename")!!.bufferedReader()
        .lines()
        .toList()
        .joinToString(separator = "\n")
