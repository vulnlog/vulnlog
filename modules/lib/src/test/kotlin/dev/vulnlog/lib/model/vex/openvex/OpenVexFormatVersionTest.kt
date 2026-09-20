// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** Orders the entries by what the versions mean, so the assertions do not lean on the declaration order. */
private val BY_VERSION =
    compareBy<OpenVexFormatVersion>(
        { it.version.substringBefore(".").toInt() },
        {
            it.version
                .substringAfter(".")
                .substringBefore(".")
                .toInt()
        },
        { it.version.substringAfterLast(".").toInt() },
    )

class OpenVexFormatVersionTest :
    FunSpec({

        context("context") {

            test("is the OpenVEX namespace of the version") {
                val formatVersion = OpenVexFormatVersion.VERSION_0_2_0

                val context = formatVersion.context

                context shouldBe "https://openvex.dev/ns/v0.2.0"
            }

            test("round trips through the version it declares") {
                val contexts = OpenVexFormatVersion.entries.map(OpenVexFormatVersion::context)

                val declared = contexts.map(OpenVexFormatVersion.Companion::declaredVersion)

                declared shouldBe OpenVexFormatVersion.entries.map(OpenVexFormatVersion::version)
            }
        }

        context("LATEST") {

            test("is the newest version this build knows, whatever the declaration order") {
                val entries = OpenVexFormatVersion.entries

                val latest = OpenVexFormatVersion.LATEST

                latest shouldBe entries.maxWith(BY_VERSION)
            }
        }

        context("declaredVersion") {

            test("returns a version this build does not know as it stands") {
                val context = "https://openvex.dev/ns/v9.9.9"

                val declared = OpenVexFormatVersion.declaredVersion(context)

                declared shouldBe "9.9.9"
            }

            test("returns null for a context of another format") {
                val context = "https://cyclonedx.org/schema"

                val declared = OpenVexFormatVersion.declaredVersion(context)

                declared.shouldBeNull()
            }

            test("returns null for an OpenVEX context that names no version") {
                val context = "https://openvex.dev/ns/v"

                val declared = OpenVexFormatVersion.declaredVersion(context)

                declared.shouldBeNull()
            }
        }
    })
