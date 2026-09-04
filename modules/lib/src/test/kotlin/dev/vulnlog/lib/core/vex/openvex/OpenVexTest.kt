// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.resolution
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.vex.VexStatus
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexSkippedEntry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
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

/** Two releases, each with a container purl and the first also with a library purl. */
private val taggedFile =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry(
                    "1.0.0",
                    purls =
                        listOf(
                            mavenPurlEntry("pkg:maven/com.acme/app@1.0.0", tags = listOf("container")),
                            mavenPurlEntry("pkg:maven/com.acme/lib@1.0.0", tags = listOf("library")),
                        ),
                ),
                releaseEntry(
                    "1.0.1",
                    purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.1", tags = listOf("container"))),
                ),
            ),
        vulnerabilities = listOf(affectedInV1),
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

                val statements = collectOpenVexStatements(file).statements

                val byProduct = statements.associateBy { statement -> statement.products.single().value }

                statements shouldHaveSize 2
                byProduct.getValue("pkg:maven/com.acme/app@1.0.0").status.shouldBeInstanceOf<VexStatus.Affected>()
                byProduct.getValue("pkg:maven/com.acme/app@1.0.1").status shouldBe VexStatus.Fixed
            }

            test("anchors name every release with purls and the purls it contributes") {
                val collection = collectOpenVexStatements(taggedFile)

                collection.anchors.keys.toList() shouldContainExactly listOf(release("1.0.0"), release("1.0.1"))
                collection.anchors.getValue(release("1.0.0")).map { it.value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.0", "pkg:maven/com.acme/lib@1.0.0")
            }

            test("a release without purls produces no statement and is reported") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("1.0.0"), releaseEntry("1.0.1")),
                        vulnerabilities = listOf(affectedInV1),
                    )

                val collection = collectOpenVexStatements(file)

                collection.statements.shouldBeEmpty()
                collection.skippedReleases shouldContainExactly listOf(release("1.0.0"), release("1.0.1"))
                collection.skippedEntries shouldContainExactly
                    listOf(OpenVexSkippedEntry.NoAnchoredRelease(cve("CVE-2026-1234")))
            }

            test("skipped releases keep the order the file declares them in") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0"),
                                releaseEntry("1.0.1", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.1"))),
                                releaseEntry("1.1.0"),
                            ),
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve("CVE-2026-1234"),
                                    releases = listOf(release("1.1.0"), release("1.0.0"), release("1.0.1")),
                                ),
                            ),
                    )

                val collection = collectOpenVexStatements(file)

                collection.skippedReleases shouldContainExactly listOf(release("1.0.0"), release("1.1.0"))
            }

            test("an entry without releases is reported as such") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
                            ),
                        vulnerabilities = listOf(vulnerability(id = cve("CVE-2026-1234"))),
                    )

                val collection = collectOpenVexStatements(file)

                collection.skippedEntries shouldContainExactly
                    listOf(OpenVexSkippedEntry.NoRelease(cve("CVE-2026-1234")))
            }

            test("identical statements across releases collapse into one") {
                val shared = mavenPurlEntry("pkg:maven/com.acme/app")
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(shared)),
                                releaseEntry("1.0.1", purls = listOf(shared)),
                            ),
                        vulnerabilities =
                            listOf(
                                vulnerability(
                                    id = cve("CVE-2026-1234"),
                                    releases = listOf(release("1.0.0"), release("1.0.1")),
                                ),
                            ),
                    )

                val statements = collectOpenVexStatements(file).statements

                statements shouldHaveSize 1
            }
        }

        context("release scope") {

            test("only a release in scope anchors a statement") {
                val scope = OpenVexScope(releases = setOf(release("1.0.0")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                statements.single().products.map { it.value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.0", "pkg:maven/com.acme/lib@1.0.0")
            }

            test("a fix outside the scope still drives the action statement") {
                val scope = OpenVexScope(releases = setOf(release("1.0.0")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                val status = statements.single().status.shouldBeInstanceOf<VexStatus.Affected>()
                status.actionStatement shouldBe "Update to release 1.0.1."
            }

            test("a release outside the scope is not reported as missing purls") {
                val file =
                    vulnlogFile(
                        releases =
                            listOf(
                                releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
                                releaseEntry("1.0.1"),
                            ),
                        vulnerabilities = listOf(affectedInV1),
                    )
                val scope = OpenVexScope(releases = setOf(release("1.0.0")))

                val collection = collectOpenVexStatements(file, scope)

                collection.skippedReleases.shouldBeEmpty()
            }
        }

        context("tag scope") {

            test("only a purl carrying the tag becomes a product") {
                val scope = OpenVexScope(tags = setOf(tag("container")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                statements.map { statement -> statement.products.single().value } shouldContainExactly
                    listOf("pkg:maven/com.acme/app@1.0.0", "pkg:maven/com.acme/app@1.0.1")
            }

            test("a release left without purls by the tag is reported as missing purls") {
                val scope = OpenVexScope(tags = setOf(tag("library")))

                val collection = collectOpenVexStatements(taggedFile, scope)

                collection.skippedReleases shouldContainExactly listOf(release("1.0.1"))
            }

            test("a tag no purl carries produces no statement") {
                val scope = OpenVexScope(tags = setOf(tag("binary")))

                val statements = collectOpenVexStatements(taggedFile, scope).statements

                statements.shouldBeEmpty()
            }
        }

        context("openVexAuthor") {

            test("appends the contact in parentheses") {
                val project = Project("Acme Corp", "Acme Web App", "Acme Security Team", "security@acme.example")

                openVexAuthor(project) shouldBe "Acme Security Team (security@acme.example)"
            }

            test("is the author alone without a contact") {
                val project = Project("Acme Corp", "Acme Web App", "Acme Security Team")

                openVexAuthor(project) shouldBe "Acme Security Team"
            }
        }

        context("vocabulary") {

            test("every status maps to its OpenVEX token") {
                openVexStatus(VexStatus.UnderInvestigation) shouldBe "under_investigation"
                openVexStatus(VexStatus.Fixed) shouldBe "fixed"
                openVexStatus(VexStatus.NotAffected(VexJustification.COMPONENT_NOT_PRESENT)) shouldBe "not_affected"
                openVexStatus(VexStatus.Affected("Update to release 1.0.1.")) shouldBe "affected"
            }

            test("every justification maps to its OpenVEX token") {
                VexJustification.entries.map(::openVexJustification) shouldContainExactly
                    listOf(
                        "component_not_present",
                        "inline_mitigations_already_exist",
                        "vulnerable_code_cannot_be_controlled_by_adversary",
                        "vulnerable_code_not_in_execute_path",
                        "vulnerable_code_not_present",
                    )
            }
        }
    })
