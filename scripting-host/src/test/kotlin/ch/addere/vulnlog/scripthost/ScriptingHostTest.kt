package ch.addere.vulnlog.scripthost

import ch.addere.vulnlog.core.group
import ch.addere.vulnlog.core.owasp
import ch.addere.vulnlog.core.suppress
import ch.addere.vulnlog.core.versions
import ch.addere.vulnlog.core.vulnerability
import ch.addere.vulnlog.scriptinghost.ScriptingHost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ScriptingHostTest : FunSpec({

    val host = ScriptingHost()

    test("test empty script should not throw") {
        val script = readLinesFrom("empty.vulnlog.kts")

        val result = host.evalScript(script)

        result.allVersions shouldBe emptySet()
        result.allGroups shouldBe emptySet()
        result.allVulnerabilities shouldBe emptySet()
    }

    test("test non related content should not throw") {
        val script = readLinesFrom("something.vulnlog.kts")

        val result = host.evalScript(script)

        result.allVersions shouldBe emptySet()
        result.allGroups shouldBe emptySet()
        result.allVulnerabilities shouldBe emptySet()
    }

    test("test minimal script should validate") {
        val script = readLinesFrom("minimal.vulnlog.kts")

        val result = host.evalScript(script)

        result.allVersions shouldContainExactly versions("1.0.0", "0.1.0")
        result.allGroups shouldContainExactly setOf(group("release 1", "1.0.0", "0.1.0"))
        result.allVulnerabilities shouldContainExactly
            setOf(
                vulnerability(
                    "CVE-2019-10782",
                    owasp("0.1.0"),
                    suppress = suppress("0.1.0", rationale = "This is a test suppress for demonstration purpose"),
                ),
                vulnerability(
                    "CVE-2019-9658",
                    owasp("0.1.0"),
                    suppress = suppress("0.1.0", rationale = "This is a test suppress for demonstration purpose"),
                ),
                vulnerability(
                    "CVE-2023-6378",
                    owasp("0.1.0"),
                    suppress = suppress("0.1.0", rationale = "This is a test suppress for demonstration purpose"),
                ),
            )
    }
})

private fun readLinesFrom(filename: String): String =
    object {}.javaClass.getResourceAsStream("/$filename")!!.bufferedReader()
        .lines()
        .toList()
        .joinToString(separator = "\n")
