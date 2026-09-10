// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.parse.vex.openvex.OpenVexReader
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant

private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val UPDATED_AT = Instant.parse("2026-05-02T00:00:00Z")

/** One entry affecting every release given, so each release contributes a statement. */
private fun fileWith(vararg releases: String): VulnlogFile =
    vulnlogFile(
        releases =
            releases.map { id -> releaseEntry(id, purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@$id"))) },
        vulnerabilities = listOf(vulnerability(id = cve("CVE-2026-1111"), releases = releases.map(::release))),
    )

private fun generate(
    file: VulnlogFile,
    baseline: OpenVexBaseline? = null,
    now: Instant = ISSUED_AT,
    tooling: String? = null,
): OpenVexOutcome.Generated = generateOpenVex(file, OpenVexScope(), baseline, now, tooling).shouldBeInstanceOf()

/** The baseline a later run reads from what an earlier one wrote. */
private fun baselineOf(generated: OpenVexOutcome.Generated): OpenVexBaseline =
    OpenVexReader.readBaseline(generated.content) ?: error("the writer produced no readable baseline")

class OpenVexRunTest :
    FunSpec({

        test("a run without a baseline issues the first version") {
            val generated = generate(fileWith("1.0.0"))

            generated.document.identity.id shouldStartWith OPEN_VEX_ID_PREFIX
            generated.document.identity.version shouldBe 1
            generated.document.identity.timestamp shouldBe ISSUED_AT
            generated.unchanged shouldBe false
            generated.content shouldEndWith "}\n"
        }

        test("names the tooling in the document") {
            val tooling = "Vulnlog CLI version 0.18.0, https://vulnlog.dev/"

            val generated = generate(fileWith("1.0.0"), tooling = tooling)

            generated.document.tooling shouldBe tooling
            generated.content shouldContain "\"tooling\": \"$tooling\""
        }

        test("the clock is cut to whole seconds") {
            val generated = generate(fileWith("1.0.0"), now = Instant.parse("2026-04-25T00:00:00.123456Z"))

            generated.document.identity.timestamp shouldBe ISSUED_AT
        }

        test("a rerun over an unchanged file keeps the baseline bytes") {
            val first = generate(fileWith("1.0.0"))

            val second = generate(fileWith("1.0.0"), baseline = baselineOf(first), now = UPDATED_AT)

            second.unchanged shouldBe true
            second.content shouldBe first.content
        }

        test("a changed file continues the identity and counts the version up") {
            val first = generate(fileWith("1.0.0"))

            val second = generate(fileWith("1.0.0", "1.1.0"), baseline = baselineOf(first), now = UPDATED_AT)

            second.unchanged shouldBe false
            second.document.identity.id shouldBe first.document.identity.id
            second.document.identity.timestamp shouldBe UPDATED_AT
            second.document.identity.version shouldBe 2
        }

        test("a file without an anchoring release yields no document") {
            val bare = vulnlogFile(releases = listOf(releaseEntry("1.0.0")))

            val outcome = generateOpenVex(bare, OpenVexScope(), baseline = null, now = ISSUED_AT, tooling = null)

            outcome.shouldBeInstanceOf<OpenVexOutcome.Empty>()
        }
    })
