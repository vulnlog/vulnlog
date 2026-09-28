// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.openvex

import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class OpenVexContextTest :
    FunSpec({

        context("openVexContext") {

            test("is the OpenVEX namespace of the version") {
                openVexContext(OpenVexFormatVersion.VERSION_0_2_0) shouldBe "https://openvex.dev/ns/v0.2.0"
            }

            test("round trips through the version it declares") {
                val contexts = OpenVexFormatVersion.entries.map(::openVexContext)

                val declared = contexts.map(::declaredOpenVexVersion)

                declared shouldBe OpenVexFormatVersion.entries.map(OpenVexFormatVersion::version)
            }
        }

        context("declaredOpenVexVersion") {

            test("returns a version this build does not know as it stands") {
                declaredOpenVexVersion("https://openvex.dev/ns/v9.9.9") shouldBe "9.9.9"
            }

            test("returns null for a context of another format") {
                declaredOpenVexVersion("https://cyclonedx.org/schema").shouldBeNull()
            }

            test("returns null for an OpenVEX context with an empty version") {
                declaredOpenVexVersion("https://openvex.dev/ns/v").shouldBeNull()
            }

            test("reads the OpenVEX context without a version as 0.0.1, as the specification defines") {
                declaredOpenVexVersion("https://openvex.dev/ns") shouldBe "0.0.1"
            }
        }
    })
