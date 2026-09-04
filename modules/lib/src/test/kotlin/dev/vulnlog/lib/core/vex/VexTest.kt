// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.vex.VexStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val affected = Verdict.Affected(Severity.HIGH)

private fun affectedIn(
    releases: List<String>,
    disposition: Disposition? = null,
    fixedIn: String? = null,
    note: String? = null,
) = vulnerability(
    id = cve("CVE-2026-1234"),
    releases = releases.map(::release),
    verdict = Verdict.Affected(Severity.HIGH, disposition),
    resolution = fixedIn?.let { resolution(release = it, note = note) },
)

class VexTest :
    FunSpec({

        context("deriveVexStatus") {

            test("the resolution release is fixed even though the verdict says affected") {
                val entry = affectedIn(releases = listOf("1.0.0"), fixedIn = "1.0.1")

                val status = deriveVexStatus(entry, release("1.0.1"))

                status shouldBe VexStatus.Fixed
            }

            test("a release other than the resolution release keeps the verdict's status") {
                val entry = affectedIn(releases = listOf("1.0.0"), fixedIn = "1.0.1")

                val status = deriveVexStatus(entry, release("1.0.0"))

                status.shouldBeInstanceOf<VexStatus.Affected>()
            }
        }

        context("vexActionStatement") {

            test("points at the fix release and appends the resolution note") {
                val entry = affectedIn(releases = listOf("1.0.0"), fixedIn = "1.0.1", note = "Bumped log4j to 2.17.1.")

                val action = vexActionStatement(entry)

                action shouldBe "Update to release 1.0.1. Bumped log4j to 2.17.1."
            }

            test("points at the fix release alone when no note is recorded") {
                val entry =
                    affectedIn(releases = listOf("1.0.0"), disposition = Disposition.WILL_FIX, fixedIn = "1.0.1")

                val action = vexActionStatement(entry)

                action shouldBe "Update to release 1.0.1."
            }

            test("states that no remediation exists when neither intent nor fix is recorded") {
                val entry = vulnerability(id = cve("CVE-2026-1234"), verdict = affected)

                val action = vexActionStatement(entry)

                action shouldBe "No remediation is available yet."
            }

            test("states that a fix is planned for 'will fix' without a resolution") {
                val entry = affectedIn(releases = listOf("1.0.0"), disposition = Disposition.WILL_FIX)

                val action = vexActionStatement(entry)

                action shouldBe "A fix is planned but not yet available."
            }

            test("states the accepted risk for 'wont fix' without a resolution") {
                val entry = affectedIn(releases = listOf("1.0.0"), disposition = Disposition.WONT_FIX)

                val action = vexActionStatement(entry)

                action shouldBe "The risk is accepted. No fix is planned."
            }

            test("keeps the accepted risk and never appends the note when 'wont fix' has a resolution") {
                val entry =
                    affectedIn(
                        releases = listOf("1.0.0"),
                        disposition = Disposition.WONT_FIX,
                        fixedIn = "1.0.1",
                        note = "Bumped log4j to 2.17.1.",
                    )

                val action = vexActionStatement(entry)

                action shouldBe "The risk is accepted for this release. A fix ships with release 1.0.1."
            }
        }
    })
