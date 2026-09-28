// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentId
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocumentVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexIdentity
import dev.vulnlog.lib.model.vex.openvex.OpenVexRevision
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val UPDATED_AT = Instant.parse("2026-05-02T00:00:00Z")

class OpenVexIdentityTest :
    FunSpec({

        context("freshOpenVexIdentity") {

            test("is the first version, issued now") {
                val id = OpenVexDocumentId("https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79")

                val identity = freshOpenVexIdentity(id, ISSUED_AT)

                identity.id shouldBe id
                identity.timestamp shouldBe ISSUED_AT
                identity.version shouldBe OpenVexDocumentVersion.FIRST
            }
        }

        context("nextOpenVexIdentity") {

            test("keeps the baseline identifier, counts the version up, and issues the revision now") {
                val baseline =
                    OpenVexBaseline(
                        formatVersion = OpenVexFormatVersion.LATEST,
                        id = OpenVexDocumentId("https://vulnlog.dev/vex/abc"),
                        version = OpenVexDocumentVersion(3),
                    )

                val identity = nextOpenVexIdentity(baseline, UPDATED_AT)

                identity.id shouldBe OpenVexDocumentId("https://vulnlog.dev/vex/abc")
                identity.timestamp shouldBe UPDATED_AT
                identity.version shouldBe OpenVexDocumentVersion(4)
            }
        }

        context("resolveOpenVexIdentity") {

            test("issues the first revision under its id") {
                val id = OpenVexDocumentId("https://vulnlog.dev/vex/abc")

                val identity = resolveOpenVexIdentity(OpenVexRevision.First(id), ISSUED_AT)

                identity shouldBe OpenVexIdentity(id, ISSUED_AT, OpenVexDocumentVersion.FIRST)
            }

            test("continues the baseline in the next revision") {
                val id = OpenVexDocumentId("https://vulnlog.dev/vex/abc")
                val baseline =
                    OpenVexBaseline(OpenVexFormatVersion.LATEST, id, OpenVexDocumentVersion(3))

                val identity = resolveOpenVexIdentity(OpenVexRevision.Next(baseline), UPDATED_AT)

                identity shouldBe OpenVexIdentity(id, UPDATED_AT, OpenVexDocumentVersion(4))
            }

            test("cuts the clock to whole seconds") {
                val first = OpenVexRevision.First(OpenVexDocumentId("https://vulnlog.dev/vex/abc"))

                val identity = resolveOpenVexIdentity(first, Instant.parse("2026-04-25T00:00:00.987654Z"))

                identity.timestamp shouldBe ISSUED_AT
            }
        }
    })
