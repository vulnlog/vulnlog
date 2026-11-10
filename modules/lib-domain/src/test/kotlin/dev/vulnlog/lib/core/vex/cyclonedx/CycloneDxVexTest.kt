// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.cyclonedx

import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.vex.VexDocumentVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxSerialNumber
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexBaseline
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexIdentity
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexRevision
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

private val SERIAL_NUMBER = CycloneDxSerialNumber("urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79")
private val NOW = Instant.parse("2026-04-25T10:15:30Z")

private val file =
    vulnlogFile(
        tags = listOf(tagEntry("container"), tagEntry("library")),
        releases = listOf(releaseEntry("1.0.0"), releaseEntry("1.1.0")),
    )

class CycloneDxVexTest :
    FunSpec({

        context("resolveCycloneDxVexIdentity") {

            test("issues the first revision under its serial number") {
                val identity = resolveCycloneDxVexIdentity(CycloneDxVexRevision.First(SERIAL_NUMBER), NOW)

                identity shouldBe CycloneDxVexIdentity(SERIAL_NUMBER, NOW, VexDocumentVersion.FIRST)
            }

            test("continues the baseline in the next revision") {
                val baseline =
                    CycloneDxVexBaseline(CycloneDxVexFormatVersion.VERSION_1_7, SERIAL_NUMBER, VexDocumentVersion(4))

                val identity = resolveCycloneDxVexIdentity(CycloneDxVexRevision.Next(baseline), NOW)

                identity shouldBe CycloneDxVexIdentity(SERIAL_NUMBER, NOW, VexDocumentVersion(5))
            }

            test("cuts the clock to whole seconds") {
                val identity =
                    resolveCycloneDxVexIdentity(CycloneDxVexRevision.First(SERIAL_NUMBER), NOW.plusNanos(987_654_321))

                identity.timestamp shouldBe NOW
            }
        }

        context("resolveCycloneDxVexScope") {

            test("resolves the release and the tags the file defines") {
                val result = resolveCycloneDxVexScope("1.1.0", setOf("container"), file)

                result shouldBe
                    CycloneDxVexScopeResult.Resolved(CycloneDxVexScope(release("1.1.0"), setOf(tag("container"))))
            }

            test("restricts no tags without any") {
                val result = resolveCycloneDxVexScope("1.0.0", emptySet(), file)

                result shouldBe CycloneDxVexScopeResult.Resolved(CycloneDxVexScope(release("1.0.0")))
            }

            test("rejects an unknown release and an unknown tag together") {
                val result = resolveCycloneDxVexScope("9.9.9", setOf("binary"), file)

                result shouldBe
                    CycloneDxVexScopeResult.Rejected(
                        listOf(
                            FilterProblem.UnknownRelease(release("9.9.9"), listOf(release("1.0.0"), release("1.1.0"))),
                            FilterProblem.UnknownTags(listOf(tag("binary")), listOf(tag("container"), tag("library"))),
                        ),
                    )
            }

            test("rejects a blank release, because a document is always about one") {
                val result = resolveCycloneDxVexScope(" ", emptySet(), file)

                result shouldBe
                    CycloneDxVexScopeResult.Rejected(
                        listOf(FilterProblem.BlankRelease(listOf(release("1.0.0"), release("1.1.0")))),
                    )
            }
        }
    })
