// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex.openvex

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.lang.module.ModuleDescriptor

class OpenVexIdentityTest :
    FunSpec({

        context("OpenVexFormatVersion") {

            test("LATEST is the newest version, so a new entry must be declared last") {
                val newest = OpenVexFormatVersion.entries.maxBy { ModuleDescriptor.Version.parse(it.version) }

                OpenVexFormatVersion.LATEST shouldBe newest
            }
        }

        context("OpenVexDocumentId") {

            test("parses an absolute IRI") {
                val values = listOf("https://vulnlog.dev/vex/abc", "urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79")

                val ids = values.map(OpenVexDocumentId::parse)

                ids.map { it?.value } shouldBe values
            }

            test("parses nothing that is no absolute IRI") {
                val values =
                    listOf("", " ", "vex-1", "/vex/abc", " https://vulnlog.dev/vex/abc", "https://vulnlog.dev/a b")

                val ids = values.map(OpenVexDocumentId::parse)

                ids.forEach { it.shouldBeNull() }
            }

            test("cannot be built from a value that is no absolute IRI") {
                shouldThrow<IllegalArgumentException> { OpenVexDocumentId("vex-1") }
            }
        }

        context("OpenVexDocumentVersion") {

            test("the next version is one more") {
                val version = OpenVexDocumentVersion(3)

                val next = version.next()

                next shouldBe OpenVexDocumentVersion(4)
            }

            test("parses a version that has a successor") {
                val values = listOf(1L, Int.MAX_VALUE - 1L)

                val versions = values.map(OpenVexDocumentVersion::parse)

                versions.map { it?.value } shouldBe listOf(1, Int.MAX_VALUE - 1)
            }

            test("parses no version below 1 or without a successor") {
                val values = listOf(0L, -1L, Int.MAX_VALUE.toLong(), Long.MAX_VALUE)

                val versions = values.map(OpenVexDocumentVersion::parse)

                versions.forEach { it.shouldBeNull() }
            }

            test("cannot be built below 1") {
                shouldThrow<IllegalArgumentException> { OpenVexDocumentVersion(0) }
            }
        }
    })
