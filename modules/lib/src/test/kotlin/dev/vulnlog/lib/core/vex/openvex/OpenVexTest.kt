// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.vex.VexStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val affectedInV1 =
    vulnerability(
        id = cve("CVE-2026-1234"),
        releases = listOf(release("1.0.0")),
        verdict = Verdict.Affected(Severity.HIGH),
        resolution = resolution(release = "1.0.1"),
    )

class OpenVexTest :
    FunSpec({

        context("collectOpenVexStatements") {

            test("a release named only by the resolution gets its own fixed statement") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
                                releaseEntry("1.0.1", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.1"))),
                            ),
                        vulnerabilities = listOf(affectedInV1),
                    )

                val statements = collectOpenVexStatements(file)

                val byProduct = statements.associateBy { statement -> statement.products.single().value }

                statements shouldHaveSize 2
                byProduct.getValue("pkg:maven/com.acme/app@1.0.0").status.shouldBeInstanceOf<VexStatus.Affected>()
                byProduct.getValue("pkg:maven/com.acme/app@1.0.1").status shouldBe VexStatus.Fixed
            }

            test("a release without purls produces no statement") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("1.0.0"), releaseEntry("1.0.1")),
                        vulnerabilities = listOf(affectedInV1),
                    )

                val statements = collectOpenVexStatements(file)

                statements.shouldBeEmpty()
            }
        }
    })
