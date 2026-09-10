// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.vex.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import java.time.Instant

private val ISSUED_AT = Instant.parse("2026-04-25T00:00:00Z")
private val UPDATED_AT = Instant.parse("2026-05-02T00:00:00Z")

class OpenVexIdentityTest :
    FunSpec({

        context("freshOpenVexIdentity") {

            test("is the first version, issued now") {
                val id = "https://vulnlog.dev/vex/3e671687-395b-41f5-a30f-a58921a69b79"

                val identity = freshOpenVexIdentity(id, ISSUED_AT)

                identity.id shouldBe id
                identity.timestamp shouldBe ISSUED_AT
                identity.version shouldBe 1
            }
        }

        context("nextOpenVexIdentity") {

            test("keeps the baseline identifier, counts the version up, and issues the revision now") {
                val baseline = OpenVexBaseline(id = "https://vulnlog.dev/vex/abc", version = 3, content = "")

                val identity = nextOpenVexIdentity(baseline, UPDATED_AT)

                identity.id shouldBe "https://vulnlog.dev/vex/abc"
                identity.timestamp shouldBe UPDATED_AT
                identity.version shouldBe 4
            }
        }

        context("newOpenVexDocumentId") {

            test("mints an identifier under the Vulnlog namespace") {
                val id = newOpenVexDocumentId()

                id shouldStartWith "https://vulnlog.dev/vex/"
                id shouldNotBe newOpenVexDocumentId()
            }
        }
    })
