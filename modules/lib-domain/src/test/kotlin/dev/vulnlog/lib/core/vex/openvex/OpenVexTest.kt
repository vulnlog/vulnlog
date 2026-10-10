// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.fixtures.mavenPurlEntry
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexReleaseScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexRevision
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.Instant

private val ID = OpenVexDocumentId("https://vulnlog.dev/vex/abc")
private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")

private val taggedFile =
    vulnlogFile(
        releases =
            listOf(
                releaseEntry("1.0.0", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.0"))),
                releaseEntry("1.0.1", purls = listOf(mavenPurlEntry("pkg:maven/com.acme/app@1.0.1"))),
            ),
        tags = listOf(tagEntry("container")),
    )

class OpenVexTest :
    FunSpec({

        context("resolveOpenVexIdentity") {

            test("issues the first revision under its id") {
                val revision = OpenVexRevision.First(ID)

                val identity = resolveOpenVexIdentity(revision, ISSUED_AT)

                identity shouldBe OpenVexIdentity(ID, ISSUED_AT, OpenVexDocumentVersion.FIRST)
            }

            test("continues the baseline in the next revision") {
                val baseline = OpenVexBaseline(OpenVexFormatVersion.LATEST, ID, OpenVexDocumentVersion(3))

                val identity = resolveOpenVexIdentity(OpenVexRevision.Next(baseline), ISSUED_AT)

                identity shouldBe OpenVexIdentity(ID, ISSUED_AT, OpenVexDocumentVersion(4))
            }

            test("cuts the clock to whole seconds") {
                val now = Instant.parse("2026-04-25T00:00:00.987654Z")

                val identity = resolveOpenVexIdentity(OpenVexRevision.First(ID), now)

                identity.timestamp shouldBe ISSUED_AT
            }
        }

        context("resolveOpenVexScope") {

            test("resolves the release and the tags the file defines") {
                val result = resolveOpenVexScope("1.0.1", setOf("container"), taggedFile)

                result shouldBe
                    OpenVexScopeResult.Resolved(
                        OpenVexScope(
                            release = OpenVexReleaseScope.Named(release("1.0.1")),
                            tags = setOf(tag("container")),
                        ),
                    )
            }

            test("covers the published releases and every purl without a release or tags") {
                val result = resolveOpenVexScope(null, emptySet(), taggedFile)

                result shouldBe
                    OpenVexScopeResult.Resolved(
                        OpenVexScope(release = OpenVexReleaseScope.Published, tags = emptySet()),
                    )
            }

            test("rejects an unknown release and an unknown tag together") {
                val result = resolveOpenVexScope("9.9.9", setOf("binary"), taggedFile)

                result.shouldBeInstanceOf<OpenVexScopeResult.Rejected>().problems shouldContainExactly
                    listOf(
                        FilterProblem.UnknownRelease(release("9.9.9"), listOf(release("1.0.0"), release("1.0.1"))),
                        FilterProblem.UnknownTags(listOf(tag("binary")), listOf(tag("container"))),
                    )
            }
        }
    })
